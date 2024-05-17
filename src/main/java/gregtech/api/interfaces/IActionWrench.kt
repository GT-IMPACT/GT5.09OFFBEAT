package gregtech.api.interfaces

import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity

fun interface IActionWrench {
    fun doAction(block: Block, meta: Int, tile: TileEntity?, side: Int): Boolean
}
