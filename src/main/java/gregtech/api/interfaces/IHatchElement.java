package gregtech.api.interfaces;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;

/**
 * A hatch category shared by validation and survival construction. Matching must be side-effect free.
 */
public interface IHatchElement {

    boolean matches(IMetaTileEntity tile);

    /**
     * Candidates that can fulfill this category's minimum during automatic construction.
     * Override when additional legacy hatch variants are accepted but do not contribute to count().
     */
    default boolean matchesForPlacement(IMetaTileEntity tile) {
        return matches(tile);
    }

    int count(GT_MetaTileEntity_MultiBlockBase machine);

    boolean add(GT_MetaTileEntity_MultiBlockBase machine, IGregTechTileEntity tile, int textureIndex);

    String getDescription();
}
