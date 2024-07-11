package com.dongzh1.chunkworld

import com.fastasyncworldedit.core.Fawe
import com.fastasyncworldedit.core.util.TaskManager
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.CylinderRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import com.sk89q.worldedit.session.request.RequestSelection
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockType
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block

object WorldEdit {
    /**
     * 设置区域内的所有方块为指定方块
     * @param pos1 区域的第一个位置
     * @param pos2 区域的第二个位置
     * @param blockType 指定的方块
     */
    private fun setBlock(pos1:Location, pos2:Location, blockType: BlockType) {
            TaskManager.taskManager().async {
                val world = BukkitAdapter.adapt(pos1.world)
                val region = CuboidRegion(
                    BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
                    BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
                )
                // 创建一个编辑会话
                val editSession: EditSession = WorldEdit.getInstance().newEditSessionBuilder().world(world).fastMode(true).build()

                // 替换区域内的所有方块为指定方块
                editSession.use { session ->
                    session.setBlocks(region as Region, blockType.defaultState)
                }
            }
    }
    fun setVoid(chunk: Chunk){
        val pos1 = Location(chunk.world,chunk.x*16.toDouble(),-64.0,chunk.z*16.toDouble())
        val pos2 = Location(chunk.world,chunk.x*16.toDouble()+15,319.0,chunk.z*16.toDouble()+15)
        TaskManager.taskManager().async {
            val world = BukkitAdapter.adapt(pos1.world)
            val region = CuboidRegion(
                BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
                BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
            )
            // 创建一个编辑会话
            val editSession: EditSession = WorldEdit.getInstance().newEditSessionBuilder().world(world).fastMode(true).build()

            // 替换区域内的所有方块为指定方块
            editSession.use { session ->
                session.setBlocks(region as Region, BlockTypes.AIR!!.defaultState)
            }
        }
    }
    /**
     * 替换区域内的方块为指定方块
     */
    fun setBlock(pos1:Location, pos2:Location,beReplaceType:BlockType, blockType: BlockType) {
        TaskManager.taskManager().async {
            val world = BukkitAdapter.adapt(pos1.world)
            val region = CuboidRegion(
                BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
                BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
            )
            // 创建一个编辑会话
            val editSession: EditSession = WorldEdit.getInstance().newEditSessionBuilder().world(world).fastMode(true).build()

            // 替换区域内的所有方块为指定方块
            editSession.use { session ->
                session.replaceBlocks(region as Region, mutableSetOf(BaseBlock(beReplaceType.defaultState)),blockType.defaultState)
            }
        }
    }
    /**
     * 复制指定区块到指定位置，保留群系
     * @param chunk 区块
     * @param targetChunk 目标区块
     */
    fun copyChunk(chunk:Chunk,targetChunk:Chunk){
        TaskManager.taskManager().async{
            copy(chunk, targetChunk)
        }
    }

    private fun copy(chunk:Chunk,targetChunk:Chunk){
        val weWorld = BukkitAdapter.adapt(chunk.world)
        val region = CuboidRegion(
            weWorld,
            BlockVector3.at(chunk.x*16,-64,chunk.z*16),
            BlockVector3.at(chunk.x*16+15,319,chunk.z*16+15)
        )
        val clipboard = BlockArrayClipboard(region)
        region.compile(clipboard,weWorld)
        clipboard.paste(
            BukkitAdapter.adapt(targetChunk.world),
            BlockVector3.at(targetChunk.x*16,-64,targetChunk.z*16),
            false,
            true,
            null
        )
    }
    /**
     * 复制群系
     */
    fun copyChunkBiome(chunk:Chunk,targetChunk:Chunk){
        TaskManager.taskManager().async{
            copy(chunk, targetChunk)
            val pos1 = Location(targetChunk.world,targetChunk.x*16.toDouble(),-64.0,targetChunk.z*16.toDouble())
            val pos2 = Location(targetChunk.world,targetChunk.x*16.toDouble()+15,319.0,targetChunk.z*16.toDouble()+15)
            setBlock(pos1,pos2,BlockTypes.AIR!!)
        }
    }
    private fun Region.compile(clipboard: BlockArrayClipboard,world: com.sk89q.worldedit.world.World){
        WorldEdit.getInstance().newEditSession(world).use {
            it.disableHistory()
            val forward = ForwardExtentCopy(it,this,clipboard,clipboard.minimumPoint)
            forward.isCopyingBiomes = true
            forward.isCopyingEntities = true
            Operations.complete(forward)
        }
    }
    /**
     * 设置区块边界
     */
    fun setBarrier(chunkList: Set<Pair<Int, Int>>, world: World){
        val barrierList = mutableListOf<Pair<Location,Location>>()
        for (chunk in chunkList){
            //获取设置区块的两角坐标
            val pos1 = Location(world,chunk.first*16.toDouble(),-64.0,chunk.second*16.toDouble())
            val pos2 = Location(world,chunk.first*16.toDouble()+15,319.0,chunk.second*16.toDouble()+15)
            //北面
            barrierList.add(pos1.clone().add(0.0,0.0,-1.0) to pos2.clone().add(0.0,0.0,-16.0))
            //西面
            barrierList.add(pos1.clone().add(-1.0,0.0,0.0) to pos2.clone().add(-16.0,0.0,0.0))
            //南面
            barrierList.add(pos1.clone().add(0.0,0.0,16.0) to pos2.clone().add(0.0,0.0,1.0))
            //东面
            barrierList.add(pos1.clone().add(16.0,0.0,0.0) to pos2.clone().add(1.0,0.0,0.0))
            //根据是否有相临的区块来删除屏障
            for (otherChunk in chunkList){
                //就是目标chunk
                if(otherChunk.first == chunk.first && otherChunk.second == chunk.second){
                    continue
                }
                //判断其他被占领的区块和他有没有相接，如果相接就去掉一面屏障
                if(otherChunk.first == chunk.first && otherChunk.second == chunk.second-1){
                    barrierList.remove(pos1.clone().add(0.0,0.0,-1.0) to pos2.clone().add(0.0,0.0,-16.0))
                }
                if(otherChunk.first == chunk.first && otherChunk.second == chunk.second+1){
                    barrierList.remove(pos1.clone().add(0.0,0.0,16.0) to pos2.clone().add(0.0,0.0,1.0))
                }
                if(otherChunk.first == chunk.first-1 && otherChunk.second == chunk.second){
                    barrierList.remove(pos1.clone().add(-1.0,0.0,0.0) to pos2.clone().add(-16.0,0.0,0.0))
                }
                if(otherChunk.first == chunk.first+1 && otherChunk.second == chunk.second){
                    barrierList.remove(pos1.clone().add(16.0,0.0,0.0) to pos2.clone().add(1.0,0.0,0.0))
                }
            }
        }
        for (region in barrierList){
            setBlock(region.first,region.second,BlockTypes.BARRIER!!)
        }

    }

    /**
     * 设置玩家区块边界为屏障,所有被占领的区块外部都被屏障包裹
     * @param chunkList 玩家区块列表
     * @param chunk 玩家要占领的区块
     */
    fun setBarrier(chunkList:Set<Pair<Int,Int>>,chunk:Pair<Int,Int>,world: World){
        //获取设置区块的两角坐标
        val pos1 = Location(world,chunk.first*16.toDouble(),-64.0,chunk.second*16.toDouble())
        val pos2 = Location(world,chunk.first*16.toDouble()+15,319.0,chunk.second*16.toDouble()+15)
        //获取围绕区块的四面
        val barrierRegions = mutableListOf(
            //北面
            pos1.clone().add(0.0,0.0,-1.0) to pos2.clone().add(0.0,0.0,-16.0),
            //西面
            pos1.clone().add(-1.0,0.0,0.0) to pos2.clone().add(-16.0,0.0,0.0),
            //南面
            pos1.clone().add(0.0,0.0,16.0) to pos2.clone().add(0.0,0.0,1.0),
            //东面
            pos1.clone().add(16.0,0.0,0.0) to pos2.clone().add(1.0,0.0,0.0)
        )
        //根据是否有相临的区块来删除屏障
        for (otherChunk in chunkList){
            //就是目标chunk
            if(otherChunk.first == chunk.first && otherChunk.second == chunk.second){
                continue
            }
            //判断其他被占领的区块和他有没有相接，如果相接就去掉一面屏障
            if(otherChunk.first == chunk.first && otherChunk.second == chunk.second-1){
                barrierRegions.remove(pos1.clone().add(0.0,0.0,-1.0) to pos2.clone().add(0.0,0.0,-16.0))
            }
            if(otherChunk.first == chunk.first && otherChunk.second == chunk.second+1){
                barrierRegions.remove(pos1.clone().add(0.0,0.0,16.0) to pos2.clone().add(0.0,0.0,1.0))
            }
            if(otherChunk.first == chunk.first-1 && otherChunk.second == chunk.second){
                barrierRegions.remove(pos1.clone().add(-1.0,0.0,0.0) to pos2.clone().add(-16.0,0.0,0.0))
            }
            if(otherChunk.first == chunk.first+1 && otherChunk.second == chunk.second){
                barrierRegions.remove(pos1.clone().add(16.0,0.0,0.0) to pos2.clone().add(1.0,0.0,0.0))
            }
        }
        for (region in barrierRegions){
            setBlock(region.first,region.second,BlockTypes.BARRIER!!)
        }
    }
}