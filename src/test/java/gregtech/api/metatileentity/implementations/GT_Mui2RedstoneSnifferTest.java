package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ListWidget;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.covers.CoverContext;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.covers.CoverRedstoneWirelessBase;
import gregtech.common.covers.modes.WirelessRole;
import gregtech.common.items.ItemRedstoneSniffer;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_PortableTank;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GT_Mui2RedstoneSnifferTest {

    @Test
    public void playerInventoryFactoryBuildsSnifferPanelOnServer() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "open", "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GT_ProcessingTestSupport.setField(Loader.instance(), "namedMods", new HashMap<String, ModContainer>());
        GTGuis.init();

        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        EntityPlayer player = new GT_StructureIntegrationTest.TestPlayer(world);
        player.inventory.setInventorySlotContents(0, new ItemStack(new ItemRedstoneSniffer()));
        PlayerInventoryGuiData data = new PlayerInventoryGuiData(player, InventoryTypes.PLAYER, 0);
        PanelSyncManager syncManager = new PanelSyncManager(new ModularSyncManager(false), true);
        ModularPanel panel = GuiFactories.playerInventory().createPanel(
            data,
            syncManager,
            new UISettings(RecipeViewerSettings.DUMMY)
        );

        assertEquals("redstone_sniffer", panel.getName());
        assertTrue(panel.getChildren().get(0).getChildren().get(2) instanceof ListWidget);

        GenericListSyncHandler<?> entries = (GenericListSyncHandler<?>) syncManager.getSyncHandler("regular_map:0");
        assertTrue(entries.getValue().isEmpty());

        GT_StructureIntegrationTest.TestTile tile = new GT_StructureIntegrationTest.TestTile(
            world,
            new GT_MetaTileEntity_PortableTank("sniffer_tank", 1, "", null),
            ForgeDirection.NORTH
        );
        world.putTile(4, 64, 7, tile);
        attachReceiver(tile, ForgeDirection.EAST, WirelessRole.RECEIVER_INTERNAL);
        attachReceiver(tile, ForgeDirection.WEST, WirelessRole.RECEIVER_EXTERNAL);

        assertTrue(entries.updateCacheFromSource(false));
        assertEquals(2, entries.getValue().size());

        tile.detachCover(ForgeDirection.EAST);
        assertTrue(entries.updateCacheFromSource(false));
        assertEquals(1, entries.getValue().size());

        tile.onChunkUnload();
        assertTrue(entries.updateCacheFromSource(false));
        assertTrue(entries.getValue().isEmpty());
    }

    private static void attachReceiver(
        GT_StructureIntegrationTest.TestTile tile,
        ForgeDirection side,
        WirelessRole role
    ) {
        CoverRedstoneWirelessBase receiver = new CoverRedstoneWirelessBase(
            new CoverContext(new ItemStack(Items.stick), side, tile),
            role
        );
        tile.attachCover(receiver);
        receiver.doCoverThings((byte) 0, 1);
    }
}
