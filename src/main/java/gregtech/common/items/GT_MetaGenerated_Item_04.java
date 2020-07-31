package gregtech.common.items;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.items.GT_MetaGenerated_Item_X32;
import gregtech.common.items.behaviors.Behaviour_NoteBook;

public class GT_MetaGenerated_Item_04
        extends GT_MetaGenerated_Item_X32 {
    public static GT_MetaGenerated_Item_04 INSTANCE;

    public GT_MetaGenerated_Item_04() {
        super("metaitem.04");
        INSTANCE = this;
        int tLastID = 0;


        ItemList.Tool_NoteBook.set(addItem(tLastID = 0, "Laptop", "Personal Computer", Behaviour_NoteBook.INSTANCE));
        setElectricStats(32000 + tLastID, 100000L, GT_Values.V[1], 2L, -1L, false);
    }

    public boolean doesShowInCreative(OrePrefixes aPrefix, Materials aMaterial, boolean aDoShowAllItems) {
        return aDoShowAllItems;
    }
}
