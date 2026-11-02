package gregtech.common.covers;

import gregtech.api.enums.GT_Values;
import gregtech.api.gui.GT_GUICover;
import gregtech.api.gui.widgets.GT_GuiIcon;
import gregtech.api.gui.widgets.GT_GuiIconButton;
import gregtech.api.gui.widgets.GT_GuiIconCheckButton;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.net.GT_Packet_TileEntityCover;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_Utility;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;

public class GT_Cover_DoesWork extends GT_CoverBehavior {

    public int doCoverThings(
        byte aSide,
        byte aInputRedstone,
        int aCoverID,
        int aCoverVariable,
        ICoverable aTileEntity,
        long aTimer
    ) {
        int signal = 0;
        if (aTileEntity instanceof IMachineProgress) {
            IMachineProgress machine = (IMachineProgress) aTileEntity;
            boolean inverted = (aCoverVariable & 1) != 0;
            if ((aCoverVariable & 4) != 0) {
                signal = inverted == machine.isAllowedToWork() ? 0 : 15;
            } else if ((aCoverVariable & 2) != 0) {
                signal = inverted == (machine.getMaxProgress() == 0) ? 0 : 15;
            } else {
                int scale = machine.getMaxProgress() / 15;
                if (scale > 0 && machine.hasThingsToDo()) {
                    signal = inverted ? 15 - machine.getProgress() / scale : machine.getProgress() / scale;
                } else {
                    signal = inverted ? 15 : 0;
                }
            }
        }
        aTileEntity.setOutputRedstoneSignal(aSide, (byte) signal);
        return aCoverVariable;
    }

    public int onCoverScrewdriverclick(
        byte aSide,
        int aCoverID,
        int aCoverVariable,
        ICoverable aTileEntity,
        EntityPlayer aPlayer,
        float aX,
        float aY,
        float aZ
    ) {
        aCoverVariable = Math.floorMod(aCoverVariable + (aPlayer.isSneaking() ? -1 : 1), 6);
        switch (aCoverVariable) {
            case 0:
                GT_Utility.sendChatToPlayer(aPlayer, trans("018", "Normal"));
                break;
            case 1:
                GT_Utility.sendChatToPlayer(aPlayer, trans("019", "Inverted"));
                break;
            case 2:
                GT_Utility.sendChatToPlayer(aPlayer, trans("020", "Ready to work"));
                break;
            case 3:
                GT_Utility.sendChatToPlayer(aPlayer, trans("021", "Not ready to work"));
                break;
            case 4:
                GT_Utility.sendChatToPlayer(aPlayer, StatCollector.translateToLocal("gt.interact.desc.mach_on"));
                break;
            case 5:
                GT_Utility.sendChatToPlayer(aPlayer, StatCollector.translateToLocal("gt.interact.desc.mach_off"));
                break;
        }
        return aCoverVariable;
    }

    public boolean letsEnergyIn(byte aSide, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        return true;
    }

    public boolean letsEnergyOut(byte aSide, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        return true;
    }

    public boolean letsFluidIn(byte aSide, int aCoverID, int aCoverVariable, Fluid aFluid, ICoverable aTileEntity) {
        return true;
    }

    public boolean letsFluidOut(byte aSide, int aCoverID, int aCoverVariable, Fluid aFluid, ICoverable aTileEntity) {
        return true;
    }

    public boolean letsItemsIn(byte aSide, int aCoverID, int aCoverVariable, int aSlot, ICoverable aTileEntity) {
        return true;
    }

    public boolean letsItemsOut(byte aSide, int aCoverID, int aCoverVariable, int aSlot, ICoverable aTileEntity) {
        return true;
    }

    public boolean manipulatesSidedRedstoneOutput(byte aSide, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        return true;
    }

    public int getTickRate(byte aSide, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        return 5;
    }

    /**
     * GUI Stuff
     */

    @Override
    public boolean hasCoverGUI() {
        return true;
    }

    @Override
    public Object getClientGUI(byte aSide, int aCoverID, int coverData, ICoverable aTileEntity)  {
        return new GT_Cover_DoesWork.GUI(aSide, aCoverID, coverData, aTileEntity);
    }

    private class GUI extends GT_GUICover {
        private final byte side;
        private final int coverID;
        private int coverVariable;

        private final static int startX = 10;
        private final static int startY = 25;
        private final static int spaceX = 18;
        private final static int spaceY = 18;

        public GUI(byte aSide, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
            super(aTileEntity, 176, 107, GT_Utility.intToStack(aCoverID));
            this.side = aSide;
            this.coverID = aCoverID;
            this.coverVariable = aCoverVariable;

            GuiButton b;
            b = new GT_GuiIconButton(this, 0, startX + spaceX*0, startY+spaceY*0, GT_GuiIcon.PROGRESS);
            b = new GT_GuiIconButton(this, 1, startX + spaceX*1, startY+spaceY*0, GT_GuiIcon.CHECKMARK);
            b = new GT_GuiIconCheckButton(this, 2, startX + spaceX*0, startY+spaceY*1, GT_GuiIcon.REDSTONE_ON, GT_GuiIcon.REDSTONE_OFF);
        }

        @Override
        public void drawExtras(int mouseX, int mouseY, float parTicks) {
            super.drawExtras(mouseX, mouseY, parTicks);
            String s1, s2;
            if ((coverVariable & 0x2) > 0)
                s1 = trans("242", "Machine idle");
            else
                s1 = trans("241", "Recipe progress");
            if ((coverVariable & 0x1) > 0)
                s2 = trans("INVERTED","Inverted");
            else
                s2 = trans("NORMAL","Normal");
            this.fontRendererObj.drawString(s1,  startX + spaceX*3, 4+startY+spaceY*0, 0xFF555555);
            this.fontRendererObj.drawString(s2,  startX + spaceX*3, 4+startY+spaceY*1, 0xFF555555);
        }

        @Override
        protected void onInitGui(int guiLeft, int guiTop, int gui_width, int gui_height) {
            updateButtons();
        }

        public void buttonClicked(GuiButton btn){
            if (getClickable(btn.id)){
                boolean state = false;
                if (btn.id == 2)
                    state = ((GT_GuiIconCheckButton) btn).isChecked();

                coverVariable = getNewCoverVariable(btn.id, state);
                GT_Values.NW.sendToServer(new GT_Packet_TileEntityCover(side, coverID, coverVariable, tile));
            }
            updateButtons();
        }

        private void updateButtons(){
            GuiButton b;
            for (Object o : buttonList) {
                b = (GuiButton) o;
                if(b.id == 2){
                    ((GT_GuiIconCheckButton) b).setChecked((coverVariable & 0x1) > 0);
                } else {
                    b.enabled = getClickable(b.id);
                }
            }
        }

        private int getNewCoverVariable(int id, boolean buttonState) {
            switch (id) {
                case 0:
                    return coverVariable & ~0x2;
                case 1:
                    return coverVariable | 0x2;
                case 2:
                    if (buttonState)
                        return coverVariable & ~0x1;
                    return coverVariable | 0x1;
            }
            return coverVariable;
        }

        private boolean getClickable(int id) {
            switch (id) {
                case 0:
                    return (coverVariable & 0x2) > 0;
                case 1:
                    return (coverVariable & 0x2) == 0;
                case 2:
                    return true;
            }
            return false;
        }
    }
}
