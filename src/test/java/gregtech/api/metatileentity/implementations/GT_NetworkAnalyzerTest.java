package gregtech.api.metatileentity.implementations;

import appeng.api.util.DimensionalCoord;
import com.google.common.io.ByteStreams;
import gregtech.api.enums.Materials;
import gregtech.api.graphs.PowerNode;
import gregtech.api.graphs.consumers.ConsumerNode;
import gregtech.api.graphs.paths.PowerNodePath;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.common.networkanalyzer.NetworkAnalyzerData;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.AnalyzerModes;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.GraphNodeState;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.OverlayEdge;
import gregtech.common.networkanalyzer.NetworkAnalyzerData.SeverityLevel;
import gregtech.common.networkanalyzer.net.GTPacketNetworkAnalyzer;
import gregtech.common.networkanalyzer.network.PowerNetworkBuilder;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GT_NetworkAnalyzerTest {

    @Test
    public void cableGraphKeepsTopologyActivityAndWarningsThroughPacketRoundTrip() throws Throwable {
        try (GT_ProcessingTestSupport.GameRuntime runtime = new GT_ProcessingTestSupport.GameRuntime()) {
            runtime.run(getClass(), "graph", "");
        }
    }

    public static void runScenario(String scenario, String unused) throws Exception {
        DedicatedServer server = new DedicatedServer(new File("."));
        GT_ProcessingTestSupport.ProcessingWorld world = new GT_ProcessingTestSupport.ProcessingWorld();
        GT_MetaTileEntity_Hatch_Dynamo dynamo = new GT_MetaTileEntity_Hatch_Dynamo("analyzer_dynamo", 2, "", null);
        world.putTile(0, 64, 0, new GT_StructureIntegrationTest.TestTile(world, dynamo, ForgeDirection.EAST));

        GT_MetaPipeEntity_Cable cable = new GT_MetaPipeEntity_Cable(
            "analyzer_cable", 0.25f, Materials.Copper, 1, 2, 32, true, false
        );
        BaseMetaPipeEntity pipe = new BaseMetaPipeEntity();
        pipe.setWorldObj(world);
        pipe.xCoord = 1;
        pipe.yCoord = 64;
        cable.setBaseMetaTileEntity(pipe);
        cable.mConnections = (byte) ((1 << ForgeDirection.WEST.ordinal()) | (1 << ForgeDirection.EAST.ordinal()));

        ArrayList<ConsumerNode> consumers = new ArrayList<>();
        PowerNode source = new PowerNode(1, pipe, consumers);
        TileEntity load = new TileEntity();
        load.xCoord = 2;
        load.yCoord = 64;
        ConsumerNode consumer = new ConsumerNode(2, load, (byte) ForgeDirection.WEST.ordinal(), consumers);
        consumers.add(consumer);
        PowerNodePath edge = new PowerNodePath(new MetaPipeEntity[0]);
        source.mSelfPath = new PowerNodePath(new MetaPipeEntity[] { cable });
        source.mNeighbourNodes[ForgeDirection.EAST.ordinal()] = consumer;
        source.mNodePaths[ForgeDirection.EAST.ordinal()] = edge;
        consumer.mNeighbourNodes[ForgeDirection.WEST.ordinal()] = source;
        consumer.mNodePaths[ForgeDirection.WEST.ordinal()] = edge;

        GT_ProcessingTestSupport.setField(server, "tickCounter", 1);
        edge.addAmps(1);
        edge.addAmps(1);
        GT_ProcessingTestSupport.setField(server, "tickCounter", 2);
        edge.addAmps(1);
        assertEquals(2, edge.getAmperage());

        PowerNetworkBuilder builder = new PowerNetworkBuilder();
        builder.addNetwork(source);
        NetworkAnalyzerData topology = builder.getData(AnalyzerModes.TOPOLOGY);
        assertEquals(2, topology.nodes.size());
        assertTrue(topology.nodes.stream().anyMatch(node -> node.state == GraphNodeState.INPUT));
        assertTrue(topology.nodes.stream().anyMatch(node -> node.state == GraphNodeState.OUTPUT));
        OverlayEdge critical = builder.getData(AnalyzerModes.CRITICAL).edges.get(0);
        assertEquals(SeverityLevel.ALERT, critical.level);
        assertEquals(1, builder.getData(AnalyzerModes.ACTIVE).edges.size());
        assertEquals(1, builder.getData(AnalyzerModes.AMPERAGE).edges.size());
        assertEquals(SeverityLevel.ALERT, builder.getData(AnalyzerModes.VOLTAGE).edges.get(0).level);

        GTPacketNetworkAnalyzer packet = new GTPacketNetworkAnalyzer(new DimensionalCoord(1, 64, 0, 0), topology);
        byte[] payload = packet.encode();
        GTPacketNetworkAnalyzer decoded = (GTPacketNetworkAnalyzer) new GTPacketNetworkAnalyzer()
            .decode(ByteStreams.newDataInput(payload));
        assertArrayEquals(payload, decoded.encode());
    }
}
