package gregtech.common.gui.modularui.singleblock;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.gui.modularui.singleblock.base.MTESpecialFilterBaseGui;
import gregtech.common.tileentities.automation.GT_MetaTileEntity_TypeFilter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MTETypeFilterGui extends MTESpecialFilterBaseGui<GT_MetaTileEntity_TypeFilter> {

    public MTETypeFilterGui(GT_MetaTileEntity_TypeFilter machine) {
        super(machine);
    }

    @Override
    protected BooleanSyncValue createInvertFilterSyncValue() {
        return new BooleanSyncValue(() -> machine.bInvertFilter, value -> {
            machine.bInvertFilter = value;
            GTGuis.markDirty(machine);
        }).allowC2S();
    }

    @Override
    protected BooleanSyncValue createAllowNbtSyncValue() {
        return new BooleanSyncValue(() -> machine.bNBTAllowed, value -> {
            machine.bNBTAllowed = value;
            GTGuis.markDirty(machine);
        }).allowC2S();
    }

    @Override
    protected List<String> getEmptyFilterSlotTooltip(ModularPanel panel, PanelSyncManager syncManager) {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getFilledFilterSlotTooltip(ModularPanel panel, PanelSyncManager syncManager) {
        OrePrefixes prefix = OrePrefixes.getPrefix(
            syncManager.findSyncHandler("orePrefix", StringSyncValue.class).getValue(), OrePrefixes.ore);
        List<String> tooltip = new ArrayList<>();
        tooltip.add(StatCollector.translateToLocalFormatted(
            "GT5U.tooltip.typefilter.set_to", prefix.mRegularLocalName));
        tooltip.add(StatCollector.translateToLocalFormatted(
            "GT5U.tooltip.typefilter.ore_prefix", "§e" + prefix + "§r"));
        tooltip.add(StatCollector.translateToLocalFormatted(
            "GT5U.tooltip.typefilter.size", "§e" + prefix.mPrefixedItems.size() + "§r"));
        tooltip.add(StatCollector.translateToLocal("GT5U.type_filter.representation_slot.tooltip"));
        tooltip.add(StatCollector.translateToLocal("GT5U.type_filter.representation_slot.tooltip.1"));
        return tooltip;
    }

    @Override
    protected ItemSlot createFilterSlotBase(ModularPanel panel, PanelSyncManager syncManager) {
        IntSyncValue rotationIndexSyncer = syncManager.findSyncHandler("rotationIndex", IntSyncValue.class);
        StringSyncValue prefixSyncer = syncManager.findSyncHandler("orePrefix", StringSyncValue.class);
        return new PhantomItemSlot() {

            @Override
            public PhantomItemSlot slot(ModularSlot slot) {
                return syncHandler(new PhantomItemSlotSH(slot) {

                    @Override
                    protected void phantomClick(MouseData mouseData, ItemStack cursorStack) {
                        machine.clickTypeIcon(mouseData.mouseButton != 0);
                        rotationIndexSyncer.setIntValue(machine.mRotationIndex);
                        prefixSyncer.setValue(machine.mPrefix.toString());
                        GTGuis.markDirty(machine);
                    }

                    @Override
                    protected void phantomScroll(MouseData mouseData) {
                    }
                });
            }
        };
    }

    @Override
    protected boolean supportsStocking() {
        return false;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue("rotationIndex", new IntSyncValue(() -> machine.mRotationIndex, value -> {
            machine.mRotationIndex = Math.max(-1, value);
            GTGuis.markDirty(machine);
        }).allowC2S());
        syncManager.syncValue("orePrefix", new StringSyncValue(() -> machine.mPrefix.toString(), value -> {
            OrePrefixes prefix = OrePrefixes.getPrefix(value, machine.mPrefix);
            if (prefix.mPrefixInto == prefix && !prefix.mPrefixedItems.isEmpty()) {
                machine.mPrefix = prefix;
                GTGuis.markDirty(machine);
            }
        }).allowC2S());
    }
}
