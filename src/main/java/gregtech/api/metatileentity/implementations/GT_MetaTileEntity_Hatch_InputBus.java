package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IConfigurationCircuitSupport;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_InventoryUtils;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.gui.modularui.hatch.MTEHatchInputBusGui;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

public class GT_MetaTileEntity_Hatch_InputBus extends GT_MetaTileEntity_Hatch
    implements IConfigurationCircuitSupport {

    public GT_Recipe_Map mRecipeMap = null;
    public boolean disableSort;
    public boolean disableFilter = true;
    public boolean disableLimited = true;

    public static int getSlots(int tier) {
        return (tier + 1) * (tier + 1);
    }

    public GT_MetaTileEntity_Hatch_InputBus(int aID, String aName, String aNameRegional, int aTier) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            getSlots(aTier) + 1,
            new String[]{
                "Item Input for Multiblocks",
                "Screwdriver: toggle input filter",
                "Shift + screwdriver: cycle sorting and insertion limit",
                "Capacity: " + getSlots(aTier) + " stack" + (getSlots(aTier) >= 2 ? "s" : "")
            }
        );
    }

    public GT_MetaTileEntity_Hatch_InputBus(
        int aID,
        String aName,
        String aNameRegional,
        int aTier,
        int aAmountSlots,
        String[] aDesc
    ) {
        super(aID, aName, aNameRegional, aTier, aAmountSlots + 1, aDesc);
    }

    public GT_MetaTileEntity_Hatch_InputBus(
        String aName,
        int aTier,
        int aAmountSlots,
        String[] aDescription,
        ITexture[][][] aTextures
    ) {
        super(aName, aTier, aAmountSlots + 1, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Hatch_InputBus(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, getSlots(aTier) + 1, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Hatch_InputBus(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, getSlots(aTier) + 1, aDescription, aTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN)};
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_IN)};
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return true;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return aIndex >= 0 && aIndex < getCircuitSlot();
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_InputBus(mName, mTier, getCircuitSlot(), mDescriptionArray, mTextures);
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        }
        aBaseMetaTileEntity.openGUI(aPlayer);
        return true;
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEHatchInputBusGui(this).build(data, syncManager, settings);
    }

    @Override
    public boolean allowSelectCircuit() {
        return true;
    }

    @Override
    public int getCircuitSlot() {
        return mInventory.length - 1;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == getCircuitSlot() && stack != null) {
            if (!(stack.getItem() instanceof GT_IntegratedCircuit_Item)
                || stack.getItemDamage() < 0 || stack.getItemDamage() > GhostCircuitItemStackHandler.MAX_CONFIG) {
                stack = null;
            } else {
                stack = stack.copy();
                stack.stackSize = 0;
            }
        }
        super.setInventorySlotContents(index, stack);
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        return index == getCircuitSlot() ? null : super.decrStackSize(index, amount);
    }

    @Override
    public void onPostTick(IGregTechTileEntity base, long timer) {
        if (base.isServerSide()) {
            updateSlots();
        }
    }

    public void updateSlots() {
        for (int index = 0; index < getCircuitSlot(); index++) {
            if (mInventory[index] != null && mInventory[index].stackSize <= 0) {
                mInventory[index] = null;
            }
        }
        if (!disableSort) {
            fillStacksIntoFirstSlots();
        }
    }

    protected void fillStacksIntoFirstSlots() {
        if (GT_InventoryUtils.compactInventory(mInventory, 0, getCircuitSlot())) {
            markDirty();
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("disableSort", disableSort);
        aNBT.setBoolean("disableFilter", disableFilter);
        aNBT.setBoolean("disableLimited", disableLimited);
        if (mRecipeMap != null) {
            aNBT.setString("recipeMap", mRecipeMap.mUnlocalizedName);
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        disableSort = aNBT.getBoolean("disableSort");
        // These controls did not exist in legacy saves of this branch.
        disableFilter = !aNBT.hasKey("disableFilter") || aNBT.getBoolean("disableFilter");
        disableLimited = !aNBT.hasKey("disableLimited") || aNBT.getBoolean("disableLimited");
        mRecipeMap = null;
        for (GT_Recipe_Map map : GT_Recipe_Map.sMappings) {
            if (map.mUnlocalizedName.equals(aNBT.getString("recipeMap"))) {
                mRecipeMap = map;
                break;
            }
        }
        setInventorySlotContents(getCircuitSlot(), mInventory[getCircuitSlot()]);
    }

    @Override
    public void onScrewdriverRightClick(
        byte side,
        EntityPlayer player,
        float x,
        float y,
        float z
    ) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (!base.getCoverBehaviorAtSide(side).isGUIClickable(side, base.getCoverIDAtSide(side),
            base.getCoverDataAtSide(side), base)) {
            return;
        }
        if (player.isSneaking()) {
            if (disableSort) {
                disableSort = false;
            } else if (disableLimited) {
                disableLimited = false;
            } else {
                disableSort = true;
                disableLimited = true;
            }
            GT_Utility.sendChatToPlayer(player,
                StatCollector.translateToLocal("GT5U.hatch.disableSort." + disableSort) + "   "
                    + StatCollector.translateToLocal("GT5U.hatch.disableLimited." + disableLimited));
        } else {
            disableFilter = !disableFilter;
            GT_Utility.sendChatToPlayer(player,
                StatCollector.translateToLocal("GT5U.hatch.disableFilter." + disableFilter));
        }
        markDirty();
    }

    @Override
    public String trans(String key, String english) {
        return GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_" + key, english, false);
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity base, int index, byte side, ItemStack stack) {
        return isValidSlot(index) && side == base.getFrontFacing();
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity base, int index, byte side, ItemStack stack) {
        return isValidSlot(index) && side == base.getFrontFacing()
            && (mRecipeMap == null || disableFilter || mRecipeMap.containsInput(stack))
            && (disableLimited || limitedAllowPutStack(index, stack));
    }

    protected boolean limitedAllowPutStack(int index, ItemStack stack) {
        for (int slot = 0; slot < getSizeInventory(); slot++) {
            if (GT_Utility.areStacksEqual(GT_OreDictUnificator.get_nocopy(stack), mInventory[slot])) {
                return slot == index;
            }
        }
        return mInventory[index] == null;
    }
}
