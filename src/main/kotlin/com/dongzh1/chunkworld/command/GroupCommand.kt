package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.listener.SingleListener.setBanMap
import com.dongzh1.chunkworld.listener.SingleListener.setChunkMap
import com.dongzh1.chunkworld.listener.SingleListener.setPlayerDaoMap
import com.dongzh1.chunkworld.listener.SingleListener.setTrustMap
import com.dongzh1.chunkworld.listener.SingleListener.setUUIDtoName
import com.dongzh1.chunkworld.redis.RedisData
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.World.Environment
import org.bukkit.World.Environment.*
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

@ECommandHeader(command = "chunkworld")
object GroupCommand {
    private val environment = ArgNode(("environment"),
        exec = {
            listOf("NORMAL","NETHER")
        }, parse = {
            Environment.valueOf(it)
        })
    private
    @CommandBody
    val tp = command<Player>("tp"){
        description = "传送到自己或别人家园，自己没有的话会创建"
        players(optional = true) {
            exec {
                val p = valueOfOrNull(it)?:sender
                if (p != sender){
                    //去别人家，按理说应该菜单去，这个是给op的，让op能方便的去到任何人的家，所以判断权限
                    if (!sender.hasPermission("chunkworld.tpOther")){
                        sender.sendMessage("§c你无法使用此指令")
                        return@exec
                    }
                }
                //查看对应世界是否加载了
            }
        }
    }
    @CommandBody
    val create = command<CommandSender>("create"){
        description = "创建此玩家世界并决定是否创建完毕后传送玩家过去，如果已经存在世界了，会加载世界并传送玩家过去"
        permission = "chunkworld.admin"
        arg(environment){ environmentArg ->
            players { playersArg ->
                booleans {
                    exec {
                        val envir = valueOf(environmentArg)
                        val p = valueOfOrNull(playersArg)
                        if (p == null){
                            sender.sendMessage("§c此玩家不在线，无法创建")
                            return@exec
                        }
                        //要创建的世界的位置
                        val file :File
                        val templeFile:File
                        val worldName :String
                        val isTeleport = valueOf(it)
                        when(envir){
                            NORMAL -> {
                                file = File(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world")
                                templeFile = File(ChunkWorld.inst.dataFolder, "world")
                                worldName = ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world"
                                //有level.dat_old文件说明是加载过的
                                if (File(file,"level.dat_old").exists()) {
                                    if (!isTeleport){
                                        //不传送,说明这个世界已经创建了
                                        sender.sendMessage("§c此玩家主世界已创建,不能重复创建")
                                        return@exec
                                    }else{
                                        //执行传送指令,将此玩家送回她的世界
                                        //todo
                                        return@exec
                                    }

                                }
                            }
                            NETHER -> {
                                file = File(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/nether")
                                templeFile = File(ChunkWorld.inst.dataFolder, "nether")
                                worldName = ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/nether"
                                if (File(file,"level.dat_old").exists()) {
                                    //说明这个世界已经创建了
                                    sender.sendMessage("§c此玩家地狱世界已创建,不能重复创建")
                                    return@exec
                                }
                            }
                            else -> {
                                sender.sendMessage("§c世界类型错误")
                                return@exec
                            }
                        }
                        //开启协程，会在异步和同步之间来回使用,现在是异步
                        launchCoroutine(SynchronizationContext.ASYNC) {
                            //创建家园世界,先复制模板文件
                            try {
                                templeFile.copyRecursively(file)
                            }catch (ex:Exception) {
                                //踢出玩家并提示联系管理员
                                p.sendMessage("§c你的世界创建失败，原因为复制失败，请尽快联系管理员")
                                kotlin.error("${p.name}玩家的${p.uniqueId}/${envir}世界复制文件失败")
                            }
                            //复制完毕，加载世界
                            val wc = WorldCreator(worldName)
                            wc.environment(envir)
                            //现在是同步
                            switchContext(SynchronizationContext.SYNC)
                            val world = wc.createWorld()
                            if (world == null){
                                //世界加载失败了
                                p.sendMessage("§c你的世界创建失败，原因为加载世界失败，请尽快联系管理员")
                                kotlin.error("${p.name}玩家的${p.uniqueId}/${envir}世界创建加载失败")
                            }
                            //设置世界规则等
                            world.isAutoSave = true
                            world.setGameRule(GameRule.KEEP_INVENTORY,true)
                            world.setGameRule(GameRule.MOB_GRIEFING,false)
                            world.setGameRule(GameRule.SPAWN_CHUNK_RADIUS,0)
                            //现在是异步
                            switchContext(SynchronizationContext.ASYNC)
                                var playerDao = ChunkWorld.db.playerGet(p.name)
                                //第一次加载世界完毕，建立玩家信息
                                if (playerDao == null) {
                                    playerDao = PlayerDao().apply {
                                        name = p.name
                                        uuid = p.uniqueId
                                        createTime = java.text.SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒")
                                            .format(java.util.Date(System.currentTimeMillis()))
                                        when(envir){
                                            NORMAL -> {
                                                spawn =
                                                    "${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}"
                                                netherSpawn = "null"
                                            }
                                            NETHER -> {
                                                netherSpawn =
                                                    "${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}"
                                                spawn = "null"
                                            }
                                            else ->{}
                                        }
                                        worldStatus = 0
                                        lastTime = System.currentTimeMillis()
                                        teleport = "${world.name.split("/").last()},${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}"
                                    }
                                    //玩家数据存入数据库
                                    ChunkWorld.db.playerCreate(playerDao)
                                    //取出玩家数据，获取id
                                    playerDao = ChunkWorld.db.playerGet(p.name)!!
                                    //出生的区块也存入
                                    val chunkDao = ChunkDao().apply {
                                        playerID = playerDao.id
                                        x = world.spawnLocation.chunk.x
                                        z = world.spawnLocation.chunk.z
                                        when(envir){
                                            NORMAL -> { worldType = 0 }
                                            NETHER -> { worldType = 1 }
                                            else -> {}
                                        }
                                    }
                                    //区块数据存入数据库
                                    ChunkWorld.db.chunkCreate(chunkDao)
                                    //新建了玩家数据，可以存入内存
                                    RedisData.setPlayerDao(playerDao)
                                    RedisData.setChunks(p.uniqueId.toString(),setOf(chunkDao.x to chunkDao.z),chunkDao.worldType)
                                    //现在是同步
                                    switchContext(SynchronizationContext.SYNC)
                                    //这里是第一次加载，通过worldedit插件复制屏障到占领的区块边缘
                                    WorldEdit.setBarrier(
                                        setOf(chunkDao.x to chunkDao.z),
                                        chunkDao.x to chunkDao.z,
                                        world
                                    )
                                    //存储世界
                                    world.save()
                                } else {
                                    //有玩家数据,导入所有到内存
                                    spawnLocation = Location(world,playerDao.x(),playerDao.y(),playerDao.z(),playerDao.yaw(),playerDao.pitch())
                                    setPlayerDaoMap(e.player.name,playerDao)
                                    setUUIDtoName(e.player.uniqueId,e.player.name)
                                    val chunList = ChunkWorld.db.chunkGet(playerDao.id)
                                    if (chunList.isEmpty()){
                                        //说明玩家上次区块信息没存入，重新存
                                        val chunkDao = ChunkDao().apply {
                                            playerID = playerDao.id
                                            x = world.spawnLocation.chunk.x
                                            z = world.spawnLocation.chunk.z
                                        }
                                        //区块数据存入数据库
                                        ChunkWorld.db.chunkCreate(chunkDao)
                                        chunkMap[e.player] = setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z)
                                        //创建第一次的屏障
                                        //现在是同步
                                        switchContext(SynchronizationContext.SYNC)
                                        WorldEdit.setBarrier(
                                            setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z),
                                            world.spawnLocation.chunk.x to world.spawnLocation.chunk.z, world
                                        )
                                    }else{
                                        //有区块信息，存入内存
                                        setChunkMap(e.player,chunList.toSet())
                                    }
                                    SynchronizationContext.ASYNC
                                    setTrustMap(e.player, ChunkWorld.db.getShip(playerDao.id,true).map { it.uuid }.toSet())
                                    setBanMap(e.player, ChunkWorld.db.getShip(playerDao.id,false).map { it.uuid }.toSet())
                                }
                                //切换主线程，加载区块并传送玩家过去
                                switchContext(SynchronizationContext.SYNC)
                                //传送玩家
                                e.player.teleportAsync(spawnLocation).thenAccept {
                                    e.player.clearTitle()
                                    e.player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
                                }

                        }
                    }

                    }
                }
            }
        }
    }
}