package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.api.util.GT_Utility;
import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;
import gregtech.common.tileentities.machines.basic.GT_MetaTileEntity_Teleporter;
import net.minecraft.util.StatCollector;

/** GTNH's coordinate input panel, adapted to this branch's machine fields. */
public class MTETeleporterGui extends MTETieredMachineBlockBaseGui<GT_MetaTileEntity_Teleporter> {

    public MTETeleporterGui(GT_MetaTileEntity_Teleporter machine) {
        super(machine);
    }

    @Override
    protected boolean supportsTopRightCornerFlow() {
        return false;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        IntSyncValue xSyncer = new IntSyncValue(() -> machine.mTargetX, value -> {
            machine.mTargetX = value;
            GTGuis.markDirty(machine);
        }).allowC2S();
        IntSyncValue ySyncer = new IntSyncValue(() -> machine.mTargetY, value -> {
            machine.mTargetY = value;
            GTGuis.markDirty(machine);
        }).allowC2S();
        IntSyncValue zSyncer = new IntSyncValue(() -> machine.mTargetZ, value -> {
            machine.mTargetZ = value;
            GTGuis.markDirty(machine);
        }).allowC2S();
        IntSyncValue dimSyncer = new IntSyncValue(() -> machine.mTargetD, value -> {
            machine.mTargetD = value;
            GTGuis.markDirty(machine);
        }).allowC2S();

        Flow mainColumn = Flow.column()
            .coverChildren()
            .childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.START);
        mainColumn.child(createInputRow("x", xSyncer, false));
        mainColumn.child(createInputRow("y", ySyncer, false));
        mainColumn.child(createInputRow("z", zSyncer, false));
        mainColumn.child(createInputRow("dim", dimSyncer, true));
        return super.createContentSection(panel, syncManager).child(mainColumn);
    }

    private Flow createInputRow(String key, IntSyncValue value, boolean isDimRow) {
        Flow row = Flow.row()
            .coverChildren()
            .childPadding(3)
            .collapseDisabledChild();
        row.child(
            IKey.lang("GT5U.gui.text." + key)
                .asWidget()
                .width(15)
                .textAlign(Alignment.END));
        row.child(
            new TextFieldWidget().value(value)
                .formatAsInteger(true)
                .numbersInt()
                .setMaxLength(11)
                .size(80, 12));
        row.childIf(
            isDimRow && machine.hasDimensionalTeleportCapability(),
            () -> GTGuiTextures.OVERLAY_BUTTON_CHECKMARK.asWidget()
                .setEnabledIf(widget -> GT_Utility.isRealDimension(value.getIntValue()))
                .tooltip(tooltip -> tooltip.addLine(StatCollector.translateToLocal("GT5U.gui.text.dim.valid")))
                .decoration());
        row.childIf(
            isDimRow && machine.hasDimensionalTeleportCapability(),
            () -> GTGuiTextures.OVERLAY_BUTTON_CROSS.asWidget()
                .setEnabledIf(widget -> !GT_Utility.isRealDimension(value.getIntValue()))
                .tooltip(tooltip -> tooltip.addLine(StatCollector.translateToLocal("GT5U.gui.text.dim.invalid")))
                .decoration());
        return row;
    }
}
