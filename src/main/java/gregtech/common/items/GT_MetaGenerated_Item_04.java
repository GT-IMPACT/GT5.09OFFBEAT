package gregtech.common.items;

import gregtech.api.GregTech_API;
import gregtech.api.enums.*;
import gregtech.api.interfaces.ITexture;
import gregtech.api.items.GT_MetaGenerated_Item_X32;
import gregtech.api.objects.GT_MultiTexture;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.common.covers.GT_Cover_FluidRegulator;
import gregtech.common.items.behaviors.Behaviour_NoteBook;

public class GT_MetaGenerated_Item_04
        extends GT_MetaGenerated_Item_X32 {
    public static GT_MetaGenerated_Item_04 INSTANCE;

    public GT_MetaGenerated_Item_04() {
        super("metaitem.04");
        INSTANCE = this;
        int tLastID = 0;


        ItemList.Tool_NoteBook.set(addItem(tLastID, "Laptop", "Personal Computer", Behaviour_NoteBook.INSTANCE));
        setElectricStats(32000 + tLastID++, 400000L, GT_Values.V[1], 2L, -1L, false);

        ItemList.BrokenDrill_LV.set(addItem(tLastID++, "Broken LV Drill", "Crystal Battery"));
        ItemList.BrokenDrill_MV.set(addItem(tLastID++, "Broken MV Drill", "Crystal Battery"));
        ItemList.BrokenDrill_HV.set(addItem(tLastID++, "Broken HV Drill", "Crystal Battery"));
        ItemList.BrokenDrill_EV.set(addItem(tLastID++, "Broken EV Drill", "Crystal Battery"));
        ItemList.BrokenDrill_IV.set(addItem(tLastID++, "Broken IV Drill", "Crystal Battery"));

        System.out.println("Last ID for GT_MI 4.0: " + tLastID);

        tLastID = 390;
        ItemList.Steam_Valve_LV.set(addItem(tLastID++, "LV Steam Regulator", "Configuable up to 20.480 L/sec (as Cover)"));
        ItemList.Steam_Valve_MV.set(addItem(tLastID++, "MV Steam Regulator", "Configuable up to 40.960 L/sec (as Cover)"));
        ItemList.Steam_Valve_MV.set(addItem(tLastID++, "HV Steam Regulator", "Configuable up to 81.920 L/sec (as Cover)"));
        ItemList.Steam_Valve_EV.set(addItem(tLastID++, "EV Steam Regulator", "Configuable up to 163.840 L/sec (as Cover)"));
        ItemList.Steam_Valve_IV.set(addItem(tLastID++, "IV Steam Regulator", "Configuable up to 327.680 L/sec (as Cover)"));

        GregTech_API.registerCover(ItemList.Steam_Valve_LV.get(1L), new GT_MultiTexture(new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[1][0], new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PUMP)}), new GT_Cover_FluidRegulator(1024));
        GregTech_API.registerCover(ItemList.Steam_Valve_MV.get(1L), new GT_MultiTexture(new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[2][0], new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PUMP)}), new GT_Cover_FluidRegulator(2048));
        GregTech_API.registerCover(ItemList.Steam_Valve_MV.get(1L), new GT_MultiTexture(new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[3][0], new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PUMP)}), new GT_Cover_FluidRegulator(4096));
        GregTech_API.registerCover(ItemList.Steam_Valve_EV.get(1L), new GT_MultiTexture(new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[4][0], new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PUMP)}), new GT_Cover_FluidRegulator(8192));
        GregTech_API.registerCover(ItemList.Steam_Valve_IV.get(1L), new GT_MultiTexture(new ITexture[]{Textures.BlockIcons.MACHINE_CASINGS[5][0], new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PUMP)}), new GT_Cover_FluidRegulator(16384));

        System.out.println("Last ID for GT_MI 4.1: " + tLastID);
    }

    public boolean doesShowInCreative(OrePrefixes aPrefix, Materials aMaterial, boolean aDoShowAllItems) {
        return aDoShowAllItems;
    }
}
