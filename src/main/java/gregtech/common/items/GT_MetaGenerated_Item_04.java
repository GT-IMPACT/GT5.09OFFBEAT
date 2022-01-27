package gregtech.common.items;

import gregtech.api.GregTech_API;
import gregtech.api.enums.*;
import gregtech.api.interfaces.ITexture;
import gregtech.api.items.GT_MetaGenerated_Item_X32;
import gregtech.api.objects.GT_MultiTexture;

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
    }

    public boolean doesShowInCreative(OrePrefixes aPrefix, Materials aMaterial, boolean aDoShowAllItems) {
        return aDoShowAllItems;
    }
}
