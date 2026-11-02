package gregtech.common.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.ItemList;
import gregtech.common.items.GT_FluidDisplayItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GT_FluidDisplayStackRenderer implements IItemRenderer {

    public GT_FluidDisplayStackRenderer() {
        MinecraftForgeClient.registerItemRenderer(ItemList.Display_Fluid.getItem(), this);
    }

    @Override
    public boolean handleRenderType (ItemStack item, ItemRenderType type)
    {
        if(!item.hasTagCompound())
            return false;
        return type == ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
    {
        //not sure what this does.
        return false;
    }

    @Override
    public void renderItem (ItemRenderType type, ItemStack item, Object... data) {
        if (item == null || item.getItem() == null || !(item.getItem() instanceof GT_FluidDisplayItem))
            return;

        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);

        IIcon icon = item.getItem().getIconFromDamage(item.getItemDamage());

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // draw a simple rectangle for the inventory icon
        final float x_min = icon.getMinU();
        final float x_max = icon.getMaxU();
        final float y_min = icon.getMinV();
        final float y_max = icon.getMaxV();
        tess.addVertexWithUV( 0, 16, 0, x_min, y_max);
        tess.addVertexWithUV(16, 16, 0, x_max, y_max);
        tess.addVertexWithUV(16,  0, 0, x_max, y_min);
        tess.addVertexWithUV( 0,  0, 0, x_min, y_min);
        tess.draw();

        if(item.getTagCompound() == null) {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            return;
        }

        drawFluidAmount(item.getTagCompound().getLong("mFluidDisplayAmount"), 0, 0, 16);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    /** Draw the original fluid-display label without allowing it to scale with the amount's width. */
    public static void drawFluidAmount(long fluidAmount, int x, int y, int height) {
        if (fluidAmount <= 0 || height <= 0) {
            return;
        }
        FontRenderer fontRender = Minecraft.getMinecraft().fontRenderer;
        float smallTextScale = fontRender.getUnicodeFlag() ? 3F / 4F : 1F / 2F;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glTranslatef(x, y, 0);
            GL11.glScalef(smallTextScale, smallTextScale, 1.0F);
            fontRender.drawString(
                formatFluidAmount(fluidAmount),
                0,
                (int) (height / smallTextScale) - fontRender.FONT_HEIGHT + 1,
                0xFFFFFF,
                true
            );
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    static String formatFluidAmount(long fluidAmount) {
        if (fluidAmount < 10000) {
            return fluidAmount + "L";
        }
        int exp = (int) (Math.log(fluidAmount) / Math.log(1000));
        double shortAmount = fluidAmount / Math.pow(1000, exp);
        if (shortAmount >= 100) {
            return String.format("%.0f%cL", shortAmount, "kMGTPE".charAt(exp - 1));
        }
        if (shortAmount >= 10) {
            return String.format("%.1f%cL", shortAmount, "kMGTPE".charAt(exp - 1));
        }
        return String.format("%.2f%cL", shortAmount, "kMGTPE".charAt(exp - 1));
    }
}
