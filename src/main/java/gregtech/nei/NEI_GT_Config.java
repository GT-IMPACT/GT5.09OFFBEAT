package gregtech.nei;

import codechicken.nei.api.IConfigureNEI;
import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.util.GT_Recipe;

public class NEI_GT_Config
        implements IConfigureNEI {
    public static boolean sIsAdded = true;
    public static GT_NEI_AssLineHandler ALH;
    public static GT_NEI_SawMill SAW;
    public static GT_NEI_Pyro PYRO;
    public static GT_NEI_3DPrinter D3;

    @Override
    public void loadConfig() {
        sIsAdded = false;
        for (GT_Recipe.GT_Recipe_Map tMap : GT_Recipe.GT_Recipe_Map.sMappings) {
            if (tMap.mNEIAllowed) {
                new GT_NEI_DefaultHandler(tMap);
            }
        }
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            ALH = new GT_NEI_AssLineHandler(GT_Recipe.GT_Recipe_Map.sAssemblylineVisualRecipes);
            SAW = new GT_NEI_SawMill(GT_Recipe.GT_Recipe_Map.sSawMillVisual);
            PYRO = new GT_NEI_Pyro(GT_Recipe.GT_Recipe_Map.sPyrolyseBasicVisual);
            D3 = new GT_NEI_3DPrinter(GT_Recipe.GT_Recipe_Map.sPrimitiveLine);
            D3 = new GT_NEI_3DPrinter(GT_Recipe.GT_Recipe_Map.sBasicline);
        }
        sIsAdded = true;
    }

    @Override
    public String getName() {
        return "GregTech NEI Plugin";
    }

    @Override
    public String getVersion() {
        return "(5.03a)";
    }
}
