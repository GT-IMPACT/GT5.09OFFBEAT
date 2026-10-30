package gregtech.api.interfaces;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;

/**
 * A hatch category shared by validation and survival construction. Matching must be side-effect free.
 */
public interface IHatchElement {

    boolean matches(IMetaTileEntity tile);

    int count(GT_MetaTileEntity_MultiBlockBase machine);

    boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int textureIndex);

    String getDescription();
}
