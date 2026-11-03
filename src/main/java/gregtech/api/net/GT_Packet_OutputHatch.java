package gregtech.api.net;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class GT_Packet_OutputHatch extends GT_Packet {
	private Fluid fluid;
	private int dim, playerID;
	
	public GT_Packet_OutputHatch() {
		super(true);
	}
	
	public GT_Packet_OutputHatch(int dim, int playerID, Fluid fluid) {
		super(false);
		this.fluid    = fluid;
		this.dim      = dim;
		this.playerID = playerID;
	}
	
	public GT_Packet_OutputHatch(EntityPlayer player, Fluid fluid) {
		super(false);
		this.dim      = player.dimension;
		this.playerID = player.getEntityId();
		this.fluid    = fluid;
	}
	
	@Override
	public byte[] encode() {
		ByteArrayDataOutput tOut = ByteStreams.newDataOutput(32);
		tOut.writeInt(dim);
		tOut.writeInt(playerID);
		tOut.writeInt(fluid.getID());
		return tOut.toByteArray();
	}
	
	@Override
	public GT_Packet decode(ByteArrayDataInput aData) {
		return new GT_Packet_OutputHatch(aData.readInt(), aData.readInt(), FluidRegistry.getFluid(aData.readInt()));
	}
	
    @Override
    public void process(IBlockAccess world) {
        // Keep packet ID 9 reserved; output hatch actions now use MUI2 container synchronization.
    }
	
	@Override
	public byte getPacketID() {
		return 9;
	}
}
