package gregtech.api.metatileentity.implementations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;

import org.junit.Test;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.asm.transformers.ModAPITransformer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.asm.ASMModParser;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.enums.ToolboxSlot;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.items.ItemGTToolbox;
import gregtech.common.items.toolbox.ToolboxItemStackHandler;
import gregtech.common.items.toolbox.ToolboxUtil;

public class GT_Mui2ToolboxTest {

    @Test
    public void inventoryLocksHeldToolboxAndPersistsOnlyRealChanges() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "open", "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GT_ProcessingTestSupport.setField(Loader.instance(), "namedMods", new HashMap<String, ModContainer>());

        Launch.classLoader.registerTransformer(ModAPITransformer.class.getName());
        ASMDataTable optionalApis = new ASMDataTable();
        try (InputStream bytes = Launch.classLoader.getResourceAsStream("gregtech/common/items/ItemGTToolbox.class")) {
            new ASMModParser(bytes).sendToTable(optionalApis, null);
        }
        for (Object transformer : Launch.classLoader.getTransformers()) {
            if (transformer instanceof ModAPITransformer) {
                ((ModAPITransformer) transformer).initTable(optionalApis);
            }
        }

        GTGuis.init();
        EntityPlayer player = new GT_StructureIntegrationTest.TestPlayer(new GT_ProcessingTestSupport.ProcessingWorld());
        ItemStack toolbox = new ItemStack(new ItemGTToolbox("test_toolbox", "Test Toolbox", ""));
        int hotbarSlot = 3;
        player.inventory.currentItem = hotbarSlot;
        player.inventory.setInventorySlotContents(hotbarSlot, toolbox);

        PlayerInventoryGuiData data = new PlayerInventoryGuiData(player, InventoryTypes.PLAYER, hotbarSlot);
        ModularSyncManager sync = new ModularSyncManager(false);
        PanelSyncManager handlers = new PanelSyncManager(sync, true);
        UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
        settings.defaultCanInteractWith(GuiFactories.playerInventory(), data);
        ModularPanel panel = GuiFactories.playerInventory().createPanel(data, handlers, settings);
        WidgetTree.collectSyncValues(handlers, panel);
        ModularContainer container = new ModularContainer();
        container.construct(player, sync, settings, panel.getName(), data);
        player.openContainer = container;

        ModularSlot wrench = findSlot(container, ToolboxSlot.WRENCH.getSlotID(), false);
        ModularSlot general = findSlot(container, ToolboxSlot.GENERIC_SLOT0.getSlotID(), false);
        ModularSlot held = findSlot(container, hotbarSlot, true);
        ItemStack sticks = new ItemStack(Items.stick, 8);
        assertEquals(50, container.inventorySlots.size());
        assertTrue(ItemGTToolbox.isInventoryOpen(toolbox));
        assertFalse(wrench.isItemValid(sticks));
        assertFalse(general.isItemValid(toolbox));
        assertFalse(held.canTakeStack(player));
        assertFalse(held.isItemValid(sticks));

        assertTrue(general.isItemValid(sticks));
        general.putStack(sticks);
        assertTrue(general.isItemValid(sticks));
        assertEquals(64, general.getItemStackLimit(sticks));
        assertSame(sticks, general.getStack());
        assertEquals(8, ToolboxUtil.getItemInside(toolbox, ToolboxSlot.GENERIC_SLOT0).get().stackSize);
        assertEquals(8, general.decrStackSize(8).stackSize);
        assertNull(general.getStack());
        assertFalse(ToolboxUtil.getItemInside(toolbox, ToolboxSlot.GENERIC_SLOT0).isPresent());
    }

    private static ModularSlot findSlot(ModularContainer container, int index, boolean playerSlot) {
        for (Object entry : container.inventorySlots) {
            Slot slot = (Slot) entry;
            if (slot instanceof ModularSlot) {
                ModularSlot modular = (ModularSlot) slot;
                boolean toolboxSlot = modular.getItemHandler() instanceof ToolboxItemStackHandler;
                if (slot.getSlotIndex() == index && toolboxSlot != playerSlot) {
                    return modular;
                }
            }
        }
        throw new AssertionError("Missing container slot " + index);
    }
}
