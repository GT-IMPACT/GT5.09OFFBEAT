package gregtech.common.tileentities.machines.multi;

import com.impact.mods.gregtech.tileentities.hatches.lasers.GTMTE_LaserEnergy_In;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.ItemList;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.*;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.StructureRequirements;
import ic2.core.Ic2Items;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import space.impact.api.alignment.constructable.ISurvivalConstructable;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.IStructureDefinition;
import space.impact.api.structure.ISurvivalBuildEnvironment;
import space.impact.api.structure.StructureDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static gregtech.GT_Mod.GT_FML_LOGGER;
import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.api.structure.StructureUtility.ofBlockAnyMeta;
import static space.impact.api.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_AssemblyLine
        extends GT_MetaTileEntity_EnhancedMultiBlockBase<GT_MetaTileEntity_AssemblyLine>
        implements ISurvivalConstructable {
    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 16;
    private static final HintColor BOTTOM_HINT = HintColor.BLUE;
    private static final HintColor ENERGY_HINT = HintColor.YELLOW;
    private static final HintColor INPUT_HINT = HintColor.ORANGE;
    private static final HintColor OUTPUT_HINT = HintColor.GREEN;
    private static final HintColor DATA_HINT = HintColor.PURPLE;
    private static final StructureRequirements BOTTOM = StructureRequirements.builder(Casings.SOLID_STEEL, 0)
            .atLeast(HatchElement.Maintenance)
            .build();
    private static final StructureRequirements ENERGY = StructureRequirements.builder(Casings.SOLID_STEEL, 0)
            .atLeast(LineHatch.ENERGY)
            .build();
    private static final StructureRequirements OUTPUT = StructureRequirements.builder(Casings.SOLID_STEEL, 0)
            .atLeast(LineHatch.OUTPUT_BUS)
            .build();
    private static final StructureRequirements[] INPUTS = createInputRequirements();
    private static IStructureDefinition<GT_MetaTileEntity_AssemblyLine> structureDefinition;
    private int buildingLength;

    private static StructureRequirements[] createInputRequirements() {
        StructureRequirements[] inputs = new StructureRequirements[MAX_LENGTH - 1];
        for (int slice = 0; slice < inputs.length; slice++) {
            inputs[slice] = StructureRequirements.builder(Casings.SOLID_STEEL, 0)
                    .atLeast(new SliceInput(slice))
                    .build();
        }
        return inputs;
    }

    public ArrayList<GT_MetaTileEntity_Hatch_DataAccess> mDataAccessHatches = new ArrayList<GT_MetaTileEntity_Hatch_DataAccess>();

    public GT_MetaTileEntity_AssemblyLine(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_AssemblyLine(String aName) {
        super(aName);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_AssemblyLine(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .addMachineType("Assembly Line")
                .addInfo("Data stick: controller or Data Access Hatch")
                .addInfo("Inputs follow slice order; fluids: front before back")
                .beginVariableStructureBlock(MIN_LENGTH, MAX_LENGTH, 4, 4, 3, 3, false)
                .addController("Slice 1, front, third layer from bottom")
                .addRequirements(BOTTOM, "Bottom sides", BOTTOM_HINT)
                .addRequirements(ENERGY, "Top center", ENERGY_HINT)
                .addRequirements(INPUTS[0], "Bottom center; each slice except last; slot 1", INPUT_HINT)
                .addRequirements(OUTPUT, "Last slice, bottom center", OUTPUT_HINT)
                .addStructureHint("Fluid Input (optional, single-fluid)", BOTTOM_HINT)
                .addStructureHint("Data Access Hatch (optional, next to controller)", DATA_HINT)
                .addStructureInfo("Power: 1-8 basic OR 1 multi-amp; no mixing or lasers")
                .addMasterChannel("Length: channel + 4 (5-16)")
                .addSubChannel(GTStructureUtility.HATCH_CHANNEL, "Auto-place required hatches")
                .toolTipFinisher();
    }

    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            if (aActive)
                return new ITexture[]{
                        Casings.SOLID_STEEL.getTexture(),
                        TextureFactory.of(OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE),
                        TextureFactory.builder().addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    Casings.SOLID_STEEL.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_ASSEMBLY_LINE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_GLOW).glow().build()};
        }
        return new ITexture[]{Casings.SOLID_STEEL.getTexture()};
    }

    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "AssemblyLine.png");
    }

    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return null;
    }

    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    public boolean checkRecipe(ItemStack aStack) {
        if(GT_Values.D1)
            GT_FML_LOGGER.info("Start ALine recipe check");
        ArrayList<ItemStack> tDataStickList = getDataItems(2);
        if (tDataStickList.size() == 0) return false;
            if(GT_Values.D1)
                GT_FML_LOGGER.info("Stick accepted, " + tDataStickList.size() + " Data Sticks found");

        ItemStack tStack[] = new ItemStack[15];
        FluidStack[] tFluids = new FluidStack[4];
        boolean findRecipe = false;
        nextDS:for (ItemStack tDataStick : tDataStickList){
            NBTTagCompound tTag = tDataStick.getTagCompound();
            if (tTag == null)
                continue;
            // Each data stick is a separate candidate; failed candidates must not leave inputs behind.
            Arrays.fill(tStack, null);
            Arrays.fill(tFluids, null);
            for (int i = 0; i < 15; i++) {
    			int count = tTag.getInteger("a"+i);
                if (!tTag.hasKey("" + i) && count <= 0)
                    continue;
                if (i >= mInputBusses.size() || mInputBusses.get(i) == null) {
                    continue nextDS;
                }
                
                ItemStack stackInSlot = mInputBusses.get(i).getBaseMetaTileEntity().getStackInSlot(0);
                boolean flag = true;
                if (count > 0) {
            		for (int j = 0; j < count; j++) {
            			tStack[i] = GT_Utility.loadItem(tTag, "a" + i + ":" + j);
            			if (tStack[i] == null) continue;
            			    if(GT_Values.D1)
            			        GT_FML_LOGGER.info("Item "+i+" : "+tStack[i].getUnlocalizedName());
            			if (GT_Utility.areStacksEqual(tStack[i], stackInSlot, true) && tStack[i].stackSize <= stackInSlot.stackSize) {
            				flag = false;
            				break;
            			}
            		}
            	}
                if (flag) {
            		tStack[i] = GT_Utility.loadItem(tTag, "" + i);
            		if (tStack[i] == null) {
            			flag = false;
            			continue;
            		}
            		if(GT_Values.D1)
            		    GT_FML_LOGGER.info("Item "+i+" : "+tStack[i].getUnlocalizedName());
        			if (GT_Utility.areStacksEqual(tStack[i], stackInSlot, true) && tStack[i].stackSize <= stackInSlot.stackSize) {
        				flag = false;
        			}
            	}
                if(GT_Values.D1)
                    GT_FML_LOGGER.info(i + (flag ? " not accepted" : " accepted"));
                if (flag)
                    continue nextDS;
            }

            if(GT_Values.D1)GT_FML_LOGGER.info("All Items done, start fluid check");
            for (int i = 0; i < 4; i++) {
                if (!tTag.hasKey("f" + i)) continue;
                tFluids[i] = GT_Utility.loadFluid(tTag, "f" + i);
                if (tFluids[i] == null) continue;
                    if(GT_Values.D1)
                        GT_FML_LOGGER.info("Fluid "+i+" "+tFluids[i].getUnlocalizedName());
                if (i >= mInputHatches.size() || mInputHatches.get(i) == null) {
                    continue nextDS;
                }
                FluidStack fluidInHatch = mInputHatches.get(i).mFluid;
                if (fluidInHatch == null || !GT_Utility.areFluidsEqual(fluidInHatch, tFluids[i], true) || fluidInHatch.amount < tFluids[i].amount) {                    if(GT_Values.D1)
                        GT_FML_LOGGER.info(i+" not accepted");
                    continue nextDS;
                }
                if(GT_Values.D1)
                    GT_FML_LOGGER.info(i+" accepted");
            }

            if(GT_Values.D1)
                GT_FML_LOGGER.info("Input accepted, check other values");
            if (!tTag.hasKey("output"))
                continue;
            mOutputItems = new ItemStack[]{GT_Utility.loadItem(tTag, "output")};
            if (mOutputItems[0] == null || !GT_Utility.isStackValid(mOutputItems[0]))
                continue;

            if (!tTag.hasKey("time"))
                continue;
            mMaxProgresstime = tTag.getInteger("time");
            if (mMaxProgresstime <= 0)
                continue;

            if (!tTag.hasKey("eu"))
                continue;
            mEUt = tTag.getInteger("eu");

           /* if (Math.abs(this.mEUt) > this.getMaxInputVoltage()) {
                if(GT_Values.D1)
                    GT_FML_LOGGER.info("Found avaiable recipe, but Voltage too low!");
                return false;
            }*/
            if(GT_Values.D1)GT_FML_LOGGER.info("Find avaiable recipe");
                findRecipe = true;
            break;
        }
        if (!findRecipe)
            return false;

        if(GT_Values.D1)GT_FML_LOGGER.info("All checked start consuming inputs");
        for (int i = 0; i < 15; i++) {
            if (tStack[i] == null)
                continue;
            ItemStack stackInSlot = mInputBusses.get(i).getBaseMetaTileEntity().getStackInSlot(0);
            stackInSlot.stackSize -= tStack[i].stackSize;
        }

        for (int i = 0; i < 4; i++) {
            if (tFluids[i] == null)
                continue;
            mInputHatches.get(i).mFluid.amount -= tFluids[i].amount;
            if (mInputHatches.get(i).mFluid.amount <= 0) {
                mInputHatches.get(i).mFluid = null;
            }
        }
        if(GT_Values.D1)GT_FML_LOGGER.info("Check overclock");

        byte tTier = (byte) Math.max(1, Math.min(15, GT_Utility.getTier(getMaxInputVoltage())));
        this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
        this.mEfficiencyIncrease = 10000;
        if (mEUt <= 16) {
            this.mEUt = (mEUt * (1 << tTier - 1) * (1 << tTier - 1));
            this.mMaxProgresstime = (mMaxProgresstime / (1 << tTier - 1));
        } else {
            while (this.mEUt <= gregtech.api.enums.GT_Values.V[(tTier - 1)]) {
                this.mEUt *= 4;
                this.mMaxProgresstime /= 2;
            }
        }
        if (this.mEUt > 0) {
            this.mEUt = -this.mEUt;
        }
        this.mMaxProgresstime = Math.max(1, this.mMaxProgresstime);
        updateSlots();
        if(GT_Values.D1)
            GT_FML_LOGGER.info("Recipe sucessfull");
        return true;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        for(GT_MetaTileEntity_Hatch_DataAccess hatch_dataAccess:mDataAccessHatches){
            hatch_dataAccess.setActive(true);
        }
        return super.onRunningTick(aStack);
    }

    public void startSoundLoop(byte aIndex, double aX, double aY, double aZ) {
        super.startSoundLoop(aIndex, aX, aY, aZ);
        if (aIndex == 20) {
            GT_Utility.doSoundAtClient(GregTech_API.sSoundList.get(212), 10, 1.0F, aX, aY, aZ);
        }
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        mDataAccessHatches.clear();
        int length = buildingLength == 0 ? findLength() : buildingLength;
        boolean valid = checkPiece(piece(Math.max(MIN_LENGTH, length)), 0, 1, 0);
        // Structure traversal groups rows by depth; recipes require the original slice-first ordering.
        mInputBusses.sort(Comparator.comparingInt(hatch -> sliceOf(hatch.getBaseMetaTileEntity())));
        mInputHatches.sort(Comparator.comparingInt(hatch -> inputOrder(hatch.getBaseMetaTileEntity())));
        if (!valid || length < MIN_LENGTH || !BOTTOM.isSatisfied(this, 0)
                || !ENERGY.isSatisfied(this, 0) || !OUTPUT.isSatisfied(this, 0)) {
            return false;
        }
        for (int slice = 0; slice < length - 1; slice++) {
            if (!INPUTS[slice].isSatisfied(this, 0)) {
                return false;
            }
        }
        return mEnergyHatchesMulti.isEmpty() ? mEnergyHatches.size() <= 8
                : mEnergyHatchesMulti.size() == 1 && mEnergyHatches.isEmpty();
    }

    private int findLength() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        ForgeDirection along = getExtendedFacing().getRelativeLeftInWorld();
        ForgeDirection back = getExtendedFacing().getRelativeBackInWorld();
        for (int slice = 0; slice < MAX_LENGTH; slice++) {
            IGregTechTileEntity tile = base.getIGregTechTileEntityOffset(
                    along.offsetX * slice + back.offsetX, -2, along.offsetZ * slice + back.offsetZ);
            if (tile != null && tile.getMetaTileEntity() instanceof GT_MetaTileEntity_Hatch_OutputBus) {
                return slice + 1;
            }
        }
        return MAX_LENGTH;
    }

    private int sliceOf(IGregTechTileEntity tile) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        ForgeDirection along = getExtendedFacing().getRelativeLeftInWorld();
        return (tile.getXCoord() - base.getXCoord()) * along.offsetX
                + (tile.getZCoord() - base.getZCoord()) * along.offsetZ;
    }

    private int inputOrder(IGregTechTileEntity tile) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        ForgeDirection back = getExtendedFacing().getRelativeBackInWorld();
        return sliceOf(tile) * 3 + (tile.getXCoord() - base.getXCoord()) * back.offsetX
                + (tile.getZCoord() - base.getZCoord()) * back.offsetZ;
    }

    private static String piece(int length) {
        return "line_" + length;
    }

    private static int constructionLength(ItemStack trigger) {
        return Math.max(MIN_LENGTH, Math.min(MAX_LENGTH - 4, trigger.stackSize) + 4);
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildPiece(piece(constructionLength(trigger)), trigger, hintsOnly, 0, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        // Limit hatch discovery to the selected length even before its output bus exists.
        buildingLength = constructionLength(trigger);
        try {
            return survivalBuildPiece(piece(buildingLength), trigger, 0, 1, 0, elementBudget, environment);
        } finally {
            buildingLength = 0;
        }
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_AssemblyLine> getStructureDefinition() {
        if (structureDefinition == null) {
            StructureDefinition.Builder<GT_MetaTileEntity_AssemblyLine> builder = StructureDefinition
                    .<GT_MetaTileEntity_AssemblyLine>builder()
                    .addChannel(GTStructureUtility.HATCH_CHANNEL, 1, 1)
                    .addElement('B', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_AssemblyLine.class)
                            .allow(LineHatch.FLUID_INPUT)
                            .buildAndChain(BOTTOM)
                            .withHint(BOTTOM_HINT))
                    .addElement('E', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_AssemblyLine.class)
                            .buildAndChain(ENERGY)
                            .withHint(ENERGY_HINT))
                    .addElement('O', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_AssemblyLine.class)
                            .hint(OUTPUT_HINT)
                            .build(OUTPUT))
                    .addElement('D', GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_AssemblyLine.class)
                            .allow(LineHatch.DATA)
                            .buildAndChain(StructureRequirements.builder(Casings.GRATE, 0).build())
                            .withHint(DATA_HINT))
                    .addElement('G', Casings.GRATE.asElement())
                    .addElement('A', Casings.ASSEMBLER.asElement())
                    .addElement('C', Casings.ASSEMBLY_LINE.asElement())
                    .addElement('L', ofBlockAnyMeta(GT_Utility.getBlockFromStack(Ic2Items.reinforcedGlass),
                            GT_Utility.getBlockFromStack(Ic2Items.reinforcedGlass), Ic2Items.reinforcedGlass.getItemDamage()));
            for (int slice = 0; slice < MAX_LENGTH - 1; slice++) {
                builder.addElement((char) ('a' + slice), GTStructureUtility.buildHatchAdder(GT_MetaTileEntity_AssemblyLine.class)
                        .hint(INPUT_HINT)
                        .build(INPUTS[slice]));
            }
            for (int length = MIN_LENGTH; length <= MAX_LENGTH; length++) {
                StringBuilder buses = new StringBuilder();
                for (int slice = 0; slice < length - 1; slice++) {
                    buses.append((char) ('a' + slice));
                }
                buses.append('O');
                builder.addShape(piece(length), transpose(new String[][]{
                        {row('-', length), row('E', length), row('-', length)},
                        {"~D" + row('G', length - 2), row('A', length), row('G', length)},
                        {row('L', length), row('C', length), row('L', length)},
                        {row('B', length), buses.toString(), row('B', length)}
                }));
            }
            structureDefinition = builder.build();
        }
        return structureDefinition;
    }

    private static String row(char element, int length) {
        char[] row = new char[length];
        Arrays.fill(row, element);
        return new String(row);
    }

    private static final class SliceInput implements IHatchElement {
        private final int slice;

        private SliceInput(int slice) {
            this.slice = slice;
        }

        @Override
        public boolean matches(IMetaTileEntity tile) {
            return tile instanceof GT_MetaTileEntity_Hatch_InputBus;
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            GT_MetaTileEntity_AssemblyLine line = (GT_MetaTileEntity_AssemblyLine) machine;
            return (int) line.mInputBusses.stream()
                    .filter(hatch -> line.sliceOf(hatch.getBaseMetaTileEntity()) == slice)
                    .count();
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            return machine.addInputToMachineList(tile, texture);
        }

        @Override
        public String getDescription() {
            return StatCollector.translateToLocal("gt.tooltip.hatch.input_bus");
        }
    }

    private enum LineHatch implements IHatchElement {
        ENERGY,
        OUTPUT_BUS,
        FLUID_INPUT,
        DATA;

        @Override
        public boolean matches(IMetaTileEntity tile) {
            switch (this) {
                case ENERGY:
                    return HatchElement.Energy.matches(tile) && !(tile instanceof GTMTE_LaserEnergy_In);
                case OUTPUT_BUS:
                    return tile instanceof GT_MetaTileEntity_Hatch_OutputBus;
                case FLUID_INPUT:
                    return tile instanceof GT_MetaTileEntity_Hatch_Input && !(tile instanceof GTMTE_Multi_Hatch_Input);
                case DATA:
                    return tile instanceof GT_MetaTileEntity_Hatch_DataAccess;
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public int count(GT_MetaTileEntity_MultiBlockBase machine) {
            switch (this) {
                case ENERGY:
                    return HatchElement.Energy.count(machine);
                case OUTPUT_BUS:
                    return machine.mOutputBusses.size();
                case FLUID_INPUT:
                    return machine.mInputHatches.size();
                case DATA:
                    return ((GT_MetaTileEntity_AssemblyLine) machine).mDataAccessHatches.size();
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int texture) {
            switch (this) {
                case ENERGY:
                    return machine.addEnergyInputToMachineList(tile, texture);
                case OUTPUT_BUS:
                    return machine.addOutputToMachineList(tile, texture);
                case FLUID_INPUT:
                    return machine.addInputToMachineList(tile, texture);
                case DATA:
                    return ((GT_MetaTileEntity_AssemblyLine) machine).addDataAccessToMachineList(tile, texture);
                default:
                    throw new AssertionError(this);
            }
        }

        @Override
        public String getDescription() {
            switch (this) {
                case ENERGY:
                    return HatchElement.Energy.getDescription();
                case OUTPUT_BUS:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.output_bus");
                case FLUID_INPUT:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.input_hatch");
                case DATA:
                    return StatCollector.translateToLocal("gt.tooltip.hatch.data_access");
                default:
                    throw new AssertionError(this);
            }
        }
    }

    /**
     * @param state using bitmask, 1 for IntegratedCircuit, 2 for DataStick, 4 for DataOrb
     */
    private boolean isCorrectDataItem(ItemStack aStack, int state){
        if ((state & 1) != 0 && ItemList.Circuit_Integrated.isStackEqual(aStack, true, true)) return true;
        if ((state & 2) != 0 && ItemList.Tool_DataStick.isStackEqual(aStack, false, true)) return true;
        if ((state & 4) != 0 && ItemList.Tool_DataOrb.isStackEqual(aStack, false, true)) return true;
        return false;
    }

    /**
     * @param state using bitmask, 1 for IntegratedCircuit, 2 for DataStick, 4 for DataOrb
     */
    public ArrayList<ItemStack> getDataItems(int state) {
        ArrayList<ItemStack> rList = new ArrayList<ItemStack>();
        if (GT_Utility.isStackValid(mInventory[1]) && isCorrectDataItem(mInventory[1], state)) {
            rList.add(mInventory[1]);
        }
        for (GT_MetaTileEntity_Hatch_DataAccess tHatch : mDataAccessHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                for (int i = 0; i < tHatch.getBaseMetaTileEntity().getSizeInventory(); i++) {
                    if (tHatch.getBaseMetaTileEntity().getStackInSlot(i) != null
                            && isCorrectDataItem(tHatch.getBaseMetaTileEntity().getStackInSlot(i), state))
                        rList.add(tHatch.getBaseMetaTileEntity().getStackInSlot(i));
                }
            }
        }
        return rList;
    }

    public boolean addDataAccessToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_DataAccess) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            return mDataAccessHatches.add((GT_MetaTileEntity_Hatch_DataAccess) aMetaTileEntity);
        }
        return false;
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    public int getPollutionPerTick(ItemStack aStack) {
        return 0;
    }

    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }
}
