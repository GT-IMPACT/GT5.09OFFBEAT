package gregtech.loaders.misc;

import gregtech.api.GregTech_API;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.enums.Textures;
import gregtech.api.objects.GT_CopiedBlockTexture;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_ModHandler;
import gregtech.common.covers.CoverVent;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class GT_CoverLoader implements Runnable {
    @Override
    public void run() {
        ItemStack tStack;
        for (byte i = 0; i < 16; i = (byte) (i + 1)) {
            tStack = new ItemStack(Blocks.wool, 0, i);
            CoverRegistry.registerDecorativeCover(new ItemStack(Blocks.carpet, 1, i),
                    TextureFactory.of(Block.getBlockFromItem(tStack.getItem()), tStack.getItemDamage()));
        }
        CoverRegistry.registerCover(GT_ModHandler.getIC2Item("reactorVent", 1L, 1),
                TextureFactory.of(Textures.BlockIcons.VENT_NORMAL), context -> new CoverVent(context, 1));
        CoverRegistry.registerCover(GT_ModHandler.getIC2Item("reactorVentCore", 1L, 1),
                TextureFactory.of(Textures.BlockIcons.VENT_NORMAL), context -> new CoverVent(context, 1));
        CoverRegistry.registerCover(GT_ModHandler.getIC2Item("reactorVentGold", 1L, 1),
                TextureFactory.of(Textures.BlockIcons.VENT_ADVANCED), context -> new CoverVent(context, 2));
        CoverRegistry.registerCover(GT_ModHandler.getIC2Item("reactorVentSpread", 1L),
                TextureFactory.of(Textures.BlockIcons.VENT_NORMAL), context -> new CoverVent(context, 2));
        CoverRegistry.registerCover(GT_ModHandler.getIC2Item("reactorVentDiamond", 1L, 1),
                TextureFactory.of(Textures.BlockIcons.VENT_ADVANCED), context -> new CoverVent(context, 3));
    }
}
