// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   GT_Client.java

package gregtech.common;

import codechicken.lib.vec.Rotation;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.interfaces.IHasFluidDisplayItem;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.ITurnable;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.net.GT_Packet_ClientPreference;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.util.*;
import gregtech.common.net.MessageUpdateFluidDisplayItem;
import gregtech.common.render.*;
import ic2.api.tile.IWrenchable;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.opengl.GL11;

import java.net.URL;
import java.util.*;

// Referenced classes of package gregtech.common:
//            GT_Proxy

public class GT_Client extends GT_Proxy implements Runnable {
    
    private static final List<Block> ROTATABLE_VANILLA_BLOCKS = Arrays.asList(Blocks.piston, Blocks.sticky_piston, Blocks.furnace, Blocks.lit_furnace, Blocks.dropper, Blocks.dispenser, Blocks.chest, Blocks.trapped_chest, Blocks.ender_chest, Blocks.hopper, Blocks.pumpkin, Blocks.lit_pumpkin);
    
    private final HashSet<String> mCapeList = new HashSet<>();
    private final GT_CapeRenderer mCapeRenderer;
    private final List<Materials> mPosR;
    private final List<Materials> mPosG;
    private final List<Materials> mPosB;
    private final List<Materials> mPosA = Collections.emptyList();
    private final List<Materials> mNegR;
    private final List<Materials> mNegG;
    private final List<Materials> mNegB;
    private final List<Materials> mNegA = Collections.emptyList();
    private final List<Materials> mMoltenPosR;
    private final List<Materials> mMoltenPosG;
    private final List<Materials> mMoltenPosB;
    private final List<Materials> mMoltenPosA = Collections.emptyList();
    private final List<Materials> mMoltenNegR;
    private final List<Materials> mMoltenNegG;
    private final List<Materials> mMoltenNegB;
    private final List<Materials> mMoltenNegA = Collections.emptyList();
    private long mAnimationTick;
    /**
     * This is the place to def the value used below
     **/
    private long afterSomeTime;
    private boolean mAnimationDirection;
    private int mLastUpdatedBlockX;
    private int mLastUpdatedBlockY;
    private int mLastUpdatedBlockZ;
    private boolean isFirstClientPlayerTick;
    private String mMessage;
    private GT_ClientPreference mPreference;
    private boolean mFirstTick = false;
    public GT_Client() {
        mCapeRenderer       = new GT_CapeRenderer(mCapeList);
        mAnimationTick      = 0L;
        mAnimationDirection = false;
        mPosR               = Arrays.asList(Materials.Enderium, Materials.Uranium235, Materials.Plutonium241, Materials.NaquadahEnriched, Materials.Naquadria, Materials.Sunnarium, Materials.Glowstone);
        mPosG               = Arrays.asList(Materials.Enderium, Materials.Uranium235, Materials.Plutonium241, Materials.NaquadahEnriched, Materials.Naquadria, Materials.Sunnarium, Materials.Glowstone);
        mPosB               = Arrays.asList(Materials.Enderium, Materials.Uranium235, Materials.Plutonium241, Materials.NaquadahEnriched, Materials.Naquadria);
        mNegR               = Collections.singletonList(Materials.NetherStar);
        mNegG               = Collections.singletonList(Materials.NetherStar);
        mNegB               = Collections.singletonList(Materials.NetherStar);
        mMoltenPosR         = Arrays.asList(Materials.Enderium, Materials.NetherStar, Materials.Uranium235, Materials.Plutonium241, Materials.NaquadahEnriched, Materials.Naquadria, Materials.Sunnarium, Materials.Glowstone);
        mMoltenPosG         = Arrays.asList(Materials.Enderium, Materials.NetherStar, Materials.Uranium235, Materials.Plutonium241, Materials.NaquadahEnriched, Materials.Naquadria, Materials.Sunnarium, Materials.Glowstone);
        mMoltenPosB         = Arrays.asList(Materials.Enderium, Materials.NetherStar, Materials.Uranium235, Materials.Plutonium241, Materials.NaquadahEnriched, Materials.Naquadria);
        mMoltenNegR         = Collections.emptyList();
        mMoltenNegG         = Collections.emptyList();
        mMoltenNegB         = Collections.emptyList();
    }

    private static boolean checkedForChicken = false;

    private static void drawGrid(DrawBlockHighlightEvent aEvent, boolean showCoverConnections) {
        if (!checkedForChicken) {
            try {
                Class.forName("codechicken.lib.vec.Rotation");
            } catch (Throwable e) {
                if (GT_Values.D1) {
                    e.printStackTrace(GT_Log.err);
                }
                return;
            }
            checkedForChicken = true;
        }
        GL11.glPushMatrix();
        GL11.glTranslated(-(aEvent.player.lastTickPosX + (aEvent.player.posX - aEvent.player.lastTickPosX) * (double) aEvent.partialTicks), -(aEvent.player.lastTickPosY + (aEvent.player.posY - aEvent.player.lastTickPosY) * (double) aEvent.partialTicks), -(aEvent.player.lastTickPosZ + (aEvent.player.posZ - aEvent.player.lastTickPosZ) * (double) aEvent.partialTicks));
        GL11.glTranslated((float) aEvent.target.blockX + 0.5F, (float) aEvent.target.blockY + 0.5F, (float) aEvent.target.blockZ + 0.5F);
        Rotation.sideRotations[aEvent.target.sideHit].glApply();
        GL11.glTranslated(0.0D, -0.501D, 0.0D);
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.5F);
        GL11.glBegin(1);
        GL11.glVertex3d(+.50D, .0D, -.25D);
        GL11.glVertex3d(-.50D, .0D, -.25D);
        GL11.glVertex3d(+.50D, .0D, +.25D);
        GL11.glVertex3d(-.50D, .0D, +.25D);
        GL11.glVertex3d(+.25D, .0D, -.50D);
        GL11.glVertex3d(+.25D, .0D, +.50D);
        GL11.glVertex3d(-.25D, .0D, -.50D);
        GL11.glVertex3d(-.25D, .0D, +.50D);
        TileEntity tTile = aEvent.player.worldObj.getTileEntity(aEvent.target.blockX, aEvent.target.blockY, aEvent.target.blockZ);
        byte tConnections = 0;
        if (tTile instanceof ICoverable){
            if (showCoverConnections) {
                for (byte i = 0; i < 6; i++) {
                    if ( ((ICoverable) tTile).getCoverIDAtSide(i) > 0)
                        tConnections = (byte)(tConnections + (1 << i));
                }
            }
            else if (tTile instanceof BaseMetaPipeEntity)
                tConnections = ((BaseMetaPipeEntity) tTile).mConnections;
        }

        if (tConnections>0) {
        	int[][] GridSwitchArr = new int[][]{
            	{0, 5, 3, 1, 2, 4},
            	{5, 0, 1, 3, 2, 4},
            	{1, 3, 0, 5, 2, 4},
            	{3, 1, 5, 0, 2, 4},
            	{4, 2, 3, 1, 0, 5},
            	{2, 4, 3, 1, 5, 0},
            };

        	for (byte i = 0; i < 6; i++) {
        		if ((tConnections & (1 << i)) != 0) {
        			switch (GridSwitchArr[aEvent.target.sideHit][i]) {
        	        case 0:
        	        	GL11.glVertex3d(+.25D, .0D, +.25D);
        	        	GL11.glVertex3d(-.25D, .0D, -.25D);
        	        	GL11.glVertex3d(-.25D, .0D, +.25D);
        	        	GL11.glVertex3d(+.25D, .0D, -.25D);
        	        	break;
        	        case 1:
        	        	GL11.glVertex3d(-.25D, .0D, +.50D);
        				GL11.glVertex3d(+.25D, .0D, +.25D);
        				GL11.glVertex3d(-.25D, .0D, +.25D);
        				GL11.glVertex3d(+.25D, .0D, +.50D);
        	        	break;
        	        case 2:
        	        	GL11.glVertex3d(-.50D, .0D, -.25D);
        				GL11.glVertex3d(-.25D, .0D, +.25D);
        				GL11.glVertex3d(-.50D, .0D, +.25D);
        				GL11.glVertex3d(-.25D, .0D, -.25D);
        	        	break;
        	        case 3:
        	        	GL11.glVertex3d(-.25D, .0D, -.50D);
        				GL11.glVertex3d(+.25D, .0D, -.25D);
        				GL11.glVertex3d(-.25D, .0D, -.25D);
        				GL11.glVertex3d(+.25D, .0D, -.50D);
        	        	break;
        	        case 4:
        	        	GL11.glVertex3d(+.50D, .0D, -.25D);
        				GL11.glVertex3d(+.25D, .0D, +.25D);
        				GL11.glVertex3d(+.50D, .0D, +.25D);
        				GL11.glVertex3d(+.25D, .0D, -.25D);
        	        	break;
        	        case 5:
        	        	GL11.glVertex3d(+.50D, .0D, +.50D);
        	        	GL11.glVertex3d(+.25D, .0D, +.25D);
        	        	GL11.glVertex3d(+.50D, .0D, +.25D);
        	        	GL11.glVertex3d(+.25D, .0D, +.50D);
        	        	GL11.glVertex3d(+.50D, .0D, -.50D);
        	        	GL11.glVertex3d(+.25D, .0D, -.25D);
        	        	GL11.glVertex3d(+.50D, .0D, -.25D);
        	        	GL11.glVertex3d(+.25D, .0D, -.50D);
        	        	GL11.glVertex3d(-.50D, .0D, +.50D);
        	        	GL11.glVertex3d(-.25D, .0D, +.25D);
        	        	GL11.glVertex3d(-.50D, .0D, +.25D);
        	        	GL11.glVertex3d(-.25D, .0D, +.50D);
        	        	GL11.glVertex3d(-.50D, .0D, -.50D);
        	        	GL11.glVertex3d(-.25D, .0D, -.25D);
        	        	GL11.glVertex3d(-.50D, .0D, -.25D);
        	        	GL11.glVertex3d(-.25D, .0D, -.50D);
        	        	break;
        	        }
        		}
        	}
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private static void drawGrid(DrawBlockHighlightEvent aEvent) {
        drawGrid(aEvent, false);
    }

    public boolean isServerSide() {
        return true;
    }

    public boolean isClientSide() {
        return true;
    }

    public boolean isBukkitSide() {
        return false;
    }

    public EntityPlayer getThePlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    public int addArmor(String aPrefix) {
        return RenderingRegistry.addNewArmourRendererPrefix(aPrefix);
    }

    public void onPreLoad() {
        super.onPreLoad();
        String[] arr$ = {
                "renadi", "hanakocz", "MysteryDump", "Flaver4", "x_Fame", "Peluche321", "Goshen_Ithilien", "manf", "Bimgo", "leagris",
                "IAmMinecrafter02", "Cerous", "Devilin_Pixy", "Bkarlsson87", "BadAlchemy", "CaballoCraft", "melanclock", "Resursator", "demanzke", "AndrewAmmerlaan",
                "Deathlycraft", "Jirajha", "Axlegear", "kei_kouma", "Dracion", "dungi", "Dorfschwein", "Zero Tw0", "mattiagraz85", "sebastiank30",
                "Plem", "invultri", "grillo126", "malcanteth", "Malevolence_", "Nicholas_Manuel", "Sirbab", "kehaan", "bpgames123", "semig0d",
                "9000bowser", "Sovereignty89", "Kris1432", "xander_cage_", "samuraijp", "bsaa", "SpwnX", "tworf", "Kadah", "kanni",
                "Stute", "Hegik", "Onlyme", "t3hero", "Hotchi", "jagoly", "Nullav", "BH5432", "Sibmer", "inceee",
                "foxxx0", "Hartok", "TMSama", "Shlnen", "Carsso", "zessirb", "meep310", "Seldron", "yttr1um", "hohounk",
                "freebug", "Sylphio", "jmarler", "Saberawr", "r00teniy", "Neonbeta", "yinscape", "voooon24", "Quintine", "peach774",
                "lepthymo", "bildeman", "Kremnari", "Aerosalo", "OndraSter", "oscares91", "mr10movie", "Daxx367x2", "EGERTRONx", "aka13_404",
                "Abouttabs", "Johnstaal", "djshiny99", "megatronp", "DZCreeper", "Kane_Hart", "Truculent", "vidplace7", "simon6689", "MomoNasty",
                "UnknownXLV", "goreacraft", "Fluttermine", "Daddy_Cecil", "MrMaleficus", "TigersFangs", "cublikefoot", "chainman564", "NikitaBuker", "Misha999777",
                "25FiveDetail", "AntiCivilBoy", "michaelbrady", "xXxIceFirexXx", "Speedynutty68", "GarretSidzaka", "HallowCharm977", "mastermind1919", "The_Hypersonic", "diamondguy2798",
                "zF4ll3nPr3d4t0r", "CrafterOfMines57", "XxELIT3xSNIP3RxX", "SuterusuKusanagi", "xavier0014", "adamros", "alexbegt"
        };
        for (String tName : arr$) {
            mCapeList.add(tName.toLowerCase());
        }
        (new Thread(this)).start();
        
        mPreference = new GT_ClientPreference(GregTech_API.sClientDataFile);
    }

    public void onLoad() {
        super.onLoad();
        new GT_Renderer_Block();
        new GT_MetaGenerated_Item_Renderer();
        new GT_MetaGenerated_Tool_Renderer();
        new GT_FlaskRenderer();
        new GT_FluidDisplayStackRenderer();
    }

    public void onPostLoad() {
        super.onPostLoad();
        try {
            label0:
            for (int i = 1; i < GregTech_API.METATILEENTITIES.length; i++)
                do {
                    if (i >= GregTech_API.METATILEENTITIES.length)
                        continue label0;
                    if (GregTech_API.METATILEENTITIES[i] != null)
                        GregTech_API.METATILEENTITIES[i].getStackForm(1L).getTooltip(null, true);
                    i++;
                } while (true);
        } catch (Throwable e) {e.printStackTrace(GT_Log.err);}
    }

    public void run() {
        try {
            GT_Log.out.println("GT_Mod: Downloading Cape List.");
            Scanner tScanner = new Scanner(new URL("http://gregtech.overminddl1.com/com/gregoriust/gregtech/supporterlist.txt").openStream());
            while (tScanner.hasNextLine()) {
                String tName = tScanner.nextLine();
                this.mCapeList.add(tName.toLowerCase());
            }
        } catch (Throwable ignored) {}
    }

    @Override
    @SubscribeEvent
    public void onClientConnectedToServerEvent(FMLNetworkEvent.ClientConnectedToServerEvent aEvent) {
        mFirstTick = true;
    }

    @SubscribeEvent
    public void receiveRenderSpecialsEvent(net.minecraftforge.client.event.RenderPlayerEvent.Specials.Pre aEvent) {
        mCapeRenderer.receiveRenderSpecialsEvent(aEvent);
    }

    @SubscribeEvent
    public void onPlayerTickEventClient(TickEvent.PlayerTickEvent aEvent) {
        if ((aEvent.side.isClient()) && (aEvent.phase == TickEvent.Phase.END) && (!aEvent.player.isDead)) {
            if (mFirstTick) {
                mFirstTick = false;
                GT_Values.NW.sendToServer(new GT_Packet_ClientPreference(mPreference));
            }
            afterSomeTime++;
            if(afterSomeTime>=100L){
                afterSomeTime=0;
                StatFileWriter sfw= Minecraft.getMinecraft().thePlayer.getStatFileWriter();
                try {
                    for (GT_Recipe recipe : GT_Recipe.GT_Recipe_Map.sAssemblylineVisualRecipes.mRecipeList) {
                        recipe.mHidden = !sfw.hasAchievementUnlocked(GT_Mod.achievements.getAchievement(recipe.getOutput(0).getUnlocalizedName()));
                    }
                } catch (Exception ignored) {
                }
            }
            ArrayList<GT_PlayedSound> tList = new ArrayList<>();
            for (Map.Entry<GT_PlayedSound, Integer> tEntry : GT_Utility.sPlayedSoundMap.entrySet()) {
                if (tEntry.getValue() < 0) {//Integer -> Integer -> int? >_<, fix
                    tList.add(tEntry.getKey());
                } else {
                    tEntry.setValue(tEntry.getValue() - 1);
                }
            }
            GT_PlayedSound tKey;
    
            for (Iterator<GT_PlayedSound> i$ = tList.iterator(); i$.hasNext(); GT_Utility.sPlayedSoundMap.remove(tKey)) {
                tKey = i$.next();
            }
            if(!GregTech_API.mServerStarted) GregTech_API.mServerStarted = true;
            if (GT_Values.updateFluidDisplayItems) {
                MovingObjectPosition trace = Minecraft.getMinecraft().objectMouseOver;
                if (trace != null && trace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                        (mLastUpdatedBlockX != trace.blockX &&
                        mLastUpdatedBlockY != trace.blockY &&
                        mLastUpdatedBlockZ != trace.blockZ || afterSomeTime % 10 == 0)) {
                    mLastUpdatedBlockX = trace.blockX;
                    mLastUpdatedBlockY = trace.blockY;
                    mLastUpdatedBlockZ = trace.blockZ;
                    TileEntity tileEntity = aEvent.player.worldObj.getTileEntity(trace.blockX, trace.blockY, trace.blockZ);
                    if (tileEntity instanceof IGregTechTileEntity) {
                        IGregTechTileEntity gtTile = (IGregTechTileEntity) tileEntity;
                        if (gtTile.getMetaTileEntity() instanceof IHasFluidDisplayItem) {
                            GT_Values.NW.sendToServer(new MessageUpdateFluidDisplayItem(trace.blockX, trace.blockY, trace.blockZ, gtTile.getWorld().provider.dimensionId));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent aEvent) {
        Block aBlock = aEvent.player.worldObj.getBlock(aEvent.target.blockX, aEvent.target.blockY, aEvent.target.blockZ);
        TileEntity aTileEntity = aEvent.player.worldObj.getTileEntity(aEvent.target.blockX, aEvent.target.blockY, aEvent.target.blockZ);

        if (GT_Utility.isStackInList(aEvent.currentItem, GregTech_API.sWrenchList))
        {
            if (aTileEntity instanceof ITurnable || ROTATABLE_VANILLA_BLOCKS.contains(aBlock) || aTileEntity instanceof IWrenchable)
                drawGrid(aEvent, false);
            return;
        }

        if (!(aTileEntity instanceof ICoverable))
            return;

        if (GT_Utility.isStackInList(aEvent.currentItem, GregTech_API.sWireCutterList) ||
                GT_Utility.isStackInList(aEvent.currentItem, GregTech_API.sSolderingToolList) )
        {
            if (((ICoverable) aTileEntity).getCoverIDAtSide((byte) aEvent.target.sideHit) == 0)
                drawGrid(aEvent, false);
            return;
        }

        if ((aEvent.currentItem == null && aEvent.player.isSneaking()) ||
                GT_Utility.isStackInList(aEvent.currentItem, GregTech_API.sCrowbarList) ||
                GT_Utility.isStackInList(aEvent.currentItem, GregTech_API.sScrewdriverList))
        {
            if (((ICoverable) aTileEntity).getCoverIDAtSide((byte) aEvent.target.sideHit) == 0)
                for (byte i = 0; i < 6; i++)
                    if (((ICoverable) aTileEntity).getCoverIDAtSide(i) > 0) {
                        drawGrid(aEvent, true);
                        return;
                    }
            return;
        }

        if (GT_Utility.isStackInList(aEvent.currentItem, GregTech_API.sCovers.keySet()))
        {
            if (((ICoverable) aTileEntity).getCoverIDAtSide((byte) aEvent.target.sideHit) == 0)
                drawGrid(aEvent, true);
        }
    }

    @SubscribeEvent
    public void receiveRenderEvent(net.minecraftforge.client.event.RenderPlayerEvent.Pre aEvent) {
        if (GT_Utility.getFullInvisibility(aEvent.entityPlayer)) {
            aEvent.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTickEvent(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent aEvent) {
        if (aEvent.phase == cpw.mods.fml.common.gameevent.TickEvent.Phase.END) {
            if (changeDetected > 0) changeDetected--;
            final int newHideValue = shouldHeldItemHideThings();
            if (newHideValue != hideValue) {
                hideValue = newHideValue;
                changeDetected = 5;
            }
            mAnimationTick++;
            if (mAnimationTick % 50L == 0L) {
                mAnimationDirection = !mAnimationDirection;
            }
            final int tDirection = mAnimationDirection ? 1 : -1;
            for (Materials tMaterial : mPosR) {
                tMaterial.mRGBa[0] = getSafeRGBValue(tMaterial.mRGBa[0], tDirection);
            }
            for (Materials tMaterial : mPosG) {
                tMaterial.mRGBa[1] = getSafeRGBValue(tMaterial.mRGBa[1], tDirection);
            }
            for (Materials tMaterial : mPosB) {
                tMaterial.mRGBa[2] = getSafeRGBValue(tMaterial.mRGBa[2], tDirection);
            }
            for (Materials tMaterial : mPosA) {
                tMaterial.mRGBa[3] = getSafeRGBValue(tMaterial.mRGBa[3], tDirection);
            }
            for (Materials tMaterial : mNegR) {
                tMaterial.mRGBa[0] = getSafeRGBValue(tMaterial.mRGBa[0], -tDirection);
            }
            for (Materials tMaterial : mNegG) {
                tMaterial.mRGBa[1] = getSafeRGBValue(tMaterial.mRGBa[1], -tDirection);
            }
            for (Materials tMaterial : mNegB) {
                tMaterial.mRGBa[2] = getSafeRGBValue(tMaterial.mRGBa[2], -tDirection);
            }
            for (Materials tMaterial : mNegA) {
                tMaterial.mRGBa[3] = getSafeRGBValue(tMaterial.mRGBa[3], -tDirection);
            }
            for (Materials tMaterial : mMoltenPosR) {
                tMaterial.mMoltenRGBa[0] = getSafeRGBValue(tMaterial.mMoltenRGBa[0], tDirection);
            }
            for (Materials tMaterial : mMoltenPosG) {
                tMaterial.mMoltenRGBa[1] = getSafeRGBValue(tMaterial.mMoltenRGBa[1], tDirection);
            }
            for (Materials tMaterial : mMoltenPosB) {
                tMaterial.mMoltenRGBa[2] = getSafeRGBValue(tMaterial.mMoltenRGBa[2], tDirection);
            }
            for (Materials tMaterial : mMoltenPosA) {
                tMaterial.mMoltenRGBa[3] = getSafeRGBValue(tMaterial.mMoltenRGBa[3], tDirection);
            }
            for (Materials tMaterial : mMoltenNegR) {
                tMaterial.mMoltenRGBa[0] = getSafeRGBValue(tMaterial.mMoltenRGBa[0], -tDirection);
            }
            for (Materials tMaterial : mMoltenNegG) {
                tMaterial.mMoltenRGBa[1] = getSafeRGBValue(tMaterial.mMoltenRGBa[1], -tDirection);
            }
            for (Materials tMaterial : mMoltenNegB) {
                tMaterial.mMoltenRGBa[2] = getSafeRGBValue(tMaterial.mMoltenRGBa[2], -tDirection);
            }
            for (Materials tMaterial : mMoltenNegA) {
                tMaterial.mMoltenRGBa[3] = getSafeRGBValue(tMaterial.mMoltenRGBa[3], -tDirection);
            }
        }
    }
    
    public short getSafeRGBValue(short aRBG, int aDelta) {
        int tmp = aRBG + aDelta;
        if (tmp > 255) tmp = 255;
        if (tmp < 0) tmp = 0;
        return (short) tmp;
    }
    
    public void doSonictronSound(ItemStack aStack, World aWorld, double aX, double aY, double aZ) {
        if (GT_Utility.isStackInvalid(aStack))
            return;
        String tString = "note.harp";
        int i = 0;
        int j = mSoundItems.size();
        do {
            if (i >= j) break;
            if (GT_Utility.areStacksEqual(mSoundItems.get(i), aStack)) {
                tString = mSoundNames.get(i);
                break;
            }
            i++;
        } while (true);
        if (tString.startsWith("random.explode"))
            if (aStack.stackSize == 3)
                tString = "random.fuse";
            else if (aStack.stackSize == 2)
                tString = "random.old_explode";
        if (tString.startsWith("streaming."))
            switch (aStack.stackSize) {
                case 1: // '\001'
                    tString = tString + "13";
                    break;

                case 2: // '\002'
                    tString = tString + "cat";
                    break;

                case 3: // '\003'
                    tString = tString + "blocks";
                    break;

                case 4: // '\004'
                    tString = tString + "chirp";
                    break;

                case 5: // '\005'
                    tString = tString + "far";
                    break;

                case 6: // '\006'
                    tString = tString + "mall";
                    break;

                case 7: // '\007'
                    tString = tString + "mellohi";
                    break;

                case 8: // '\b'
                    tString = tString + "stal";
                    break;

                case 9: // '\t'
                    tString = tString + "strad";
                    break;

                case 10: // '\n'
                    tString = tString + "ward";
                    break;

                case 11: // '\013'
                    tString = tString + "11";
                    break;

                case 12: // '\f'
                    tString = tString + "wait";
                    break;

                default:
                    tString = tString + "wherearewenow";
                    break;
            }
        if (tString.startsWith("streaming.")) aWorld.playRecord(tString.substring(10), (int) aX, (int) aY, (int) aZ);
        else
            aWorld.playSound(aX, aY, aZ, tString, 3F, tString.startsWith("note.") ? (float) Math.pow(2D, (double) (aStack.stackSize - 13) / 12D) : 1.0F, false);
    }

    public static int hideValue=0;

    /** <p>Client tick counter that is set to 5 on hiding pipes and covers.</p>
     * <p>It triggers a texture update next client tick when reaching 4, with provision for 3 more update tasks,
     * spreading client change detection related work and network traffic on different ticks, until it reaches 0.</p>
     */
    public static int changeDetected=0;

    private static int shouldHeldItemHideThings() {
        try {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null) return 0;
            ItemStack tCurrentItem = player.getCurrentEquippedItem();
            if (tCurrentItem == null) return 0;
            int[] ids = OreDictionary.getOreIDs(tCurrentItem);
            int hide = 0;
            for (int i : ids) {
                if (OreDictionary.getOreName(i).equals("craftingToolSolderingIron")) {
                    hide |= 0x1;
                    break;
                }
            }
            if (GT_Utility.isStackInList(tCurrentItem, GregTech_API.sWrenchList)
            		|| GT_Utility.isStackInList(tCurrentItem, GregTech_API.sScrewdriverList)
            		|| GT_Utility.isStackInList(tCurrentItem, GregTech_API.sHardHammerList)
            		|| GT_Utility.isStackInList(tCurrentItem, GregTech_API.sSoftHammerList)
            		|| GT_Utility.isStackInList(tCurrentItem, GregTech_API.sWireCutterList)
            		|| GT_Utility.isStackInList(tCurrentItem, GregTech_API.sSolderingToolList)
            		|| GT_Utility.isStackInList(tCurrentItem, GregTech_API.sCrowbarList)
            		|| GregTech_API.sCovers.containsKey(new GT_ItemStack(tCurrentItem))) {
            	hide |= 0x2;
            }
            return hide;
        }catch(Exception e){
            return 0;
        }
    }
}
