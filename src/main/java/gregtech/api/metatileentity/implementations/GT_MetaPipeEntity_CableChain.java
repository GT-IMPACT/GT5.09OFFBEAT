package gregtech.api.metatileentity.implementations;

import java.util.HashSet;

import codechicken.multipart.scalatraits.TTileChangeTile;
import gregtech.api.interfaces.metatileentity.IMetaTileEntityCable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class GT_MetaPipeEntity_CableChain {
	/// Потребители энергии
	public HashSet<GT_MetaPipeEntity_CableCash> consumers = new HashSet<GT_MetaPipeEntity_CableCash>();
	/// Все провода в цепочке
	public HashSet<IMetaTileEntityCable> cablesChain = new HashSet<IMetaTileEntityCable>();
	
	public boolean isCableInChain(IMetaTileEntityCable cable) {
		return cablesChain.contains(cable);
	}
	
	public void showCablesChainsCoordinates() {
		System.out.println("==========================================================================");
		System.out.println("Cables:");
		System.out.println("==========================================================================");
		for(IMetaTileEntityCable cable : cablesChain) {
			System.out.println("x: " + ((GT_MetaPipeEntity_Cable)cable).getBaseMetaTileEntity().getXCoord());
			System.out.println("y: " + ((GT_MetaPipeEntity_Cable)cable).getBaseMetaTileEntity().getYCoord());
			System.out.println("z: " + ((GT_MetaPipeEntity_Cable)cable).getBaseMetaTileEntity().getZCoord());
			System.out.println("==========================================================================");
		}
	}
	
	public void showConsumersCoordinates() {
		System.out.println("==========================================================================");
		System.out.println("Consumers:");
		System.out.println("==========================================================================");
		for(GT_MetaPipeEntity_CableCash c : consumers) {
			System.out.println(c.tTileEntity.toString());
			System.out.println("x: " + c.tTileEntity.xCoord);
			System.out.println("y: " + c.tTileEntity.yCoord);
			System.out.println("z: " + c.tTileEntity.zCoord);
			System.out.println("==========================================================================");
		}
	}
}