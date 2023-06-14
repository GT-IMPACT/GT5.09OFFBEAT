package gregtech.api.gui.modularui;

import com.gtnewhorizons.modularui.api.screen.ITileWithModularUI;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.builder.UIBuilder;
import com.gtnewhorizons.modularui.common.builder.UIInfo;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IHasWorldObjectAndCoords;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import java.util.function.Function;

class GTUIInfo {
    
    public static final Function<ContainerConstructor, UIInfo<?, ?>> GTTileEntityUIFactory =
            containerConstructor -> UIBuilder.of()
                    .container((player, world, x, y, z) -> {
                        TileEntity te = world.getTileEntity(x, y, z);
                        if (te instanceof ITileWithModularUI) {
                            return createTileEntityContainer(
                                    player,
                                    ((ITileWithModularUI) te)::createWindow,
                                    te::markDirty,
                                    containerConstructor);
                        }
                        return null;
                    })
                    .gui(((player, world, x, y, z) -> {
                        if (!world.isRemote) return null;
                        TileEntity te = world.getTileEntity(x, y, z);
                        if (te instanceof ITileWithModularUI) {
                            return createTileEntityGuiContainer(
                                    player, ((ITileWithModularUI) te)::createWindow, containerConstructor);
                        }
                        return null;
                    }))
                    .build();
    
    private static final UIInfo<?, ?> GTTileEntityDefaultUI = GTTileEntityUIFactory.apply(ModularUIContainer::new);
    
    public static void openGTTileEntityUI(IHasWorldObjectAndCoords aTileEntity, EntityPlayer aPlayer) {
        if (aTileEntity.isClientSide()) return;
        GTTileEntityDefaultUI.open(
                aPlayer,
                aTileEntity.getWorld(),
                aTileEntity.getXCoord(),
                aTileEntity.getYCoord(),
                aTileEntity.getZCoord());
    }
    
    private static ModularUIContainer createTileEntityContainer(
            EntityPlayer player,
            Function<UIBuildContext, ModularWindow> windowCreator,
            Runnable onWidgetUpdate,
            ContainerConstructor containerCreator) {
        UIBuildContext buildContext = new UIBuildContext(player);
        ModularWindow window = windowCreator.apply(buildContext);
        if (window == null) {
            return null;
        }
        return containerCreator.of(new ModularUIContext(buildContext, onWidgetUpdate), window);
    }
    
    @SideOnly(Side.CLIENT)
    private static ModularGui createTileEntityGuiContainer(
            EntityPlayer player,
            Function<UIBuildContext, ModularWindow> windowCreator,
            ContainerConstructor containerConstructor) {
        ModularUIContainer container = createTileEntityContainer(player, windowCreator, null, containerConstructor);
        if (container == null) {
            return null;
        }
        return new ModularGui(container);
    }
    
    @FunctionalInterface
    public interface ContainerConstructor {
        ModularUIContainer of(ModularUIContext context, ModularWindow mainWindow);
    }
}
