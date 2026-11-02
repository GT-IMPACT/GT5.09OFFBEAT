package gregtech.api.covers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.common.covers.Cover;
import gregtech.common.covers.CoverArm;
import gregtech.common.covers.CoverConveyor;
import gregtech.common.covers.CoverControlsWork;
import gregtech.common.covers.CoverEUMeter;
import gregtech.common.covers.CoverFluidfilter;
import gregtech.common.covers.CoverFluidRegulator;
import gregtech.common.covers.CoverItemMeter;
import gregtech.common.covers.CoverLegacyBehavior;
import gregtech.common.covers.CoverLiquidMeter;
import gregtech.common.covers.CoverNone;
import gregtech.common.covers.CoverPump;
import gregtech.common.covers.GT_Cover_Arm;
import gregtech.common.covers.GT_Cover_Conveyor;
import gregtech.common.covers.GT_Cover_ControlsWork;
import gregtech.common.covers.GT_Cover_EUMeter;
import gregtech.common.covers.GT_Cover_Fluidfilter;
import gregtech.common.covers.GT_Cover_FluidRegulator;
import gregtech.common.covers.GT_Cover_ItemMeter;
import gregtech.common.covers.GT_Cover_LiquidMeter;
import gregtech.common.covers.GT_Cover_Pump;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

/** Factories construct a distinct stateful cover for every tile side. */
public final class CoverRegistry {

    private static final Map<GT_ItemStack, CoverRegistration> COVERS = new ConcurrentHashMap<>();
    private static final CoverPlacer DEFAULT_COVER_PLACER = CoverPlacer.builder().build();
    public static final CoverPlacer PRIMITIVE_COVER_PLACER = CoverPlacer.builder().allowOnPrimitiveBlock().build();
    public static final CoverPlacer INTERCEPTS_RIGHT_CLICK_COVER_PLACER =
        CoverPlacer.builder().blocksCoverableGuiOpening().build();
    public static final Cover NO_COVER = new CoverNone(new CoverContext(0, ForgeDirection.UNKNOWN, null));

    private CoverRegistry() {
    }

    public static void registerCover(ItemStack item, ITexture texture, CoverFactory factory) {
        registerCover(item, texture, factory, DEFAULT_COVER_PLACER);
    }

    public static void registerCover(ItemStack item, ITexture texture, CoverFactory factory, CoverPlacer placer) {
        if (item == null || item.getItem() == null || factory == null || placer == null) {
            throw new IllegalArgumentException("Cover registration requires item, factory and placement rules");
        }
        COVERS.put(new GT_ItemStack(item), new CoverRegistration(factory, placer, texture));
        if (texture != null) {
            GregTech_API.sCovers.put(new GT_ItemStack(item), texture);
        }
    }

    public static void registerDecorativeCover(ItemStack item, ITexture texture) {
        registerCover(item, texture, context -> new Cover(context, texture), PRIMITIVE_COVER_PLACER);
    }

    private static CoverRegistration getRegistration(ItemStack item) {
        if (item == null || item.getItem() == null) {
            return null;
        }
        CoverRegistration registration = COVERS.get(new GT_ItemStack(item));
        return registration != null ? registration : COVERS.get(new GT_ItemStack(item, true));
    }

    public static Cover buildCover(ItemStack item, ForgeDirection side, ICoverable tile) {
        return buildCover(new CoverContext(item, side, tile));
    }

    public static Cover buildCover(int id, ForgeDirection side, ICoverable tile) {
        return buildCover(new CoverContext(id, side, tile));
    }

    private static Cover buildCover(CoverContext context) {
        if (context.getCoverID() == 0 || context.getSide() == null || context.getSide() == ForgeDirection.UNKNOWN) {
            return NO_COVER;
        }
        CoverRegistration registration = getRegistration(context.getCoverItem());
        if (registration != null) {
            return registration.getFactory().buildCover(context);
        }
        GT_CoverBehavior oldBehavior = GregTech_API.getCoverBehavior(context.getCoverID());
        BiFunction<CoverContext, GT_CoverBehavior, Cover> factory = getNativeFactory(oldBehavior);
        return factory == null ? new CoverLegacyBehavior(context, oldBehavior) : factory.apply(context, oldBehavior);
    }

    public static CoverPlacer getCoverPlacer(ItemStack item) {
        CoverRegistration registration = getRegistration(item);
        return registration == null ? DEFAULT_COVER_PLACER : registration.getCoverPlacer();
    }

    public static boolean isCover(ItemStack item) {
        return item != null && (getRegistration(item) != null || GregTech_API.sCovers.containsKey(new GT_ItemStack(item)));
    }

    public static boolean isNativeCover(ItemStack item) {
        return item != null && (getRegistration(item) != null
            || getNativeFactory(GregTech_API.getCoverBehavior(item)) != null);
    }

    private static BiFunction<CoverContext, GT_CoverBehavior, Cover> getNativeFactory(GT_CoverBehavior behavior) {
        if (behavior instanceof GT_Cover_ControlsWork) {
            return (context, unused) -> new CoverControlsWork(context, getCoverTexture(context.getCoverItem()));
        }
        if (behavior instanceof GT_Cover_Conveyor) {
            return CoverConveyor::new;
        }
        if (behavior instanceof GT_Cover_Pump) {
            return CoverPump::new;
        }
        if (behavior instanceof GT_Cover_Arm) {
            return CoverArm::new;
        }
        if (behavior instanceof GT_Cover_Fluidfilter) {
            return CoverFluidfilter::new;
        }
        if (behavior instanceof GT_Cover_FluidRegulator) {
            return CoverFluidRegulator::new;
        }
        if (behavior instanceof GT_Cover_EUMeter) {
            return CoverEUMeter::new;
        }
        if (behavior instanceof GT_Cover_ItemMeter) {
            return CoverItemMeter::new;
        }
        if (behavior instanceof GT_Cover_LiquidMeter) {
            return CoverLiquidMeter::new;
        }
        return null;
    }

    public static ITexture getCoverTexture(ItemStack item) {
        CoverRegistration registration = getRegistration(item);
        return registration == null ? GregTech_API.sCovers.get(new GT_ItemStack(item)) : registration.getCoverTexture();
    }

    public static Cover buildCoverFromNbt(NBTTagCompound nbt, ForgeDirection side, ICoverable tile) {
        Cover cover = buildCover(nbt.getInteger("id"), side, tile);
        if (cover != NO_COVER) {
            cover.readFromNbt(nbt);
        }
        return cover;
    }

    public static void writeCoverToNbt(Cover cover, NBTTagCompound nbt) {
        nbt.setInteger("id", cover.getCoverID());
        cover.writeToNBT(nbt);
    }

    public static void cover(ICoverable tile, ForgeDirection side, int id) {
        if (id == 0) {
            tile.detachCover(side);
        } else {
            tile.attachCover(buildCover(id, side, tile));
        }
    }
}
