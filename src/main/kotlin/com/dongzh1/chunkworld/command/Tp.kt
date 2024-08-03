package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.database.dao.WorldInfo
import com.dongzh1.chunkworld.listener.GroupListener
import com.dongzh1.chunkworld.redis.RedisManager
import com.dongzh1.chunkworld.redis.RedisPush
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.task.EasyLibTask
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.util.TriState
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

object Tp {
    private val worldInfoRedis = mutableMapOf<String, EasyLibTask>()
    fun removeWorldInfo(worldName: String) {
        worldInfoRedis[worldName]?.cancel()
        worldInfoRedis.remove(worldName)
    }

    fun connect(player: Player, server: String) {
        val byteArray = ByteArrayOutputStream()
        val out = DataOutputStream(byteArray)
        try {
            out.writeUTF("Connect")
            out.writeUTF(server)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        player.sendPluginMessage(ChunkWorld.inst, "BungeeCord", byteArray.toByteArray())
    }

    /**
     * 一般仅用于指令传送，被异步执行
     */
    fun teleportPlayerWorld(p: Player, targetName: String, state: Byte, serverName: String) {
        val targetDao = ChunkWorld.db.getPlayerDao(targetName)!!
        val worldName = "chunkworlds/world/${targetDao.uuid}"
        //判断能否传送
        if (p.name != targetName && !p.hasPermission("chunkworld.admin")) {
            //不是自己世界，进行权限判断
            when (state) {
                2.toByte() -> {
                    p.sendMessage("§c目标世界仅允许世界主人进入,无法传送")
                    return
                }

                1.toByte() -> {
                    val playerDao = ChunkWorld.db.getPlayerDao(p.name)!!
                    if (!ChunkWorld.db.isTrust(playerDao.id, targetDao.id)) {
                        //不是共享玩家，拉黑玩家肯定不是共享玩家
                        p.sendMessage("§c目标世界只对共享玩家开放,无法传送")
                        return
                    }
                }

                0.toByte() -> {
                    //拉黑不许进
                    val playerDao = ChunkWorld.db.getPlayerDao(p.name)!!
                    if (ChunkWorld.db.isBan(playerDao.id, targetDao.id)) {
                        p.sendMessage("§c目标世界和你处于拉黑状态,无法传送")
                        return
                    }
                }
            }
        }
        if (serverName == ChunkWorld.serverName) {
            //在本服
            val world = Bukkit.getWorld(worldName)
            if (world == null) {
                p.sendMessage("§c目标世界主人已离线，世界已关闭")
                return
            }
            submit { p.teleportAsync(world.spawnLocation) }
            return
        }
        //现在留下的都是可以传送的,发送消息想要传过去
        RedisPush.teleportWorld(p.name, worldName, serverName).thenAccept {
            if (it == null) {
                p.sendMessage("§c链接超时,可能目标服务器正在重启，请联系管理员")
            } else {
                if (it == "true") connect(p, serverName)
                else p.sendMessage("§c目标世界主人已离线，世界已关闭")
            }
        }
    }

    /**
     * 此方法必定异步运行
     * 如果有玩家世界文件，则加载，否则创建主世界，地狱也会加载，但是地狱的创建不在这里，会将重要信息写入世界存储
     */
    fun createWorldLocal(playerUUID: UUID, name: String, id: Int, perm: Boolean): CompletableFuture<Boolean> {
        //查询文件在不在，在的话说明不是第一次加载
        val isFirst = !File("chunkworlds/world/$playerUUID/uid.dat").exists()
        val hasNether = File("chunkworlds/nether/$playerUUID/uid.dat").exists()
        val future = CompletableFuture<Boolean>()
        //复制世界
        if (isFirst) {
            val file = File("chunkworlds/world/${playerUUID}")
            val templeFile = File(ChunkWorld.inst.dataFolder, "world")
            try {
                templeFile.copyRecursively(file)
            } catch (ex: Exception) {
                //踢出玩家并提示联系管理员
                future.apply { complete(false) }
                return future
            }
        }
        //获取信任关系，离线时信任关系也可能发生改变
        val trusts = ChunkWorld.db.getTrustNames(id)
        val banners = ChunkWorld.db.getBanNames(id)
        submit {
            //在本服创建
            val worldName = "chunkworlds/world/$playerUUID"
            val wc = WorldCreator(worldName).keepSpawnLoaded(TriState.FALSE)
            val world = wc.createWorld()
            if (world == null) {
                future.apply { complete(false) }
                return@submit
            }
            //设置世界规则等
            world.isAutoSave = true
            if (isFirst) {
                world.setGameRule(GameRule.KEEP_INVENTORY, true)
                world.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0)
                world.setGameRule(GameRule.DO_FIRE_TICK, false)
                world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
                world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
                //这里是第一次加载，通过worldedit插件复制屏障到占领的区块边缘
                WorldEdit.setBarrier(
                    setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z),
                    world.spawnLocation.chunk.x to world.spawnLocation.chunk.z,
                    world
                )
                //存储一些重要信息
                world.persistentDataContainer.set(
                    NamespacedKey.fromString("chunkworld_chunks")!!,
                    PersistentDataType.STRING, "${world.spawnLocation.chunk.x},${world.spawnLocation.chunk.z}|"
                )
                world.spawnLocation.chunk.persistentDataContainer.set(
                    NamespacedKey.fromString("chunkworld_unlock")!!,
                    PersistentDataType.BOOLEAN, true
                )
                world.persistentDataContainer.set(
                    NamespacedKey.fromString("chunkworld_state")!!,
                    PersistentDataType.BYTE, 2
                )
                world.persistentDataContainer.set(
                    NamespacedKey.fromString("chunkworld_owner")!!,
                    PersistentDataType.STRING, name
                )
            }
            //存储信任玩家的name
            val trustString = trusts.joinToString("|,;|")
            val banString = banners.joinToString("|,;|")
            world.persistentDataContainer.set(
                NamespacedKey.fromString("chunkworld_trust")!!,
                PersistentDataType.STRING,
                trustString
            )
            world.persistentDataContainer.set(
                NamespacedKey.fromString("chunkworld_ban")!!,
                PersistentDataType.STRING,
                banString
            )
            //判明是否为要展示的世界
            //存储世界
            world.save()
            if (hasNether) {
                //加载地狱。创建另有方法
                val netherName = "chunkworlds/nether/$playerUUID"
                val netherWc =
                    WorldCreator(netherName).environment(World.Environment.NETHER).keepSpawnLoaded(TriState.FALSE)
                val nether = netherWc.createWorld()
                if (nether == null) {
                    future.apply { complete(false) }
                    return@submit
                }
                nether.isAutoSave = true
                nether.save()
            }
            //新建了世界数据，可以存入内存
            val state = world.persistentDataContainer.get(
                NamespacedKey.fromString("chunkworld_state")!!,
                PersistentDataType.BYTE
            )!!
            val chunks = world.persistentDataContainer.get(
                NamespacedKey.fromString("chunkworld_chunks")!!,
                PersistentDataType.STRING
            )!!.split("|").size - 1
            var netherChunks = 0
            if (hasNether) {
                val nether = Bukkit.getWorld("chunkworlds/nether/$playerUUID")!!
                netherChunks = nether.persistentDataContainer.get(
                    NamespacedKey.fromString("chunkworld_chunks")!!,
                    PersistentDataType.STRING
                )!!.split("|").size - 1
            }
            val serverName = ChunkWorld.serverName
            val worldInfo = WorldInfo(
                state = state,
                normalChunks = chunks,
                netherChunks = netherChunks,
                serverName = serverName,
                showWorld = perm
            )
            RedisManager.setWorldInfo(name, worldInfo)
            //定期发送消息告诉redis世界还在
            val task = submit(async = true, delay = 20 * 60 * 30, period = 20 * 60 * 30) {
                val worldNormal = Bukkit.getWorld("chunkworlds/world/$playerUUID")
                val worldNether = Bukkit.getWorld("chunkworlds/nether/$playerUUID")
                if (worldNormal == null && worldNether == null) {
                    //删除数据会由velocity发起
                    removeWorldInfo("chunkworlds/world/$playerUUID")
                    return@submit
                }
                if (worldNormal != null) {
                    RedisManager.setWorldInfo(name, getWorldInfo(worldNormal, worldNether, perm))
                }
            }
            worldInfoRedis["chunkworlds/world/$playerUUID"] = task
            world.getChunkAtAsync(world.spawnLocation)
            GroupListener.addLocation(name, world.spawnLocation)
            future.complete(true)
        }
        return future
    }

    fun createNether(p: Player) {
        val worldName = "chunkworlds/nether/${p.uniqueId}"
        val wc = WorldCreator(worldName).environment(World.Environment.NETHER).keepSpawnLoaded(TriState.FALSE)
        val world = wc.createWorld()
        if (world == null) {
            p.sendMessage("§c创建地狱失败")
            return
        }
        world.isAutoSave = true
        world.setGameRule(GameRule.KEEP_INVENTORY, true)
        world.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0)
        world.setGameRule(GameRule.DO_FIRE_TICK, false)
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
        WorldEdit.setBarrier(
            setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z),
            world.spawnLocation.chunk.x to world.spawnLocation.chunk.z,
            world
        )
        world.persistentDataContainer.set(
            NamespacedKey.fromString("chunkworld_chunks")!!,
            PersistentDataType.STRING, "${world.spawnLocation.chunk.x},${world.spawnLocation.chunk.z}|"
        )
        world.spawnLocation.chunk.persistentDataContainer.set(
            NamespacedKey.fromString("chunkworld_unlock")!!,
            PersistentDataType.BOOLEAN, true
        )
        world.save()
        p.teleportAsync(world.spawnLocation)
    }

    private fun getWorldInfo(world: World, netherWorld: World?, perm: Boolean): WorldInfo {
        val state =
            world.persistentDataContainer.get(NamespacedKey.fromString("chunkworld_state")!!, PersistentDataType.BYTE)!!
        val chunks = world.persistentDataContainer.get(
            NamespacedKey.fromString("chunkworld_chunks")!!,
            PersistentDataType.STRING
        )!!.split("|").size - 1
        var netherChunks = 0
        val serverName = ChunkWorld.serverName
        if (netherWorld != null) {
            netherChunks = netherWorld.persistentDataContainer.get(
                NamespacedKey.fromString("chunkworld_chunks")!!,
                PersistentDataType.STRING
            )!!.split("|").size - 1
        }
        return WorldInfo(
            state = state,
            normalChunks = chunks,
            netherChunks = netherChunks,
            serverName = serverName,
            showWorld = perm
        )
    }


    fun randomTp(p: Player, world: World, range: Int) {
        val x = Random.nextInt(-range, range)
        val z = Random.nextInt(-range, range)
        submit(async = true) {
            //异步获取对应的信息，主线程再传送和修改
            when (world.environment) {
                World.Environment.NETHER -> {
                    var locY: Int = 121
                    for (y in 120 downTo 32) {
                        if (isSafeLocation(world, x, y, z)) {
                            locY = y
                            break
                        }
                    }
                    if (locY == 121) locY = 64
                    submit {
                        if (locY == 64 && !isSafeLocation(world, x, locY, z)) {
                            world.getBlockAt(x, locY, z).type = Material.NETHERRACK
                            world.getBlockAt(x, locY + 1, z).type = Material.AIR
                            world.getBlockAt(x, locY + 2, z).type = Material.AIR
                        }
                        p.teleportAsync(Location(world, x + 0.5, locY + 1.0, z + 0.5))
                    }
                }

                World.Environment.THE_END -> {
                    var locY: Int = 71
                    for (y in 70 downTo 32) {
                        if (isSafeLocation(world, x, y, z)) {
                            locY = y
                            break
                        }
                    }
                    if (locY == 71) locY = 64
                    submit {
                        if (locY == 64 && !isSafeLocation(world, x, locY, z)) {
                            world.getBlockAt(x, locY, z).type = Material.END_STONE
                            world.getBlockAt(x, locY + 1, z).type = Material.AIR
                            world.getBlockAt(x, locY + 2, z).type = Material.AIR
                        }
                        p.teleportAsync(Location(world, x + 0.5, locY + 1.0, z + 0.5))
                    }
                }

                else -> {
                    //只考虑主世界了
                    val y = world.getHighestBlockYAt(x, z)
                    submit {
                        if (!isSafeLocation(world, x, y, z)) {
                            world.getBlockAt(x, y, z).type = Material.STONE
                            world.getBlockAt(x, y + 1, z).type = Material.AIR
                            world.getBlockAt(x, y + 2, z).type = Material.AIR
                        }
                        p.teleportAsync(Location(world, x + 0.5, y + 1.0, z + 0.5))
                    }
                }
            }
        }

    }

    private fun isSafeLocation(world: World, x: Int, y: Int, z: Int): Boolean {
        val block = world.getBlockAt(x, y, z).type
        val blockAbove = world.getBlockAt(x, y + 1, z).type
        val blockAbove2 = world.getBlockAt(x, y + 2, z).type

        // 检查传送位置是否安全（例如，方块下方是固体，上方是空气）
        return (!block.isAir && block != Material.WATER && block != Material.LAVA
                && blockAbove.isAir && blockAbove2.isAir)
    }
}