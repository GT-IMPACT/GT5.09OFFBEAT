package gregtech.api.net;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import gregtech.api.gui.GT_Container_Fluid_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Output;
import gregtech.api.util.GT_Utility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import static gregtech.api.util.GT_Utility.trans;

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
	public void process(IBlockAccess w) {
		World world = DimensionManager.getWorld(dim);
		if (world != null) {
			EntityPlayerMP playerMP = (EntityPlayerMP) world.getEntityByID(playerID);
			if (playerMP.openContainer instanceof GT_Container_Fluid_Hatch) {
				GT_Container_Fluid_Hatch cont = (GT_Container_Fluid_Hatch) playerMP.openContainer;
				if (cont.mTileEntity != null && cont.mTileEntity.getMetaTileEntity() instanceof GT_MetaTileEntity_Hatch_Output) {
					GT_MetaTileEntity_Hatch_Output hatch_output = (GT_MetaTileEntity_Hatch_Output) cont.mTileEntity.getMetaTileEntity();
					hatch_output.mMode = 8;
					hatch_output.setLockedFluid(fluid);
					hatch_output.doDisplayThings();
					GT_Utility.sendChatToPlayer(playerMP, String.format(trans("151.4", "Sucessfully locked Fluid to %s"), fluid.getLocalizedName()));
				}
			}
		}
	}
	
	@Override
	public byte getPacketID() {
		return 9;
	}
}
