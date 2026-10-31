package gregtech.api.interfaces;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import java.util.Objects;

import gregtech.api.enums.Textures;
import space.impact.structures.structure.IStructureElement;
import space.impact.structures.structure.StructureUtility;

/**
 * A casing shared by rendering, structure checks, holograms and construction.
 * Addons can implement this interface with their own blocks and registered texture IDs.
 * Resolve blocks and create structure elements after block registration.
 */
public interface ICasing {
    Block getBlock();

    int getMetadata();

    /**
     * Global GregTech texture ID: {@code page * 128 + indexWithinPage}, not block metadata.
     */
    int getTextureId();

    /**
     * Resolved at display time so language changes and addon item names are respected.
     */
    default String getDisplayName() {
        Block block = Objects.requireNonNull(getBlock(), "Casing block is not registered");
        Item item = Item.getItemFromBlock(block);
        return item == null ? StatCollector.translateToLocal(block.getUnlocalizedName() + ".name")
                : new ItemStack(item, 1, getMetadata()).getDisplayName();
    }

    default ITexture getTexture() {
        return Textures.BlockIcons.getCasingTextureForId(getTextureId());
    }

    /**
     * Uses the same block and metadata for checks, hints, creative and survival placement.
     */
    default <T> IStructureElement<T> asElement() {
        return StructureUtility.ofBlock(Objects.requireNonNull(getBlock(), "Casing block is not registered"), getMetadata());
    }
}
