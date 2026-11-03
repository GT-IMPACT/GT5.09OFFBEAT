package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.core.mixins.early.minecraft.ContainerAccessor;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.GregTech_API;
import gregtech.api.enchants.Enchantment_Radioactivity;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.api.util.GT_Config;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_Utility;
import gregtech.common.tileentities.machines.multi.GT_MetaTileEntity_LargeTurbine;
import gregtech.common.tools.GT_Tool_Turbine_Huge;
import gregtech.common.tools.GT_Tool_Turbine_Large;
import gregtech.common.tools.GT_Tool_Turbine_Normal;
import gregtech.common.tools.GT_Tool_Turbine_Small;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2TurbineRotorTest {

    private static final String[] MODELS = {"Steam", "HPSteam", "Gas", "Plasma"};
    private static final int[] ROTOR_METADATA = {170, 172, 174, 176};
    private static RotorItem rotorItem;

    @Test
    public void allFourRotorTypesRemainIntactAfterTheExistingCalibrationTicks() throws Throwable {
        run("calibration");
    }

    @Test
    public void nativeGuiInsertsTheFirstRotorAndRejectsAllRemovalClicksImmediately() throws Throwable {
        run("clicks");
    }

    @Test
    public void handlerAndTileApisRejectExtractionAndReplacementOfInstalledRotors() throws Throwable {
        run("handlers");
    }

    @Test
    public void fullTileNbtKeepsTheOriginalItemAndItsLockedNativeSlot() throws Throwable {
        run("persistence");
    }

    @Test
    public void legacyCalibratedMachinesKeepTheirStatsWithoutInventingAMissingRotor() throws Throwable {
        run("legacy");
    }

    @Test
    public void ordinaryItemsInTheControllerSlotRemainRemovable() throws Throwable {
        run("ordinary");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
        FMLServerHandler.instance();
        GT_ProcessingTestSupport.setField(Loader.instance(), "namedMods", new HashMap<String, ModContainer>());
        GT_Config.sConfigFileIDs = GregTech_API.sMachineFile.mConfig;
        new Enchantment_Radioactivity();
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            GTGuis.init();
            rotorItem = new RotorItem();
            switch (scenario) {
                case "calibration":
                    checkCalibration();
                    break;
                case "clicks":
                    checkClicks();
                    break;
                case "handlers":
                    checkHandlers();
                    break;
                case "persistence":
                    checkPersistence();
                    break;
                case "legacy":
                    checkLegacy();
                    break;
                case "ordinary":
                    checkOrdinaryItems();
                    break;
                default:
                    throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkCalibration() throws Exception {
        for (String model : MODELS) {
            for (int metadata : ROTOR_METADATA) {
                Fixture fixture = new Fixture(model);
                ItemStack rotor = rotor(metadata);
                ItemStack expected = rotor.copy();
                int efficiency = GT_Utility.safeInt((long) ((5F + rotorItem.getToolCombatDamage(rotor)) * 1000F));
                int flow = GT_Utility.safeInt((long) Math.max(Float.MIN_NORMAL,
                    rotorItem.getToolStats(rotor).getSpeedMultiplier() * Materials.Steel.mToolSpeed * 50));
                String name = GT_LanguageManager.getTranslation(rotor.getDisplayName())
                    + " | Material Rotor: " + Materials.Steel.mLocalizedName;
                fixture.insert(rotor);
                assertTrue(fixture.machine.checkguislot);
                assertTrue(fixture.machine.isRotorLocked());
                assertFalse(fixture.controller.canTakeStack(fixture.player));
                fixture.machine.onPostTick(fixture.tile, 100);
                assertRotor(expected, fixture.machine.mInventory[1]);
                assertEquals(efficiency, fixture.machine.baseEff);
                assertEquals(flow, fixture.machine.optFlow);
                assertEquals(name, fixture.machine.mNameRotorMaterial);
                assertFalse(fixture.machine.checkguislot);
                fixture.machine.onPostTick(fixture.tile, 200);
                assertRotor(expected, fixture.machine.mInventory[1]);
                assertEquals(efficiency, fixture.machine.baseEff);
                assertEquals(flow, fixture.machine.optFlow);
                assertEquals(name, fixture.machine.mNameRotorMaterial);
                assertTrue(fixture.machine.isRotorLocked());
            }
        }
    }

    private static void checkClicks() throws Exception {
        int[][] clicks = {
            {0, 0}, {1, 0}, {0, 1}, {1, 1}, {0, 2}, {1, 2}, {0, 4}, {1, 4}, {0, 6}, {1, 6}
        };
        for (String model : MODELS) {
            Fixture fixture = new Fixture(model);
            ItemStack rotor = rotor(172);
            ItemStack expected = rotor.copy();
            fixture.insert(rotor);
            assertTrue(fixture.machine.checkguislot);
            assertTrue(fixture.machine.isRotorLocked());
            assertFalse(fixture.controller.canTakeStack(fixture.player));
            assertFalse(fixture.controller.isItemValid(rotor(174)));
            ItemStack hotbar = new ItemStack(Items.apple, 3);
            fixture.player.inventory.mainInventory[0] = hotbar;
            for (int[] click : clicks) {
                ItemStack cursor = click[1] == 6 ? expected.copy() : null;
                fixture.player.inventory.setItemStack(cursor);
                fixture.click(click[0], click[1]);
                assertRotor(expected, fixture.machine.mInventory[1]);
                assertSame(cursor, fixture.player.inventory.getItemStack());
                assertSame(hotbar, fixture.player.inventory.mainInventory[0]);
                assertEquals(3, hotbar.stackSize);
                assertNull(fixture.player.inventory.mainInventory[1]);
                assertEquals(0, countPlayerRotors(fixture));
            }
            for (ItemStack offered : new ItemStack[]{expected.copy(), rotor(174)}) {
                fixture.player.inventory.setItemStack(offered);
                fixture.click(0, 0);
                assertSame(offered, fixture.player.inventory.getItemStack());
                assertRotor(expected, fixture.machine.mInventory[1]);
                fixture.click(1, 0);
                assertSame(offered, fixture.player.inventory.getItemStack());
                assertRotor(expected, fixture.machine.mInventory[1]);
            }
            fixture.player.inventory.setItemStack(null);
            fixture.machine.onPostTick(fixture.tile, 100);
            fixture.click(0, 1);
            assertRotor(expected, fixture.machine.mInventory[1]);
            assertEquals(0, countPlayerRotors(fixture));
        }
    }

    private static void checkHandlers() throws Exception {
        for (String model : MODELS) {
            Fixture fixture = new Fixture(model);
            fixture.insert(rotor(176));
            ItemStack installed = fixture.machine.mInventory[1];
            ItemStack expected = installed.copy();
            ItemStackHandler handler = fixture.machine.getInventoryHandler();
            assertSame(handler, fixture.controller.getItemHandler());
            assertNull(handler.extractItem(1, 1, true));
            assertNull(handler.extractItem(1, 1, false));
            assertNull(fixture.machine.decrStackSize(1, 1));
            assertNull(fixture.tile.decrStackSize(1, 1));
            assertFalse(fixture.machine.isValidSlot(1));
            assertFalse(fixture.controller.canTakeStack(fixture.player));
            for (ItemStack offered : new ItemStack[]{installed.copy(), rotor(170), new ItemStack(Items.book)}) {
                assertSame(offered, handler.insertItem(1, offered, true));
                assertSame(offered, handler.insertItem(1, offered, false));
                handler.setStackInSlot(1, offered);
                fixture.machine.setInventorySlotContents(1, offered);
                fixture.tile.setInventorySlotContents(1, offered);
                assertSame(installed, fixture.machine.mInventory[1]);
                assertRotor(expected, fixture.machine.mInventory[1]);
            }
            handler.setStackInSlot(1, null);
            fixture.machine.setInventorySlotContents(1, null);
            fixture.tile.setInventorySlotContents(1, null);
            assertSame(installed, fixture.machine.mInventory[1]);
            fixture.machine.setBaseMetaTileEntity(null);
            handler.setStackInSlot(1, null);
            fixture.machine.setInventorySlotContents(1, null);
            assertNull(handler.extractItem(1, 1, false));
            assertRotor(expected, fixture.machine.mInventory[1]);
            fixture.machine.setBaseMetaTileEntity(fixture.tile);
            fixture.resources.world.isRemote = true;
            handler.setStackInSlot(1, null);
            assertNull(fixture.machine.mInventory[1]);
            assertFalse(fixture.machine.isRotorLocked());
            ItemStack authoritative = rotor(170);
            fixture.machine.setInventorySlotContents(1, authoritative);
            assertSame(authoritative, fixture.machine.mInventory[1]);
            assertTrue(fixture.machine.isRotorLocked());
        }
    }

    private static void checkPersistence() throws Exception {
        GameRegistry.registerTileEntity(GT_StructureIntegrationTest.TestTile.class, "mui2_turbine_rotor_test");
        IMetaTileEntity previous = GregTech_API.METATILEENTITIES[32000];
        try {
            for (String model : MODELS) {
                Fixture fixture = new Fixture(model);
                ItemStack rotor = rotor(174);
                ItemStack expected = rotor.copy();
                fixture.insert(rotor);
                fixture.machine.onPostTick(fixture.tile, 100);
                int efficiency = fixture.machine.baseEff;
                int flow = fixture.machine.optFlow;
                String name = fixture.machine.mNameRotorMaterial;
                GregTech_API.METATILEENTITIES[32000] = fixture.machine;
                fixture.tile.setMetaTileID((short) 32000);
                NBTTagCompound saved = new NBTTagCompound();
                fixture.tile.writeToNBT(saved);
                BaseMetaTileEntity restoredTile = new BaseMetaTileEntity();
                restoredTile.setWorldObj(fixture.resources.world);
                restoredTile.readFromNBT(saved);
                GT_MetaTileEntity_LargeTurbine restored =
                    (GT_MetaTileEntity_LargeTurbine) restoredTile.getMetaTileEntity();
                assertNotNull(restored);
                assertRotor(expected, restored.mInventory[1]);
                assertTrue(restored.isRotorLocked());
                assertFalse(restored.isValidSlot(1));
                assertNull(restored.getInventoryHandler().extractItem(1, 1, true));
                assertNull(restored.getInventoryHandler().extractItem(1, 1, false));
                assertFalse(restored.checkguislot);
                assertEquals(efficiency, restored.baseEff);
                assertEquals(flow, restored.optFlow);
                assertEquals(name, restored.mNameRotorMaterial);
                restored.mStartUpCheck = restored.mUpdate = 10000;
                restored.onPostTick(restoredTile, 200);
                assertRotor(expected, restored.mInventory[1]);
                assertEquals(efficiency, restored.baseEff);
                assertEquals(flow, restored.optFlow);
            }
        } finally {
            GregTech_API.METATILEENTITIES[32000] = previous;
        }
    }

    private static void checkLegacy() throws Exception {
        for (String model : MODELS) {
            Fixture original = new Fixture(model);
            original.machine.baseEff = 9876;
            original.machine.optFlow = 4321;
            original.machine.checkguislot = false;
            original.machine.mNameRotorMaterial = "Previously consumed legacy rotor";
            NBTTagCompound legacy = new NBTTagCompound();
            original.machine.saveNBTData(legacy);
            Fixture restored = new Fixture(model);
            restored.machine.loadNBTData(legacy);
            restored.machine.onPostTick(restored.tile, 100);
            restored.machine.onPostTick(restored.tile, 200);
            assertNull(restored.machine.mInventory[1]);
            assertFalse(restored.machine.isRotorLocked());
            assertFalse(restored.machine.checkguislot);
            assertEquals(9876, restored.machine.baseEff);
            assertEquals(4321, restored.machine.optFlow);
            assertEquals("Previously consumed legacy rotor", restored.machine.mNameRotorMaterial);
        }
    }

    private static void checkOrdinaryItems() throws Exception {
        for (String model : MODELS) {
            Fixture fixture = new Fixture(model);
            ItemStack item = new ItemStack(Items.book, 3);
            fixture.machine.setInventorySlotContents(1, item);
            assertFalse(fixture.machine.isRotorLocked());
            assertTrue(fixture.machine.isValidSlot(1));
            assertTrue(fixture.controller.canTakeStack(fixture.player));
            fixture.click(0, 0);
            assertNull(fixture.machine.mInventory[1]);
            assertSame(Items.book, fixture.player.inventory.getItemStack().getItem());
            assertEquals(3, fixture.player.inventory.getItemStack().stackSize);
            fixture.player.inventory.setItemStack(null);
            fixture.machine.setInventorySlotContents(1, new ItemStack(Items.book, 4));
            assertEquals(1, fixture.machine.getInventoryHandler().extractItem(1, 1, true).stackSize);
            assertEquals(4, fixture.machine.mInventory[1].stackSize);
            assertEquals(1, fixture.machine.getInventoryHandler().extractItem(1, 1, false).stackSize);
            assertEquals(3, fixture.machine.mInventory[1].stackSize);
            fixture.click(0, 1);
            assertNull(fixture.machine.mInventory[1]);
            int books = 0;
            for (ItemStack stack : fixture.player.inventory.mainInventory) {
                if (stack != null && stack.getItem() == Items.book) {
                    books += stack.stackSize;
                }
            }
            assertEquals(3, books);
        }
    }

    private static ItemStack rotor(int metadata) {
        ItemStack rotor = rotorItem.getToolWithStats(
            metadata,
            1,
            Materials.Steel,
            Materials.Steel,
            null
        );
        assertNotNull(rotor.getTagCompound());
        assertTrue(GT_MetaGenerated_Tool.setToolDamage(rotor, 123));
        rotor.setStackDisplayName("Installed rotor " + metadata);
        NBTTagCompound identity = new NBTTagCompound();
        identity.setString("origin", "Original physical rotor");
        identity.setLong("serial", Long.MAX_VALUE - metadata);
        identity.setByteArray("signature", new byte[]{3, 1, 4, 1, 5});
        rotor.getTagCompound().setTag("RotorIdentity", identity);
        return rotor;
    }

    private static void assertRotor(ItemStack expected, ItemStack actual) {
        assertNotNull(actual);
        assertSame(expected.getItem(), actual.getItem());
        assertEquals(expected.getItemDamage(), actual.getItemDamage());
        assertEquals(1, actual.stackSize);
        assertEquals(expected.getTagCompound(), actual.getTagCompound());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
        assertEquals(123, GT_MetaGenerated_Tool.getToolDamage(actual));
    }

    private static int countPlayerRotors(Fixture fixture) {
        int amount = 0;
        for (ItemStack stack : fixture.player.inventory.mainInventory) {
            if (stack != null && stack.getItem() == rotorItem) {
                amount += stack.stackSize;
            }
        }
        return amount;
    }

    private static ItemSlot controllerWidget(IWidget widget) {
        if (widget.isName("processing_controller")) {
            assertSame(ItemSlot.class, widget.getClass());
            return (ItemSlot) widget;
        }
        for (IWidget child : widget.getChildren()) {
            ItemSlot controller = controllerWidget(child);
            if (controller != null) {
                return controller;
            }
        }
        return null;
    }

    private static final class Fixture {

        private final GT_MetaTileEntity_LargeTurbine machine;
        private final GT_ProcessingTestSupport.Fixture resources;
        private final GT_StructureIntegrationTest.TestTile tile;
        private final GT_StructureIntegrationTest.TestPlayer player;
        private final NativeTestContainer container = new NativeTestContainer();
        private final ModularSlot controller;

        private Fixture(String model) throws Exception {
            machine =
                (GT_MetaTileEntity_LargeTurbine) GT_MultiblockRecipeProcessingTest.machine("LargeTurbine_" + model);
            resources = new GT_ProcessingTestSupport.Fixture(machine, 1);
            tile = resources.tile;
            resources.world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            machine.mStartUpCheck = machine.mUpdate = 10000;
            tile.disableWorking();
            player = new GT_StructureIntegrationTest.TestPlayer(resources.world);
            player.setPosition(4.5, 64.5, 7.5);
            ModularSyncManager sync = new ModularSyncManager(false);
            PanelSyncManager handlers = new PanelSyncManager(sync, true);
            PosGuiData data = new PosGuiData(player, 4, 64, 7);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            ModularPanel panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, handlers, settings);
            WidgetTree.collectSyncValues(handlers, panel);
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            player.openContainer = container;
            ItemSlot widget = controllerWidget(panel);
            assertNotNull(widget);
            controller = widget.getSlot();
            assertEquals(1, controller.getSlotIndex());
            assertEquals(37, container.inventorySlots.size());
            assertTrue(container.canInteractWith(player));
        }

        private void insert(ItemStack rotor) {
            assertNull(machine.mInventory[1]);
            assertFalse(machine.isRotorLocked());
            assertTrue(controller.isItemValid(rotor));
            player.inventory.setItemStack(rotor);
            click(0, 0);
            assertNull(player.inventory.getItemStack());
            assertNotNull(machine.mInventory[1]);
            assertTrue(machine.isRotorLocked());
        }

        private void click(int button, int mode) {
            container.slotClick(controller.slotNumber, button, mode, player);
        }
    }

    /** Supplies the real vanilla drag-state accessor that the isolated runtime does not mix into Container. */
    private static final class NativeTestContainer extends ModularContainer implements ContainerAccessor {

        private final Field dragEvent;

        private NativeTestContainer() throws Exception {
            dragEvent = Container.class.getDeclaredField("field_94536_g");
            dragEvent.setAccessible(true);
        }

        @Override
        public int getDragEvent() {
            try {
                return dragEvent.getInt(this);
            } catch (IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static final class RotorItem extends GT_MetaGenerated_Tool {

        private RotorItem() {
            super("mui2_turbine_rotor_test");
            addTool(170, "Small test rotor", "", new GT_Tool_Turbine_Small());
            addTool(172, "Normal test rotor", "", new GT_Tool_Turbine_Normal());
            addTool(174, "Large test rotor", "", new GT_Tool_Turbine_Large());
            addTool(176, "Huge test rotor", "", new GT_Tool_Turbine_Huge());
        }
    }
}
