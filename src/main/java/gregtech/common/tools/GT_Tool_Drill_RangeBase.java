package gregtech.common.tools;

import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.IOverlayItem;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.common.items.behaviors.Behaviour_Drill;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

import java.util.List;
import tconstruct.library.tools.AbilityHelper;

import static gregtech.api.util.GT_Utility.ItemNBT.getDrillRangeMode;

abstract class GT_Tool_Drill_RangeBase
        extends GT_Tool_Drill_LV implements IOverlayItem {

    private ThreadLocal<Object> sIsHarvesting = new ThreadLocal();

    public static MovingObjectPosition raytraceFromEntity(World world, Entity player, boolean wut, double range) {
        float f = 1.0F;
        float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
        float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
        double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double) f;
        double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double) f;
        if (!world.isRemote && player instanceof EntityPlayer)
            d1 += 1.62D;
        double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double) f;
        Vec3 vec3 = Vec3.createVectorHelper(d0, d1, d2);
        float f3 = MathHelper.cos(-f2 * 0.017453292F - (float) Math.PI);
        float f4 = MathHelper.sin(-f2 * 0.017453292F - (float) Math.PI);
        float f5 = -MathHelper.cos(-f1 * 0.017453292F);
        float f6 = MathHelper.sin(-f1 * 0.017453292F);
        float f7 = f4 * f5;
        float f8 = f3 * f5;
        double d3 = range;
        if (player instanceof EntityPlayerMP)
        {
            d3 = ((EntityPlayerMP) player).theItemInWorldManager.getBlockReachDistance();
        }
        Vec3 vec31 = vec3.addVector((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
        return world.func_147447_a(vec3, vec31, wut, !wut, wut);
    }

    public IIconContainer getOverlay(boolean isOverlay, ItemStack aStack) {
        int mode = getDrillRangeMode(aStack);
        IIconContainer container = isOverlay ? Textures.ItemIcons.DRILLOVERLAY[mode] : Textures.ItemIcons.VOID;
        return container != null ? container : Textures.ItemIcons.VOID;
    }

    public int getToolDamagePerBlockBreak() {
        return 20;
    }

    abstract int setToolDamagePerBlockBreak();

    public int getToolDamagePerDropConversion() {
        return 20;
    }

    abstract int setToolDamagePerDropConversion();

    public int getToolDamagePerContainerCraft() {
        return 100;
    }

    abstract int setToolDamagePerContainerCraft();

    public int getToolDamagePerEntityAttack() {
        return 200;
    }

    abstract int setToolDamagePerEntityAttack();

    public int getBaseQuality() {
        return 0;
    }

    abstract int setBaseQuality();

    public float getBaseDamage() {
        return 2.0F;
    }

    abstract float setBaseDamage();

    public float getSpeedMultiplier() {
        return setSpeedMultiplier();
    }

    abstract float setSpeedMultiplier();

    public float getMaxDurabilityMultiplier() {
        return 1.0F * (setTier() + 1);
    }

    abstract float setMaxDurabilityMultiplier();

    public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
        return aIsToolHead ? GT_MetaGenerated_Tool.getPrimaryMaterial(aStack).mIconSet.mTextures[OrePrefixes.toolHeadDrill.mTextureIndex] : setIcon();
    }

    abstract IIconContainer setIcon();

    abstract byte RangeWidthandDepth();

    abstract byte RangeHeight();

    public String getCraftingSound() {
        return GregTech_API.sSoundList.get(106);
    }

    public String getEntityHitSound() {
        return GregTech_API.sSoundList.get(106);
    }

    public String getBreakingSound() {
        return GregTech_API.sSoundList.get(106);
    }

    public String getMiningSound() {
        return GregTech_API.sSoundList.get(106);
    }

    public boolean canBlock() {
        return false;
    }

    public boolean isCrowbar() {
        return false;
    }

    public boolean isMinableBlock(Block aBlock, byte aMetaData) {
        String tTool = aBlock.getHarvestTool(aMetaData);
        return aBlock.getHarvestLevel(aMetaData) != -1 && (tTool == null || tTool.isEmpty() || ((tTool.equals("pickaxe")) || (tTool.equals("shovel"))))
                || (aBlock.getMaterial() == Material.rock) || (aBlock.getMaterial() == Material.iron) || (aBlock.getMaterial() == Material.anvil)
                || (aBlock.getMaterial() == Material.sand) || (aBlock.getMaterial() == Material.grass) || (aBlock.getMaterial() == Material.ground)
                || (aBlock.getMaterial() == Material.snow) || (aBlock.getMaterial() == Material.clay) || (aBlock.getMaterial() == Material.glass)
                || (aBlock instanceof BlockBreakable);
    }

    public ItemStack getBrokenItem(ItemStack aStack) {
        return setBrokenItem();
    }

    public abstract ItemStack setBrokenItem();

    public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
        return aIsToolHead ? GT_MetaGenerated_Tool.getPrimaryMaterial(aStack).mRGBa : GT_MetaGenerated_Tool.getSecondaryMaterial(aStack).mRGBa;
    }

    public void onStatsAddedToTool(GT_MetaGenerated_Tool aItem, int aID) {
        aItem.addItemBehavior(aID, new Behaviour_Drill(setRange(), setTier()));
    }

    abstract String setRange();

    abstract int setTier();

    public void onToolCrafted(ItemStack aStack, EntityPlayer aPlayer) {
        super.onToolCrafted(aStack, aPlayer);
        aPlayer.triggerAchievement(AchievementList.buildPickaxe);
        aPlayer.triggerAchievement(AchievementList.buildBetterPickaxe);
        try {
            GT_Mod.achievements.issueAchievement(aPlayer, "driltime");
            GT_Mod.achievements.issueAchievement(aPlayer, "buildDrill");
        } catch (Exception ignored) {
        }
    }

    public IChatComponent getDeathMessage(EntityLivingBase aPlayer, EntityLivingBase aEntity) {
        return new ChatComponentText(EnumChatFormatting.RED + aEntity.getCommandSenderName() + EnumChatFormatting.WHITE + " got the Drill! (by " + EnumChatFormatting.GREEN + aPlayer.getCommandSenderName() + EnumChatFormatting.WHITE + ")");
    }

    public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, EntityPlayer aPlayer, Block aBlock, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockEvent.HarvestDropsEvent aEvent) {
        int rConversions = 0;

        int mode = getDrillRangeMode(aStack);

        int WD = mode == 1 ? 1 : mode == 2 ? 2 : mode == 3 ? 3 : mode == 4 ? 4 : 0;
        int H = mode == 1 ? 1 : mode == 2 ? 3 : mode == 3 ? 5 : mode == 4 ? 7 : 0;

        MovingObjectPosition mop = raytraceFromEntity(aPlayer.worldObj, aPlayer, false, 10);

        if (mop != null) {
            int sideHit = mop.sideHit;
            int xRange = WD;
            int yRange = WD;
            int zRange = 0;

            switch (sideHit) {
                case 0:
                case 1:
                    yRange = 0;
                    zRange = WD;
                    break;
                case 2:
                case 3:
                    xRange = WD;
                    zRange = 0;
                    break;
                case 4:
                case 5:
                    xRange = 0;
                    zRange = WD;
                    break;
            }

            if ((this.sIsHarvesting.get() == null) && ((aPlayer instanceof EntityPlayerMP))) {
                this.sIsHarvesting.set(this);
                for (int xPos = -xRange; xPos <= xRange; xPos++) {
                    for (int yPos = mode == 0 ? 0 : -1; yPos <= (yRange == 0 ? 0 : yRange*2-1); yPos++) {
                        for (int zPos = -zRange; zPos <= zRange; zPos++) {
                            if (xPos == aX && yPos == aY && zPos == aZ) {
                                continue;
                            }
                            if (aEvent.world.getBlock(aX + xPos, aY + yPos, aZ + zPos) == Blocks.bedrock) {
                            } else {
                                if (((xPos != 0) || (yPos != 0) || (zPos != 0)) && (aStack.getItem().getDigSpeed(aStack,
                                        aPlayer.worldObj.getBlock(aX + xPos, aY + yPos, aZ + zPos),
                                        aPlayer.worldObj.getBlockMetadata(aX + xPos, aY + yPos, aZ + zPos)) > 0.0F) &&
                                        (((EntityPlayerMP) aPlayer).theItemInWorldManager.tryHarvestBlock(aX + xPos, aY + yPos, aZ + zPos))) {
                                    rConversions++;
                                }
                            }
                        }
                    }
                }
                this.sIsHarvesting.set(null);
            }
        }
        return rConversions;
    }
}
