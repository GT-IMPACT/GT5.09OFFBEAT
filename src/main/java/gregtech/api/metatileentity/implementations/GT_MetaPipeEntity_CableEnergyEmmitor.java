package gregtech.api.metatileentity.implementations;

import ic2.api.energy.tile.IEnergySource;

public class GT_MetaPipeEntity_CableEnergyEmmitor {
	public IEnergySource energySource;
	public byte aSide;
	
	public GT_MetaPipeEntity_CableEnergyEmmitor(IEnergySource energySource, byte aSide) {
		this.energySource = energySource;
		this.aSide = aSide;
	}
}