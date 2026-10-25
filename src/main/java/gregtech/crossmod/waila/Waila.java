package gregtech.crossmod.waila;

import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

/** Loaded by Waila through the IMC callback, on both the client and the server. */
public final class Waila {

    private Waila() {
    }

    public static void callbackRegister(IWailaRegistrar registrar) {
        registrar.addConfig("GregTech", "gregtech.machineFacing", "GT5U.waila.config.machine_facing", true);
        registrar.addConfig("GregTech", "gregtech.transformer", "GT5U.waila.config.transformer", true);
        registrar.addConfig("GregTech", "gregtech.solar", "GT5U.waila.config.solar", true);
        registrar.addConfig("GregTech", "gregtech.multiblock", "GT5U.waila.config.multiblock", true);
        registrar.addConfig("GregTech", "gregtech.fluidFilter", "GT5U.waila.config.fluid_filter", true);
        registrar.addConfig("GregTech", "gregtech.basicmachine", "GT5U.waila.config.basic_machine", true);
        IWailaDataProvider provider = new GregtechTEWailaDataProvider();
        registrar.registerStackProvider(provider, BaseMetaTileEntity.class);
        registrar.registerStackProvider(provider, BaseMetaPipeEntity.class);
        registrar.registerBodyProvider(provider, BaseMetaTileEntity.class);
        registrar.registerBodyProvider(provider, BaseMetaPipeEntity.class);
        registrar.registerNBTProvider(provider, BaseMetaTileEntity.class);
        registrar.registerNBTProvider(provider, BaseMetaPipeEntity.class);
    }
}
