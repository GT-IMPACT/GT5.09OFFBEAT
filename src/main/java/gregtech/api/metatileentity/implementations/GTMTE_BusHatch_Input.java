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
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.item.GhostCircuitItemStackHandler;
import gregtech.common.gui.modularui.hatch.MTEBusHatchInputGui;
import gregtech.common.items.GT_IntegratedCircuit_Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import java.util.ArrayList;

public class GTMTE_BusHatch_Input extends GT_MetaTileEntity_Hatch implements IConfigurationCircuitSupport {
	
	public static boolean mIgnoreMap = false;
	public GT_Recipe.GT_Recipe_Map mRecipeMap = null;
	public boolean disableSort;
    public boolean disableLimited = true;
	public boolean restrictFluid;
	public final int capacity;

    public static ArrayList<ItemStack> getStoredInputs(GTMTE_BusHatch_Input aBus) {
        ArrayList<ItemStack> inputs = new ArrayList<>();
        IGregTechTileEntity tile = aBus.getBaseMetaTileEntity();
        for (int i = tile.getSizeInventory() - 1; i >= 0; i--) {
            if (i != aBus.getCircuitSlot() && (!aBus.isValidSlot(i) || i == aBus.getInputSlot()
                    || i == aBus.getOutputSlot() || i == aBus.getStackDisplaySlot())) {
                continue;
            }
            ItemStack input = tile.getStackInSlot(i);
            if (input != null) {
                inputs.add(input);
            }
        }
        return inputs;
    }
	
    public GTMTE_BusHatch_Input(
        int aID,
        String aName,
        String aNameRegional,
        int aTier,
        int capacity
    ) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            20,
            new String[]{
                "Fluid and Items Input for Multiblocks",
                String.format("Capacity Fluid: %s L", GT_Utility.formatNumbers(capacity)),
                "Capacity Items: 16 stacks"
            }
        );
        this.capacity = capacity;
    }

	public GTMTE_BusHatch_Input(int aID, String aName, String aNameRegional, int aTier) {
        super(
            aID,
            aName,
            aNameRegional,
            aTier,
            20,
            new String[]{
                "Fluid and Items Input for Multiblocks",
                String.format("Capacity Fluid: %s L", GT_Utility.formatNumbers(32000)),
                "Capacity Items: 16 stacks"
            }
        );
		this.capacity = 32000;
	}
	
    public GTMTE_BusHatch_Input(
        String aName,
        int aTier,
        String[] aDescription,
        ITexture[][][] aTextures,
        int capacity
    ) {
        super(aName, aTier, 20, aDescription, aTextures);
        this.capacity = capacity;
    }
	
	@Override
	public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
		return new GTMTE_BusHatch_Input(mName, mTier, mDescriptionArray, mTextures, capacity);
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
        return new MTEBusHatchInputGui(this).build(data, syncManager, settings);
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
    public boolean isValidSlot(int index) {
        return index >= 0 && index < getCircuitSlot() && super.isValidSlot(index);
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
	public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTimer) {
		if (aBaseMetaTileEntity.isServerSide()) {
			if (aBaseMetaTileEntity.hasInventoryBeenModified()) {
				fillStacksIntoFirstSlots();
			}
			
			if (aTimer % 8 == 0) {
				IInventory tTileEntity = aBaseMetaTileEntity.getIInventoryAtSide(aBaseMetaTileEntity.getFrontFacing());
				if (tTileEntity != null && mInventory[getOutputSlot()] != null) {
					GT_Utility.moveOneItemStack(aBaseMetaTileEntity, tTileEntity,
							aBaseMetaTileEntity.getFrontFacing(), aBaseMetaTileEntity.getBackFacing(),
							null, false, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
				}
				
				
			}
		}
	}
	
	protected void fillStacksIntoFirstSlots() {
		if (disableSort) {
			for (int i = 1; i <= 16; i++)
				for (int j = i + 1; j <= 16; j++)
					if (mInventory[j] != null && mInventory[j].stackSize <= 0 && (mInventory[i] == null || GT_Utility.areStacksEqual(mInventory[i], mInventory[j])))
						GT_Utility.moveStackFromSlotAToSlotB(getBaseMetaTileEntity(), getBaseMetaTileEntity(), j, i, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
		} else {
			for (int i = 1; i <= 16; i++)
				for (int j = i + 1; j <= 16; j++)
					if (mInventory[j] != null && (mInventory[i] == null || GT_Utility.areStacksEqual(mInventory[i], mInventory[j])))
						GT_Utility.moveStackFromSlotAToSlotB(getBaseMetaTileEntity(), getBaseMetaTileEntity(), j, i, (byte) 64, (byte) 1, (byte) 64, (byte) 1);
		}
	}
	
	public void updateSlots() {
		if (mInventory[0] != null && mInventory[0].stackSize <= 0) {
			mInventory[0] = null;
		}
		for (int i = 1; i <= 16; i++)
			if (mInventory[i] != null && mInventory[i].stackSize <= 0) mInventory[i] = null;
		fillStacksIntoFirstSlots();
	}
	
	@Override
	public void onScrewdriverRightClick(byte aSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
		if (!getBaseMetaTileEntity().getCoverBehaviorAtSide(aSide).isGUIClickable(aSide, getBaseMetaTileEntity().getCoverIDAtSide(aSide), getBaseMetaTileEntity().getCoverDataAtSide(aSide), getBaseMetaTileEntity()))
			return;
		if (aPlayer.isSneaking()) {
			disableSort = !disableSort;
			GT_Utility.sendChatToPlayer(aPlayer, trans("200", "Sort mode: " + (disableSort ? "Disabled" : "Enabled")));
		}
	}

	@Override
	public boolean onSolderingToolRightClick(byte aSide, byte aWrenchingSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
		restrictFluid = !restrictFluid;
		GT_Utility.sendChatToPlayer(aPlayer, trans("200", "Restrict Fluid: " + (restrictFluid ? "Enabled" : "Disabled")));
		return true;
	}
	
	public String trans(String aKey, String aEnglish) {
		return GT_LanguageManager.addStringLocalization("Interaction_DESCRIPTION_Index_" + aKey, aEnglish, false);
	}
	
    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("disableSort", disableSort);
        aNBT.setBoolean("disableLimited", disableLimited);
        aNBT.setBoolean("restrictFluid", restrictFluid);
    }
	
    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        disableSort = aNBT.getBoolean("disableSort");
        disableLimited = !aNBT.hasKey("disableLimited") || aNBT.getBoolean("disableLimited");
        restrictFluid = aNBT.getBoolean("restrictFluid");
        setInventorySlotContents(getCircuitSlot(), mInventory[getCircuitSlot()]);
    }
	
	@Override
	public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
		return aIndex == 17;
	}
	
    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        boolean isFluid = aStack.getItem() instanceof IFluidContainerItem
                && ((IFluidContainerItem) aStack.getItem()).getCapacity(aStack) > 0
                && ((IFluidContainerItem) aStack.getItem()).getFluid(aStack) != null;
        if (aIndex == 0) {
            return isFluid;
        }
        if (aIndex == 17) {
            return false;
        }
        return (aIndex >= 1 && aIndex <= 16) && (!restrictFluid || !isFluid)
                && (disableLimited || limitedAllowPutStack(aIndex, aStack));
    }

    protected boolean limitedAllowPutStack(int index, ItemStack stack) {
        for (int slot = 1; slot <= 16; slot++) {
            if (GT_Utility.areStacksEqual(GT_OreDictUnificator.get_nocopy(stack), mInventory[slot])) {
                return slot == index;
            }
        }
        return mInventory[index] == null;
    }
	
	@Override
	public int getOutputSlot() {
		return 17;
	}
	
	@Override
	public int getStackDisplaySlot() {
		return 18;
	}
	
	@Override
	public boolean doesFillContainers() {
		return true;
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
	public boolean doesEmptyContainers() {
		return true;
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
	public boolean isFluidInputAllowed(FluidStack aFluid) {
		return true;
	}
	
	@Override
	public int getCapacity() {
		return capacity;
	}
	
	@Override
	public int getTankPressure() {
		return -100;
	}
}
