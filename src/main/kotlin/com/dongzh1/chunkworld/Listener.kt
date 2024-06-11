package com.dongzh1.chunkworld

import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.asyncDispatcher
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.io.File
import java.io.IOException
import java.time.Duration
import javax.xml.crypto.Data
import kotlin.coroutines.suspendCoroutine

object Listener:Listener {
    private val playerDaoMap = mutableMapOf<Player,PlayerDao>()
    private val chunkMap = mutableMapOf<Player,List<Pair<Int,Int>>>()
    private val trustMap = mutableMapOf<Player,List<String>>()
    private val banMap = mutableMapOf<Player,List<String>>()
    private val beTrustMap = mutableMapOf<Player,List<String>>()
    private val beBanMap = mutableMapOf<Player,List<String>>()

    @EventHandler
    fun onLogin(e:PlayerLoginEvent) {
    }
    @EventHandler
    fun onSpawn(e:PlayerSpawnLocationEvent){

    }
    @EventHandler
    fun onJoin(e:PlayerJoinEvent){

        //给玩家致盲效果
        e.player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS,20*60,3))
        //提示title
        e.player.showTitle(Title.title(Component.text("§a欢迎回家"), Component.text("§f正在将您传送至家园..."),
            Times.times(Duration.ofSeconds(1), Duration.ofMinutes(1), Duration.ofSeconds(1))))
        //开启协程，会在异步和同步之间来回使用,现在是异步
        launchCoroutine(SynchronizationContext.ASYNC) {
            //查询玩家信息并存入内存
            var playerDao = ChunkWorld.db.playerGet(e.player.name)
            if (playerDao == null) {
                //说明第一次进服，先创建世界
                //说明世界还没创建过,创建家园世界,先复制模板文件
                val worldFolder = File(ChunkWorld.inst.dataFolder, "world")
                try {
                    worldFolder.copyRecursively(File(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"))
                }catch (ex:Exception) {
                    //踢出玩家并提示联系管理员
                    switchContext(SynchronizationContext.SYNC)
                    e.player.kick(Component.text("世界文件复制失败，请联系管理员"))
                    error("世界文件复制失败")
                }
            }
            //确定世界文件是否存在
            if (!File(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}/poi/r.0.0.mca").exists()) {
                //根据玩家进度进行处理
                switchContext(SynchronizationContext.SYNC)
                e.player.kick(Component.text("世界文件不完整或第一次加载未正确保留，请联系管理员"))
                error("世界文件不完整")
            }
            //现在是同步
            switchContext(SynchronizationContext.SYNC)
            //复制完毕，加载世界
            val world = Bukkit.createWorld(WorldCreator(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"))
            if (world == null){
                //世界加载失败了
                e.player.kick(Component.text("世界加载失败，请联系管理员"))
                error("世界加载失败")
            }else {
                //现在是异步
                switchContext(SynchronizationContext.ASYNC)
                //第一次加载世界完毕，建立玩家信息
                if (playerDao == null) {
                    playerDao = PlayerDao().apply {
                        name = e.player.name
                        uuid = e.player.uniqueId
                        createTime = java.text.SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒")
                            .format(java.util.Date(System.currentTimeMillis()))
                        spawn =
                            "${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}"
                        worldStatus = 0
                    }
                    //玩家数据存入数据库
                    ChunkWorld.db.playerCreate(playerDao)
                    //取出玩家数据，获取id
                    playerDao = ChunkWorld.db.playerGet(e.player.name)!!
                    //出生的区块也存入
                    val chunkDao = ChunkDao().apply {
                        playerID = playerDao.id
                        x = world.spawnLocation.chunk.x
                        z = world.spawnLocation.chunk.z
                    }
                    //区块数据存入数据库
                    ChunkWorld.db.chunkCreate(chunkDao)
                    //新建了玩家数据，可以存入内存
                    playerDaoMap[e.player] = playerDao
                    chunkMap[e.player] = listOf(chunkDao.x to chunkDao.z)
                    trustMap[e.player] = emptyList()
                    banMap[e.player] = emptyList()
                    beTrustMap[e.player] = emptyList()
                    beBanMap[e.player] = emptyList()
                    //这里是第一次加载，通过异步复制屏障到占领的区块边缘

                } else {
                    //有玩家数据,导入所有到内存
                    playerDaoMap[e.player] = playerDao
                    chunkMap[e.player] = ChunkWorld.db.chunkGet(playerDao.id)
                    if (chunkMap[e.player]!!.isEmpty()) {
                        //说明玩家上次区块信息没存入，重新存
                        val chunkDao = ChunkDao().apply {
                            playerID = playerDao.id
                            x = world.spawnLocation.chunk.x
                            z = world.spawnLocation.chunk.z
                        }
                        //区块数据存入数据库
                        ChunkWorld.db.chunkCreate(chunkDao)
                        chunkMap[e.player] = listOf(chunkDao.x to chunkDao.z)
                    }
                    trustMap[e.player] = ChunkWorld.db.trustGet(playerDao.id)
                    banMap[e.player] = ChunkWorld.db.banGet(playerDao.id)
                    beTrustMap[e.player] = ChunkWorld.db.beTrustGet(playerDao.id)
                    beBanMap[e.player] = ChunkWorld.db.beBanGet(playerDao.id)
                }
                //切换主线程，加载区块并传送玩家过去
                switchContext(SynchronizationContext.SYNC)
                //传送玩家
                e.player.teleportAsync(world.spawnLocation).thenAccept {
                    e.player.clearTitle()
                    e.player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
                    //存储世界
                    world.save()
                }
            }
        }

    }


}