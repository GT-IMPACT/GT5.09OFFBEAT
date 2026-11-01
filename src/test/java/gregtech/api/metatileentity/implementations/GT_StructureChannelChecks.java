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
                    assertTrue(key + ".label", StatCollector.canTranslate(key + ".label"));
                    assertEquals(StatCollector.translateToLocal(key + ".label"), GTStructureChannels.getLabel(channel));
                }
                checkSection(language);
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
    }

    static void checkEveryController() throws Exception {
        java.nio.file.Path source = java.nio.file.Paths.get("src/main/java/gregtech/common/tileentities/machines/multi");
        int checked = 0;
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(source)) {
            for (java.nio.file.Path file : (Iterable<java.nio.file.Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                String name = "gregtech.common.tileentities.machines.multi." + file.getFileName().toString().replace(".java", "");
                Class<?> type = Class.forName(name);
                if (java.lang.reflect.Modifier.isAbstract(type.getModifiers())
                        || !GT_MetaTileEntity_EnhancedMultiBlockBase.class.isAssignableFrom(type)) continue;
                GT_MetaTileEntity_EnhancedMultiBlockBase<?> machine = (GT_MetaTileEntity_EnhancedMultiBlockBase<?>) type
                        .getConstructor(String.class).newInstance("channel-audit." + type.getSimpleName());
                String primary = String.join("\n", machine.getPrimaryDescription());
                String header = StatCollector.translateToLocal("gt.tooltip.channels");
                assertFalse(name, primary.contains(header));
                for (String[] lines : new String[][] { machine.getSecondaryDescription(), machine.getStructureDescription(null) }) {
                    String description = EnumChatFormatting.getTextWithoutFormattingCodes(String.join("\n", lines));
                    for (space.impact.structures.structure.IChannelDefinition channel : machine.getStructureDefinition().getChannelDefinitions()) {
                        String label = GTStructureChannels.getLabel(channel.getChannel()) + " (" + channel.getChannel() + ")";
                        assertTrue(name + " missing channel section", description.indexOf(header) >= 0);
                        assertTrue(name + " missing " + label, description.indexOf(label) > description.indexOf(header));
                        assertFalse(name + " generic description", description.contains(GTStructureChannels.getDescription(channel.getChannel())));
                        assertEquals(name + " repeated channel", 1, Arrays.stream(lines)
                                .filter(line -> EnumChatFormatting.getTextWithoutFormattingCodes(line).contains(label)).count());
                    }
                }
                checked++;
            }
        }
        assertTrue("Controller discovery failed", checked >= 30);
        System.out.println("Channel tooltip audit: " + checked + " GregTech controllers");
    }

    static int materialLineCount(String[] rows) {
        String header = StatCollector.translateToLocal("gt.tooltip.channels");
        for (int index = 0; index < rows.length; index++) {
            if (EnumChatFormatting.getTextWithoutFormattingCodes(rows[index]).equals(header)) return index;
        }
        return rows.length;
    }

    private static void checkSection(String language) {
        IStructureDefinition<Object> definition = StructureDefinition.<Object>builder()
                .addChannel(GTStructureChannels.HEIGHT, 1, 12)
                .addChannel(GTStructureChannels.HATCH, 1, 1).build();
        MultiblockTooltipBuilder tooltip = new MultiblockTooltipBuilder()
                .addStructureChannels(definition)
                .addSubChannel(GTStructureChannels.HEIGHT, "Height = value + 3 (1-12)")
                .addOtherStructurePart("Glass", "Walls")
                .addSubChannel(GTStructureChannels.HEIGHT, "A second height detail")
                .addSubChannel(GTStructureChannels.HEIGHT, "A second height detail")
                .addStructureChannels(definition)
                .addStructureHint("Glass walls", space.impact.structures.hint.HintColor.GREEN);
        String header = StatCollector.translateToLocal("gt.tooltip.channels");
        String label = GTStructureChannels.getLabel(GTStructureChannels.HEIGHT);
        for (String[] lines : new String[][] { tooltip.getStructureInformation(), tooltip.getStructureHint() }) {
            String plain = EnumChatFormatting.getTextWithoutFormattingCodes(String.join("\n", lines));
            assertEquals(language, 1, Arrays.stream(lines)
                    .filter(line -> EnumChatFormatting.getTextWithoutFormattingCodes(line).equals(header)).count());
            int section = plain.indexOf(header);
            assertTrue(plain.indexOf("Glass walls") < section);
            assertTrue(plain.indexOf("Height = value + 3") > section);
            assertTrue(plain.indexOf("A second height detail") > section);
            assertEquals(1, Arrays.stream(lines).filter(line -> line.contains("(height)")).count());
            assertEquals(1, Arrays.stream(lines).filter(line -> line.contains("(gt_hatch)")).count());
            assertEquals(1, Arrays.stream(lines).filter(line -> line.contains("A second height detail")).count());
            assertTrue(String.join("\n", lines).contains(EnumChatFormatting.AQUA + label + EnumChatFormatting.DARK_GRAY));
            assertFalse(plain.contains(GTStructureChannels.getDescription(GTStructureChannels.HEIGHT)));
            assertTrue(plain.contains("(1-1)"));
        }
        assertArrayEquals(tooltip.getStructureInformation(), tooltip.getStructureInformation());
        assertFalse(String.join("\n", tooltip.getInformation()).contains(header));
        MultiblockTooltipBuilder fixed = new MultiblockTooltipBuilder().addStructureChannels(
                StructureDefinition.<Object>builder().build());
        assertFalse(String.join("\n", fixed.getStructureInformation()).contains(header));
        assertFalse(String.join("\n", fixed.getStructureHint()).contains(header));
        MultiblockTooltipBuilder legacy = new MultiblockTooltipBuilder().addMasterChannel("Rack layers");
        assertTrue(String.join("\n", legacy.getStructureInformation()).contains(header));
        assertTrue(String.join("\n", legacy.getStructureHint()).contains("Rack layers"));
        assertEquals("Custom channel", GTStructureChannels.getLabel("custom_channel"));
    }
}
