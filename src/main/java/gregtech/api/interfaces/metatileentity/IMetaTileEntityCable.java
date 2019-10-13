package gregtech.api.interfaces.metatileentity;

import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.HashSet;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_Cable;
import gregtech.api.metatileentity.implementations.GT_MetaPipeEntity_CableChain;

public interface IMetaTileEntityCable extends IMetaTileEntity {
    @Deprecated
    public long transferElectricity(byte aSide, long aVoltage, long aAmperage, ArrayList<TileEntity> aAlreadyPassedTileEntityList);

    public long transferElectricity(byte aSide, long aVoltage, long aAmperage, HashSet<TileEntity> aAlreadyPassedSet);
    
    public long transferElectricity(IMetaTileEntityCable startCable, byte aSide, long aVoltage, long aAmperege, HashSet<TileEntity> aAlreadyPassedSet);
    
    public GT_MetaPipeEntity_CableChain recalculateCables(IMetaTileEntityCable startCable, GT_MetaPipeEntity_Cable lastConsumerCable, 
    													  GT_MetaPipeEntity_CableChain result, HashSet<TileEntity> aAlreadyPassedSet, 
    													  long aAmperage, long rUsedAmperes, long aVoltage, 
    													  byte aSide, IGregTechTileEntity baseMetaTile);
}