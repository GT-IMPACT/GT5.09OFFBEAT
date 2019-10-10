package gregtech.api.metatileentity.implementations;

import net.minecraft.tileentity.TileEntity;

public class GT_MetaPipeEntity_CableCash {
	public TileEntity tTileEntity;
	public byte tSide;
	public long voltage;
	public long aAmperage;
	public float distanceFromStart;
	public GT_MetaPipeEntity_Cable cable;
	
	public GT_MetaPipeEntity_CableCash(TileEntity tTileEntity, byte tSide, long voltage, long aAmperage, float distanceFromStart, GT_MetaPipeEntity_Cable cable) {
		this.tTileEntity = tTileEntity;
		this.tSide = tSide;
		this.voltage = voltage;
		this.aAmperage = aAmperage;
		this.distanceFromStart = distanceFromStart;
		this.cable = cable;
	}
	
	public float getDistance() {
		return distanceFromStart;
	}
	
	public GT_MetaPipeEntity_CableCash() {
		
	}
}