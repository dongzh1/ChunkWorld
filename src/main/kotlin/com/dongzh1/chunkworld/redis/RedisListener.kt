package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.command.Tp
import com.dongzh1.chunkworld.listener.GroupListener
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.util.TriState
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.WorldCreator
import redis.clients.jedis.JedisPubSub
import java.util.*

class RedisListener : JedisPubSub() {
    override fun onMessage(channel: String?, message: String?) {
        if (channel == ChunkWorld.CHANNEL) {
            val mList = message!!.split("|,|")
            when (mList[0]) {
                "teleportWorld" -> {
                    val worldName = mList[1]
                    //看看这个世界在本服是否已加载
                    val world = Bukkit.getWorld(worldName)
                    if (world != null) {
                        val xyz = mList[2].split(",")
                        val playerName = mList[4]
                        val location = Location(world, xyz[0].toDouble(), xyz[1].toDouble(), xyz[2].toDouble())
                        //预加载区块，等待传送
                        world.getChunkAtAsync(location)
                        GroupListener.addLocation(playerName,location)
                        RedisPush.teleportWorldFound(mList[3], worldName+playerName)
                    }
                }
                "teleportWorldFound" -> {
                    val serverName = mList[1]
                    //找到发出请求的服务器
                    if (ChunkWorld.inst.config.getString("serverName")!! == serverName){
                        val worldNameAndPlayerName = mList[2]
                        val targetServerName = mList[3]
                        //返回目标服务器ip和端口
                        RedisPush.getFuture("teleportWorld$worldNameAndPlayerName")?.complete(targetServerName)
                        RedisPush.removeFuture("teleportWorld$worldNameAndPlayerName")
                    }
                }
                "loadWorldTeleport" -> {
                    val serverName = mList[2]
                    //找到发出请求的服务器
                    if (ChunkWorld.inst.config.getString("serverName")!! == serverName){
                        val worldName = mList[1]
                        val xyz = mList[3].split(",")
                        val targetServerName = mList[4]
                        val playerName = mList[5]
                        //加载世界
                        submit {
                            if (RedisData.getPlayerDaoByName(playerName) == null){
                                submit(async = true) {
                                    RedisData.setPlayerDao(ChunkWorld.db.playerGet(playerName)!!)
                                }
                            }
                            val worldCreator = WorldCreator(worldName).keepSpawnLoaded(TriState.FALSE)
                            val world = worldCreator.createWorld()
                            Bukkit.getConsoleSender().sendMessage("§a世界 $worldName 加载中")
                            if (world != null) {
                                Bukkit.getConsoleSender().sendMessage("§a世界 $worldName 加载成功")
                                //加载成功
                                val location = Location(world, xyz[0].toDouble(), xyz[1].toDouble(), xyz[2].toDouble())
                                world.getChunkAtAsync(location)
                                GroupListener.addLocation(playerName,location)
                                RedisPush.loadWorldResult(targetServerName,worldName+playerName,"true")
                                Bukkit.getConsoleSender().sendMessage("§a世界 $worldName 信息发送")
                            }else{
                                RedisPush.loadWorldResult(targetServerName,worldName+playerName,null)
                            }
                        }
                    }
                }
                "loadWorldResult" -> {
                    val serverName = mList[2]
                    Bukkit.getConsoleSender().sendMessage("§a收到世界加载结果")
                    //找到发出请求的服务器
                    if (ChunkWorld.inst.config.getString("serverName")!! == serverName){
                        Bukkit.getConsoleSender().sendMessage("§a找到发出请求的服务器")
                        val worldNameAndPlayerName = mList[1]
                        val result = mList[3]
                        RedisPush.getFuture("loadWorld$worldNameAndPlayerName")?.complete(if (result == "true") "true" else null)
                        RedisPush.removeFuture("loadWorld$worldNameAndPlayerName")
                    }
                }
                "createWorld" -> {
                    val serverName = mList[3]
                    //找到发出请求的服务器
                    if (ChunkWorld.inst.config.getString("serverName")!! == serverName){
                        val uuid = mList[1]
                        val playerName = mList[2]
                        val targetServerName = mList[4]
                        //创建世界
                        Tp.createWorldLocal(UUID.fromString(uuid),playerName).thenAccept {
                            if (it.first){
                                val dao = it.second!!
                                val world = Bukkit.getWorld(dao.tName)
                                val location = Location(world,dao.tX,dao.tY,dao.tZ,dao.tYaw,dao.tPitch)
                                world!!.getChunkAtAsync(location)
                                GroupListener.addLocation(playerName,location)
                            }
                            RedisPush.createWorldResult(targetServerName,uuid,it.first)
                        }
                    }
                }
                "createWorldResult" -> {
                    val serverName = mList[2]
                    val uuid = mList[1]
                    val isSuccess = mList[3].toBoolean()
                    //找到发出请求的服务器
                    if (ChunkWorld.inst.config.getString("serverName")!! == serverName){
                        //返回目标服务器ip和端口
                        RedisPush.getFuture("createWorld$uuid")?.complete(if (isSuccess) "true" else null)
                        RedisPush.removeFuture("createWorld$uuid")
                    }
                }
                "cancelFriend" -> {
                    val targetUUID = mList[1]
                    val playerName = mList[2]
                    val target = Bukkit.getPlayer(UUID.fromString(targetUUID))?: return
                    target.sendMessage("§c $playerName 已取消和你共享世界")
                }
            }
        }
    }
}