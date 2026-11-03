package gregtech.api.metatileentity.implementations;

import codechicken.nei.ItemPanels;
import codechicken.nei.recipe.CheatItemHandler;
import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.IVanillaSlot;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RecipeViewerSettingsImpl;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.screen.viewport.LocatedWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.screen.viewport.TransformationMatrix;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.modularui2.GTModularScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GT_Mui2NeiDragTest {

    @Test
    public void ordinarySlotsDelegateItemAndBookmarkDropsToNeiBeforeMuiConsumesThem() throws Throwable {
        run("ordinary");
    }

    @Test
    public void phantomSlotsAndDisabledNeiNeverUseTheOrdinarySlotCheatHandler() throws Throwable {
        run("phantom");
    }

    @Test
    public void nativeNeiHandlerStillRejectsOutputSlotsAndFilteredItems() throws Throwable {
        run("rejected");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        Launch.classLoader.registerTransformer(GuiAccessTransformer.class.getName());
        GT_ProcessingTestSupport.setField(ModularUI.Mods.NEI, "initialized", true);
        GT_ProcessingTestSupport.setField(ModularUI.Mods.NEI, "loaded", true);
        Fixture fixture = new Fixture();
        if ("ordinary".equals(scenario)) {
            for (boolean bookmark : new boolean[]{false, true}) {
                for (int button = 0; button < 2; button++) {
                    ItemStack dragged = new ItemStack(Items.redstone, 16);
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setString("drag_test", "preserve");
                    dragged.setTagCompound(tag);
                    fixture.hover(new ItemSlot().slot(new ModularSlot(new ItemStackHandler(1), 0)));
                    fixture.drag(dragged, bookmark);
                    fixture.handler.consume = button == 0 ? 16 : 1;
                    int previousCalls = fixture.handler.calls;
                    assertTrue(fixture.screen.onMousePressed(button));
                    assertEquals(previousCalls + 1, fixture.handler.calls);
                    assertSame(fixture.gui, fixture.handler.gui);
                    assertSame(dragged, fixture.handler.stack);
                    assertEquals(button, fixture.handler.button);
                    assertEquals(41, fixture.handler.x);
                    assertEquals(73, fixture.handler.y);
                    assertEquals("preserve", dragged.getTagCompound().getString("drag_test"));
                    if (button == 0) {
                        assertNull(bookmark
                            ? ItemPanels.bookmarkPanel.draggedStack : ItemPanels.itemPanel.draggedStack);
                    } else {
                        assertSame(dragged,
                            bookmark ? ItemPanels.bookmarkPanel.draggedStack : ItemPanels.itemPanel.draggedStack);
                        assertEquals(15, dragged.stackSize);
                    }
                }
            }
        } else if ("phantom".equals(scenario)) {
            ItemStack dragged = new ItemStack(Items.redstone, 16);
            fixture.hover(new PhantomItemSlot().slot(new ModularSlot(new ItemStackHandler(1), 0)));
            fixture.drag(dragged, false);
            assertFalse(fixture.screen.onMousePressed(0));
            assertEquals(0, fixture.handler.calls);
            assertEquals(16, dragged.stackSize);
            fixture.hover(new ItemSlot().slot(new ModularSlot(new ItemStackHandler(1), 0)));
            fixture.settings.disable();
            assertFalse(fixture.screen.onMousePressed(0));
            assertEquals(0, fixture.handler.calls);
            fixture.settings.enable();
            GT_ProcessingTestSupport.setField(ModularUI.Mods.NEI, "loaded", false);
            assertFalse(fixture.screen.onMousePressed(0));
            assertEquals(0, fixture.handler.calls);
        } else if ("rejected".equals(scenario)) {
            GT_ProcessingTestSupport.setField(fixture.screen, "neiCheatHandler", new CheatItemHandler());
            ModularSlot[] rejected = {
                new ModularSlot(new ItemStackHandler(1), 0).accessibility(false, true),
                new ModularSlot(new ItemStackHandler(1), 0).filter(stack -> stack.getItem() == Items.coal)
            };
            for (ModularSlot slot : rejected) {
                ItemStack dragged = new ItemStack(Items.redstone, 16);
                fixture.hover(new ItemSlot().slot(slot));
                fixture.drag(dragged, false);
                assertFalse(fixture.screen.onMousePressed(0));
                assertNull(slot.getStack());
                assertEquals(16, dragged.stackSize);
            }
        } else {
            throw new AssertionError(scenario);
        }
    }

    private static final class Fixture {

        private final GTModularScreen screen = new GTModularScreen(new ModularPanel("nei_drag"), GTGuiThemes.STANDARD);
        private final RecordingCheatHandler handler = new RecordingCheatHandler();
        private final RecipeViewerSettingsImpl settings = new RecipeViewerSettingsImpl();
        private final TestContext context;
        private final TestGui gui;

        private Fixture() throws Exception {
            context = new TestContext(screen);
            gui = new TestGui(screen);
            settings.enable();
            GT_ProcessingTestSupport.setField(context, "settings", new UISettings(settings));
            context.updateState(41, 73, 0);
            GT_ProcessingTestSupport.setField(screen, "context", context);
            GT_ProcessingTestSupport.setField(screen, "screenWrapper", gui);
            // Replace only the external NEI/network boundary; exercise the actual screen input routing.
            GT_ProcessingTestSupport.setField(screen, "neiCheatHandler", handler);
        }

        private void hover(IWidget widget) throws Exception {
            GT_ProcessingTestSupport.setField(context, "hovered",
                Collections.singletonList(new LocatedWidget(widget, new TransformationMatrix(), null)));
            gui.hoveredSlot = widget instanceof IVanillaSlot && ((IVanillaSlot) widget).handleAsVanillaSlot()
                ? ((IVanillaSlot) widget).getVanillaSlot() : null;
        }

        private void drag(ItemStack stack, boolean bookmark) {
            ItemPanels.itemPanel.draggedStack = bookmark ? null : stack;
            ItemPanels.bookmarkPanel.draggedStack = bookmark ? stack : null;
        }
    }

    private static final class TestContext extends ModularGuiContext {

        private TestContext(GTModularScreen screen) {
            super(screen);
        }

        @Override
        public boolean isMouseItemEmpty() {
            return true;
        }
    }

    private static final class TestGui extends GuiContainer implements IMuiScreen {

        private final GTModularScreen screen;
        private Slot hoveredSlot;

        private TestGui(GTModularScreen screen) {
            super(new ModularContainer());
            this.screen = screen;
        }

        @Override
        public GTModularScreen getScreen() {
            return screen;
        }

        // NEI makes this vanilla method public with its access transformer in a running client.
        public Slot getSlotAtPosition(int x, int y) {
            assertEquals(41, x);
            assertEquals(73, y);
            return hoveredSlot;
        }

        @Override
        protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        }
    }

    public static final class GuiAccessTransformer implements IClassTransformer {

        @Override
        public byte[] transform(String name, String transformedName, byte[] bytes) {
            if (!"net.minecraft.client.gui.inventory.GuiContainer".equals(transformedName) || bytes == null) {
                return bytes;
            }
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            for (MethodNode method : node.methods) {
                if ("getSlotAtPosition".equals(method.name)) {
                    method.access = method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
                }
            }
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);
            return writer.toByteArray();
        }
    }

    private static final class RecordingCheatHandler extends CheatItemHandler {

        private int calls;
        private GuiContainer gui;
        private ItemStack stack;
        private int button;
        private int x;
        private int y;
        private int consume;

        @Override
        public boolean handleDragNDrop(
            GuiContainer gui,
            int mouseX,
            int mouseY,
            ItemStack draggedStack,
            int mouseButton
        ) {
            calls++;
            this.gui = gui;
            stack = draggedStack;
            button = mouseButton;
            x = mouseX;
            y = mouseY;
            draggedStack.stackSize -= consume;
            return true;
        }
    }
}
