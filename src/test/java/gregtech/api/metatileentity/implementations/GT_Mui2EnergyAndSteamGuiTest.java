package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.RecipeViewerSettings;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import cpw.mods.fml.server.FMLServerHandler;
import gregtech.api.enums.TieredVariant;
import gregtech.api.interfaces.ITexture;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.examples.GT_MetaTileEntity_E_Furnace;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.modularui2.MetaTileEntityGuiHandler;
import gregtech.common.tileentities.boilers.GT_MetaTileEntity_Boiler;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2EnergyAndSteamGuiTest {

    @Test
    public void generatorFamiliesKeepFuelContainersAndNativeFactoryRouting() throws Throwable {
        run("generators");
    }

    @Test
    public void bronzeAndSteelSteamMachinesKeepRecipeSlotsAndTheirSteamPresentation() throws Throwable {
        run("steam");
    }

    @Test
    public void boilersKeepAllFourSlotsAndReadOnlyLiveWaterAndSteamGauges() throws Throwable {
        run("boilers");
    }

    @Test
    public void electricFurnaceUsesNativeUiAndStillConsumesItsActualSmeltingInput() throws Throwable {
        run("electric");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Thread thread = Thread.currentThread();
        String previousName = thread.getName();
        thread.setName("Server thread");
        try {
            Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
            Launch.classLoader.registerTransformer("cpw.mods.fml.common.asm.transformers.EventSubscriptionTransformer");
            FMLServerHandler.instance();
            GTGuis.init();
            assertSame(MetaTileEntityGuiHandler.INSTANCE, GuiManager.getFactory("gregtech:mte"));
            switch (scenario) {
                case "generators":
                    checkGenerators();
                    break;
                case "steam":
                    checkSteam();
                    break;
                case "boilers":
                    checkBoilers();
                    break;
                case "electric":
                    checkElectricFurnace();
                    break;
                default:
                    throw new AssertionError(scenario);
            }
        } finally {
            thread.setName(previousName);
        }
    }

    private static void checkGenerators() throws Exception {
        String[] models = {
            "DieselGenerator", "GasTurbine", "SteamTurbine", "PlasmaGenerator", "MagicEnergyConverter",
            "FluidNaquadahReactor", "SolidNaquadahReactor", "SolidNaquadahReactor2", "SolidNaquadahReactor3",
            "SolidNaquadahReactor4", "SolidNaquadahReactor5", "SolidNaquadahReactor6", "SolidNaquadahReactor7"
        };
        for (String model : models) {
            GT_MetaTileEntity_BasicGenerator machine = (GT_MetaTileEntity_BasicGenerator) construct(
                "generators",
                model,
                false
            );
            Fixture fixture = new Fixture(machine);
            fixture.build(new int[]{0, 1});
            assertTrue(fixture.nativeSlots[0].isCanPut());
            assertFalse(fixture.nativeSlots[1].isCanPut());
            assertNull(fixture.nativeSlots[2]);
            ItemStack fuel = new ItemStack(Items.coal, 3);
            fixture.nativeSlots[0].putStack(fuel);
            assertSame(fuel, machine.mInventory[0]);
            assertSame(fuel, fixture.nativeSlots[0].getStack());
            fixture.checkRightClicks();
        }
    }

    private static void checkSteam() throws Exception {
        String[] models = {"AlloySmelter", "Compressor", "Extractor", "ForgeHammer", "Furnace", "Macerator"};
        for (String variant : new String[]{"Bronze", "Steel"}) {
            for (String model : models) {
                GT_MetaTileEntity_BasicMachine_Bronze machine = (GT_MetaTileEntity_BasicMachine_Bronze) construct(
                    "machines.steam",
                    model + "_" + variant,
                    true
                );
                Fixture fixture = new Fixture(machine);
                fixture.build("AlloySmelter".equals(model) ? new int[]{4, 5, 6} : new int[]{4, 5});
                assertFalse(machine.allowSelectCircuit());
                assertNull(fixture.nativeSlots[1]);
                assertNull(fixture.nativeSlots[3]);
                assertEquals(0, fluidSlots(fixture.panel).size());
                TieredVariant tieredVariant = "Steel".equals(variant) ? TieredVariant.STEEL : TieredVariant.BRONZE;
                assertSame(GTGuiThemes.TIERED_VARIANTS.get(tieredVariant), machine.getGuiTheme());
                if ("ForgeHammer".equals(model)) {
                    assertSame(
                        GTGuiTextures.PROGRESSBAR_HAMMER_STEAM.get(tieredVariant),
                        machine.getGuiProgressTexture()
                    );
                    assertEquals(6, machine.getGuiProgressDirection());
                    assertEquals(3, machine.getGuiProgressSegments());
                }
                if ("Furnace".equals(model)) {
                    checkSmelting(machine);
                }
                fixture.checkRightClicks();
            }
        }
    }

    private static void checkBoilers() throws Exception {
        String[] models = {"Boiler_Bronze", "Boiler_Steel", "Boiler_Lava", "Boiler_Solar", "Boiler_Solar_Steel"};
        for (String model : models) {
            GT_MetaTileEntity_Boiler machine = (GT_MetaTileEntity_Boiler) construct("boilers", model, false);
            machine.mFluid = new FluidStack(GT_ProcessingTestSupport.fluid("mui2_boiler_water"), 8000);
            machine.mSteam = new FluidStack(GT_ProcessingTestSupport.fluid("mui2_boiler_steam"), 4000);
            machine.mTemperature = 250;
            machine.mProcessingEnergy = 160;
            Fixture fixture = new Fixture(machine);
            fixture.build(new int[]{0, 1, 2, 3});
            assertTrue(fixture.nativeSlots[0].isCanPut());
            assertTrue(fixture.nativeSlots[2].isCanPut());
            assertFalse(fixture.nativeSlots[1].isCanPut());
            assertFalse(fixture.nativeSlots[3].isCanPut());
            assertNotNull(fixture.panelSync.findSyncHandler("heat"));
            List<FluidSlot> gauges = fluidSlots(fixture.panel);
            assertEquals(2, gauges.size());
            for (FluidSlot gauge : gauges) {
                FluidStack stored = gauge.getFluidTank().getFluid();
                assertTrue(stored == machine.mFluid || stored == machine.mSteam);
                assertEquals(0, gauge.getFluidTank().fill(stored.copy(), true));
                assertNull(gauge.getFluidTank().drain(1000, true));
                assertFalse(gauge.getSyncHandler().canDrainSlot());
                assertFalse(gauge.getSyncHandler().canFillSlot());
                assertFalse(gauge.getSyncHandler().controlsAmount());
            }
            assertEquals(8000, machine.mFluid.amount);
            assertEquals(4000, machine.mSteam.amount);
            machine.mSteam.amount = 50000;
            for (FluidSlot gauge : gauges) {
                assertTrue(gauge.getFluidTank().getFluidAmount() <= gauge.getFluidTank().getCapacity());
            }
            assertEquals(50000, machine.mSteam.amount);
            ItemStack fuel = new ItemStack(Items.lava_bucket);
            fixture.nativeSlots[2].putStack(fuel);
            assertSame(fuel, machine.mInventory[2]);
            fixture.checkRightClicks();
        }
    }

    private static void checkElectricFurnace() throws Exception {
        GT_MetaTileEntity_E_Furnace machine = new GT_MetaTileEntity_E_Furnace(
            "mui2_e_furnace",
            1,
            "",
            null,
            "E_Furnace.png",
            "smelting"
        );
        Fixture fixture = new Fixture(machine);
        fixture.build(new int[]{1, 3, 4, 5});
        ItemStack special = new ItemStack(Items.book);
        fixture.nativeSlots[3].putStack(special);
        assertSame(special, machine.getSpecialSlot());
        checkSmelting(machine);
        fixture.checkRightClicks();
    }

    private static void checkSmelting(GT_MetaTileEntity_BasicMachine machine) {
        machine.setInventorySlotContents(machine.getInputSlot(), new ItemStack(Blocks.iron_ore, 2));
        assertEquals(GT_MetaTileEntity_BasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, machine.checkRecipe());
        assertEquals(1, machine.getInputAt(0).stackSize);
        assertSame(Items.iron_ingot, machine.mOutputItems[0].getItem());
    }

    private static MetaTileEntity construct(String family, String model, boolean steam) throws Exception {
        Class<?> type = Class.forName("gregtech.common.tileentities." + family + ".GT_MetaTileEntity_" + model);
        if (steam) {
            Constructor<?> constructor = type.getConstructor(String.class, String[].class, ITexture[][][].class);
            return (MetaTileEntity) constructor.newInstance("mui2_" + model, new String[]{""}, null);
        }
        Constructor<?> constructor = type.getConstructor(String.class, int.class, String.class, ITexture[][][].class);
        return (MetaTileEntity) constructor.newInstance("mui2_" + model, 1, "", null);
    }

    private static List<FluidSlot> fluidSlots(IWidget widget) {
        List<FluidSlot> slots = new ArrayList<>();
        if (widget instanceof FluidSlot) {
            slots.add((FluidSlot) widget);
        }
        for (IWidget child : widget.getChildren()) {
            slots.addAll(fluidSlots(child));
        }
        return slots;
    }

    private static final class Fixture {

        private final GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        private final MetaTileEntity machine;
        private final GT_StructureIntegrationTest.TestTile tile;
        private final NoLegacyGuiPlayer player;
        private ModularPanel panel;
        private PanelSyncManager panelSync;
        private ModularSlot[] nativeSlots;

        private Fixture(MetaTileEntity machine) throws Exception {
            this.machine = machine;
            tile = new GT_StructureIntegrationTest.TestTile(world, machine, ForgeDirection.NORTH);
            world.putTile(4, 64, 7, tile);
            GT_ProcessingTestSupport.setField(tile, "mTickTimer", 51L);
            player = new NoLegacyGuiPlayer(world);
            player.setPosition(4.5, 64.5, 7.5);
        }

        private void build(int[] visibleIndices) {
            assertTrue(machine.getClass().getSimpleName(), machine.useMui2());
            PosGuiData data = new PosGuiData(player, 4, 64, 7);
            assertSame(machine, MetaTileEntityGuiHandler.INSTANCE.getGuiHolder(data));
            ModularSyncManager sync = new ModularSyncManager(false);
            panelSync = new PanelSyncManager(sync, true);
            UISettings settings = new UISettings(RecipeViewerSettings.DUMMY);
            panel = MetaTileEntityGuiHandler.INSTANCE.createPanel(data, panelSync, settings);
            WidgetTree.collectSyncValues(panelSync, panel);
            ModularContainer container = new ModularContainer();
            container.construct(
                player,
                sync,
                settings,
                panel.getName(),
                data
            );
            assertTrue(container.isInitialized());
            assertTrue(container.canInteractWith(player));
            assertEquals(36 + visibleIndices.length, container.inventorySlots.size());
            assertNotNull(panelSync.findSyncHandler("powerSwitch"));
            nativeSlots = new ModularSlot[machine.mInventory.length];
            for (Object entry : container.inventorySlots) {
                ModularSlot slot = (ModularSlot) entry;
                if (slot.getItemHandler() == machine.getInventoryHandler()) {
                    assertNull(nativeSlots[slot.getSlotIndex()]);
                    nativeSlots[slot.getSlotIndex()] = slot;
                }
            }
            for (int index : visibleIndices) {
                assertNotNull(machine.getClass().getSimpleName() + " native slot " + index, nativeSlots[index]);
                assertSame(machine.mInventory[index], nativeSlots[index].getStack());
                assertTrue(nativeSlots[index].isCanTake());
            }
        }

        private void checkRightClicks() {
            for (byte side = 0; side < 6; side++) {
                assertTrue(tile.onRightclick(
                    player,
                    side,
                    0.5F,
                    0.5F,
                    0.5F
                ));
            }
            world.isRemote = true;
            try {
                assertTrue(machine.onRightclick(tile, player));
                assertTrue(GTGuis.open(player, machine));
            } finally {
                world.isRemote = false;
            }
        }
    }

    private static final class NoLegacyGuiPlayer extends GT_StructureIntegrationTest.TestPlayer {

        private NoLegacyGuiPlayer(World world) {
            super(world);
        }

        @Override
        public void openGui(
            Object mod,
            int guiId,
            World world,
            int x,
            int y,
            int z
        ) {
            throw new AssertionError("A migrated machine attempted to open its legacy GUI");
        }
    }
}
