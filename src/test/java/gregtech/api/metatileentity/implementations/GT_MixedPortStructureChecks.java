package gregtech.api.metatileentity.implementations;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Casings;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTStructureChannels;
import gregtech.common.tileentities.machines.multi.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import space.impact.structures.alignment.constructable.ChannelDataAccessor;
import space.impact.structures.alignment.constructable.ISurvivalConstructable;
import space.impact.structures.structure.ISurvivalBuildEnvironment;
import static gregtech.api.metatileentity.implementations.GT_StructureIntegrationTest.*;
import static org.junit.Assert.*;

final class GT_MixedPortStructureChecks {
    static void run() {
        List<Supplier<MetaTileEntity>> factories = Arrays.asList(
                () -> new GT_MetaTileEntity_Hatch_Maintenance("ports-maintenance", 1, "", null, false),
                () -> new GT_MetaTileEntity_Hatch_Energy("ports-energy", 4, "", null),
                () -> new GT_MetaTileEntity_Hatch_InputBus("ports-item-input", 4, "", null),
                () -> new GT_MetaTileEntity_Hatch_OutputBus("ports-item-output", 4, "", null),
                () -> new GT_MetaTileEntity_Hatch_Input("ports-fluid-input", 4, "", null),
                () -> new GT_MetaTileEntity_Hatch_Output("ports-fluid-output", 4, "", null));
        IMetaTileEntity[] previous = new IMetaTileEntity[factories.size()];
        for (int i = 0; i < factories.size(); i++) {
            previous[i] = GregTech_API.METATILEENTITIES[31700 + i];
            GregTech_API.METATILEENTITIES[31700 + i] = factories.get(i).get();
        }
        Block block = new Block(Material.iron) {};
        Block.blockRegistry.addObject(3013, "structure_test:mixed_ports", block);
        Item item = new gregtech.common.blocks.GT_Item_Machines(block) {
            @Override
            public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                    int side, float hitX, float hitY, float hitZ) {
                ((TestWorld) world).putTile(x, y, z, new TestTile(world,
                        factories.get(stack.getItemDamage() - 31700).get(), ForgeDirection.NORTH));
                stack.stackSize--;
                return true;
            }
        };
        Item.itemRegistry.addObject(3013, "structure_test:mixed_ports", item);
        List<Supplier<GT_MetaTileEntity_EnhancedMultiBlockBase<?>>> machines = Arrays.asList(
                () -> new GT_MetaTileEntity_LargeChemicalReactor("ports-reactor"),
                () -> new GT_MetaTileEntity_MultiblockElectrolyzer("ports-electrolyzer"),
                () -> new GT_MetaTileEntity_MultiblockCentrifuge("ports-centrifuge"),
                () -> new GT_MetaTileEntity_VacuumFreezer("ports-freezer"));
        try {
            for (Supplier<GT_MetaTileEntity_EnhancedMultiBlockBase<?>> factory : machines) {
                for (boolean fluidFirst : new boolean[] { false, true }) {
                    GT_MetaTileEntity_EnhancedMultiBlockBase<?> machine = factory.get();
                    TestWorld world = new TestWorld();
                    TestTile tile = new TestTile(world, machine, ForgeDirection.NORTH);
                    world.putTile(0, 64, 0, tile);
                    List<ItemStack> inventory = new ArrayList<>();
                    for (int i : fluidFirst ? new int[] { 0, 1, 4, 5, 2, 3 } : new int[] { 0, 1, 2, 3, 4, 5 })
                        inventory.add(new ItemStack(item, 1, 31700 + i));
                    for (Casings casing : Casings.values())
                        if (casing.getBlock() != null)
                            inventory.add(new ItemStack(casing.getBlock(), 64, casing.getMetadata()));
                    Source source = new Source(inventory.toArray(new ItemStack[0]));
                    ItemStack trigger = new ItemStack(Items.stick);
                    ChannelDataAccessor.setChannelData(trigger, GTStructureChannels.HATCH, 1);
                    ISurvivalBuildEnvironment environment = ISurvivalBuildEnvironment.create(source, new TestPlayer(world));
                    for (int step = 0; step < 100; step++)
                        if (((ISurvivalConstructable) machine).survivalConstruct(trigger, 3, environment) < 0) break;
                    machine.setupMachine(tile, null);
                    assertTrue(machine.getClass().getSimpleName(), machine.mMachine);
                    assertEquals(1, machine.mInputBusses.size());
                    assertEquals(1, machine.mOutputBusses.size());
                    assertEquals(1, machine.mInputHatches.size());
                    assertEquals(1, machine.mOutputHatches.size());
                    for (int i = 0; i < 6; i++) assertEquals(0, inventory.get(i).stackSize);
                }
            }
        } finally {
            for (int i = 0; i < previous.length; i++) GregTech_API.METATILEENTITIES[31700 + i] = previous[i];
        }
    }
}
