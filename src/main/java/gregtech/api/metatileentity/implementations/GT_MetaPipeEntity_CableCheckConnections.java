package gregtech.api.metatileentity.implementations;

import gregtech.api.metatileentity.MetaPipeEntity;

public class GT_MetaPipeEntity_CableCheckConnections extends Thread {
	private MetaPipeEntity cable;
	byte aSide;
	
	public GT_MetaPipeEntity_CableCheckConnections(MetaPipeEntity cable, byte aSide) {
		this.cable = cable;
		this.aSide = aSide;
		start();
	}
	
	public void run() {
		if ((!cable.getGT6StyleConnection() || cable.isConnectedAtSide(aSide)) && cable.connect(aSide) == 0) {
			cable.disconnect(aSide);
        }
	}
}
