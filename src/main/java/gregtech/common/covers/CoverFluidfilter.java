package gregtech.common.covers;

import gregtech.api.covers.CoverContext;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.covers.modes.BlockMode;
import gregtech.common.covers.modes.FilterDirectionMode;
import gregtech.common.covers.modes.FilterType;
import gregtech.common.gui.modularui.cover.CoverFluidfilterGui;
import gregtech.common.gui.modularui.cover.base.CoverBaseGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import static gregtech.api.enums.GT_Values.E;

public class CoverFluidfilter extends CoverLegacyData {

    public CoverFluidfilter(CoverContext context) {
        super(context);
    }

    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    protected CoverBaseGui<?> getCoverGui() {
        return new CoverFluidfilterGui(this);
    }

    public int getFluidId() {
        return getVariable() >>> 3;
    }

    public CoverFluidfilter setFluidId(int fluidId) {
        if (isValid() && (fluidId == -1
                || fluidId >= 0 && fluidId <= 0x1fffffff && FluidRegistry.getFluid(fluidId) != null)) {
            setGuiVariable(getFilterMode() | (fluidId == -1 ? 0 : fluidId << 3));
        }
        return this;
    }

    public int getFilterMode() {
        return getVariable() & 7;
    }

    public CoverFluidfilter setFilterMode(int filterMode) {
        if (isValid() && filterMode >= 0 && filterMode <= 7) {
            setGuiVariable((getVariable() & ~7) | filterMode);
        }
        return this;
    }

    public FilterDirectionMode getFilterDirection() {
        return (getFilterMode() & 4) == 0 ? FilterDirectionMode.INPUT : FilterDirectionMode.OUTPUT;
    }

    public void setFilterDirection(FilterDirectionMode mode) {
        if (mode != null) {
            setFilterMode(mode == FilterDirectionMode.INPUT ? getFilterMode() & 3 : getFilterMode() | 4);
        }
    }

    public FilterType getFilterType() {
        return (getFilterMode() & 1) == 0 ? FilterType.WHITELIST : FilterType.BLACKLIST;
    }

    public void setFilterType(FilterType type) {
        if (type != null) {
            setFilterMode(type == FilterType.WHITELIST ? getFilterMode() & 6 : getFilterMode() | 1);
        }
    }

    public BlockMode getBlockMode() {
        return (getFilterMode() & 2) == 0 ? BlockMode.BLOCK : BlockMode.ALLOW;
    }

    public void setBlockMode(BlockMode mode) {
        if (mode != null) {
            setFilterMode(mode == BlockMode.BLOCK ? getFilterMode() & 5 : getFilterMode() | 2);
        }
    }

    private final int FILTER_INPUT_DENY_OUTPUT = 0;

    private final int INVERT_INPUT_DENY_OUTPUT = 1;

    private final int FILTER_INPUT_ANY_OUTPUT = 2;

    private final int INVERT_INPUT_ANY_OUTPUT = 3;

    private final int DENY_INPUT_FILTER_OUTPUT = 4;

    private final int DENY_INPUT_INVERT_OUTPUT = 5;

    private final int ANY_INPUT_FILTER_OUTPUT = 6;

    private final int ANY_INPUT_INVERT_OUTPUT = 7;

    @Override
    public String getDescription() {
        int aFilterMode = coverData & 7;
        int aFilterFluid = coverData >>> 3;
        final Fluid fluid = FluidRegistry.getFluid(aFilterFluid);
        if (fluid == null) {
            return E;
        }
        final FluidStack sFluid = new FluidStack(fluid, 1000);
        return (String.format("Filtering Fluid: %s  Mode: %s", sFluid.getLocalizedName(), getFilterMode(aFilterMode)));
    }

    @Override
    public void doCoverThings(byte redstone, long timer) {
        return;
    }

    @Override
    public void onCoverScrewdriverClick(EntityPlayer player, float x, float y, float z) {
        int aFilterMode = coverData & 7;
        coverData ^= aFilterMode;
        aFilterMode = (aFilterMode + (player.isSneaking() ? -1 : 1)) % 8;
        if (aFilterMode < 0) {
            aFilterMode = 7;
        }
        GT_Utility.sendChatToPlayer(player, getFilterMode(aFilterMode));
        coverData |= aFilterMode;
        markGuiChanged();
    }

    @Override
    public boolean onCoverRightClick(EntityPlayer player, float x, float y, float z) {
        ICoverable tile = getTile();
        byte side = (byte) coverSide.ordinal();
        if (((x > 0.375) && (x < 0.625)) || ((side > 3) && ((y > 0.375) && (y < 0.625)))
                || ((side < 2) && ((z > 0.375) && (z < 0.625))) || (side == 2) || (side == 3)) {
            ItemStack tStack = player.inventory.getCurrentItem();
            if (tStack == null) {
                return true;
            }
            FluidStack tFluid = FluidContainerRegistry.getFluidForFilledItem(tStack);
            if (tFluid != null) {
                int fluid = tFluid.getFluidID();
                coverData = (coverData & 7) | (fluid << 3);
                markGuiChanged();
                FluidStack sFluid = new FluidStack(FluidRegistry.getFluid(fluid), 1000);
                GT_Utility.sendChatToPlayer(player,
                        GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_047", "Filter Fluid: ")
                                + sFluid.getLocalizedName());
            } else if (tStack.getItem() instanceof IFluidContainerItem) {
                IFluidContainerItem tContainer = (IFluidContainerItem) tStack.getItem();
                if (tContainer.getFluid(tStack) != null) {
                    int fluid = tContainer.getFluid(tStack).getFluidID();
                    coverData = (coverData & 7) | (fluid << 3);
                    markGuiChanged();
                    FluidStack sFluid = new FluidStack(FluidRegistry.getFluid(fluid), 1000);
                    GT_Utility.sendChatToPlayer(player,
                            GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_047",
                                    "Filter Fluid: ") + sFluid.getLocalizedName());
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean letsFluidIn(Fluid fluid) {
        if (fluid == null) {
            return true;
        }
        int aFilterMode = coverData & 7;
        int aFilterFluid = coverData >>> 3;
        if (aFilterMode == DENY_INPUT_FILTER_OUTPUT || aFilterMode == DENY_INPUT_INVERT_OUTPUT) {
            return false;
        } else if (aFilterMode == ANY_INPUT_FILTER_OUTPUT || aFilterMode == ANY_INPUT_INVERT_OUTPUT) {
            return true;
        } else if (fluid.getID() == aFilterFluid) {
            return aFilterMode == FILTER_INPUT_DENY_OUTPUT || aFilterMode == FILTER_INPUT_ANY_OUTPUT;
        } else {
            return aFilterMode == INVERT_INPUT_DENY_OUTPUT || aFilterMode == INVERT_INPUT_ANY_OUTPUT;
        }
    }

    @Override
    public boolean letsFluidOut(Fluid fluid) {
        if (fluid == null) {
            return true;
        }
        int aFilterMode = coverData & 7;
        int aFilterFluid = coverData >>> 3;
        if (aFilterMode == FILTER_INPUT_DENY_OUTPUT || aFilterMode == INVERT_INPUT_DENY_OUTPUT) {
            return false;
        } else if (aFilterMode == FILTER_INPUT_ANY_OUTPUT || aFilterMode == INVERT_INPUT_ANY_OUTPUT) {
            return true;
        } else if (fluid.getID() == aFilterFluid) {
            return aFilterMode == DENY_INPUT_FILTER_OUTPUT || aFilterMode == ANY_INPUT_FILTER_OUTPUT;
        } else {
            return aFilterMode == DENY_INPUT_INVERT_OUTPUT || aFilterMode == ANY_INPUT_INVERT_OUTPUT;
        }
    }

    @Override
    public boolean alwaysLookConnected() {
        return true;
    }

    @Override
    public int getMinimumTickRate() {
        return 0;
    }

    @Override
    protected NBTBase saveDataToNbt() {
        NBTTagCompound state = new NBTTagCompound();
        state.setInteger("legacy", coverData);
        Fluid fluid = FluidRegistry.getFluid(coverData >>> 3);
        if (fluid != null) {
            state.setString("fluid", FluidRegistry.getFluidName(fluid));
        }
        return state;
    }

    @Override
    protected void readDataFromNbt(NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound state = (NBTTagCompound) nbt;
            coverData = state.getInteger("legacy");
            if (state.hasKey("fluid")) {
                coverData = (coverData & 7) | (FluidRegistry.getFluidID(state.getString("fluid")) << 3);
            }
        } else {
            super.readDataFromNbt(nbt);
        }
    }

    @Override
    public boolean hasCoverGUI() {
        return true;
    }

    public String getFilterMode(int aFilterMode) {
        switch (aFilterMode) {
        case FILTER_INPUT_DENY_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_043",
                    "Filter input, Deny output"));

        case INVERT_INPUT_DENY_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_044",
                    "Invert input, Deny output"));

        case FILTER_INPUT_ANY_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_045",
                    "Filter input, Permit any output"));

        case INVERT_INPUT_ANY_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_046",
                    "Invert input, Permit any output"));

        case DENY_INPUT_FILTER_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_219",
                    "Deny input, Filter output"));

        case DENY_INPUT_INVERT_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_220",
                    "Deny input, Invert output"));

        case ANY_INPUT_FILTER_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_221",
                    "Permit any input, Filter output"));

        case ANY_INPUT_INVERT_OUTPUT:
            return (GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_222",
                    "Permit any input, Invert output"));

        default:
            return ("UNKNOWN");

        }
    }
}
