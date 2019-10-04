package gregtech.api.metatileentity.implementations;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import gregtech.GT_Mod;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.metatileentity.IMetaTileEntityCable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.util.GT_Utility;
import net.minecraft.tileentity.TileEntity;

public class GT_MetaPipeEntity_CableTransferElectricity implements Callable<Long> {

	private GT_MetaPipeEntity_Cable cable;
	private byte aSide;
	private long aVoltage;
	private long aAmperage;
	private HashSet<TileEntity> aAlreadyPassedSet;

	public GT_MetaPipeEntity_CableTransferElectricity(GT_MetaPipeEntity_Cable cable, byte aSide, long aVoltage,
			long aAmperage, HashSet<TileEntity> aAlreadyPassedSet) {
		this.cable = cable;
		this.aSide = aSide;
		this.aVoltage = aVoltage;
		this.aAmperage = aAmperage;
		this.aAlreadyPassedSet = aAlreadyPassedSet;
	}

	private IMetaTileEntityCable tmpCable;

	@Override
	public Long call() throws Exception {
		if (!cable.isConnectedAtSide(aSide) && aSide != 6)
			return 0L;

		long rUsedAmperes = 0;
		final IGregTechTileEntity baseMetaTile = cable.getBaseMetaTileEntity();

		byte i = (byte) ((((aSide / 2) * 2) + 2) % 6); // this bit of trickery makes sure a direction goes to the next
														// cardinal pair. IE, NS goes to E, EW goes to U, UD goes to N.
														// It's a lame way to make sure locally connected machines on a
														// wire get EU first.

		aVoltage -= cable.mCableLossPerMeter;
		if (aVoltage > 0)
			for (byte j = 0; j < 6 && aAmperage > rUsedAmperes; j++, i = (byte) ((i + 1) % 6)) {
				if (i != aSide && cable.isConnectedAtSide(i) && baseMetaTile.getCoverBehaviorAtSide(i).letsEnergyOut(i,
						baseMetaTile.getCoverIDAtSide(i), baseMetaTile.getCoverDataAtSide(i), baseMetaTile)) {
					final TileEntity tTileEntity = baseMetaTile.getTileEntityAtSide(i);

					if (tTileEntity != null && aAlreadyPassedSet.add(tTileEntity)) {
						final byte tSide = GT_Utility.getOppositeSide(i);
						final IGregTechTileEntity tBaseMetaTile = tTileEntity instanceof IGregTechTileEntity
								? ((IGregTechTileEntity) tTileEntity)
								: null;
						final IMetaTileEntity tMeta = tBaseMetaTile != null ? tBaseMetaTile.getMetaTileEntity() : null;

						if (tMeta instanceof IMetaTileEntityCable) {
							if (tBaseMetaTile.getCoverBehaviorAtSide(tSide).letsEnergyIn(tSide,
									tBaseMetaTile.getCoverIDAtSide(tSide), tBaseMetaTile.getCoverDataAtSide(tSide),
									tBaseMetaTile) && ((IGregTechTileEntity) tTileEntity).getTimer() > 50) {

								tmpCable = ((IMetaTileEntityCable) ((IGregTechTileEntity) tTileEntity)
										.getMetaTileEntity());
								
								ExecutorService es = Executors.newCachedThreadPool();
								
								Future<Long> transferResult = es.submit(new GT_MetaPipeEntity_CableTransferElectricity((GT_MetaPipeEntity_Cable) tmpCable, tSide, aVoltage, 
																						aAmperage - rUsedAmperes, aAlreadyPassedSet));
								
								rUsedAmperes += transferResult.get();
								
								es.shutdown();
								try {
									boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								/*rUsedAmperes += tmpCable.transferElectricity(tSide, aVoltage, aAmperage - rUsedAmperes,
										aAlreadyPassedSet);*/
							}
						} else {
							rUsedAmperes += cable.insertEnergyInto(tTileEntity, tSide, aVoltage, aAmperage - rUsedAmperes);
						}

					}
				}
			}
		cable.mTransferredAmperage += rUsedAmperes;
		cable.mTransferredVoltageLast20 = Math.max(cable.mTransferredVoltageLast20, aVoltage);
		cable.mTransferredAmperageLast20 = Math.max(cable.mTransferredAmperageLast20, cable.mTransferredAmperage);
		if (aVoltage > cable.mVoltage || cable.mTransferredAmperage > cable.mAmperage) {
			if (cable.mOverheat > GT_Mod.gregtechproxy.mWireHeatingTicks * 100) {
				cable.getBaseMetaTileEntity().setToFire();
			} else {
				cable.mOverheat += 100;
			}
			return aAmperage;
		}
		return rUsedAmperes;
	}

}