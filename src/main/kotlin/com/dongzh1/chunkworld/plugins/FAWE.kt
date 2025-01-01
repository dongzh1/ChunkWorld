package com.dongzh1.chunkworld.plugins

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.mask.AbstractExtentMask
import com.sk89q.worldedit.function.mask.BlockTypeMask
import com.sk89q.worldedit.function.mask.Mask
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Container
import org.bukkit.entity.Player
import java.io.File
import kotlin.random.Random

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

    /**
     * 把一个区块除了空气和基岩外的其他方块转化为石头
     * 地狱的话转化为地狱岩
     */
    fun replaceEx(chunk: Chunk) {
        val world = chunk.world
        val start :Location
        val end:Location
        val newMaterial:Material
        val exclude = Material.AIR
        val exclude2 = Material.BEDROCK
        if (world.environment == World.Environment.NORMAL){
            start = Location(world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
            end = Location(world, chunk.x * 16.toDouble()+15, 319.0, chunk.z * 16.toDouble()+15)
            newMaterial = Material.STONE
        }else if (world.environment == World.Environment.NETHER){
            start = Location(world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
            end = Location(world, chunk.x * 16.toDouble() + 15, 255.0, chunk.z * 16.toDouble() + 15)
            newMaterial = Material.NETHERRACK
        }else{
            return
        }
        val edit = WorldEdit.getInstance().newEditSessionBuilder()
            .world(BukkitAdapter.adapt(world))
            .build()

        val region: Region = CuboidRegion(
            BukkitAdapter.adapt(world),
            BukkitAdapter.asBlockVector(start),
            BukkitAdapter.asBlockVector(end)
        )
        val blockType = BukkitAdapter.asBlockType(exclude) ?: return
        val blockType2 = BukkitAdapter.asBlockType(exclude2)?: return
        val newBlockType = BukkitAdapter.asBlockType(newMaterial) ?: return
        edit.replaceBlocks(region, object : AbstractExtentMask(region.world) {

            override fun test(
                extent: Extent,
                position: BlockVector3
            ): Boolean {
                return test(position)
            }

            override fun test(vector: BlockVector3): Boolean {
                val type = extent.getBlock(vector).blockType
                return (type.internalId != blockType.internalId && type.internalId!= blockType2.internalId)
            }

            override fun copy(): Mask? {
                return null
            }

        }, newBlockType)
        edit.flushQueue()
        edit.close()
    }

    /**
     * 把一个区块除了容器外的其他方块转化为空气
     */
    fun replaceExclude(chunk: Chunk) {
        val world = chunk.world
        val start :Location
        val end:Location
        val newMaterial:Material = Material.AIR
        if (world.environment == World.Environment.NORMAL){
            start = Location(world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
            end = Location(world, chunk.x * 16.toDouble()+15, 319.0, chunk.z * 16.toDouble()+15)
        }else if (world.environment == World.Environment.NETHER){
            start = Location(world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
            end = Location(world, chunk.x * 16.toDouble() + 15, 255.0, chunk.z * 16.toDouble() + 15)
        }else{
            return
        }
        val edit = WorldEdit.getInstance().newEditSessionBuilder()
            .world(BukkitAdapter.adapt(world))
            .build()
        val region: Region = CuboidRegion(
            BukkitAdapter.adapt(world),
            BukkitAdapter.asBlockVector(start),
            BukkitAdapter.asBlockVector(end)
        )
        val newBlockType = BukkitAdapter.asBlockType(newMaterial) ?: return
        edit.replaceBlocks(region, object : AbstractExtentMask(region.world) {
            override fun test(
                extent: Extent,
                position: BlockVector3
            ): Boolean {
                return test(position)
            }
            override fun test(vector: BlockVector3): Boolean {
                val block =
                    world.getBlockAt(Location(world, vector.x.toDouble(), vector.y.toDouble(), vector.z.toDouble()))
                return block.state is Container
            }

            override fun copy(): Mask? {
                return null
            }

        }, newBlockType)
        edit.flushQueue()
        edit.close()
    }
    fun placeSchem(file: File, location: Location) {
        val format = ClipboardFormats.findByFile(file) ?: error("$file 文件格式错误")
        val clip = format.load(file)
        //取最小位置点进行放置
        clip.origin = clip.region.minimumPoint
        clip.paste(BukkitWorld(location.world), BlockVector3.at(location.x, location.y, location.z),false,true,true,null)
    }
    fun savePlayerSchem(chunk: Chunk,player: Player):String? {
        val world = chunk.world
        val pos1:Location
        val pos2:Location
        val file:File
        if (world.environment == World.Environment.NORMAL){
            pos1 = Location(world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
            pos2 = Location(world, chunk.x * 16.toDouble() + 15, 319.0, chunk.z * 16.toDouble() + 15)
            file = File("/home/pixelServer/playerTemples/world/${player.uniqueId}_${System.currentTimeMillis()}.schem")
        }else if (world.environment == World.Environment.NETHER){
            pos1 = Location(world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
            pos2 = Location(world, chunk.x * 16.toDouble() + 15, 255.0, chunk.z * 16.toDouble() + 15)
            file = File("/home/pixelServer/playerTemples/nether/${player.uniqueId}_${System.currentTimeMillis()}.schem")
        }else {
            return null
        }
        try {
            val region = CuboidRegion(
                BukkitAdapter.adapt(world),
                BukkitAdapter.asBlockVector(pos1),
                BukkitAdapter.asBlockVector(pos2)
            )
            val clipboard = BlockArrayClipboard(region)
            WorldEdit.getInstance().newEditSession(region.world).use {
                it.disableHistory()
                val forward = ForwardExtentCopy(it, region, clipboard, region.minimumPoint)
                //不复制实体
                forward.isCopyingEntities = false
                forward.isCopyingBiomes = true
                Operations.complete(forward)
            }
            clipboard.save(file, BuiltInClipboardFormat.FAST)
            return file.absolutePath
        }catch (e:Exception){
            return null
        }
    }
    fun saveSchem(chunk:Chunk,name:String,rare:String): Boolean {
        val pos1:Location
        val pos2:Location
        val file:File
        if (chunk.world.environment == World.Environment.NORMAL){
            pos1 = Location(chunk.world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
            pos2 = Location(chunk.world, chunk.x * 16.toDouble() + 15, 319.0, chunk.z * 16.toDouble() + 15)
            //绝对目录
            file = File("/home/pixelServer/temples/$rare/world/$name.schem")
        }else if (chunk.world.environment == World.Environment.NETHER){
            pos1 = Location(chunk.world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
            pos2 = Location(chunk.world, chunk.x * 16.toDouble() + 15, 255.0, chunk.z * 16.toDouble() + 15)
            file = File("/home/pixelServer/temples/$rare/nether/$name.schem")
        }else {
            return false
        }
        try {
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
            clipboard.save(file, BuiltInClipboardFormat.FAST)
            return true
        }catch (e:Exception){
            return false
        }
    }
    fun getRandomSchem(isNormalWorld:Boolean):Pair<String,File?>{
        //稀有度概率
        val worldType = if (isNormalWorld) "world" else "nether"
        val rare = Random.nextInt(10001)
        var xiyoudu :String
        val fileString :String
        when(rare){
            in 0..6999 -> {
                xiyoudu = "§f普通"
                fileString = "/home/pixelServer/temples/putong/$worldType"
            }
            in 7000..9499 -> {
                xiyoudu = "§b稀有"
                fileString = "/home/pixelServer/temples/xiyou/$worldType"
            }
            in 9500..9900 -> {
                xiyoudu = "§d史诗"
                fileString = "/home/pixelServer/temples/shishi/$worldType"
            }
            in 9901..9999 -> {
                xiyoudu = "§e传说"
                fileString = "/home/pixelServer/temples/chuanshuo/$worldType"
            }
            else -> {
                xiyoudu = "§c唯一"
                fileString = "/home/pixelServer/temples/weiyi/$worldType"
            }
        }
        val folder = File(fileString)
        if (folder.exists() && folder.isDirectory) {
            var schems = folder.listFiles()
                ?.filter { it.name.endsWith(".schem") }
                ?.toList() ?: emptyList()
            if (schems.isEmpty()) {
                //没货，从普通里抽取
                val newFolder = File("/home/pixelServer/temples/putong/$worldType")
                schems = newFolder.listFiles()
                   ?.filter { it.name.endsWith(".schem") }
                   ?.toList()?: emptyList()
                xiyoudu = "§f普通"
            }
            val file = schems.random()
            return xiyoudu to file
        }else{
            return xiyoudu to null
        }
    }
}
