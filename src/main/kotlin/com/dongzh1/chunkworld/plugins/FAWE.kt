package com.dongzh1.chunkworld.plugins

import com.dongzh1.chunkworld.ChunkWorld
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.mask.BlockTypeMask
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.util.BlockVector
import java.io.File

val fawe by lazy { FAWE() }
class FAWE {

    fun fill(world: World, start: Location, end: Location, material: Material) {
        val edit = WorldEdit.getInstance().newEditSessionBuilder()
            .world(BukkitAdapter.adapt(world))
            .build()

        val region: Region = CuboidRegion(
            BukkitAdapter.adapt(world),
            BukkitAdapter.asBlockVector(start),
            BukkitAdapter.asBlockVector(end)
        )
        val blockType = BukkitAdapter.asBlockType(material) ?: return
        edit.setBlocks(region, blockType)
        edit.flushQueue()
        edit.close()
    }

    fun replace(world: World, start: Location, end: Location, material: Material, newMaterial: Material) {
        val edit = WorldEdit.getInstance().newEditSessionBuilder()
            .world(BukkitAdapter.adapt(world))
            .build()

        val region: Region = CuboidRegion(
            BukkitAdapter.adapt(world),
            BukkitAdapter.asBlockVector(start),
            BukkitAdapter.asBlockVector(end)
        )
        val blockType = BukkitAdapter.asBlockType(material) ?: return
        val newBlockType = BukkitAdapter.asBlockType(newMaterial) ?: return
        edit.replaceBlocks(region, BlockTypeMask(region.world, blockType), newBlockType)
        edit.flushQueue()
        edit.close()
    }
    fun placeSchem(file: File, location: Location) {
        val format = ClipboardFormats.findByFile(file) ?: error("$file 文件格式错误")
        val clip = format.load(file)
        //取最小位置点进行放置
        clip.origin = clip.region.minimumPoint
        clip.paste(BukkitWorld(location.world), BlockVector3.at(location.x, location.y, location.z),false,true,true,null,)
    }
    fun saveSchem(chunk:Chunk,name:String): Boolean {
        try {
            val pos1 = Location(chunk.world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
            val pos2 = Location(chunk.world, chunk.x * 16.toDouble() + 15, 319.0, chunk.z * 16.toDouble() + 15)
            val region = CuboidRegion(
                BukkitAdapter.adapt(chunk.world),
                BukkitAdapter.asBlockVector(pos1),
                BukkitAdapter.asBlockVector(pos2)
            )
            val clipboard = BlockArrayClipboard(region)
            WorldEdit.getInstance().newEditSession(region.world).use {
                it.disableHistory()
                val forward = ForwardExtentCopy(it, region, clipboard, region.minimumPoint)
                forward.isCopyingEntities = true
                forward.isCopyingBiomes = true
                Operations.complete(forward)
            }
            //绝对目录
            val file = File("/home/pixelServer/temples/$name.schem")
            clipboard.save(file, BuiltInClipboardFormat.FAST)
            return true
        }catch (e:Exception){
            return false
        }
    }

}