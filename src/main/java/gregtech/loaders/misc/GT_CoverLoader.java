package gregtech.loaders.misc;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.objects.GT_CopiedBlockTexture;

import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_ModHandler;
import gregtech.common.covers.GT_Cover_Vent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class GT_CoverLoader
        implements Runnable {
    @Override
    public void run() {
        ItemStack tStack;
        for (byte i = 0; i < 16; i = (byte) (i + 1)) {
            tStack = new ItemStack(Blocks.wool, 0, i);
            GregTech_API.registerCover(new ItemStack(Blocks.carpet, 1, i), TextureFactory.of(Block.getBlockFromItem(tStack.getItem()), tStack.getItemDamage()), null);
        }
        GregTech_API.registerCover(GT_ModHandler.getIC2Item("reactorVent", 1L, 1), TextureFactory.of(Textures.BlockIcons.VENT_NORMAL), new GT_Cover_Vent(1));
        GregTech_API.registerCover(GT_ModHandler.getIC2Item("reactorVentCore", 1L, 1), TextureFactory.of(Textures.BlockIcons.VENT_NORMAL), new GT_Cover_Vent(1));
        GregTech_API.registerCover(GT_ModHandler.getIC2Item("reactorVentGold", 1L, 1), TextureFactory.of(Textures.BlockIcons.VENT_ADVANCED), new GT_Cover_Vent(2));
        GregTech_API.registerCover(GT_ModHandler.getIC2Item("reactorVentSpread", 1L), TextureFactory.of(Textures.BlockIcons.VENT_NORMAL), new GT_Cover_Vent(2));
        GregTech_API.registerCover(GT_ModHandler.getIC2Item("reactorVentDiamond", 1L, 1), TextureFactory.of(Textures.BlockIcons.VENT_ADVANCED), new GT_Cover_Vent(3));
    }
}
