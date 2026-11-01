package gregtech.api.metatileentity.implementations;

import com.google.common.collect.ImmutableBiMap;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModMetadata;
import gregtech.api.util.GTStructureChannels;
import gregtech.api.util.MultiblockTooltipBuilder;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringTranslate;
import space.impact.structures.ChannelDescription;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.structure.StructureDefinition;
import space.impact.structures.structure.IStructureDefinition;

import static org.junit.Assert.*;

final class GT_StructureChannelChecks {
    static void run() throws Exception {
        Field mods = Loader.class.getDeclaredField("namedMods");
        mods.setAccessible(true);
        Object previous = mods.get(Loader.instance());
        ModMetadata metadata = new ModMetadata();
        metadata.modId = "gregtech";
        try {
            mods.set(Loader.instance(), ImmutableBiMap.of("gregtech", new DummyModContainer(metadata)));
            GTStructureChannels.registerDescriptions();
            for (String language : new String[] { "ru_RU", "en_US" }) {
                try (InputStream stream = GT_StructureChannelChecks.class.getResourceAsStream(
                        "/assets/gregtech/lang/" + language + ".lang")) {
                    StringTranslate.inject(stream);
                }
                for (String channel : Arrays.asList(GTStructureChannels.HATCH, GTStructureChannels.WIDTH,
                        GTStructureChannels.DEPTH, GTStructureChannels.HEIGHT, GTStructureChannels.LENGTH,
                        GTStructureChannels.COIL, GTStructureChannels.TIER, GTStructureChannels.VARIANT)) {
                    assertTrue(channel, ChannelDescription.has(channel));
                    String key = ChannelDescription.get(channel).getDescriptions().get("gregtech");
                    assertNotNull(channel, key);
                    assertTrue(key, StatCollector.canTranslate(key));
                    assertEquals(StatCollector.translateToLocal(key), GTStructureChannels.getDescription(channel));
                }
            }
        } finally {
            mods.set(Loader.instance(), previous);
        }
        ItemStack trigger = new ItemStack(Items.stick, 8);
        ChannelDataAccessor.setChannelData(trigger, GTStructureChannels.HEIGHT, 3);
        ChannelDataAccessor.setChannelData(trigger, GTStructureChannels.COIL, 6);
        assertEquals(3, GTStructureChannels.getValue(trigger, GTStructureChannels.HEIGHT));
        assertEquals(6, GTStructureChannels.getValue(trigger, GTStructureChannels.COIL));
        assertEquals(8, GTStructureChannels.getValue(trigger, GTStructureChannels.WIDTH));
        assertEquals(1, GTStructureChannels.getValue(null, GTStructureChannels.LENGTH));
        assertEquals(8, trigger.stackSize);
        IStructureDefinition<Object> definition = StructureDefinition.<Object>builder()
                .addChannel(GTStructureChannels.HEIGHT, 1, 12)
                .addChannel(GTStructureChannels.HATCH, 1, 1).build();
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder()
                .addSubChannel(GTStructureChannels.HEIGHT, "Height = value + 3 (1-12)")
                .addStructureChannels(definition).addStructureChannels(definition);
        for (String[] lines : new String[][] { tooltip.getStructureInformation(), tooltip.getStructureHint() }) {
            assertEquals(1, Arrays.stream(lines).filter(line -> line.contains("height:")).count());
            assertEquals(1, Arrays.stream(lines).filter(line -> line.contains("gt_hatch:")).count());
            assertTrue(EnumChatFormatting.getTextWithoutFormattingCodes(String.join("\n", lines)).contains("Height = value + 3"));
            assertTrue(EnumChatFormatting.getTextWithoutFormattingCodes(String.join("\n", lines)).contains("(1-1)"));
        }
    }
}
