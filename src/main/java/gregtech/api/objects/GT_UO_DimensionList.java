package gregtech.api.objects;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

public class GT_UO_DimensionList {

    public int[] blackList = new int[0];
    private Configuration fConfig;
    private String fCategory;
    private BiMap<String, GT_UO_Dimension> fDimensionList;

    public GT_UO_DimensionList() {
        fDimensionList = HashBiMap.create();
    }

    public GT_UO_Dimension GetDimension(int aDimension) {
        if (CheckBlackList(aDimension)) return null;
        if (fDimensionList.containsKey(Integer.toString(aDimension)))
            return fDimensionList.get(Integer.toString(aDimension));
        for (BiMap.Entry<String, GT_UO_Dimension> dl : fDimensionList.entrySet())
            if (DimensionManager.getProvider(aDimension).getClass().getName().contains(dl.getValue().Dimension))
                return dl.getValue();
        return fDimensionList.get("Default");
    }

    private boolean CheckBlackList(int aDimensionId) {
        try {
            return java.util.Arrays.binarySearch(blackList, aDimensionId) >= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void SetConfigValues(String aDimensionName, String aDimension, String aName, String aRegistry, int aMinAmount, int aMaxAmount, int aChance, int aDecreasePerOperationAmount) {
        String Category = fCategory + "." + aDimensionName;
        fConfig.get(Category, "Dimension", aDimension).getString();
        Category += "." + aName;
        fConfig.get(Category, "Registry", aRegistry).getString();
        fConfig.get(Category, "MinAmount", aMinAmount).getInt(aMinAmount);
        fConfig.get(Category, "MaxAmount", aMaxAmount).getInt(aMaxAmount);
        fConfig.get(Category, "Chance", aChance).getInt(aChance);
        fConfig.get(Category, "DecreasePerOperationAmount", aDecreasePerOperationAmount).getInt(aDecreasePerOperationAmount);
        //IT IS IN BUCKETS!!!
    }

    public void SetDafultValues() {
        SetConfigValues("Overworld", "0", "gas_natural_gas", "gas_natural_gas", 0, 700, 20, 7);
        SetConfigValues("Overworld", "0", "liquid_light_oil", "liquid_light_oil", 0, 650, 20, 6);
        SetConfigValues("Overworld", "0", "liquid_medium_oil", "liquid_medium_oil", 0, 600, 20, 5);
        SetConfigValues("Overworld", "0", "liquid_heavy_oil", "liquid_heavy_oil", 0, 550, 20, 4);
        SetConfigValues("Overworld", "0", "oil", "oil", 0, 600, 20, 5);
        SetConfigValues("aroma1997", "45", "gas_natural_gas", "gas_natural_gas", 100, 800, 33, 6);
        SetConfigValues("aroma1997", "45", "liquid_medium_oil", "liquid_medium_oil", 90, 700, 33, 4);
        SetConfigValues("aroma1997", "45", "oil", "oil", 80, 600, 34, 5);
        SetConfigValues("Moon", "Moon", "helium-3", "helium-3", 100, 425, 80, 3);
        SetConfigValues("Moon", "Moon", "saltwater", "saltwater", 0, 50, 20, 2);
        SetConfigValues("Nether", "-1", "lava", "lava", 0, 800, 100, 5);
        // T2
        SetConfigValues("Mars", "Mars", "saltwater", "saltwater", 80, 200, 20, 2);
        SetConfigValues("Mars", "Mars", "chlorobenzene", "chlorobenzene", 150, 400, 30, 5);
        SetConfigValues("Mars", "Mars", "bacterialsludge", "bacterialsludge", 70, 200, 50, 5);
        SetConfigValues("Deimos", "Deimos", "nitrogen", "nitrogen", 70, 200, 30, 3);
        SetConfigValues("Phobos", "Phobos", "saltwater", "saltwater", 160, 400, 70, 3);
        // T3
        SetConfigValues("Europa", "Europa", "saltwater", "saltwater", 100, 600, 40, 5);
        SetConfigValues("Europa", "Europa", "liquid_extra_heavy_oil", "liquid_extra_heavy_oil", 80, 200, 20, 5);
        SetConfigValues("Europa", "Europa", "liquid_heavy_oil", "liquid_heavy_oil", 200, 600, 40, 1);
        SetConfigValues("Callisto", "Callisto", "oxygen", "oxygen", 80, 200, 50, 5);
        SetConfigValues("Callisto", "Callisto", "oil", "oil", 180, 700, 50, 1);
        SetConfigValues("Ganymede", "Ganymede", "gas_natural_gas", "gas_natural_gas", 100, 600, 70, 1);
        SetConfigValues("Ganymede", "Ganymede", "helium", "helium", 0, 100, 30, 2);
        SetConfigValues("Ceres", "Ceres", "nitrogen", "nitrogen", 180, 300, 70, 3);
        SetConfigValues("Ceres", "Ceres", "fluorine", "fluorine", 0, 50, 30, 1);
        // T4
        SetConfigValues("Mercury", "Mercury", "helium-3", "helium-3", 250, 600, 100, 2);
        SetConfigValues("Venus", "Venus", "argon", "argon", 0, 50, 20, 1);
        SetConfigValues("Venus", "Venus", "molten_lead", "molten.lead", 100, 600, 60, 5);
        SetConfigValues("Venus", "Venus", "sulfuricacid", "sulfuricacid", 0, 300, 20, 4);
        SetConfigValues("Io", "Io", "lava", "lava", 600, 2000, 100, 2);
        // T5
        SetConfigValues("Titan", "Titan", "methane", "methane", 200, 800, 60, 5);
        SetConfigValues("Titan", "Titan", "ethane", "ethane", 50, 200, 40, 5);
        SetConfigValues("Miranda", "Miranda", "liquid_sufluriclight_fuel", "liquid_sufluriclight_fuel", 100, 500, 100, 3);
        SetConfigValues("Oberon", "Oberon", "liquid_sulfuricheavy_fuel", "liquid_sulfuricheavy_fuel", 100, 500, 50, 3);
        SetConfigValues("Oberon", "Oberon", "liquid_sulfuricnaphtha", "liquid_sulfuricnaphtha", 100, 500, 50, 3);
        SetConfigValues("Enceladus", "Enceladus", "radon", "radon", 0, 100, 10, 5);
        SetConfigValues("Enceladus", "Enceladus", "gas_sulfuricgas", "gas_sulfuricgas", 200, 500, 80, 3);
        SetConfigValues("Enceladus", "Enceladus", "molten_glowstone", "molten.glowstone", 0, 100, 10, 5);
        // T6
        SetConfigValues("Triton", "Triton", "molten_indium", "molten.indium", 0, 50, 5, 5);
        SetConfigValues("Triton", "Triton", "molten_naquadah", "molten.naquadah", 50, 200, 5, 5);
        SetConfigValues("Proteus", "Proteus", "liquid_toluene", "liquid_toluene", 50, 300, 90, 4);
        SetConfigValues("Proteus", "Proteus", "molten_lutetium", "molten.lutetium", 0, 50, 10, 3);
        // T7
        SetConfigValues("Pluto", "Pluto", "ethylene", "ethylene", 400, 800, 70, 3);
        SetConfigValues("Pluto", "Pluto", "chlorine", "chlorine", 100, 400, 30, 4);
        SetConfigValues("Haumea", "Haumea", "nitrogen", "nitrogen", 400, 800, 100, 1);
        SetConfigValues("MakeMake", "MakeMake", "oxygen", "oxygen", 400, 800, 90, 1);
        SetConfigValues("MakeMake", "MakeMake", "molten_naquadahenriched", "molten.naquadahenriched", 50, 200, 10, 5);
        // T8
        SetConfigValues("BarnardC", "BarnardC", "liquid_extra_heavy_oil", "liquid_extra_heavy_oil", 400, 800, 70, 1);
        SetConfigValues("BarnardC", "BarnardC", "fluorine", "fluorine", 50, 100, 30, 3);
        SetConfigValues("BarnardE", "BarnardE", "radon", "radon", 50, 100, 100, 3);
        SetConfigValues("TCetiE", "TCetiE", "gas_natural_gas", "gas_natural_gas", 400, 800, 60, 1);
        SetConfigValues("TCetiE", "TCetiE", "hydrogen", "hydrogen", 150, 400, 40, 1);
        SetConfigValues("BarnardF", "BarnardF", "liquidair", "liquidair", 350, 800, 100, 1);
        SetConfigValues("VegaB", "VegaB", "helium", "helium", 200, 500, 100, 1);
        SetConfigValues("CentauriA", "CentauriA", "lava", "lava", 500, 800, 50, 1);
        SetConfigValues("CentauriA", "CentauriA", "molten_naquadria", "molten.naquadria", 100, 800, 50, 5);
    }

    public void getConfig(Configuration aConfig, String aCategory) {
        fCategory = aCategory;
        fConfig = aConfig;
        if (!fConfig.hasCategory(fCategory))
            SetDafultValues();

        fConfig.setCategoryComment(fCategory, "Config Underground Fluids (Delete this Category for regenerate)");
        fConfig.setCategoryComment(fCategory + ".Default", "Set Default Generating (Use this Category for Default settings)");
        fConfig.setCategoryComment(fCategory + ".Overworld", "Set Overworld Generating");
        fConfig.setCategoryComment(fCategory + ".Moon", "Set Moon Generating");
        fConfig.setCategoryComment(fCategory + ".Nether", "Set Nether Generating");

        blackList = new int[]{1};
        blackList = aConfig.get(fCategory, "DimBlackList", blackList, "Dimension IDs Black List").getIntList();
        java.util.Arrays.sort(blackList);

        for (int i = 0; i < fConfig.getCategory(fCategory).getChildren().size(); i++) {
            GT_UO_Dimension Dimension = new GT_UO_Dimension((ConfigCategory) fConfig.getCategory(fCategory).getChildren().toArray()[i]);
            fDimensionList.put(Dimension.Dimension, Dimension);
        }
    }
}
