package com.dongzh1.chunkworld.listener

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.redis.RedisData
import com.dongzh1.chunkworld.redis.RedisManager
import com.google.common.base.Utf8
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldInitEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.util.*

object GroupListener : Listener {
    //玩家名对应世界，用于跨服传送
    private val locationMap = mutableMapOf<String,String>()
    private val kickPlayer = mutableMapOf<String,String>()
    private val toLobbyMap = mutableMapOf<String,String>()
    private val passPlayer = mutableSetOf<String>()
    fun addLocation(name:String,location:String){
        locationMap[name] = location
        submit(delay = 60) { locationMap.remove(name) }
    }
    private fun removeLocation(name: String){
        locationMap.remove(name)
    }
    private fun getLocation(name: String):String?{
        return locationMap[name]
    }
    fun addLobby(name:String,time:String){
        toLobbyMap[name] = time
        submit(delay = 60) { toLobbyMap.remove(name) }
    }
    private fun removeLobby(name: String){
        toLobbyMap.remove(name)
    }
    private fun getLobby(name: String):String?{
        return toLobbyMap[name]
    }
    @EventHandler
    fun onWorldInit(e:WorldInitEvent){
        //判断是不是玩家世界
        if (e.world.name.startsWith(ChunkWorld.inst.config.getString("World")!!)){
            //将数据从数据库存入redis
            submit(async = true) {
                val list = e.world.name.split("/")
                val uuidString = list[list.size-2]
                val playerDao: PlayerDao = ChunkWorld.db.playerGet(UUID.fromString(uuidString)) ?: //说明这个世界是第一次创建，在创建办法里会去上传
                return@submit
                val chunkDaoList = ChunkWorld.db.chunkGet(playerDao.id)
                //存入redis
                RedisData.setPlayerDao(playerDao)
                RedisData.setChunks(playerDao.uuid.toString(),chunkDaoList)
            }
        }
    }
    @EventHandler
    fun perLogin(e:AsyncPlayerPreLoginEvent){
        //todo
        //防止有玩家直接加入，只接受转接
        //防止非法跨服传送
        if (e.isTransferred){
            if (!passPlayer.contains(e.name)) kickPlayer[e.name] = "§c你的跨服时间过久"
        }
    }
    @EventHandler
    fun onLogin(e:PlayerLoginEvent){
        //检测转过来的是否合法
        val p = e.player
        val name = p.name
        var time = getLobby(name)
        val info = getLocation(name)
        if (info != null) time = info.split("|||||")[0]
        if (time != null){
            //转发进服，验证是否合法转发
            val cookie = p.retrieveCookie(NamespacedKey(ChunkWorld.inst,"transferTime"))
            cookie.thenAccept {
                //没有这个key会自动结束任务，不执行
                val cookieTime = it.toString(Charsets.UTF_8)
                if (cookieTime != time){
                    //不合法
                    kickPlayer[p.name] = "§c请勿伪造自制cookies作弊"
                }else{
                    //验证通过了,删除prelogin的阻止
                    if (kickPlayer.contains(p.name)){
                        //说明prelogin 已经加进去了删除就行
                        kickPlayer.remove(p.name)
                    }else{
                        //说明异步的还没加进去,禁止他加
                        passPlayer.add(name)
                    }

                }
            }
        }
        val uuidString = p.uniqueId.toString()
        //检测是否存入了黑白名单
        if (RedisData.getBanners(uuidString) == null || RedisData.getFriends(uuidString) == null){
            //没有存入，存入
            submit(async = true) {
                val playerDao = RedisData.getPlayerDao(uuidString) ?: ChunkWorld.db.playerGet(p.uniqueId)
                if (playerDao == null){
                    //存空值即可
                    RedisData.setShip(uuidString, setOf(), setOf())
                }else{
                    //从数据库获取
                    RedisData.setShip(uuidString,ChunkWorld.db.getShip(playerDao.id,true).map { it.uuid }.toSet(),ChunkWorld.db.getShip(playerDao.id,false).map { it.uuid }.toSet())
                }
            }
        }
    }
    @EventHandler
    fun onSpawn(e:PlayerSpawnLocationEvent){
        val p = e.player
        val info = getLocation(p.name)
        removeLocation(p.name)
        if (info != null){
            //转发进服，先把玩家传送到对应位置
            val location = info.split("|||||")[1]
            val list = location.split(",")
            val world = Bukkit.getWorld(list[0])
            if (world == null){
                kickPlayer[p.name] = "§c目标世界未成功加载，请联系管理员,目标世界信息 ${list[0]}"
                return
            }
            val x = list[1].toDouble()
            val y = list[2].toDouble()
            val z = list[3].toDouble()
            val yaw = list[4].toFloat()
            val pitch = list[5].toFloat()
            e.spawnLocation = Location(world,x,y,z,yaw,pitch)
        }
    }
    @EventHandler
    fun join(e:PlayerJoinEvent){
        val p = e.player
        if (kickPlayer.contains(p.name)){
            p.kick(Component.text(kickPlayer[p.name]!!),PlayerKickEvent.Cause.PLUGIN)
            kickPlayer.remove(p.name)
        }
    }
    @EventHandler
    //玩家离开服务器,清除内存数据
    fun onQuit(e: PlayerQuitEvent){
        //不知道是否真的退群组服了
        val p =e.player
        val cookie = p.retrieveCookie(NamespacedKey(ChunkWorld.inst,"transferTime"))
        cookie.thenAccept {
            val time = it.toString(Charsets.UTF_8)
            if (time == "0"){
                //真的退群组服了,清除redis数据
            }
        }
    }
    @EventHandler
    fun explode(e:EntityExplodeEvent){
        if (e.entityType != EntityType.TNT) e.isCancelled = true
    }
}