package gregtech.loaders.materialprocessing;

import cpw.mods.fml.common.Loader;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;

public class ProcessingModSupport implements gregtech.api.interfaces.IMaterialHandler {
    public static boolean aTGregSupport = Loader.isModLoaded("TGregworks") || Loader.isModLoaded("spartakcore");
    public static boolean aEnableUBCMats = Loader.isModLoaded("UndergroundBiomes") || aTGregSupport;
    public static boolean aEnableThaumcraftMats = Loader.isModLoaded("Thaumcraft") || aTGregSupport;
    public static boolean aEnableRotaryCraftMats = Loader.isModLoaded("RotaryCraft") || aTGregSupport;
    public static boolean aEnableThermalFoundationMats = Loader.isModLoaded("ThermalFoundation") || aTGregSupport;
    public static boolean aEnableEnderIOMats = Loader.isModLoaded("EnderIO") || aTGregSupport;
    //public static boolean aEnableRailcraftMats = Loader.isModLoaded(GT_Values.MOD_ID_RC) || aTGregSupport;
    public static boolean aEnableGCMarsMats = Loader.isModLoaded("GalacticraftMars") || aTGregSupport;
    //public static boolean aEnableTwilightMats = Loader.isModLoaded(GT_Values.MOD_ID_TF) || aTGregSupport;
    public static boolean aEnableMetallurgyMats = Loader.isModLoaded("Metallurgy") || aTGregSupport;
    //public static boolean aEnableProjectRedMats = Loader.isModLoaded("ProjRed|Core") || aTGregSupport;

    public ProcessingModSupport() {
        Materials.add(this);
    }

    @Override
    public void onMaterialsInit() {
        //Disable Materials if Parent Mod is not loaded
        if (!aTGregSupport) {
            Materials.Indium.mHasParentMod = false;
            Materials.Lanthanum.mHasParentMod = false;
            Materials.Rubidium.mHasParentMod = false;
            Materials.Samarium.mHasParentMod = false;
            Materials.Terbium.mHasParentMod = false;
            Materials.ElectrumFlux.mHasParentMod = false;
            Materials.Nikolite.mHasParentMod = false;
            Materials.Sunnarium.mHasParentMod = false;
            Materials.BlueAlloy.mHasParentMod = false;
        }
        if (!aEnableMetallurgyMats) {
            Materials.Atlarus.mHasParentMod = false;
            Materials.Infuscolium.mHasParentMod = false;
            Materials.Kalendrite.mHasParentMod = false;
            Materials.Orichalcum.mHasParentMod = false;
            Materials.Oureclase.mHasParentMod = false;
            Materials.Prometheum.mHasParentMod = false;
            Materials.Rubracium.mHasParentMod = false;
            Materials.Sanguinite.mHasParentMod = false;
            Materials.Tartarite.mHasParentMod = false;
            Materials.Vulcanite.mHasParentMod = false;
            Materials.Vyroxeres.mHasParentMod = false;
            Materials.DeepIron.mHasParentMod = false;
            Materials.Adamantium.mHasParentMod = false;
        }
        if (!aEnableUBCMats) {
            Materials.Blueschist.mHasParentMod = false;
            Materials.Chert.mHasParentMod = false;
            Materials.Eclogite.mHasParentMod = false;
            Materials.Gabbro.mHasParentMod = false;
            Materials.Gneiss.mHasParentMod = false;
            Materials.Greenschist.mHasParentMod = false;
            Materials.Greywacke.mHasParentMod = false;
            Materials.Komatiite.mHasParentMod = false;
            Materials.Rhyolite.mHasParentMod = false;
            Materials.Siltstone.mHasParentMod = false;
        }
        if (!aEnableGCMarsMats) {
            Materials.Desh.mHasParentMod = false;
            Materials.MeteoricIron.mHasParentMod = false;
            Materials.MeteoricSteel.mHasParentMod = false;
        }
        if (!aEnableThermalFoundationMats) {
            Materials.Blizz.mHasParentMod = false;
            Materials.Enderium.mHasParentMod = false;
        }
        if (!aEnableRotaryCraftMats) {
            Materials.HSLA.mHasParentMod = false;
        }
        if (!aEnableEnderIOMats) {
            Materials.DarkSteel.mHasParentMod = false;
        }

        //Enable Materials if correct mod is Loaded
    }

    @Override
    public void onComponentInit() {
    }

    @Override
    public void onComponentIteration(Materials aMaterial) {
        //NOOP
    }
}
