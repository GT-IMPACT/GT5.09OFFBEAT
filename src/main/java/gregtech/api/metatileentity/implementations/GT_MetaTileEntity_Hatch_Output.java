package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import java.lang.ref.WeakReference;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.OutputHatchType;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IFluidLockableMui2;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.hatch.MTEHatchOutputGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidContainerItem;

public class GT_MetaTileEntity_Hatch_Output extends GT_MetaTileEntity_Hatch implements IFluidLockableMui2 {
	
	public byte mMode = 0;
	public FluidStack lockedFluid = null;
    private WeakReference<EntityPlayer> playerThatLockedfluid;
	
	public GT_MetaTileEntity_Hatch_Output(int aID, String aName, String aNameRegional, int aTier) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            4,
            new String[]{
                "Fluid Output for Multiblocks",
                "Capacity: " + GT_Utility.formatNumbers((2L << aTier + 2) * 1000L) + "L",
                "Right click with screwdriver to restrict output",
                "Can be restricted to put out Items and/or Steam/No Steam/1 specific Fluid",
                "Restricted Output Hatches are given priority for Multiblock Fluid output"}
        );
	}
	
	public GT_MetaTileEntity_Hatch_Output(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
		super(aName, aTier, 4, aDescription, aTextures);
	}
	
	public GT_MetaTileEntity_Hatch_Output(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
		super(aName, aTier, 4, aDescription, aTextures);
	}

    public byte getMode() {
        return mMode;
    }

    public void setMode(byte mode) {
        mMode = mode;
        lockFluid(isFluidLocked());
    }
	
	@Override
	public ITexture[] getTexturesActive(ITexture aBaseTexture) {
		return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT)};
	}
	
	@Override
	public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
		return new ITexture[]{aBaseTexture, TextureFactory.of(Textures.BlockIcons.OVERLAY_PIPE_OUT)};
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
	public boolean isValidSlot(int aIndex) {
		return aIndex != getStackDisplaySlot() && aIndex != getLockedDisplaySlot();
	}
	
	@Override
	public boolean isAccessAllowed(EntityPlayer aPlayer) {
		return true;
	}
	
	@Override
	public boolean isLiquidInput(byte aSide) {
		return false;
	}
	
	@Override
	public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
		return new GT_MetaTileEntity_Hatch_Output(mName, mTier, mDescriptionArray, mTextures);
	}
	
	@Override
	public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
		if (aBaseMetaTileEntity.isClientSide()) return true;
		aBaseMetaTileEntity.openGUI(aPlayer);
		return true;
	}
	
    @Override
    public boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new MTEHatchOutputGui(this).build(data, syncManager, settings);
    }
	
	@Override
	public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
		super.onPostTick(aBaseMetaTileEntity, aTick);
		if (aBaseMetaTileEntity.isServerSide()) {
			if (aBaseMetaTileEntity.isAllowedToWork() && (aTick & 0x7) == 0) {
				IFluidHandler tTileEntity = aBaseMetaTileEntity.getITankContainerAtSide(aBaseMetaTileEntity.getFrontFacing());
				if (tTileEntity != null) {
					for (boolean temp = true; temp && mFluid != null; ) {
						temp = false;
						FluidStack tDrained = aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(aBaseMetaTileEntity.getFrontFacing()), Math.max(1, mFluid.amount), false);
						if (tDrained != null) {
							int tFilledAmount = tTileEntity.fill(ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()), tDrained, false);
							if (tFilledAmount > 0) {
								temp = true;
								tTileEntity.fill(ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()), aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(aBaseMetaTileEntity.getFrontFacing()), tFilledAmount, true), true);
							}
						}
					}
				}
			}
		}
	}
	
    public int getLockedDisplaySlot() {
        return 3;
    }

    public void doDisplayThings() {
        int displaySlot = getLockedDisplaySlot();
        if (displaySlot < 0 || displaySlot >= mInventory.length) {
            return;
        }
        if (lockedFluid == null) {
            if (ItemList.Display_Fluid.isStackEqual(mInventory[displaySlot], true, true)) {
                mInventory[displaySlot] = null;
            }
        } else if (isFluidLocked()) {
            mInventory[displaySlot] = GT_Utility.getFluidDisplayStack(new FluidStack(lockedFluid, 0), true);
        }
    }
	
	@Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setByte("mMode", mMode);
        if (lockedFluid != null) {
            nbt.setTag("lockedFluid", lockedFluid.writeToNBT(new NBTTagCompound()));
        } else {
            nbt.removeTag("lockedFluid");
        }
        nbt.removeTag("lockedFluidName");
    }
	
	@Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        mMode = nbt.getByte("mMode");
        lockedFluid = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("lockedFluid"));
        if (!nbt.hasKey("lockedFluid") && nbt.hasKey("lockedFluidName")) {
            Fluid fluid = FluidRegistry.getFluid(nbt.getString("lockedFluidName"));
            lockedFluid = fluid == null ? null : new FluidStack(fluid, 0);
        }
        playerThatLockedfluid = null;
    }
	
	@Override
	public boolean doesFillContainers() {
		return true;
	}
	
	@Override
	public boolean doesEmptyContainers() {
		return false;
	}
	
	@Override
	public boolean canTankBeFilled() {
		return true;
	}
	
	@Override
	public boolean canTankBeEmptied() {
		return true;
	}
	
	@Override
	public boolean displaysItemStack() {
		return true;
	}
	
	@Override
	public boolean displaysStackSize() {
		return false;
	}
	
	@Override
	public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return aSide == aBaseMetaTileEntity.getFrontFacing() && aIndex == 1;
	}
	
	@Override
	public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return aSide == aBaseMetaTileEntity.getFrontFacing() && aIndex == 0;
	}
	
	@Override
	public int getCapacity() {
		return (2 << mTier + 2) * 1000;
	}
	
    @Override
    public void onScrewdriverRightClick(
        byte side,
        EntityPlayer player,
        float x,
        float y,
        float z
    ) {
        if (!isGuiClickable(side)) {
            return;
        }
        mMode = (byte) ((mMode + (player.isSneaking() ? 9 : 1)) % 10);
        if (mMode < 8) {
            setLockedFluid(null);
            player.addChatMessage(new ChatComponentTranslation(getLangKeyForMode(mMode)));
        } else {
            playerThatLockedfluid = new WeakReference<>(player);
            Object fluidName;
            if (mFluid == null) {
                setLockedFluid(null);
                fluidName = new ChatComponentTranslation("GT5U.chat.hatch.output.in_brackets.none");
            } else {
                setLockedFluid(getDrainableStack().getFluid());
                fluidName = getDrainableStack().getLocalizedName();
            }
            player.addChatMessage(new ChatComponentTranslation(getLangKeyForMode(mMode), fluidName));
        }
    }

    public static String getLangKeyForMode(byte mode) {
        switch (mode) {
            case 0:
                return "GT5U.chat.hatch.output.fluid_steam_item";
            case 1:
                return "GT5U.chat.hatch.output.steam_item";
            case 2:
                return "GT5U.chat.hatch.output.steam_fluid";
            case 3:
                return "GT5U.chat.hatch.output.steam";
            case 4:
                return "GT5U.chat.hatch.output.fluid_item";
            case 5:
                return "GT5U.chat.hatch.output.item";
            case 6:
                return "GT5U.chat.hatch.output.fluid";
            case 7:
                return "GT5U.chat.hatch.output.nothing";
            case 8:
                return "GT5U.chat.hatch.output.items_and_specific_fluid";
            case 9:
                return "GT5U.chat.hatch.output.specific_fluid";
            default:
                return "";
        }
    }

    private boolean isGuiClickable(byte side) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base.getCoverBehaviorAtSide(side).isGUIClickable(
            side,
            base.getCoverIDAtSide(side),
            base.getCoverDataAtSide(side),
            base
        );
    }

    private boolean tryToLockHatch(EntityPlayer player, byte side) {
        if (!isGuiClickable(side) || !isFluidLocked()) {
            return false;
        }
        ItemStack current = player.inventory.getCurrentItem();
        if (current == null) {
            return false;
        }
        FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(current);
        if (fluid == null && current.getItem() instanceof IFluidContainerItem) {
            fluid = ((IFluidContainerItem) current.getItem()).getFluid(current);
        }
        if (fluid == null || !acceptsFluidLock(fluid.getFluid())) {
            return false;
        }
        if (getLockedFluid() != null && !getLockedFluid().equals(fluid.getFluid())) {
            String name = getLockedFluid().getName();
            FluidStack locked = FluidRegistry.getFluidStack(name, 1);
            player.addChatMessage(new ChatComponentTranslation(
                "GT5U.chat.hatch.output.fluid.already_locked", locked == null ? name : locked.getLocalizedName()));
        } else {
            setLockedFluid(fluid.getFluid());
            player.addChatMessage(new ChatComponentTranslation(getLangKeyForMode(mMode), fluid.getLocalizedName()));
        }
        return true;
    }

    @Override
    public boolean onRightclick(
        IGregTechTileEntity base,
        EntityPlayer player,
        byte side,
        float x,
        float y,
        float z
    ) {
        if (tryToLockHatch(player, side)) {
            return true;
        }
        return super.onRightclick(base, player, side, x, y, z);
    }
	
	public String trans(String aKey, String aEnglish) {
		return GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_" + aKey, aEnglish, false);
	}
	
	public boolean outputsSteam() {
		return mMode < 4;
	}
	
	public boolean outputsLiquids() {
		return mMode % 2 == 0 || mMode == 9;
	}
	
	public boolean outputsItems() {
		return mMode % 4 < 2 && mMode != 9;
	}
	
	public boolean isFluidLocked() {
		return mMode == 8 || mMode == 9;
	}

    public boolean isFiltered() {
        return isFluidLocked();
    }

    public boolean isFilteredToFluid(FluidStack stack) {
        return !isFluidLocked() || getLockedFluidName() == null
                || getLockedFluidName().equals(stack.getFluid().getUnlocalizedName());
    }

    public OutputHatchType getHatchType() {
        // Legacy steam, liquid and automatic-lock modes already precede unrestricted hatches.
        return mMode != 0 ? OutputHatchType.StandardFiltered : OutputHatchType.StandardUnfiltered;
    }
	
	public String getLockedFluidName() {
		if (lockedFluid == null) {
			return null;
		}
		return lockedFluid.getUnlocalizedName();
	}
	
	public void setLockedFluidName(String lockedFluidName) {
		setLockedFluid(FluidRegistry.getFluid(lockedFluidName));
	}
	
    @Override
    public void setLockedFluid(Fluid fluid) {
        lockedFluid = fluid == null ? null : new FluidStack(fluid, 0);
        markStorageDirty();
    }

    @Override
    public Fluid getLockedFluid() {
        return lockedFluid == null ? null : lockedFluid.getFluid();
    }

    @Override
    public void lockFluid(boolean lock) {
        if (lock) {
            if (!isFluidLocked()) {
                mMode = 9;
            }
            Fluid fluid = mFluid == null ? null : mFluid.getFluid();
            setLockedFluid(getLockedFluid() == null ? fluid : getLockedFluid());
        } else {
            if (isFluidLocked()) {
                mMode = 0;
            }
            setLockedFluid(null);
        }
    }

    @Override
    public boolean acceptsFluidLock(Fluid fluid) {
        return true;
    }

    public boolean canStoreFluid(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) {
            return false;
        }
        if (mFluid != null && !GT_Utility.areFluidsEqual(mFluid, fluid)) {
            return false;
        }
        if (isFluidLocked()) {
            return getLockedFluid() == null || getLockedFluid().equals(fluid.getFluid());
        }
        return GT_ModHandler.isSteam(fluid) ? outputsSteam() : outputsLiquids();
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack fluid) {
        return canStoreFluid(fluid);
    }
	
	@Override
	public int getTankPressure() {
		return +100;
	}
	
    @Override
    public void onEmptyingContainerWhenEmpty() {
        if (lockedFluid == null && mFluid != null && isFluidLocked()) {
            setLockedFluid(mFluid.getFluid());
            doDisplayThings();
            EntityPlayer player = playerThatLockedfluid == null ? null : playerThatLockedfluid.get();
            if (player != null) {
                player.addChatMessage(new ChatComponentTranslation(
                        "GT5U.chat.hatch.output.fluid.locked_to", mFluid.getLocalizedName()));
                playerThatLockedfluid = null;
            }
        }
    }
}
