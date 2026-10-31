package gregtech.api.util;

import gregtech.api.GregTech_API;
import gregtech.api.enums.HeatingCoil;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Frame;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_EnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.common.blocks.GT_Item_Machines;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import space.impact.api.ImpactAPI;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.AutoPlaceEnvironment;
import space.impact.api.structure.IStructureElement;
import space.impact.api.structure.StructureUtility;
import space.impact.api.util.ItemStackPredicate;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class GTStructureUtility {
    public static final String HATCH_CHANNEL = "gt_hatch";

    private GTStructureUtility() {
    }

    public static <T extends GT_MetaTileEntity_MultiBlockBase> HatchElementBuilder<T> buildHatchAdder(Class<T> type) {
        return new HatchElementBuilder<>();
    }

    /** Frame material is stored in its metatile entity, not in the shared machine block metadata. */
    public static <T> IStructureElement<T> ofFrame(Materials material) {
        Objects.requireNonNull(material);
        return new IStructureElement<T>() {
            private ItemStack stack() {
                return GT_OreDictUnificator.get(OrePrefixes.frameGt, material, 1);
            }

            @Override
            public boolean check(T machine, World world, int x, int y, int z) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof IGregTechTileEntity)) return false;
                IMetaTileEntity meta = ((IGregTechTileEntity) tile).getMetaTileEntity();
                return meta instanceof GT_MetaPipeEntity_Frame && ((GT_MetaPipeEntity_Frame) meta).mMaterial == material;
            }

            @Override
            public boolean couldBeValid(T machine, World world, int x, int y, int z, ItemStack trigger) {
                return check(machine, world, x, y, z);
            }

            @Override
            public boolean spawnHint(T machine, World world, int x, int y, int z, ItemStack trigger) {
                IIcon icon = material.mIconSet.mTextures[OrePrefixes.frameGt.mTextureIndex].getIcon();
                ImpactAPI.hintParticleTinted(world, x, y, z, new IIcon[]{icon, icon, icon, icon, icon, icon},
                        material.mRGBa, stack());
                return true;
            }

            @Override
            public boolean placeBlock(T machine, World world, int x, int y, int z, ItemStack trigger) {
                if (check(machine, world, x, y, z)) return true;
                if (!world.isAirBlock(x, y, z)) return false;
                ItemStack frame = stack();
                return frame != null && frame.getItem() instanceof ItemBlock
                        && ((ItemBlock) frame.getItem()).placeBlockAt(frame, null, world, x, y, z,
                                ForgeDirection.UP.ordinal(), 0.5f, 0.5f, 0.5f, frame.getItemDamage());
            }

            @Override
            public BlocksToPlace getBlocksToPlace(T machine, World world, int x, int y, int z,
                                                  ItemStack trigger, AutoPlaceEnvironment environment) {
                return BlocksToPlace.create(stack());
            }

            @Override
            public PlaceResult survivalPlaceBlock(T machine, World world, int x, int y, int z,
                                                  ItemStack trigger, AutoPlaceEnvironment environment) {
                if (check(machine, world, x, y, z)) return PlaceResult.SKIP;
                ItemStack frame = stack();
                if (frame == null) return PlaceResult.REJECT;
                return StructureUtility.survivalPlaceBlock(frame, ItemStackPredicate.NBTMode.EXACT,
                        frame.getTagCompound(), false, world, x, y, z, environment.getSource(),
                        environment.getActor(), environment.getChatter());
            }
        };
    }

    /**
     * The check callback records/compares the machine's coil tier. The selection callback must be read-only:
     * it is also used by holograms and the construction conflict scan.
     */
    public static <T> IStructureElement<T> ofHeatingCoil(BiPredicate<T, HeatingCoil> check,
                                                         BiFunction<T, ItemStack, HeatingCoil> selection) {
        Objects.requireNonNull(check);
        Objects.requireNonNull(selection);
        return new IStructureElement<T>() {
            @Override
            public boolean check(T machine, World world, int x, int y, int z) {
                HeatingCoil coil = HeatingCoil.at(world, x, y, z);
                return coil != null && check.test(machine, coil);
            }

            @Override
            public boolean couldBeValid(T machine, World world, int x, int y, int z, ItemStack trigger) {
                return HeatingCoil.at(world, x, y, z) == selection.apply(machine, trigger);
            }

            @Override
            public boolean spawnHint(T machine, World world, int x, int y, int z, ItemStack trigger) {
                return selection.apply(machine, trigger).<T>asElement().spawnHint(machine, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(T machine, World world, int x, int y, int z, ItemStack trigger) {
                if (couldBeValid(machine, world, x, y, z, trigger)) {
                    return true;
                }
                if (!world.isAirBlock(x, y, z)) {
                    return false;
                }
                return selection.apply(machine, trigger).<T>asElement().placeBlock(machine, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(T machine, World world, int x, int y, int z,
                                                  ItemStack trigger, AutoPlaceEnvironment environment) {
                return selection.apply(machine, trigger).<T>asElement()
                        .getBlocksToPlace(machine, world, x, y, z, trigger, environment);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T machine, World world, int x, int y, int z,
                                                  ItemStack trigger, AutoPlaceEnvironment environment) {
                if (couldBeValid(machine, world, x, y, z, trigger)) {
                    return PlaceResult.SKIP;
                }
                return selection.apply(machine, trigger).<T>asElement()
                        .survivalPlaceBlock(machine, world, x, y, z, trigger, environment);
            }
        };
    }

    /**
     * A casing component in any of the five positions beside the center of a 3x3x3 machine
     * with a front-centered controller. Use this element at all five positions; use the supplied
     * shell element elsewhere. Checks accept every existing component, while construction selects
     * only one free position if none is present. The machine owns the component count.
     */
    public static <T extends GT_MetaTileEntity_EnhancedMultiBlockBase<T>> IStructureElement<T> ofCasingAroundCenter(
            ICasing casing, Consumer<T> onCheck, IStructureElement<T> shell) {
        Objects.requireNonNull(casing);
        Objects.requireNonNull(onCheck);
        Objects.requireNonNull(shell);
        IStructureElement<T> component = casing.asElement();
        return new IStructureElement<T>() {
            private IStructureElement<T> constructionElement(
                    T machine, World world, int x, int y, int z) {
                if (matchesCasing(casing, world, x, y, z)) {
                    return component;
                }
                int[] selected = findCasingPosition(machine, casing, world);
                return selected != null && selected[0] == x && selected[1] == y && selected[2] == z ? component : shell;
            }

            @Override
            public boolean check(T machine, World world, int x, int y, int z) {
                if (matchesCasing(casing, world, x, y, z)) {
                    onCheck.accept(machine);
                    return true;
                }
                return shell.check(machine, world, x, y, z);
            }

            @Override
            public boolean couldBeValid(T machine, World world,
                                         int x, int y, int z, ItemStack trigger) {
                return matchesCasing(casing, world, x, y, z) || shell.couldBeValid(machine, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T machine, World world,
                                     int x, int y, int z, ItemStack trigger) {
                return constructionElement(machine, world, x, y, z).spawnHint(machine, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(T machine, World world,
                                      int x, int y, int z, ItemStack trigger) {
                if (couldBeValid(machine, world, x, y, z, trigger)) {
                    return true;
                }
                if (!world.isAirBlock(x, y, z)) {
                    return false;
                }
                return constructionElement(machine, world, x, y, z).placeBlock(machine, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(T machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                return constructionElement(machine, world, x, y, z)
                        .getBlocksToPlace(machine, world, x, y, z, trigger, environment);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T machine, World world,
                                                  int x, int y, int z, ItemStack trigger, AutoPlaceEnvironment environment) {
                if (couldBeValid(machine, world, x, y, z, trigger)) {
                    return PlaceResult.SKIP;
                }
                return constructionElement(machine, world, x, y, z)
                        .survivalPlaceBlock(machine, world, x, y, z, trigger, environment);
            }
        };
    }

    private static boolean matchesCasing(ICasing casing, World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == casing.getBlock()
                && world.getBlockMetadata(x, y, z) == casing.getMetadata();
    }

    /** Read the world rather than check counters: hints must be pure and placement may span several passes. */
    private static <T extends GT_MetaTileEntity_EnhancedMultiBlockBase<T>> int[] findCasingPosition(
            T machine, ICasing casing, World world) {
        IGregTechTileEntity base = machine.getBaseMetaTileEntity();
        ForgeDirection front = machine.getExtendedFacing().getDirection();
        int centerX = base.getXCoord() - front.offsetX;
        int centerY = base.getYCoord();
        int centerZ = base.getZCoord() - front.offsetZ;
        int[] selected = null;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (side == front) {
                continue;
            }
            int x = centerX + side.offsetX;
            int y = centerY + side.offsetY;
            int z = centerZ + side.offsetZ;
            if (matchesCasing(casing, world, x, y, z)) {
                return null;
            }
            if (selected == null && world.isAirBlock(x, y, z)) {
                selected = new int[]{x, y, z};
            }
        }
        // With a closed shell, show which casing to replace manually; never overwrite it during construction.
        return selected == null ? new int[]{centerX, centerY - 1, centerZ} : selected;
    }

    /** Minecraft 1.7 text uses a fixed palette; orange and brown use its gold color. */
    public static EnumChatFormatting getHintTextColor(HintColor color) {
        switch (color) {
            case WHITE:
                return EnumChatFormatting.WHITE;
            case ORANGE:
            case BROWN:
                return EnumChatFormatting.GOLD;
            case MAGENTA:
            case PINK:
                return EnumChatFormatting.LIGHT_PURPLE;
            case LIGHT_BLUE:
                return EnumChatFormatting.AQUA;
            case YELLOW:
                return EnumChatFormatting.YELLOW;
            case LIME:
                return EnumChatFormatting.GREEN;
            case GRAY:
                return EnumChatFormatting.DARK_GRAY;
            case LIGHT_GRAY:
                return EnumChatFormatting.GRAY;
            case CYAN:
                return EnumChatFormatting.DARK_AQUA;
            case PURPLE:
                return EnumChatFormatting.DARK_PURPLE;
            case BLUE:
                return EnumChatFormatting.BLUE;
            case GREEN:
                return EnumChatFormatting.DARK_GREEN;
            case RED:
                return EnumChatFormatting.RED;
            case BLACK:
                return EnumChatFormatting.BLACK;
            default:
                throw new IllegalArgumentException("Unknown hint color: " + color);
        }
    }

    public static String formatHintText(HintColor color, String text) {
        return getHintTextColor(color) + text + EnumChatFormatting.RESET;
    }

    public static IMetaTileEntity getMetaTileEntity(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof GT_Item_Machines)) return null;
        int id = stack.getItemDamage();
        return id > 0 && id < GregTech_API.METATILEENTITIES.length ? GregTech_API.METATILEENTITIES[id] : null;
    }

    static void orientHatch(IGregTechTileEntity tile, AutoPlaceEnvironment environment) {
        if (environment.getAPILevel() != AutoPlaceEnvironment.APILevel.V2) return;
        ForgeDirection selected = null;
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (environment.isContainedInPiece(direction.offsetX, -direction.offsetY, direction.offsetZ)) continue;
            ForgeDirection facing = environment.getFacing().getWorldDirection(
                    direction.offsetY == 0 ? direction : direction.getOpposite());
            if (!tile.isValidFacing((byte) facing.ordinal())) continue;
            selected = facing;
            if (facing.offsetY == 0) break;
        }
        if (selected != null) tile.setFrontFacing((byte) selected.ordinal());
    }
}
