package gregtech.api.metatileentity.implementations;

import net.minecraft.tileentity.TileEntity;

public class GT_MetaPipeEntity_CableCash {
	public TileEntity tTileEntity;
	public byte tSide;
	public long voltage;
	public long aAmperage;
	
	public GT_MetaPipeEntity_CableCash(TileEntity tTileEntity, byte tSide, long voltage, long aAmperage) {
		this.tTileEntity = tTileEntity;
		this.tSide = tSide;
		this.voltage = voltage;
		this.aAmperage = aAmperage;
	}
	
	public GT_MetaPipeEntity_CableCash() {
		
	}
}