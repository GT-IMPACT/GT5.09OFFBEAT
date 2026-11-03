package gregtech.api.metatileentity.implementations;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.ResizeNode;
import com.cleanroommc.modularui.widget.sizer.StaticResizer;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;
import net.minecraft.launchwrapper.Launch;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GT_Mui2RecipeSlotLayoutTest {

    @Test
    public void everyInputOutputCombinationIsCenteredAroundTheProgressArrow() throws Throwable {
        run("geometry");
    }

    @Test
    public void fiveInputsKeepTheGtnhTopRightPairAndFullBottomRow() throws Throwable {
        run("five_inputs");
    }

    @Test
    public void incompleteThirdRowsStayAgainstTheProgressArrow() throws Throwable {
        run("incomplete_rows");
    }

    private void run(String scenario) throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), scenario, "");
        }
    }

    public static void runScenario(String scenario, String unused) {
        Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        if ("five_inputs".equals(scenario)) {
            checkFiveInputs();
            return;
        }
        if ("incomplete_rows".equals(scenario)) {
            checkIncompleteRows();
            return;
        }
        for (int inputs = 1; inputs <= 9; inputs++) {
            for (int outputs = 1; outputs <= 9; outputs++) {
                checkGeometry(inputs, outputs);
            }
        }
    }

    private static void checkGeometry(int inputs, int outputs) {
        TestMachine machine = new TestMachine(inputs, outputs);
        InspectableGui gui = new InspectableGui(machine);
        ParentWidget<?> inputSlots = gui.inputs();
        ParentWidget<?> outputSlots = gui.outputs();
        ProgressWidget arrow = gui.arrow();
        ParentWidget<?> content = new ParentWidget<>()
            .pos(0, CommonWidgets.MACHINE_HEADER_HEIGHT)
            .size(176, 184)
            .child(inputSlots)
            .child(arrow)
            .child(outputSlots);
        resizeAreas(content, new Viewport());

        assertEquals(inputs, inputSlots.getChildren().size());
        assertEquals(outputs, outputSlots.getChildren().size());
        Area inputBounds = bounds(inputSlots);
        Area outputBounds = bounds(outputSlots);
        Area progressBounds = arrow.getArea();
        // Slot frame edges leave equal eight-pixel gaps around the twenty-pixel arrow.
        assertEquals(8, progressBounds.x - inputBounds.ex());
        assertEquals(8, outputBounds.x - progressBounds.ex());
        assertEquals(progressBounds.my(), inputBounds.my());
        assertEquals(progressBounds.my(), outputBounds.my());
        assertTrue(inputBounds.y >= CommonWidgets.MACHINE_HEADER_HEIGHT);
        assertTrue(outputBounds.y >= CommonWidgets.MACHINE_HEADER_HEIGHT);
        checkSlots(inputSlots, machine.getInputSlot(), true);
        checkSlots(outputSlots, machine.getOutputSlot(), false);
    }

    private static void checkFiveInputs() {
        InspectableGui gui = new InspectableGui(new TestMachine(5, 1));
        ParentWidget<?> inputSlots = gui.inputs();
        resizeAreas(inputSlots, new Viewport());
        int[][] expectedFrames = {
            {34, 15}, {52, 15}, {16, 33}, {34, 33}, {52, 33}
        };
        for (int index = 0; index < expectedFrames.length; index++) {
            Area frame = inputSlots.getChildren().get(index).getArea();
            assertEquals(expectedFrames[index][0], frame.x);
            assertEquals(expectedFrames[index][1], frame.y);
        }
    }

    private static void checkIncompleteRows() {
        checkThirdRow(7, new int[] {52}, new int[] {106});
        checkThirdRow(8, new int[] {34, 52}, new int[] {106, 124});
    }

    private static void checkThirdRow(int count, int[] expectedInputX, int[] expectedOutputX) {
        InspectableGui gui = new InspectableGui(new TestMachine(count, count));
        ParentWidget<?> inputs = gui.inputs();
        ParentWidget<?> outputs = gui.outputs();
        resizeAreas(inputs, new Viewport());
        resizeAreas(outputs, new Viewport());
        for (int index = 0; index < expectedInputX.length; index++) {
            Area inputFrame = inputs.getChildren().get(6 + index).getArea();
            Area outputFrame = outputs.getChildren().get(6 + index).getArea();
            assertEquals(expectedInputX[index], inputFrame.x);
            assertEquals(expectedOutputX[index], outputFrame.x);
            assertEquals(51, inputFrame.y);
            assertEquals(51, outputFrame.y);
        }
    }

    private static void checkSlots(ParentWidget<?> group, int firstIndex, boolean canPut) {
        for (int index = 0; index < group.getChildren().size(); index++) {
            ItemSlot slot = (ItemSlot) group.getChildren().get(index);
            assertEquals(18, slot.getArea().width);
            assertEquals(18, slot.getArea().height);
            assertEquals(firstIndex + index, slot.getSlot().getSlotIndex());
            assertEquals(canPut, slot.getSlot().isCanPut());
            assertTrue(slot.getSlot().isCanTake());
            for (int other = index + 1; other < group.getChildren().size(); other++) {
                assertFalse(slot.getArea().intersects(group.getChildren().get(other).getArea()));
            }
        }
    }

    private static Area bounds(ParentWidget<?> group) {
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (IWidget slot : group.getChildren()) {
            Area frame = slot.getArea();
            left = Math.min(left, frame.x);
            top = Math.min(top, frame.y);
            right = Math.max(right, frame.ex());
            bottom = Math.max(bottom, frame.ey());
        }
        return new Area(left, top, right - left, bottom - top);
    }

    /** Run MUI's real sizing and absolute-position stages without a renderer or client screen. */
    static void resizeAreas(IWidget widget) {
        resizeAreas(widget, new Viewport());
    }

    private static void resizeAreas(IWidget widget, ResizeNode parent) {
        ResizeNode resizer = widget.resizer();
        resizer.initialize(parent, parent);
        resizer.initResizing(false);
        assertTrue(resizer.resize(false));
        resizer.preApplyPos();
        for (IWidget child : widget.getChildren()) {
            resizeAreas(child, resizer);
        }
    }

    private static final class Viewport extends StaticResizer {

        private final Area area = new Area(0, 0, 176, 220);

        @Override
        public Area getArea() {
            return area;
        }

        @Override
        public String getDebugDisplayName() {
            return "Recipe slot layout viewport";
        }

        @Override
        public String toString() {
            return getDebugDisplayName();
        }
    }

    private static final class InspectableGui extends MTEBasicMachineBaseGui<TestMachine> {

        private InspectableGui(TestMachine machine) {
            super(machine, false, false);
        }

        private ParentWidget<?> inputs() {
            return createItemInputSlots(null);
        }

        private ParentWidget<?> outputs() {
            return createItemOutputSlots(null);
        }

        private ProgressWidget arrow() {
            return createProgressBar(null);
        }
    }

    private static final class TestMachine extends GT_MetaTileEntity_BasicMachine {

        private TestMachine(int inputs, int outputs) {
            super(
                "mui2_layout",
                1,
                1,
                "",
                null,
                inputs,
                outputs,
                "",
                ""
            );
        }

        @Override
        public MetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
            return new TestMachine(mInputSlotCount, mOutputItems.length);
        }
    }
}
