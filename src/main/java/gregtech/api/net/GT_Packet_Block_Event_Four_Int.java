package gregtech.api.net;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import scala.tools.cmd.Meta;


public class GT_Packet_Block_Event_Four_Int extends GT_Packet {
    private int mX, mZ;
    private short mY;
    private int mID;
    private int b1, b2, b3, b4;

    public GT_Packet_Block_Event_Four_Int() {
        super(true);
    }

    public GT_Packet_Block_Event_Four_Int(int aX, short aY, int aZ, int aID, int aB1, int aB2, int aB3, int aB4) {
        super(false);
        mX = aX;
        mY = aY;
        mZ = aZ;
        mID = aID;
        b1 = aB1;
        b2 = aB2;
        b3 = aB3;
        b4 = aB4;
    }

    @Override
    public byte[] encode() {
        ByteArrayDataOutput tOut = ByteStreams.newDataOutput(32);
        tOut.writeInt(mX);
        tOut.writeShort(mY);
        tOut.writeInt(mZ);
        tOut.writeInt(mID);
        tOut.writeInt(b1);
        tOut.writeInt(b2);
        tOut.writeInt(b3);
        tOut.writeInt(b4);
        return tOut.toByteArray();
    }

    @Override
    public GT_Packet decode(ByteArrayDataInput aData) {
        return new GT_Packet_Block_Event_Four_Int(aData.readInt(), aData.readShort(), aData.readInt(), aData.readInt(),
                aData.readInt(), aData.readInt(), aData.readInt(), aData.readInt());
    }

    @Override
    public void process(IBlockAccess aWorld) {
        if (aWorld != null) {
            TileEntity tTileEntity = aWorld.getTileEntity(mX, mY, mZ);
            if (tTileEntity != null) {
                if (tTileEntity instanceof IGregTechTileEntity) {
                    MetaTileEntity mte = (MetaTileEntity) ((IGregTechTileEntity)tTileEntity).getMetaTileEntity();
                   if (mte != null)
                    mte.receiveClientEvent(b1, b2, b3, b4);
                }
            }
        }
    }

    @Override
    public byte getPacketID() {
        return 8;
    }
}