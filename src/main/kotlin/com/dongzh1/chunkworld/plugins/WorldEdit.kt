package com.dongzh1.chunkworld.plugins

import ParticleEffect
import com.dongzh1.chunkworld.ChunkWorld
import com.fastasyncworldedit.core.util.TaskManager
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockType
import com.sk89q.worldedit.world.block.BlockTypes
import com.xbaimiao.easylib.util.submit
import me.arcaniax.hdb.api.HeadDatabaseAPI
import org.bukkit.*
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import kotlin.random.Random


object WorldEdit {
    /**
     * 设置区域内的所有方块为指定方块
     * @param pos1 区域的第一个位置
     * @param pos2 区域的第二个位置
     * @param blockType 指定的方块
     */
    private fun setBlock(pos1: Location, pos2: Location, blockType: BlockType) {
        TaskManager.taskManager().async {
            val world = BukkitAdapter.adapt(pos1.world)
            val region = CuboidRegion(
                BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
                BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
            )
            // 创建一个编辑会话
            val editSession: EditSession =
                WorldEdit.getInstance().newEditSessionBuilder().world(world).fastMode(true).build()

            // 替换区域内的所有方块为指定方块
            editSession.use { session ->
                session.setBlocks(region as Region, blockType.defaultState)
            }
        }
    }

    fun setVoid(chunk: Chunk) {
        if (chunk.world.environment == World.Environment.NORMAL){
            val pos1 = Location(chunk.world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
            val pos2 = Location(chunk.world, chunk.x * 16.toDouble() + 15, 319.0, chunk.z * 16.toDouble() + 15)
            TaskManager.taskManager().async {
                val world = BukkitAdapter.adapt(pos1.world)
                val region = CuboidRegion(
                    BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
                    BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
                )
                // 创建一个编辑会话
                val editSession: EditSession =
                    WorldEdit.getInstance().newEditSessionBuilder().world(world).fastMode(true).build()

                // 替换区域内的所有方块为指定方块
                editSession.use { session ->
                    session.setBlocks(region as Region, BlockTypes.AIR!!.defaultState)
                }
            }
        }else{
            val pos1 = Location(chunk.world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
            val pos2 = Location(chunk.world, chunk.x * 16.toDouble() + 15, 255.0, chunk.z * 16.toDouble() + 15)
            TaskManager.taskManager().async {
                val world = BukkitAdapter.adapt(pos1.world)
                val region = CuboidRegion(
                    BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
                    BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
                )
                // 创建一个编辑会话
                val editSession: EditSession =
                    WorldEdit.getInstance().newEditSessionBuilder().world(world).fastMode(true).build()

                // 替换区域内的所有方块为指定方块
                editSession.use { session ->
                    session.setBlocks(region as Region, BlockTypes.AIR!!.defaultState)
                }
            }
        }

    }

    /**
     * 替换区域内的方块为指定方块
     */
    fun setBlock(pos1: Location, pos2: Location, beReplaceType: BlockType, blockType: BlockType) {
        TaskManager.taskManager().async {
            val world = BukkitAdapter.adapt(pos1.world)
            val region = CuboidRegion(
                BlockVector3.at(pos1.blockX, pos1.blockY, pos1.blockZ),
                BlockVector3.at(pos2.blockX, pos2.blockY, pos2.blockZ)
            )
            // 创建一个编辑会话
            val editSession: EditSession =
                WorldEdit.getInstance().newEditSessionBuilder().world(world).fastMode(true).build()

            // 替换区域内的所有方块为指定方块
            editSession.use { session ->
                session.replaceBlocks(
                    region as Region,
                    mutableSetOf(BaseBlock(beReplaceType.defaultState)),
                    blockType.defaultState
                )
            }
        }
    }

    /**
     * 复制指定区块到指定位置，保留群系
     * @param chunk 区块
     * @param targetChunk 目标区块
     */
    fun copyChunk(chunk: Chunk, targetChunk: Chunk, p: Player) {
        TaskManager.taskManager().async {
            copy(chunk, targetChunk)
            //生成宝藏
            addItems(targetChunk, p)
        }
    }

    private fun copy(chunk: Chunk, targetChunk: Chunk) {
        val weWorld = BukkitAdapter.adapt(chunk.world)

        if (targetChunk.world.environment == World.Environment.NORMAL) {
            val region = CuboidRegion(
                weWorld,
                BlockVector3.at(chunk.x * 16, -64, chunk.z * 16),
                BlockVector3.at(chunk.x * 16 + 15, 319, chunk.z * 16 + 15)
            )
            val clipboard = BlockArrayClipboard(region)
            region.compile(clipboard, weWorld)
            clipboard.paste(
                BukkitAdapter.adapt(targetChunk.world),
                BlockVector3.at(targetChunk.x * 16, -64, targetChunk.z * 16),
                false,
                true,
                null
            )
        } else {
            val region = CuboidRegion(
                weWorld,
                BlockVector3.at(chunk.x * 16, 0, chunk.z * 16),
                BlockVector3.at(chunk.x * 16 + 15, 255, chunk.z * 16 + 15)
            )
            val clipboard = BlockArrayClipboard(region)
            region.compile(clipboard, weWorld)
            clipboard.paste(
                BukkitAdapter.adapt(targetChunk.world),
                BlockVector3.at(targetChunk.x * 16, 0, targetChunk.z * 16),
                false,
                true,
                null
            )
        }


    }

    /**
     * 复制群系
     */
    fun copyChunkBiome(chunk: Chunk, targetChunk: Chunk) {
        TaskManager.taskManager().async {
            copy(chunk, targetChunk)
            if (chunk.world.environment == World.Environment.NORMAL) {
                val pos1 =
                    Location(targetChunk.world, targetChunk.x * 16.toDouble(), -64.0, targetChunk.z * 16.toDouble())
                val pos2 = Location(
                    targetChunk.world,
                    targetChunk.x * 16.toDouble() + 15,
                    319.0,
                    targetChunk.z * 16.toDouble() + 15
                )
                setBlock(pos1, pos2, BlockTypes.AIR!!)
            } else {
                val pos1 =
                    Location(targetChunk.world, targetChunk.x * 16.toDouble(), 0.0, targetChunk.z * 16.toDouble())
                val pos2 = Location(
                    targetChunk.world,
                    targetChunk.x * 16.toDouble() + 15,
                    255.0,
                    targetChunk.z * 16.toDouble() + 15
                )
                setBlock(pos1, pos2, BlockTypes.AIR!!)
            }

        }
    }

    private fun Region.compile(clipboard: BlockArrayClipboard, world: com.sk89q.worldedit.world.World) {
        WorldEdit.getInstance().newEditSession(world).use {
            it.disableHistory()
            val forward = ForwardExtentCopy(it, this, clipboard, clipboard.minimumPoint)
            forward.isCopyingBiomes = true
            forward.isCopyingEntities = true
            Operations.complete(forward)
        }
    }

    /**
     * 设置区块边界
     */
    fun setBarrier(chunkList: Set<Pair<Int, Int>>, world: World) {
        val barrierList = mutableListOf<Pair<Location, Location>>()
        for (chunk in chunkList) {
            //获取设置区块的两角坐标
            val pos1 = Location(world, chunk.first * 16.toDouble(), -64.0, chunk.second * 16.toDouble())
            val pos2 = Location(world, chunk.first * 16.toDouble() + 15, 319.0, chunk.second * 16.toDouble() + 15)
            //北面
            barrierList.add(pos1.clone().add(0.0, 0.0, -1.0) to pos2.clone().add(0.0, 0.0, -16.0))
            //西面
            barrierList.add(pos1.clone().add(-1.0, 0.0, 0.0) to pos2.clone().add(-16.0, 0.0, 0.0))
            //南面
            barrierList.add(pos1.clone().add(0.0, 0.0, 16.0) to pos2.clone().add(0.0, 0.0, 1.0))
            //东面
            barrierList.add(pos1.clone().add(16.0, 0.0, 0.0) to pos2.clone().add(1.0, 0.0, 0.0))
            //根据是否有相临的区块来删除屏障
            for (otherChunk in chunkList) {
                //就是目标chunk
                if (otherChunk.first == chunk.first && otherChunk.second == chunk.second) {
                    continue
                }
                //判断其他被占领的区块和他有没有相接，如果相接就去掉一面屏障
                if (otherChunk.first == chunk.first && otherChunk.second == chunk.second - 1) {
                    barrierList.remove(pos1.clone().add(0.0, 0.0, -1.0) to pos2.clone().add(0.0, 0.0, -16.0))
                }
                if (otherChunk.first == chunk.first && otherChunk.second == chunk.second + 1) {
                    barrierList.remove(pos1.clone().add(0.0, 0.0, 16.0) to pos2.clone().add(0.0, 0.0, 1.0))
                }
                if (otherChunk.first == chunk.first - 1 && otherChunk.second == chunk.second) {
                    barrierList.remove(pos1.clone().add(-1.0, 0.0, 0.0) to pos2.clone().add(-16.0, 0.0, 0.0))
                }
                if (otherChunk.first == chunk.first + 1 && otherChunk.second == chunk.second) {
                    barrierList.remove(pos1.clone().add(16.0, 0.0, 0.0) to pos2.clone().add(1.0, 0.0, 0.0))
                }
            }
        }
        for (region in barrierList) {
            setBlock(region.first, region.second, BlockTypes.BARRIER!!)
        }

    }

    /**
     * 设置玩家区块边界为屏障,所有被占领的区块外部都被屏障包裹
     * @param chunkList 玩家区块列表
     * @param chunk 玩家要占领的区块
     */
    fun setBarrier(chunkList: Set<Pair<Int, Int>>, chunk: Pair<Int, Int>, world: World) {
        val pos1: Location
        val pos2: Location
        if (world.environment == World.Environment.NORMAL) {
            //获取设置区块的两角坐标
            pos1 = Location(world, chunk.first * 16.toDouble(), -64.0, chunk.second * 16.toDouble())
            pos2 = Location(world, chunk.first * 16.toDouble() + 15, 319.0, chunk.second * 16.toDouble() + 15)
        } else {
            pos1 = Location(world, chunk.first * 16.toDouble(), 0.0, chunk.second * 16.toDouble())
            pos2 = Location(world, chunk.first * 16.toDouble() + 15, 255.0, chunk.second * 16.toDouble() + 15)
        }
        //获取围绕区块的四面
        val barrierRegions = mutableListOf(
            //北面
            pos1.clone().add(0.0, 0.0, -1.0) to pos2.clone().add(0.0, 0.0, -16.0),
            //西面
            pos1.clone().add(-1.0, 0.0, 0.0) to pos2.clone().add(-16.0, 0.0, 0.0),
            //南面
            pos1.clone().add(0.0, 0.0, 16.0) to pos2.clone().add(0.0, 0.0, 1.0),
            //东面
            pos1.clone().add(16.0, 0.0, 0.0) to pos2.clone().add(1.0, 0.0, 0.0)
        )
        //根据是否有相临的区块来删除屏障
        for (otherChunk in chunkList) {
            //就是目标chunk
            if (otherChunk.first == chunk.first && otherChunk.second == chunk.second) {
                continue
            }
            //判断其他被占领的区块和他有没有相接，如果相接就去掉一面屏障
            if (otherChunk.first == chunk.first && otherChunk.second == chunk.second - 1) {
                barrierRegions.remove(pos1.clone().add(0.0, 0.0, -1.0) to pos2.clone().add(0.0, 0.0, -16.0))
            }
            if (otherChunk.first == chunk.first && otherChunk.second == chunk.second + 1) {
                barrierRegions.remove(pos1.clone().add(0.0, 0.0, 16.0) to pos2.clone().add(0.0, 0.0, 1.0))
            }
            if (otherChunk.first == chunk.first - 1 && otherChunk.second == chunk.second) {
                barrierRegions.remove(pos1.clone().add(-1.0, 0.0, 0.0) to pos2.clone().add(-16.0, 0.0, 0.0))
            }
            if (otherChunk.first == chunk.first + 1 && otherChunk.second == chunk.second) {
                barrierRegions.remove(pos1.clone().add(16.0, 0.0, 0.0) to pos2.clone().add(1.0, 0.0, 0.0))
            }
        }
        for (region in barrierRegions) {
            setBlock(region.first, region.second, BlockTypes.BARRIER!!)
        }

    }

    private fun addItems(chunk: Chunk, p: Player) {
        fun getAmount(amountString: String):Int{
            return if (amountString.contains("-")){
                val min = amountString.split("-")[0].toInt()
                val max = amountString.split("-")[1].toInt()
                Random.nextInt(min,max+1)
            }else{
                amountString.toInt()
            }
        }
        submit {
            val config =
                if (chunk.world.environment == World.Environment.NORMAL){
                    ChunkWorld.inst.config.getConfigurationSection("Baoxiang.world")!!
                }else{
                    ChunkWorld.inst.config.getConfigurationSection("Baoxiang.nether")!!
                }
            val block = chunk.getBlock(Random.nextInt(3, 13),
                Random.nextInt(config.getInt("Ymin"), config.getInt("Ymax")),
                Random.nextInt(3, 13))
            //清理周围的方块，形成一个3*3*3的空间
            for (x in -1..1) {
                for (y in 0..2) {
                    for (z in -1..1) {
                        val block1 = block.getRelative(x, y, z)
                        block1.type = Material.AIR
                    }
                }
            }
            val light = block.getRelative(0, -1, 0)
            light.type = Material.SEA_LANTERN
            block.type = Material.PLAYER_HEAD
            //指定玩家头颅
            val meta = block.state as Skull
            val hdb = HeadDatabaseAPI()
            val head = hdb.getItemHead("59124").itemMeta as SkullMeta
            meta.ownerProfile = head.playerProfile
            //把头颅信息保存好，便于监听,并播放粒子
            val pUUID = ParticleEffect.startCircleEffect(block.location,1.0,5,Particle.WITCH)
            meta.persistentDataContainer.set(
                NamespacedKey.fromString("baozang")!!,
                PersistentDataType.STRING,pUUID.toString())
            block.chunk.persistentDataContainer.set(
                NamespacedKey.fromString("baozang_location")!!,
                PersistentDataType.STRING,"${block.x},${block.y},${block.z}")
            meta.update()
        }
    }
}