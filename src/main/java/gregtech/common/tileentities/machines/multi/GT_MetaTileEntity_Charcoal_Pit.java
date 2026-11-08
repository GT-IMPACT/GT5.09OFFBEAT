package gregtech.common.tileentities.machines.multi;

import gregtech.api.enums.Casings;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.TimedProcessingLogic;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ProcessingMultiBlockBase;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Recipe;
import gregtech.common.GT_Pollution;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.GTStructureChannels;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import space.impact.structures.ImpactStructures;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.structure.AutoPlaceEnvironment;
import space.impact.structures.structure.IStructureDefinition;
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import space.impact.structures.structure.StructureDefinition;
import space.impact.structures.structure.StructureUtility;
import space.impact.structures.util.ItemStackPredicate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static gregtech.api.enums.Textures.BlockIcons.*;
import static space.impact.structures.structure.StructureUtility.transpose;

public class GT_MetaTileEntity_Charcoal_Pit
        extends GT_MetaTileEntity_ProcessingMultiBlockBase<GT_MetaTileEntity_Charcoal_Pit>
        implements ISurvivalConstructable {
    private static final String STRUCTURE_PIECE = "pit";
    private static final int[][] NEIGHBORS = {{1, 0, 0}, {-1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, 1}, {0, 0, -1}};
    private boolean running;
    private PitLayout layout;
    private PitLayout buildingLayout;

    public GT_MetaTileEntity_Charcoal_Pit(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_Charcoal_Pit(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Charcoal_Pit(mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder(mName)
                .useCompactStructure()
                .addMachineType("Charcoal Pit")
                .addInfo("Converts logs into brittle charcoal; keep the pile sealed until the burn finishes.")
                .addInfo("No power or hatches required.")
                .addInfo("Burn time depends on the number of logs.")
                .addInfo("Pollution: 10 per burn tick, all released at ignition.")
                .beginVariableStructureBlock(3, 13, 3, 7, 3, 13, false)
                .addController("top center, directly above a log")
                .addStructureSection("Components")
                .addOtherStructurePart("Logs", "fill the pile below the controller")
                .addOtherStructurePart("Bricks", "below every log column")
                .addOtherStructurePart("Dirt or Grass", "cover the sides and top, except the controller")
                .addStructureInfo("Connected irregular piles are also accepted.")
                .addSubChannel(GTStructureChannels.WIDTH, "1-6: %s; default: stack size.", "log width = 2 x value - 1")
                .addSubChannel(GTStructureChannels.DEPTH, "1-6: %s; default: stack size.", "log depth = 2 x value - 1")
                .addSubChannel(GTStructureChannels.HEIGHT, "1-5 log layers; default: stack size.")
                .toolTipFinisher();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == 1) {
            if (aActive) return new ITexture[]{
                    Casings.BRONZE_PLATED_BRICKS.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_ROCK_BREAKER_ACTIVE),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ROCK_BREAKER_ACTIVE_GLOW).glow().build()};
            return new ITexture[]{
                    Casings.BRONZE_PLATED_BRICKS.getTexture(),
                    TextureFactory.of(OVERLAY_FRONT_ROCK_BREAKER),
                    TextureFactory.builder().addIcon(OVERLAY_FRONT_ROCK_BREAKER_GLOW).glow().build(),
            };
        }
        return new ITexture[]{Casings.BRONZE_PLATED_BRICKS.getTexture()};
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return null;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack stack) {
        return true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new TimedProcessingLogic(0, this::prepareBurn);
    }

    @Override
    public int getSupportedProcessingModes() {
        return 0;
    }

    private int prepareBurn() {
        PitLayout detected = detectLayout();
        if (detected == null || !checkLayout(detected)) {
            return 0;
        }
        return Math.max(1, (int) Math.sqrt(detected.logs.size() * 240000L));
    }

    @Override
    protected void startCycle(ProcessingLogic.Result result, int efficiency) {
        super.startCycle(result, 10000);
        running = true;
        GT_Pollution.addPollution(getBaseMetaTileEntity(), mMaxProgresstime * 10);
    }

    /** Structure checks never produce charcoal; only a finished, still-sealed burn does. */
    @Override
    protected void completeRecipe() {
        if (running) {
            PitLayout detected = detectLayout();
            if (detected == null || !checkLayout(detected)) {
                stopMachine();
                return;
            }
            World world = getBaseMetaTileEntity().getWorld();
            for (Position log : detected.logs) {
                int[] position = worldPosition(log);
                world.setBlock(position[0], position[1], position[2], Casings.BRITTLE_CHARCOAL.getBlock(),
                        Casings.BRITTLE_CHARCOAL.getMetadata(), 3);
            }
            running = false;
        }
        super.completeRecipe();
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity base, ItemStack stack) {
        mWrench = true;
        mScrewdriver = true;
        mSoftHammer = true;
        mHardHammer = true;
        mSolderingTool = true;
        mCrowbar = true;
        PitLayout detected = buildingLayout == null ? detectLayout() : buildingLayout;
        return detected != null && checkLayout(detected);
    }

    private boolean checkLayout(PitLayout candidate) {
        layout = candidate;
        return checkPiece(STRUCTURE_PIECE, -candidate.minA, -candidate.minB, -candidate.minC);
    }

    private int[] worldPosition(Position position) {
        int[] p = new int[3];
        getExtendedFacing().getWorldOffset(new int[]{position.a, position.b, position.c}, p);
        IGregTechTileEntity base = getBaseMetaTileEntity();
        p[0] += base.getXCoord();
        p[1] += base.getYCoord();
        p[2] += base.getZCoord();
        return p;
    }

    private boolean isLogAt(Position position) {
        int[] p = worldPosition(position);
        World world = getBaseMetaTileEntity().getWorld();
        return world.blockExists(p[0], p[1], p[2])
                && isWoodLog(world.getBlock(p[0], p[1], p[2]), world.getBlockMetadata(p[0], p[1], p[2]));
    }

    /** Preserve the original connected-pile rules, including non-rectangular piles and their individual boundaries. */
    private PitLayout detectLayout() {
        Position seed = new Position(0, 1, 0);
        if (!isLogAt(seed)) {
            return null;
        }
        Map<Position, Character> elements = new LinkedHashMap<>();
        Set<Position> visited = new HashSet<>();
        ArrayDeque<Position> queue = new ArrayDeque<>();
        visited.add(seed);
        queue.add(seed);
        while (!queue.isEmpty()) {
            Position log = queue.removeFirst();
            elements.put(log, 'L');
            for (int[] direction : NEIGHBORS) {
                Position next = new Position(log.a + direction[0], log.b + direction[1], log.c + direction[2]);
                if (log.equals(seed) && next.a == 0 && next.b == 0 && next.c == 0) {
                    elements.put(next, '~');
                    continue;
                }
                if (next.a == 0 && next.b == 0 && next.c == 0) {
                    return null; // Only the log directly beneath the controller may touch it.
                }
                if (Math.abs(next.a) <= 5 && Math.abs(next.c) <= 5 && next.b >= 0 && next.b <= 5 && isLogAt(next)) {
                    if (visited.add(next)) {
                        queue.addLast(next);
                    }
                } else {
                    char boundary = direction[1] == 1 ? 'B' : 'S';
                    Character previous = elements.putIfAbsent(next, boundary);
                    if (previous != null && previous != boundary) {
                        elements.put(next, '!');
                    }
                }
            }
        }
        return new PitLayout(elements);
    }

    private static PitLayout constructionLayout(ItemStack trigger) {
        int halfWidth = Math.max(1, Math.min(6, ChannelDataAccessor.getChannelData(trigger, GTStructureChannels.WIDTH))) - 1;
        int halfDepth = Math.max(1, Math.min(6, ChannelDataAccessor.getChannelData(trigger, GTStructureChannels.DEPTH))) - 1;
        int height = Math.max(1, Math.min(5, ChannelDataAccessor.getChannelData(trigger, GTStructureChannels.HEIGHT)));
        return rectangularLayout(halfWidth, height, halfDepth);
    }

    private static PitLayout rectangularLayout(int halfWidth, int height, int halfDepth) {
        Map<Position, Character> elements = new LinkedHashMap<>();
        for (int a = -halfWidth; a <= halfWidth; a++) {
            for (int c = -halfDepth; c <= halfDepth; c++) {
                elements.put(new Position(a, 0, c), a == 0 && c == 0 ? '~' : 'S');
                elements.put(new Position(a, height + 1, c), 'B');
                for (int b = 1; b <= height; b++) {
                    elements.put(new Position(a, b, c), 'L');
                    if (a == -halfWidth) {
                        elements.put(new Position(a - 1, b, c), 'S');
                    }
                    if (a == halfWidth) {
                        elements.put(new Position(a + 1, b, c), 'S');
                    }
                    if (c == -halfDepth) {
                        elements.put(new Position(a, b, c - 1), 'S');
                    }
                    if (c == halfDepth) {
                        elements.put(new Position(a, b, c + 1), 'S');
                    }
                }
            }
        }
        return new PitLayout(elements);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_Charcoal_Pit> getStructureDefinition() {
        if (layout == null) {
            layout = rectangularLayout(0, 1, 0);
        }
        return layout.definition;
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        PitLayout selected = constructionLayout(trigger);
        layout = selected;
        buildPiece(STRUCTURE_PIECE, trigger, hintsOnly, -selected.minA, -selected.minB, -selected.minC);
    }

    @Override
    public int survivalConstruct(ItemStack trigger, int elementBudget, ISurvivalBuildEnvironment environment) {
        buildingLayout = constructionLayout(trigger);
        layout = buildingLayout;
        try {
            return survivalBuildPiece(STRUCTURE_PIECE, trigger, -buildingLayout.minA, -buildingLayout.minB,
                    -buildingLayout.minC, elementBudget, environment);
        } finally {
            buildingLayout = null;
        }
    }

    private static boolean isWoodLog(Block log, int meta) {
        for (int id : OreDictionary.getOreIDs(new ItemStack(log, 1, meta))) {
            if (OreDictionary.getOreName(id).equals("logWood")) {
                return true;
            }
        }
        return OrePrefixes.log.contains(new ItemStack(log, 1, meta)) && "axe".equals(log.getHarvestTool(meta))
                && log.getMaterial() == Material.wood;
    }

    private static IStructureElement<GT_MetaTileEntity_Charcoal_Pit> naturalBlock(
            Block preview, BiPredicate<Block, Integer> accepted, String missingKey) {
        Predicate<ItemStack> item = stack -> stack != null && stack.getItem() instanceof ItemBlock
                && accepted.test(Block.getBlockFromItem(stack.getItem()),
                        ((ItemBlock) stack.getItem()).getMetadata(stack.getItemDamage()));
        return new IStructureElement<GT_MetaTileEntity_Charcoal_Pit>() {
            @Override
            public boolean check(GT_MetaTileEntity_Charcoal_Pit machine, World world, int x, int y, int z) {
                return accepted.test(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
            }

            @Override
            public boolean couldBeValid(GT_MetaTileEntity_Charcoal_Pit machine, World world,
                                         int x, int y, int z, ItemStack trigger) {
                return check(machine, world, x, y, z);
            }

            @Override
            public boolean spawnHint(GT_MetaTileEntity_Charcoal_Pit machine, World world,
                                     int x, int y, int z, ItemStack trigger) {
                boolean existing = check(machine, world, x, y, z);
                ImpactStructures.hintParticle(world, x, y, z, existing ? world.getBlock(x, y, z) : preview,
                        existing ? world.getBlockMetadata(x, y, z) : 0);
                return true;
            }

            @Override
            public boolean placeBlock(GT_MetaTileEntity_Charcoal_Pit machine, World world,
                                      int x, int y, int z, ItemStack trigger) {
                if (check(machine, world, x, y, z)) {
                    return true;
                }
                return world.isAirBlock(x, y, z) && world.setBlock(x, y, z, preview, 0, 3);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(GT_MetaTileEntity_Charcoal_Pit machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return BlocksToPlace.create(item);
            }

            @Override
            public PlaceResult survivalPlaceBlock(GT_MetaTileEntity_Charcoal_Pit machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                if (check(machine, world, x, y, z)) {
                    return PlaceResult.SKIP;
                }
                ItemStack candidate = environment.getSource().takeOne(item, true);
                if (candidate == null) {
                    environment.getChatter().accept(new ChatComponentTranslation(missingKey));
                    return PlaceResult.REJECT;
                }
                return StructureUtility.survivalPlaceBlock(candidate, ItemStackPredicate.NBTMode.EXACT,
                        candidate.getTagCompound(), true, world, x, y, z, environment.getSource(),
                        environment.getActor(), environment.getChatter());
            }
        };
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        super.saveNBTData(nbt);
        nbt.setBoolean("charcoalPitRunning", running);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        super.loadNBTData(nbt);
        running = nbt.hasKey("charcoalPitRunning") ? nbt.getBoolean("charcoalPitRunning")
                : mMaxProgresstime > 0 && mEfficiency > 0;
    }

    @Override
    public void stopMachine() {
        boolean retry = !mMachine && getBaseMetaTileEntity().isAllowedToWork();
        running = false;
        super.stopMachine();
        if (retry) {
            // Like the original igniter, automatically wait for a new sealed pile after harvesting or a broken seal.
            getBaseMetaTileEntity().enableWorking();
        }
    }

    private static final class PitLayout {
        private final IStructureDefinition<GT_MetaTileEntity_Charcoal_Pit> definition;
        private final List<Position> logs = new ArrayList<>();
        private final int minA;
        private final int minB;
        private final int minC;

        private PitLayout(Map<Position, Character> elements) {
            minA = elements.keySet().stream().mapToInt(p -> p.a).min().getAsInt();
            minB = elements.keySet().stream().mapToInt(p -> p.b).min().getAsInt();
            minC = elements.keySet().stream().mapToInt(p -> p.c).min().getAsInt();
            int maxA = elements.keySet().stream().mapToInt(p -> p.a).max().getAsInt();
            int maxB = elements.keySet().stream().mapToInt(p -> p.b).max().getAsInt();
            int maxC = elements.keySet().stream().mapToInt(p -> p.c).max().getAsInt();
            char[][][] grid = new char[maxB - minB + 1][maxC - minC + 1][maxA - minA + 1];
            for (char[][] layer : grid) {
                for (char[] row : layer) {
                    Arrays.fill(row, ' ');
                }
            }
            elements.forEach((p, element) -> {
                grid[p.b - minB][p.c - minC][p.a - minA] = element;
                if (element == 'L') {
                    logs.add(p);
                }
            });
            String[][] shape = new String[grid.length][grid[0].length];
            for (int b = 0; b < grid.length; b++) {
                for (int c = 0; c < grid[b].length; c++) {
                    shape[b][c] = new String(grid[b][c]);
                }
            }
            definition = StructureDefinition.<GT_MetaTileEntity_Charcoal_Pit>builder()
                    .addShape(STRUCTURE_PIECE, transpose(shape))
                    .addChannel(GTStructureChannels.WIDTH, 1, 6)
                    .addChannel(GTStructureChannels.DEPTH, 1, 6)
                    .addChannel(GTStructureChannels.HEIGHT, 1, 5)
                    .addElement('L', naturalBlock(Blocks.log, GT_MetaTileEntity_Charcoal_Pit::isWoodLog,
                            "gt.structure.missing_logs"))
                    .addElement('S', naturalBlock(Blocks.dirt, (block, meta) -> block == Blocks.dirt || block == Blocks.grass,
                            "gt.structure.missing_soil"))
                    .addElement('B', StructureUtility.ofBlockAnyMeta(Blocks.brick_block, Blocks.brick_block, 0))
                    .addElement('!', StructureUtility.error())
                    .build();
        }
    }

    private static final class Position {
        private final int a;
        private final int b;
        private final int c;

        private Position(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Position)) {
                return false;
            }
            Position p = (Position) other;
            return a == p.a && b == p.b && c == p.c;
        }

        @Override
        public int hashCode() {
            return (a * 31 + b) * 31 + c;
        }
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
