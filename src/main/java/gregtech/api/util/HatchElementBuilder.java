package gregtech.api.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import gregtech.api.interfaces.ICasing;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import space.impact.api.ImpactAPI;
import space.impact.api.alignment.constructable.ChannelDataAccessor;
import space.impact.api.hint.HintColor;
import space.impact.api.structure.AutoPlaceEnvironment;
import space.impact.api.structure.IItemSource;
import space.impact.api.structure.IStructureElement;
import space.impact.api.structure.StructureUtility;
import space.impact.api.util.ItemStackPredicate;

/**
 * A hatch-or-casing element. The hatch channel prioritizes available missing hatches, then builds casings.
 */
public final class HatchElementBuilder<T extends GT_MetaTileEntity_MultiBlockBase> {
    private final Map<IHatchElement, Integer> requirements = new LinkedHashMap<>();
    private int textureIndex = -1;
    private HintColor hintColor;

    public HatchElementBuilder<T> atLeast(IHatchElement... elements) {
        for (IHatchElement element : elements) requirements.merge(Objects.requireNonNull(element), 1, Integer::sum);
        return this;
    }

    public HatchElementBuilder<T> casingIndex(int index) {
        if (index < 0) throw new IllegalArgumentException("Negative casing texture index");
        textureIndex = index;
        return this;
    }

    public HatchElementBuilder<T> casingIndex(ICasing casing) {
        return casingIndex(Objects.requireNonNull(casing).getTextureId());
    }

    /**
     * Derive both hatch texture and casing structure element from the descriptor.
     */
    public IStructureElement<T> buildAndChain(ICasing casing) {
        return casingIndex(casing).buildAndChain(casing.<T>asElement());
    }

    /**
     * The callback runs only for successfully checked casings, never for hints or placement.
     */
    public IStructureElement<T> buildAndChain(ICasing casing, Consumer<T> onCasingCheck) {
        Objects.requireNonNull(onCasingCheck);
        return casingIndex(casing).buildAndChain(StructureUtility.onElementPass(onCasingCheck, casing.<T>asElement()));
    }

    /**
     * The descriptor supplies the complete requirements, replacing any earlier atLeast calls.
     */
    public IStructureElement<T> buildAndChain(StructureRequirements requirements, Consumer<T> onCasingCheck) {
        Objects.requireNonNull(requirements);
        Objects.requireNonNull(onCasingCheck);
        casingIndex(requirements.getCasing());
        return buildElement(StructureUtility.onElementPass(onCasingCheck, requirements.getCasing().<T>asElement()),
                requirements.getHatches());
    }

    public IStructureElement<T> buildAndChain(StructureRequirements requirements) {
        return buildAndChain(requirements, machine -> {
        });
    }

    /**
     * Override the casing hologram with a colored marker. By default, use the casing's hint.
     */
    public HatchElementBuilder<T> hint(HintColor color) {
        hintColor = Objects.requireNonNull(color);
        return this;
    }

    public IStructureElement<T> buildAndChain(IStructureElement<T> casing) {
        Objects.requireNonNull(casing);
        if (requirements.isEmpty() || textureIndex < 0) throw new IllegalStateException("Hatch types and texture required");
        return buildElement(casing, new LinkedHashMap<>(requirements));
    }

    private IStructureElement<T> buildElement(IStructureElement<T> casing, Map<IHatchElement, Integer> required) {
        int texture = textureIndex;
        HintColor color = hintColor;
        return new IStructureElement<T>() {
            private IHatchElement matching(World world, int x, int y, int z) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof IGregTechTileEntity)) return null;
                for (IHatchElement element : required.keySet()) {
                    if (element.matches(((IGregTechTileEntity) tile).getMetaTileEntity())) return element;
                }
                return null;
            }

            private Predicate<ItemStack> missingHatches(T machine) {
                return stack -> required.entrySet().stream().anyMatch(entry -> entry.getKey().count(machine)
                        < entry.getValue() && entry.getKey().matches(GTStructureUtility.getMetaTileEntity(stack)));
            }

            private boolean needsHatches(T machine) {
                return required.entrySet().stream().anyMatch(entry -> entry.getKey().count(machine) < entry.getValue());
            }

            @Override
            public boolean check(T machine, World world, int x, int y, int z) {
                IHatchElement hatch = matching(world, x, y, z);
                return hatch != null ? hatch.add(machine, (IGregTechTileEntity) world.getTileEntity(x, y, z), texture)
                        : casing.check(machine, world, x, y, z);
            }

            @Override
            public boolean couldBeValid(T machine, World world, int x, int y, int z, ItemStack trigger) {
                return matching(world, x, y, z) != null || casing.couldBeValid(machine, world, x, y, z, trigger);
            }

            @Override
            public List<String> getDescription(T machine) {
                List<String> descriptions = new ArrayList<>();
                for (IHatchElement element : required.keySet()) descriptions.add(element.getDescription());
                List<String> casingDescription = casing.getDescription(machine);
                if (casingDescription != null) descriptions.addAll(casingDescription);
                return descriptions;
            }

            @Override
            public boolean spawnHint(T machine, World world, int x, int y, int z, ItemStack trigger) {
                if (color == null) return casing.spawnHint(machine, world, x, y, z, trigger);
                ImpactAPI.hintParticle(world, x, y, z, color);
                return true;
            }

            @Override
            public boolean placeBlock(T machine, World world, int x, int y, int z, ItemStack trigger) {
                if (couldBeValid(machine, world, x, y, z, trigger)) return true;
                if (!world.isAirBlock(x, y, z)) return false;
                return casing.placeBlock(machine, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(T machine, World world, int x, int y, int z,
                                                  ItemStack trigger, AutoPlaceEnvironment environment) {
                if (ChannelDataAccessor.hasSubChannel(trigger, GTStructureUtility.HATCH_CHANNEL) && needsHatches(machine)) {
                    Predicate<ItemStack> missing = missingHatches(machine);
                    if (environment.getSource().takeOne(missing, true) != null) return BlocksToPlace.create(missing);
                }
                return casing.getBlocksToPlace(machine, world, x, y, z, trigger, environment);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T machine, World world, int x, int y, int z, ItemStack trigger,
                                                  IItemSource source, EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
                return survivalPlaceBlock(machine, world, x, y, z, trigger,
                        AutoPlaceEnvironment.fromLegacy(source, actor, chatter));
            }

            @Override
            public PlaceResult survivalPlaceBlock(T machine, World world, int x, int y, int z,
                                                  ItemStack trigger, AutoPlaceEnvironment environment) {
                if (couldBeValid(machine, world, x, y, z, trigger)) return PlaceResult.SKIP;
                if (world.isRemote || !ImpactAPI.isBlockTriviallyReplaceable(world, x, y, z, environment.getActor())) {
                    return PlaceResult.REJECT;
                }
                if (!ChannelDataAccessor.hasSubChannel(trigger, GTStructureUtility.HATCH_CHANNEL) || !needsHatches(machine)) {
                    return casing.survivalPlaceBlock(machine, world, x, y, z, trigger, environment);
                }
                ItemStack candidate = environment.getSource().takeOne(missingHatches(machine), true);
                if (candidate == null) {
                    return casing.survivalPlaceBlock(machine, world, x, y, z, trigger, environment);
                }
                PlaceResult result = StructureUtility.survivalPlaceBlock(candidate, ItemStackPredicate.NBTMode.EXACT,
                        candidate.getTagCompound(), true, world, x, y, z, environment.getSource(),
                        environment.getActor(), environment.getChatter());
                if (result != PlaceResult.ACCEPT) return result;
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof IGregTechTileEntity) GTStructureUtility.orientHatch((IGregTechTileEntity) tile, environment);
                return PlaceResult.ACCEPT_STOP;
            }
        };
    }
}
