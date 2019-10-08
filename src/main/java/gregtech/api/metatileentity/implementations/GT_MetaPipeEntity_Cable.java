package gregtech.api.metatileentity.implementations;

import cofh.api.energy.IEnergyReceiver;
import com.google.common.collect.Sets;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Dyes;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TextureSet;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.metatileentity.IMetaTileEntityCable;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IEnergyConnected;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.MetaPipeEntity;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_CoverBehavior;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Utility;
import gregtech.common.GT_Client;
import gregtech.common.covers.GT_Cover_SolarPanel;
import gregtech.loaders.materialprocessing.ProcessingModSupport;
import gregtech.loaders.postload.PartP2PGTPower;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.reactor.IReactorChamber;
import micdoodle8.mods.galacticraft.api.power.EnergySource;
import micdoodle8.mods.galacticraft.api.power.EnergySource.EnergySourceAdjacent;
import micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC;
import micdoodle8.mods.galacticraft.api.transmission.NetworkType;
import micdoodle8.mods.galacticraft.api.transmission.tile.IConnector;
import micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import appeng.api.parts.IPartHost;

import static gregtech.api.enums.GT_Values.VN;

public class GT_MetaPipeEntity_Cable extends MetaPipeEntity implements IMetaTileEntityCable {
    public final float mThickNess;
    public final Materials mMaterial;
    public final long mCableLossPerMeter, mAmperage, mVoltage;
    public final boolean mInsulated, mCanShock;
    public long mTransferredAmperage = 0, mTransferredAmperageLast20 = 0, mTransferredVoltageLast20 = 0;
    public long mRestRF;
    public short mOverheat;
    private boolean mCheckConnections = !GT_Mod.gregtechproxy.gt6Cable;
    
    private boolean isPlaced = true;
    
    public static HashMap<IMetaTileEntityCable, GT_MetaPipeEntity_CableChain> startCableCash = 
    		new HashMap<IMetaTileEntityCable, GT_MetaPipeEntity_CableChain>();
    
    public static HashSet<TileEntity> energyEmmiters = new HashSet<>();
    
    public GT_MetaPipeEntity_Cable(int aID, String aName, String aNameRegional, float aThickNess, Materials aMaterial, long aCableLossPerMeter, long aAmperage, long aVoltage, boolean aInsulated, boolean aCanShock) {
        super(aID, aName, aNameRegional, 0);
        mThickNess = aThickNess;
        mMaterial = aMaterial;
        mAmperage = aAmperage;
        mVoltage = aVoltage;
        mInsulated = aInsulated;
        mCanShock = aCanShock;
        mCableLossPerMeter = aCableLossPerMeter;
    }

    public GT_MetaPipeEntity_Cable(String aName, float aThickNess, Materials aMaterial, long aCableLossPerMeter, long aAmperage, long aVoltage, boolean aInsulated, boolean aCanShock) {
        super(aName, 0);
        mThickNess = aThickNess;
        mMaterial = aMaterial;
        mAmperage = aAmperage;
        mVoltage = aVoltage;
        mInsulated = aInsulated;
        mCanShock = aCanShock;
        mCableLossPerMeter = aCableLossPerMeter;
    }

    @Override
    public byte getTileEntityBaseType() {
        return (byte) (mInsulated ? 9 : 8);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaPipeEntity_Cable(mName, mThickNess, mMaterial, mCableLossPerMeter, mAmperage, mVoltage, mInsulated, mCanShock);
    }
    
    private int delay = -1;
    private boolean needUpdateNearestCables = false;
    
    public void UpdateNearestCables(int delayInSeconds) {
    	needUpdateNearestCables = true;
    	delay = delayInSeconds * 20;
    }
    
    public void UpdateNearestCables() {
    	// System.out.println("this != null: " + (this != null));
    	// System.out.println("this instanceof IMetaTileEntityCable: " + (this instanceof IMetaTileEntityCable));
    	
    	if(this != null && this instanceof IMetaTileEntityCable) {
    		UpdateCablesChain((IMetaTileEntityCable)this);
    	}
    	
    	// ќбновить соседние провода если нужно
    	for(byte i = 0; i < 6; i++) {
    		TileEntity entity = getBaseMetaTileEntity().getTileEntityAtSide(i);
    		// System.out.println("Check tile entity " + i + " " + entity);
    		if(entity != null && entity instanceof BaseMetaPipeEntity) {
    			BaseMetaPipeEntity baseMetaPipeEntityTmp = (BaseMetaPipeEntity)entity;
    			GT_MetaPipeEntity_Cable cable = (GT_MetaPipeEntity_Cable)(baseMetaPipeEntityTmp.getMetaTileEntity());
    			// System.out.println("UpdateNearestCables() entity instanceof IMetaTileEntityCable");
    			UpdateCablesChain(cable);
    		}
    	}
    }
    
    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aConnections, byte aColorIndex, boolean aConnected, boolean aRedstone) {
        if (!mInsulated)
            return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[TextureSet.INDEX_wire], Dyes.getModulation(aColorIndex, mMaterial.mRGBa) )};
        if (aConnected) {
            float tThickNess = getThickNess();
            if (tThickNess < 0.124F)
                return new ITexture[]{new GT_RenderedTexture(Textures.BlockIcons.INSULATION_FULL, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
            if (tThickNess < 0.374F)//0.375 x1
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[TextureSet.INDEX_wire], mMaterial.mRGBa), new GT_RenderedTexture(Textures.BlockIcons.INSULATION_TINY, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
            if (tThickNess < 0.499F)//0.500 x2
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[TextureSet.INDEX_wire], mMaterial.mRGBa), new GT_RenderedTexture(Textures.BlockIcons.INSULATION_SMALL, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
            if (tThickNess < 0.624F)//0.625 x4
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[TextureSet.INDEX_wire], mMaterial.mRGBa), new GT_RenderedTexture(Textures.BlockIcons.INSULATION_MEDIUM, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
            if (tThickNess < 0.749F)//0.750 x8
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[TextureSet.INDEX_wire], mMaterial.mRGBa), new GT_RenderedTexture(Textures.BlockIcons.INSULATION_MEDIUM_PLUS, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
            if (tThickNess < 0.874F)//0.825 x12
                return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[TextureSet.INDEX_wire], mMaterial.mRGBa), new GT_RenderedTexture(Textures.BlockIcons.INSULATION_LARGE, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
            return new ITexture[]{new GT_RenderedTexture(mMaterial.mIconSet.mTextures[TextureSet.INDEX_wire], mMaterial.mRGBa), new GT_RenderedTexture(Textures.BlockIcons.INSULATION_HUGE, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
        }
        return new ITexture[]{new GT_RenderedTexture(Textures.BlockIcons.INSULATION_FULL, Dyes.getModulation(aColorIndex, Dyes.CABLE_INSULATION.mRGBa))};
    }

    @Override
    public void onEntityCollidedWithBlock(World aWorld, int aX, int aY, int aZ, Entity aEntity) {
        if (mCanShock && (((BaseMetaPipeEntity) getBaseMetaTileEntity()).mConnections & -128) == 0 && aEntity instanceof EntityLivingBase && !isCoverOnSide((BaseMetaPipeEntity) getBaseMetaTileEntity(), (EntityLivingBase) aEntity))
            GT_Utility.applyElectricityDamage((EntityLivingBase) aEntity, mTransferredVoltageLast20, mTransferredAmperageLast20);
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return false;
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return true;
    }

    @Override
    public final boolean renderInside(byte aSide) {
        return false;
    }

    @Override
    public int getProgresstime() {
        return (int) mTransferredAmperage * 64;
    }

    @Override
    public int maxProgresstime() {
        return (int) mAmperage * 64;
    }

    private void pullFromIc2EnergySources(IGregTechTileEntity aBaseMetaTileEntity) {
        if(!GT_Mod.gregtechproxy.ic2EnergySourceCompat) return;
        
        // ExecutorService es = Executors.newCachedThreadPool();
        for( byte aSide = 0 ; aSide < 6 ; aSide++) {
        	// es.execute(new GT_MetaPipeEntity_CableEnergyThread(this, aBaseMetaTileEntity, aSide));
        	
        	if(isConnectedAtSide(aSide)) {
            	final TileEntity tTileEntity = aBaseMetaTileEntity.getTileEntityAtSide(aSide);
            	final TileEntity tEmitter;
            	
            	if (tTileEntity instanceof IReactorChamber) {
                	tEmitter = (TileEntity) ((IReactorChamber) tTileEntity).getReactor();
            	} else {
            		tEmitter = (tTileEntity == null || tTileEntity instanceof IEnergyTile || EnergyNet.instance == null) ? tTileEntity :
                	EnergyNet.instance.getTileEntity(tTileEntity.getWorldObj(), tTileEntity.xCoord, tTileEntity.yCoord, tTileEntity.zCoord);
            	}

            	if (tEmitter instanceof IEnergySource) {
                	final GT_CoverBehavior coverBehavior = aBaseMetaTileEntity.getCoverBehaviorAtSide(aSide);
                	final int coverId = aBaseMetaTileEntity.getCoverIDAtSide(aSide),
                    coverData = aBaseMetaTileEntity.getCoverDataAtSide(aSide);
                	final ForgeDirection tDirection = ForgeDirection.getOrientation(GT_Utility.getOppositeSide(aSide));

                if (((IEnergySource) tEmitter).emitsEnergyTo((TileEntity) aBaseMetaTileEntity, tDirection) &&
                    coverBehavior.letsEnergyIn(aSide, coverId, coverData, aBaseMetaTileEntity)) {
                    final long tEU = (long) ((IEnergySource) tEmitter).getOfferedEnergy();
                    
                    if (transferElectricity(this, aSide, tEU, 1, Sets.newHashSet((TileEntity) aBaseMetaTileEntity)) > 0)
                        ((IEnergySource) tEmitter).drawEnergy(tEU);
                    
                    //if (transferElectricity(aSide, tEU, 1, Sets.newHashSet((TileEntity) aBaseMetaTileEntity)) > 0)
                    //    ((IEnergySource) tEmitter).drawEnergy(tEU);
                }
                
                if(tEmitter != null) {
                	energyEmmiters.add(tEmitter);
                }
	            }
	        }
        }
        
        /*es.shutdown();
        try {
			boolean finished = es.awaitTermination(50, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    }

    @Override
    public long injectEnergyUnits(byte aSide, long aVoltage, long aAmperage) {
    	if (!isConnectedAtSide(aSide) && aSide != 6)
    		return 0;
        if (!getBaseMetaTileEntity().getCoverBehaviorAtSide(aSide).letsEnergyIn(aSide, getBaseMetaTileEntity().getCoverIDAtSide(aSide), getBaseMetaTileEntity().getCoverDataAtSide(aSide), getBaseMetaTileEntity()))
            return 0;
        
        /*ExecutorService es = Executors.newCachedThreadPool();
        Future<Long> transferResult = es.submit(new GT_MetaPipeEntity_CableTransferElectricity(this, aSide, aVoltage, aAmperage, Sets.newHashSet((TileEntity) getBaseMetaTileEntity())));
        
        try {
			return transferResult.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return transferElectricity(aSide, aVoltage, aAmperage, Sets.newHashSet((TileEntity) getBaseMetaTileEntity()));
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return transferElectricity(aSide, aVoltage, aAmperage, Sets.newHashSet((TileEntity) getBaseMetaTileEntity()));
		}*/
        
        return transferElectricity(this, aSide, aVoltage, aAmperage, Sets.newHashSet((TileEntity) getBaseMetaTileEntity()));
    }

    @Override
    @Deprecated
    public long transferElectricity(byte aSide, long aVoltage, long aAmperage, ArrayList<TileEntity> aAlreadyPassedTileEntityList) {
        return transferElectricity(aSide, aVoltage, aAmperage, new HashSet<>(aAlreadyPassedTileEntityList));
    }

    // Cashed method of electrisity transport
    @Override
    public long transferElectricity(IMetaTileEntityCable startCable, byte aSide, long aVoltage, long aAmperage, HashSet<TileEntity> aAlreadyPassedSet) {
    	if (!isConnectedAtSide(aSide) && aSide != 6)
			return 0;

		final IGregTechTileEntity baseMetaTile = getBaseMetaTileEntity();

		byte i = (byte) ((((aSide / 2) * 2) + 2) % 6); // this bit of trickery makes sure a direction goes to the next
														// cardinal pair. IE, NS goes to E, EW goes to U, UD goes to N.
														// It's a lame way to make sure locally connected machines on a
														// wire get EU first.
		
		long rUsedAmperes = 0;
		
		if(startCableCash.containsKey(startCable)) {
			for(GT_MetaPipeEntity_CableCash cableCash : startCableCash.get(startCable).consumers) {
				rUsedAmperes += insertEnergyInto(cableCash.tTileEntity, cableCash.tSide, cableCash.voltage, cableCash.aAmperage);
				
				if(rUsedAmperes >= aAmperage)
					break;
    		}
			
			for(IMetaTileEntityCable cable : startCableCash.get(startCable).cablesChain) {
				if(aAmperage > ((GT_MetaPipeEntity_Cable)cable).mAmperage) {
					((GT_MetaPipeEntity_Cable)cable).getBaseMetaTileEntity().setToFire();
					break;
				}
			}
    	} else {
    		GT_MetaPipeEntity_CableChain cableCashList = 
    				recalculateCables(new GT_MetaPipeEntity_CableChain(), aAlreadyPassedSet, aAmperage, rUsedAmperes, aVoltage, aSide, baseMetaTile);
    		
    		//System.out.println("Cables addded: " + cableCashList.cablesChain.size());
        	//System.out.println("Consumers added: " + cableCashList.consumers.size());
    		//cableCashList.showCablesChainsCoordinates();
        	//cableCashList.showConsumersCoordinates();
    		
    		//System.out.println("Cables recalculated!");
    		startCableCash.put(startCable, cableCashList);
    		
    		/*for(GT_MetaPipeEntity_CableCash cableCash : startCableCash.get(startCable).consumers) {
    			rUsedAmperes += insertEnergyInto(cableCash.tTileEntity, cableCash.tSide, cableCash.voltage, cableCash.aAmperage);
    		}*/
    	}
		
		/*aVoltage -= mCableLossPerMeter;
		if (aVoltage > 0)
			for (byte j = 0; j < 6 && aAmperage > rUsedAmperes; j++, i = (byte) ((i + 1) % 6))
				if (i != aSide && isConnectedAtSide(i) && baseMetaTile.getCoverBehaviorAtSide(i).letsEnergyOut(i,
						baseMetaTile.getCoverIDAtSide(i), baseMetaTile.getCoverDataAtSide(i), baseMetaTile)) {
					final TileEntity tTileEntity = baseMetaTile.getTileEntityAtSide(i);

					if (tTileEntity != null && aAlreadyPassedSet.add(tTileEntity)) {
						final byte tSide = GT_Utility.getOppositeSide(i);
						final IGregTechTileEntity tBaseMetaTile = tTileEntity instanceof IGregTechTileEntity
								? ((IGregTechTileEntity) tTileEntity)
								: null;
						final IMetaTileEntity tMeta = tBaseMetaTile != null ? tBaseMetaTile.getMetaTileEntity() : null;

						if (tMeta instanceof IMetaTileEntityCable) {
							if (tBaseMetaTile.getCoverBehaviorAtSide(tSide).
									letsEnergyIn(tSide, tBaseMetaTile.getCoverIDAtSide(tSide), tBaseMetaTile.getCoverDataAtSide(tSide), tBaseMetaTile) && 
									((IGregTechTileEntity) tTileEntity).getTimer() > 50) {
								rUsedAmperes += ((IMetaTileEntityCable) ((IGregTechTileEntity) tTileEntity)
										.getMetaTileEntity()).transferElectricity(startCable, tSide, aVoltage, aAmperage - rUsedAmperes, aAlreadyPassedSet);
							}
						} else {
							rUsedAmperes += insertEnergyInto(tTileEntity, tSide, aVoltage, aAmperage - rUsedAmperes);
						}

					}
				}*/
		
		// —горание проводов при повышенном напр€жении
		/*mTransferredAmperage += rUsedAmperes;
		mTransferredVoltageLast20 = Math.max(mTransferredVoltageLast20, aVoltage);
		mTransferredAmperageLast20 = Math.max(mTransferredAmperageLast20, mTransferredAmperage);
		if (aVoltage > mVoltage || mTransferredAmperage > mAmperage) {
			if (mOverheat > GT_Mod.gregtechproxy.mWireHeatingTicks * 100) {
				getBaseMetaTileEntity().setToFire();
			} else {
				mOverheat += 100;
			}
			return aAmperage;
		}*/
		
		return rUsedAmperes;
    }
    
    public static boolean isCableInChain(IMetaTileEntityCable cable) {
    	Iterator it = GT_MetaPipeEntity_Cable.startCableCash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            
            if(((GT_MetaPipeEntity_CableChain)pair.getValue()).isCableInChain(cable)) {
            	return true;
            }
            
            it.remove(); // avoids a ConcurrentModificationException
        }
    	
    	return false;
    }
    
    public static void UpdateCablesChain(IMetaTileEntityCable cable) {
    	ArrayList<IMetaTileEntityCable> keysToRemove = new ArrayList<IMetaTileEntityCable>();
    	
    	Iterator it = GT_MetaPipeEntity_Cable.startCableCash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            
            if(((GT_MetaPipeEntity_CableChain)pair.getValue()).isCableInChain(cable)) {
            	keysToRemove.add((IMetaTileEntityCable)pair.getKey());
            }
            
            it.remove(); // avoids a ConcurrentModificationException
        }
        
        for(IMetaTileEntityCable c : keysToRemove) {
        	System.out.println("Remove key from cash");
        	
        	GT_MetaPipeEntity_Cable.startCableCash.remove(c);
        }
        
        
        
        keysToRemove.clear();
    }
    
    public GT_MetaPipeEntity_CableChain recalculateCables(GT_MetaPipeEntity_CableChain result,
    															HashSet<TileEntity> aAlreadyPassedSet, 
    															long aAmperage, 
    															long rUsedAmperes, 
    															long aVoltage,
    															byte aSide,
    															IGregTechTileEntity baseMetaTile){
    	/*if (!isConnectedAtSide(aSide) && aSide != 6)
			return result;*/
    	
    	if(this instanceof IMetaTileEntityCable) {
    		result.cablesChain.add((IMetaTileEntityCable)this);
    	}
    	
    	byte i = (byte) ((((aSide / 2) * 2) + 2) % 6);
    	
    	long tmpRUsedAmpers = 0;
    	
    	aVoltage -= mCableLossPerMeter;
    	for (byte j = 0; j < 6 && aAmperage > rUsedAmperes; j++, i = (byte) ((i + 1) % 6)) {
			if (/*i != aSide && isConnectedAtSide(i) &&*/
				baseMetaTile.getCoverBehaviorAtSide(i).letsEnergyOut(i, baseMetaTile.getCoverIDAtSide(i), baseMetaTile.getCoverDataAtSide(i), baseMetaTile)) {
				final TileEntity tTileEntity = baseMetaTile.getTileEntityAtSide(i);

				if (tTileEntity != null && aAlreadyPassedSet.add(tTileEntity)) {
					final byte tSide = GT_Utility.getOppositeSide(i);
					final IGregTechTileEntity tBaseMetaTile = tTileEntity instanceof IGregTechTileEntity
							? ((IGregTechTileEntity) tTileEntity)
							: null;
					final IMetaTileEntity tMeta = tBaseMetaTile != null ? tBaseMetaTile.getMetaTileEntity() : null;

					if (tMeta instanceof IMetaTileEntityCable) {
						if (tBaseMetaTile.getCoverBehaviorAtSide(tSide).
								letsEnergyIn(tSide, tBaseMetaTile.getCoverIDAtSide(tSide), tBaseMetaTile.getCoverDataAtSide(tSide), tBaseMetaTile) /*&& 
								((IGregTechTileEntity) tTileEntity).getTimer() > 50*/) {
							//rUsedAmperes += ((IMetaTileEntityCable) ((IGregTechTileEntity) tTileEntity)
							//		.getMetaTileEntity()).transferElectricity(tSide, aVoltage, aAmperage - rUsedAmperes, aAlreadyPassedSet);
							
							result.cablesChain.add((IMetaTileEntityCable)tMeta);
							result = ((GT_MetaPipeEntity_Cable)tMeta).recalculateCables(result, aAlreadyPassedSet, aAmperage, rUsedAmperes, aVoltage, aSide, tBaseMetaTile);
							tmpRUsedAmpers += result.rUsedAmperes;
						}
					} else {
						if(energyEmmiters.contains(tTileEntity) == false) {
							result.consumers.add(new GT_MetaPipeEntity_CableCash(tTileEntity, tSide, aVoltage, aAmperage - rUsedAmperes));
							
							tmpRUsedAmpers += insertEnergyInto(tTileEntity, tSide, aVoltage, aAmperage - rUsedAmperes);
						}
					}

				}
			}
    	}
    	
    	mTransferredAmperage += tmpRUsedAmpers;
		mTransferredVoltageLast20 = Math.max(mTransferredVoltageLast20, aVoltage);
		mTransferredAmperageLast20 = Math.max(mTransferredAmperageLast20, mTransferredAmperage);
		
		//System.out.println("aVoltage: " + aVoltage);
		//System.out.println("mVoltage: " + mVoltage);
		//System.out.println("mTransferredAmperage: " + mTransferredAmperage);
		//System.out.println("mAmperage: " + mAmperage);
		//System.out.println("aVoltage > mVoltage || mTransferredAmperage > mAmperage");
		
		if (aVoltage > mVoltage || mTransferredAmperage > mAmperage) {
			if (mOverheat > GT_Mod.gregtechproxy.mWireHeatingTicks * 100) {
				getBaseMetaTileEntity().setToFire();
			} else {
				mOverheat += 100;
			}
			result.rUsedAmperes += aAmperage;
		}
		result.rUsedAmperes += rUsedAmperes;
    	
    	return result;
    }
    
	@Override
	public long transferElectricity(byte aSide, long aVoltage, long aAmperage, HashSet<TileEntity> aAlreadyPassedSet) {
		if (!isConnectedAtSide(aSide) && aSide != 6)
			return 0;

		long rUsedAmperes = 0;
		final IGregTechTileEntity baseMetaTile = getBaseMetaTileEntity();

		byte i = (byte) ((((aSide / 2) * 2) + 2) % 6); // this bit of trickery makes sure a direction goes to the next
														// cardinal pair. IE, NS goes to E, EW goes to U, UD goes to N.
														// It's a lame way to make sure locally connected machines on a
														// wire get EU first.

		aVoltage -= mCableLossPerMeter;
		if (aVoltage > 0)
			for (byte j = 0; j < 6 && aAmperage > rUsedAmperes; j++, i = (byte) ((i + 1) % 6))
				if (i != aSide && isConnectedAtSide(i) && baseMetaTile.getCoverBehaviorAtSide(i).letsEnergyOut(i,
						baseMetaTile.getCoverIDAtSide(i), baseMetaTile.getCoverDataAtSide(i), baseMetaTile)) {
					final TileEntity tTileEntity = baseMetaTile.getTileEntityAtSide(i);

					if (tTileEntity != null && aAlreadyPassedSet.add(tTileEntity)) {
						final byte tSide = GT_Utility.getOppositeSide(i);
						final IGregTechTileEntity tBaseMetaTile = tTileEntity instanceof IGregTechTileEntity
								? ((IGregTechTileEntity) tTileEntity)
								: null;
						final IMetaTileEntity tMeta = tBaseMetaTile != null ? tBaseMetaTile.getMetaTileEntity() : null;

						if (tMeta instanceof IMetaTileEntityCable) {
							if (tBaseMetaTile.getCoverBehaviorAtSide(tSide).
									letsEnergyIn(tSide, tBaseMetaTile.getCoverIDAtSide(tSide), tBaseMetaTile.getCoverDataAtSide(tSide), tBaseMetaTile) && 
									((IGregTechTileEntity) tTileEntity).getTimer() > 50) {
								rUsedAmperes += ((IMetaTileEntityCable) ((IGregTechTileEntity) tTileEntity)
										.getMetaTileEntity()).transferElectricity(tSide, aVoltage, aAmperage - rUsedAmperes, aAlreadyPassedSet);
							}
						} else {
							rUsedAmperes += insertEnergyInto(tTileEntity, tSide, aVoltage, aAmperage - rUsedAmperes);
						}

					}
				}
		mTransferredAmperage += rUsedAmperes;
		mTransferredVoltageLast20 = Math.max(mTransferredVoltageLast20, aVoltage);
		mTransferredAmperageLast20 = Math.max(mTransferredAmperageLast20, mTransferredAmperage);
		if (aVoltage > mVoltage || mTransferredAmperage > mAmperage) {
			if (mOverheat > GT_Mod.gregtechproxy.mWireHeatingTicks * 100) {
				getBaseMetaTileEntity().setToFire();
			} else {
				mOverheat += 100;
			}
			return aAmperage;
		}
		return rUsedAmperes;
	}
	
    public long insertEnergyInto(TileEntity tTileEntity, byte tSide, long aVoltage, long aAmperage) {
        if (aAmperage == 0 || tTileEntity == null) return 0;

        final IGregTechTileEntity baseMetaTile = getBaseMetaTileEntity();
        final ForgeDirection tDirection = ForgeDirection.getOrientation(tSide);

        if (tTileEntity instanceof IEnergyConnected) {
            return ((IEnergyConnected) tTileEntity).injectEnergyUnits(tSide, aVoltage, aAmperage);
        }

        // AE2 Compat
        if (GT_Mod.gregtechproxy.mAE2Integration && tTileEntity instanceof appeng.tile.powersink.IC2) {
            if (((appeng.tile.powersink.IC2) tTileEntity).acceptsEnergyFrom((TileEntity) baseMetaTile, tDirection)) {
                long rUsedAmperes = 0;
                while (aAmperage > rUsedAmperes && ((appeng.tile.powersink.IC2)tTileEntity).getDemandedEnergy() > 0 && 
                		((appeng.tile.powersink.IC2)tTileEntity).injectEnergy(tDirection, aVoltage, aVoltage) <= aVoltage)
                    rUsedAmperes++;

                return rUsedAmperes;
            }
            return 0;
        }

        // GC Compat
        if (GregTech_API.mGalacticraft) {
            if (tTileEntity instanceof IEnergyHandlerGC) {
                if (!(tTileEntity instanceof IConnector) || ((IConnector) tTileEntity).canConnect(tDirection, NetworkType.POWER)) {
                    EnergySource eSource = (EnergySource) GT_Utility.callConstructor("micdoodle8.mods.galacticraft.api.power.EnergySource.EnergySourceAdjacent", 0, null, false, new Object[]{tDirection});

                    float tSizeToReceive = aVoltage * EnergyConfigHandler.IC2_RATIO, tStored = ((IEnergyHandlerGC) tTileEntity).getEnergyStoredGC(eSource);
                    if (tSizeToReceive >= tStored || tSizeToReceive <= ((IEnergyHandlerGC) tTileEntity).getMaxEnergyStoredGC(eSource) - tStored) {
                        float tReceived = ((IEnergyHandlerGC) tTileEntity).receiveEnergyGC(eSource, tSizeToReceive, false);
                        if (tReceived > 0) {
                            tSizeToReceive -= tReceived;
                            while (tSizeToReceive > 0) {
                                tReceived = ((IEnergyHandlerGC) tTileEntity).receiveEnergyGC(eSource, tSizeToReceive, false);
                                if (tReceived < 1) break;
                                tSizeToReceive -= tReceived;
                            }
                            return 1;
                        }
                    }
                }
                return 0;
            }
        }

        // IC2 Compat
        {
            final TileEntity tIc2Acceptor = (tTileEntity instanceof IEnergyTile || EnergyNet.instance == null) ? tTileEntity :
                EnergyNet.instance.getTileEntity(tTileEntity.getWorldObj(), tTileEntity.xCoord, tTileEntity.yCoord, tTileEntity.zCoord);

            if (tIc2Acceptor instanceof IEnergySink && ((IEnergySink) tIc2Acceptor).acceptsEnergyFrom((TileEntity) baseMetaTile, tDirection)) {
                long rUsedAmperes = 0;
                while (aAmperage > rUsedAmperes && ((IEnergySink) tIc2Acceptor).getDemandedEnergy() > 0 && 
                		((IEnergySink) tIc2Acceptor).injectEnergy(tDirection, aVoltage, aVoltage) <= aVoltage)
                    rUsedAmperes++;
                return rUsedAmperes;
            }
        }

        // RF Compat
        if (GregTech_API.mOutputRF && tTileEntity instanceof IEnergyReceiver) {
            final IEnergyReceiver rfReceiver = (IEnergyReceiver) tTileEntity;
            long rfOUT = aVoltage * GregTech_API.mEUtoRF / 100, rUsedAmperes = 0;
            int rfOut = rfOUT > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rfOUT;

            if (rfReceiver.receiveEnergy(tDirection, rfOut, true) == rfOut) {
                rfReceiver.receiveEnergy(tDirection, rfOut, false);
                rUsedAmperes++;
            }
            else if (rfReceiver.receiveEnergy(tDirection, rfOut, true) > 0) {
                if (mRestRF == 0) {
                    int RFtrans = rfReceiver.receiveEnergy(tDirection, (int) rfOut, false);
                    rUsedAmperes++;
                    mRestRF = rfOut - RFtrans;
                } else {
                    int RFtrans = rfReceiver.receiveEnergy(tDirection, (int) mRestRF, false);
                    mRestRF = mRestRF - RFtrans;
                }
            }
            if (GregTech_API.mRFExplosions && rfReceiver.getMaxEnergyStored(tDirection) < rfOut * 600) {
                if (rfOut > 32 * GregTech_API.mEUtoRF / 100) this.doExplosion(rfOut);
            }
            return rUsedAmperes;
        }

        return 0;
    }

    @Override
    public void onCreated(ItemStack aStack, World aWorld, EntityPlayer aPlayer) {
    	super.onCreated(aStack, aWorld, aPlayer);
    	
    	// System.out.println("onCreated");
    	
    	UpdateNearestCables();
    }
    
    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
    	// TODO Auto-generated method stub
    	super.onFirstTick(aBaseMetaTileEntity);
    	
    	// System.out.println("onFirstTick");
    	
    	UpdateNearestCables();
    }
    
    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide()) {
        	if (GT_Mod.gregtechproxy.ic2EnergySourceCompat) pullFromIc2EnergySources(aBaseMetaTileEntity);
            
            mTransferredAmperage = 0;
            if(mOverheat>0) mOverheat--;
            
            if (aTick % 20 == 0) {
                mTransferredVoltageLast20 = 0;
                mTransferredAmperageLast20 = 0;
                if (!GT_Mod.gregtechproxy.gt6Cable || mCheckConnections) checkConnections();
            }
            
            if(needUpdateNearestCables) {
            	if(delay > 0) {
            		delay--;
            	} else {
            		needUpdateNearestCables = false;
            		delay = -1;
            		UpdateNearestCables();
            	}
            }
            
            ClearListOfEmmiters();
        }else if(aBaseMetaTileEntity.isClientSide() && GT_Client.changeDetected==4) aBaseMetaTileEntity.issueTextureUpdate();
    }
    
    private void ClearListOfEmmiters() {
    	HashSet<TileEntity> entityToRemove = new HashSet<TileEntity>();
        
        for(TileEntity entity : energyEmmiters) {
        	if(entity == null) {
        		entityToRemove.add(entity);
        	}
        }
        
        for(TileEntity entity : entityToRemove) {
        	energyEmmiters.remove(entity);
        }
        
        entityToRemove.clear();
    }
    
    /*@Override
    public void onRemoval() {
    	super.onRemoval();
    	
    	System.out.println("onRemoval");
    	
    	UpdateNearestCables();
    }*/
    
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer, byte aSide, float aX,
    		float aY, float aZ) {
    	
    	// System.out.println("onRightclick");
    	
    	UpdateNearestCables();
    	
    	return super.onRightclick(aBaseMetaTileEntity, aPlayer, aSide, aX, aY, aZ);
    }
    
    @Override
    public void onLeftclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
    	// System.out.println("onLeftclick");
    	
    	UpdateNearestCables();
    	
    	super.onLeftclick(aBaseMetaTileEntity, aPlayer);
    }
    
    @Override
    public boolean onWireCutterRightClick(byte aSide, byte aWrenchingSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        if (GT_Mod.gregtechproxy.gt6Cable && GT_ModHandler.damageOrDechargeItem(aPlayer.inventory.getCurrentItem(), 1, 500, aPlayer)) {
            if(isConnectedAtSide(aWrenchingSide)) {
                disconnect(aWrenchingSide);
                GT_Utility.sendChatToPlayer(aPlayer, trans("215", "Disconnected"));
            }else if(!GT_Mod.gregtechproxy.costlyCableConnection){
    			if (connect(aWrenchingSide) > 0)
    				GT_Utility.sendChatToPlayer(aPlayer, trans("214", "Connected"));
    		}
            return true;
        }
        return false;
    }

    public boolean onSolderingToolRightClick(byte aSide, byte aWrenchingSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        if (GT_Mod.gregtechproxy.gt6Cable && GT_ModHandler.damageOrDechargeItem(aPlayer.inventory.getCurrentItem(), 1, 500, aPlayer)) {
            if (isConnectedAtSide(aWrenchingSide)) {
    			disconnect(aWrenchingSide);
    			GT_Utility.sendChatToPlayer(aPlayer, trans("215", "Disconnected"));
            } else if (!GT_Mod.gregtechproxy.costlyCableConnection || GT_ModHandler.consumeSolderingMaterial(aPlayer)) {
                if (connect(aWrenchingSide) > 0)
                    GT_Utility.sendChatToPlayer(aPlayer, trans("214", "Connected"));
    		}
    		return true;
    	}
        return false;
    }

    @Override
    public boolean letsIn(GT_CoverBehavior coverBehavior, byte aSide, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        return coverBehavior.letsEnergyIn(aSide, aCoverID, aCoverVariable, aTileEntity);
    }

    @Override
    public boolean letsOut(GT_CoverBehavior coverBehavior, byte aSide, int aCoverID, int aCoverVariable, ICoverable aTileEntity) {
        return coverBehavior.letsEnergyOut(aSide, aCoverID, aCoverVariable, aTileEntity);
    }

    @Override
    public boolean canConnect(byte aSide, TileEntity tTileEntity) {
        final IGregTechTileEntity baseMetaTile = getBaseMetaTileEntity();
        final GT_CoverBehavior coverBehavior = baseMetaTile.getCoverBehaviorAtSide(aSide);
        final byte tSide = GT_Utility.getOppositeSide(aSide);
        final ForgeDirection tDir = ForgeDirection.getOrientation(tSide);

        // GT Machine handling
        if ((tTileEntity instanceof IEnergyConnected) &&
            (((IEnergyConnected) tTileEntity).inputEnergyFrom(tSide, false) || ((IEnergyConnected) tTileEntity).outputsEnergyTo(tSide, false)))
            return true;

        // Solar Panel Compat
        if (coverBehavior instanceof GT_Cover_SolarPanel) return true;

        // ((tIsGregTechTileEntity && tIsTileEntityCable) && (tAlwaysLookConnected || tLetEnergyIn || tLetEnergyOut) ) --> Not needed

        // GC Compat
        if (GregTech_API.mGalacticraft) {
            if (tTileEntity instanceof IEnergyHandlerGC && (!(tTileEntity instanceof IConnector) || ((IConnector) tTileEntity).canConnect(tDir, NetworkType.POWER)))
                return true;
        }

        // AE2-p2p Compat
        if (GT_Mod.gregtechproxy.mAE2Integration) {
            if (tTileEntity instanceof IEnergySource && tTileEntity instanceof IPartHost && ((IPartHost)tTileEntity).getPart(tDir) instanceof PartP2PGTPower && ((IEnergySource) tTileEntity).emitsEnergyTo((TileEntity) baseMetaTile, tDir))
                return true;
            if (tTileEntity instanceof appeng.tile.powersink.IC2 && ((appeng.tile.powersink.IC2)tTileEntity).acceptsEnergyFrom((TileEntity)baseMetaTile, tDir))
                return true;
        }

        // IC2 Compat
        {
            final TileEntity ic2Energy;

            if (tTileEntity instanceof IReactorChamber)
                ic2Energy = (TileEntity) ((IReactorChamber) tTileEntity).getReactor();
            else
                ic2Energy = (tTileEntity == null || tTileEntity instanceof IEnergyTile || EnergyNet.instance == null) ? tTileEntity :
                    EnergyNet.instance.getTileEntity(tTileEntity.getWorldObj(), tTileEntity.xCoord, tTileEntity.yCoord, tTileEntity.zCoord);

            // IC2 Sink Compat
            if ((ic2Energy instanceof IEnergySink) && ((IEnergySink) ic2Energy).acceptsEnergyFrom((TileEntity) baseMetaTile, tDir))
                return true;

            // IC2 Source Compat
            if (GT_Mod.gregtechproxy.ic2EnergySourceCompat && (ic2Energy instanceof IEnergySource)) {
                if (((IEnergySource) ic2Energy).emitsEnergyTo((TileEntity) baseMetaTile, tDir)) {
                    return true;
                }
            }
        }
        // RF Output Compat
        if (GregTech_API.mOutputRF && tTileEntity instanceof IEnergyReceiver && ((IEnergyReceiver) tTileEntity).canConnectEnergy(tDir))
            return true;

        // RF Input Compat
        if (GregTech_API.mInputRF && (tTileEntity instanceof IEnergyEmitter && ((IEnergyEmitter) tTileEntity).emitsEnergyTo((TileEntity)baseMetaTile, tDir)))
            return true;


        return false;
    }

	@Override
    public boolean getGT6StyleConnection() {
        // Yes if GT6 Cables are enabled
        return GT_Mod.gregtechproxy.gt6Cable;
            }


    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return false;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Max Voltage: %%%" + EnumChatFormatting.GREEN + mVoltage + " (" + VN[GT_Utility.getTier(mVoltage)] + ")" + EnumChatFormatting.GRAY,
                "Max Amperage: %%%" + EnumChatFormatting.YELLOW + mAmperage + EnumChatFormatting.GRAY,
                "Loss/Meter/Ampere: %%%" + EnumChatFormatting.RED + mCableLossPerMeter + EnumChatFormatting.GRAY + "%%% EU-Volt"
        };
    }

    @Override
    public float getThickNess() {
        if (GT_Mod.instance.isClientSide() && (GT_Client.hideValue & 0x1) != 0) return 0.0625F;
        return mThickNess;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        if (GT_Mod.gregtechproxy.gt6Cable)
        	aNBT.setByte("mConnections", mConnections);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        if (GT_Mod.gregtechproxy.gt6Cable) {
        	mConnections = aNBT.getByte("mConnections");
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World aWorld, int aX, int aY, int aZ) {
    	if (GT_Mod.instance.isClientSide() && (GT_Client.hideValue & 0x2) != 0)
    		return AxisAlignedBB.getBoundingBox(aX, aY, aZ, aX + 1, aY + 1, aZ + 1);
    	else
    		return getActualCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ);
    }

    private AxisAlignedBB getActualCollisionBoundingBoxFromPool(World aWorld, int aX, int aY, int aZ) {
    	float tSpace = (1f - mThickNess)/2;
    	float tSide0 = tSpace;
    	float tSide1 = 1f - tSpace;
    	float tSide2 = tSpace;
    	float tSide3 = 1f - tSpace;
    	float tSide4 = tSpace;
    	float tSide5 = 1f - tSpace;
    	
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 0) != 0){tSide0=tSide2=tSide4=0;tSide3=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 1) != 0){tSide2=tSide4=0;tSide1=tSide3=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 2) != 0){tSide0=tSide2=tSide4=0;tSide1=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 3) != 0){tSide0=tSide4=0;tSide1=tSide3=tSide5=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 4) != 0){tSide0=tSide2=tSide4=0;tSide1=tSide3=1;}
    	if(getBaseMetaTileEntity().getCoverIDAtSide((byte) 5) != 0){tSide0=tSide2=0;tSide1=tSide3=tSide5=1;}
    	
    	byte tConn = ((BaseMetaPipeEntity) getBaseMetaTileEntity()).mConnections;
    	if((tConn & (1 << ForgeDirection.DOWN.ordinal()) ) != 0) tSide0 = 0f;
    	if((tConn & (1 << ForgeDirection.UP.ordinal())   ) != 0) tSide1 = 1f;
    	if((tConn & (1 << ForgeDirection.NORTH.ordinal())) != 0) tSide2 = 0f;
    	if((tConn & (1 << ForgeDirection.SOUTH.ordinal())) != 0) tSide3 = 1f;
    	if((tConn & (1 << ForgeDirection.WEST.ordinal()) ) != 0) tSide4 = 0f;
    	if((tConn & (1 << ForgeDirection.EAST.ordinal()) ) != 0) tSide5 = 1f;
    	
    	return AxisAlignedBB.getBoundingBox(aX + tSide4, aY + tSide0, aZ + tSide2, aX + tSide5, aY + tSide1, aZ + tSide3);
    }

    @Override
    public void addCollisionBoxesToList(World aWorld, int aX, int aY, int aZ, AxisAlignedBB inputAABB, List<AxisAlignedBB> outputAABB, Entity collider) {
    	super.addCollisionBoxesToList(aWorld, aX, aY, aZ, inputAABB, outputAABB, collider);
    	if (GT_Mod.instance.isClientSide() && (GT_Client.hideValue & 0x2) != 0) {
    		AxisAlignedBB aabb = getActualCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ);
    		if (inputAABB.intersectsWith(aabb)) outputAABB.add(aabb);
    	}
    }
}
