package gregtech.api.metatileentity.implementations;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.reactor.IReactorChamber;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class GT_MetaPipeEntity_CableEnergyThread extends Thread {

	private IGregTechTileEntity aBaseMetaTileEntity;
	private byte aSide;
	private GT_MetaPipeEntity_Cable cable;

	public GT_MetaPipeEntity_CableEnergyThread(GT_MetaPipeEntity_Cable cable, IGregTechTileEntity aBaseMetaTileEntity,
			byte aSide) {
		this.cable = cable;
		this.aSide = aSide;
		this.aBaseMetaTileEntity = aBaseMetaTileEntity;
		start();
	}

	public void run() {
		if (cable.isConnectedAtSide(aSide)) {
			final TileEntity tTileEntity = aBaseMetaTileEntity.getTileEntityAtSide(aSide);
			final TileEntity tEmitter;
			if (tTileEntity instanceof IReactorChamber)
				tEmitter = (TileEntity) ((IReactorChamber) tTileEntity).getReactor();
			else
				tEmitter = (tTileEntity == null || tTileEntity instanceof IEnergyTile || EnergyNet.instance == null)
						? tTileEntity
						: EnergyNet.instance.getTileEntity(tTileEntity.getWorldObj(), tTileEntity.xCoord,
								tTileEntity.yCoord, tTileEntity.zCoord);

			if (tEmitter instanceof IEnergySource) {
				final GT_CoverBehavior coverBehavior = aBaseMetaTileEntity.getCoverBehaviorAtSide(aSide);
				final int coverId = aBaseMetaTileEntity.getCoverIDAtSide(aSide),
						coverData = aBaseMetaTileEntity.getCoverDataAtSide(aSide);
				final ForgeDirection tDirection = ForgeDirection.getOrientation(GT_Utility.getOppositeSide(aSide));

				if (((IEnergySource) tEmitter).emitsEnergyTo((TileEntity) aBaseMetaTileEntity, tDirection)
						&& coverBehavior.letsEnergyIn(aSide, coverId, coverData, aBaseMetaTileEntity)) {
					final long tEU = (long) ((IEnergySource) tEmitter).getOfferedEnergy();
					
					/*ExecutorService es = Executors.newCachedThreadPool();
					
					Future<Long> transferResult = es.submit(new GT_MetaPipeEntity_CableTransferElectricity(cable, aSide, tEU, 1, Sets.newHashSet((TileEntity) aBaseMetaTileEntity)));
					
					try {
						if(transferResult.get() > 0) {
						//if (cable.transferElectricity(aSide, tEU, 1, Sets.newHashSet((TileEntity) aBaseMetaTileEntity)) > 0)
							((IEnergySource) tEmitter).drawEnergy(tEU);
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					es.shutdown();
					try {
						boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					
					cable.transferElectricity(aSide, tEU, 1, Sets.newHashSet((TileEntity) aBaseMetaTileEntity));
				}
			}
		}
	}
}