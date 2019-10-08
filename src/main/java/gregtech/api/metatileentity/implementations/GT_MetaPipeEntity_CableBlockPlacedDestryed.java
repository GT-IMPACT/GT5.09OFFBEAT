package gregtech.api.metatileentity.implementations;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.interfaces.metatileentity.IMetaTileEntityCable;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

public class GT_MetaPipeEntity_CableBlockPlacedDestryed {
	private boolean needUpdateCable = false;
	private TileEntity tmpTileEntity;
	private BaseMetaPipeEntity baseMetaPipeEntityTmp;
	private Position[] pos;
	
	private int TickTimer;
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void BlockPlaced(BlockEvent.PlaceEvent event){
		if(event.block.hasTileEntity()) {
			CheckNearCables(event);
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void BlockBreacked(BlockEvent.BreakEvent event){
		if(event.block.hasTileEntity()) {
			CheckNearCables(event);
		}
	}
	
	private void CheckNearCables(BlockEvent event) {
		needUpdateCable = false;
		
		pos = new Position[6];
		
		pos[0] = new Position(event.x - 1, event.y, event.z);
		pos[1] = new Position(event.x + 1, event.y, event.z);
		pos[2] = new Position(event.x, event.y - 1, event.z);
		pos[3] = new Position(event.x, event.y + 1, event.z);
		pos[4] = new Position(event.x, event.y, event.z - 1);
		pos[5] = new Position(event.x, event.y, event.z + 1);
		
		for(int i = 0; i < 6; i++) {
			needUpdateCable = isTileEntityCable(event.world, pos[i].x, pos[i].y, pos[i].z);
			
			if(needUpdateCable) break;
		}
		
		System.out.println("needUpdateCable: " + needUpdateCable);
		System.out.println("tmpTileEntity != null: " + (tmpTileEntity != null));
		
		if(tmpTileEntity != null) {
			System.out.println("tmpTileEntity: " + tmpTileEntity);
		}
		
		baseMetaPipeEntityTmp = null;
		
		if(tmpTileEntity instanceof BaseMetaPipeEntity) {
			baseMetaPipeEntityTmp = (BaseMetaPipeEntity)tmpTileEntity;
		}
		
		if(needUpdateCable && tmpTileEntity != null && (baseMetaPipeEntityTmp.getMetaTileEntity() instanceof GT_MetaPipeEntity_Cable)) {
			GT_MetaPipeEntity_Cable cable = (GT_MetaPipeEntity_Cable)(baseMetaPipeEntityTmp.getMetaTileEntity());
			cable.UpdateNearestCables();
		}
	}
	
	private boolean isNeedToCheckCable() {
		return needUpdateCable == false && 
				   tmpTileEntity == null && 
				   (tmpTileEntity instanceof BaseMetaPipeEntity) == false;
	}
	
	private boolean isTileEntityCable(World world, int x, int y, int z) {
		tmpTileEntity = world.getTileEntity(x, y, z);
		return tmpTileEntity != null && tmpTileEntity instanceof BaseMetaPipeEntity;
	}
}