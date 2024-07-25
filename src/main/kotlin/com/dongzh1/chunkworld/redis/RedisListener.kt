package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.command.Tp
import com.dongzh1.chunkworld.listener.GroupListener
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.Location
import redis.clients.jedis.JedisPubSub
import java.util.*

class RedisListener : JedisPubSub() {
    override fun onMessage(channel: String?, message: String?) {
        if (channel == ChunkWorld.CHANNEL) {
            val mList = message!!.split("|,|")
            when (mList[0]) {
                "teleportWorld" -> {
                    val worldName = mList[1]
                    val xyz = mList[2].split(",")
                    //看看这个世界在本服是否已加载
                    val world = Bukkit.getWorld(worldName)
                    if (world != null) {
                        //预加载区块，等待传送
                        world.getChunkAtAsync(Location(world, xyz[0].toDouble(), xyz[1].toDouble(), xyz[2].toDouble()))
                        RedisPush.teleportWorldFound(mList[3], worldName)
                    }
                }
                "teleportWorldFound" -> {
                    val ipAndPort = mList[1]
                    val worldName = mList[2]
                    val targetIpAndPort = mList[3]
                    //找到发出请求的服务器
                    if ("${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}" == ipAndPort){
                        //返回目标服务器ip和端口
                        RedisPush.getFuture("teleportWorld$worldName")?.complete(targetIpAndPort)
                        RedisPush.removeFuture("teleportWorld$worldName")
                    }
                }
                "loadWorld" -> {
                    val worldName = mList[1]
                    val ipAndPort = mList[2]
                    val xyz = mList[3].split(",")
                    val targetIpAndPort = mList[4]
                    //找到发出请求的服务器
                    if ("${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}" == ipAndPort){
                        //加载世界
                        submit {
                            val world = Bukkit.createWorld(org.bukkit.WorldCreator(worldName))
                            if (world != null) {
                                //加载成功
                                world.getChunkAtAsync(Location(world, xyz[0].toDouble(), xyz[1].toDouble(), xyz[2].toDouble()))
                                RedisPush.loadWorldResult(targetIpAndPort,worldName,"true")
                            }else{
                                RedisPush.loadWorldResult(targetIpAndPort,worldName,null)
                            }
                        }
                    }
                }
                "loadWorldResult" -> {
                    val ipAndPort = mList[2]
                    val worldName = mList[1]
                    val result = mList[3]
                    //找到发出请求的服务器
                    if ("${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}" == ipAndPort){
                        //返回目标服务器ip和端口
                        RedisPush.getFuture("loadWorld$worldName")?.complete(if (result == "true") "true" else null)
                        RedisPush.removeFuture("loadWorld$worldName")
                    }
                }
                "createWorld" -> {
                    val uuid = mList[1]
                    val playerName = mList[2]
                    val ipAndPort = mList[3]
                    val targetIpAndPort = mList[4]
                    //找到发出请求的服务器
                    if ("${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}" == ipAndPort){
                        //创建世界
                        submit {
                            Tp.createWorldLocal(UUID.fromString(uuid),playerName).thenAccept {
                                RedisPush.createWorldResult(targetIpAndPort,uuid,it)
                            }
                        }
                    }
                }
                "createWorldResult" -> {
                    val ipAndPort = mList[2]
                    val uuid = mList[1]
                    val isSuccess = mList[3].toBoolean()
                    val info = mList[4]
                    //找到发出请求的服务器
                    if ("${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}" == ipAndPort){
                        //返回目标服务器ip和端口
                        RedisPush.getFuture("createWorld$uuid")?.complete(if (isSuccess) info else null)
                        RedisPush.removeFuture("createWorld$uuid")
                    }
                }
                "transferInfo" -> {
                    val ipAndPort = mList[1]
                    if ("${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}" == ipAndPort){
                        val name = mList[2]
                        val info = mList[3]
                        GroupListener.addLocation(name,info)
                    }
                }
                "transferLobby" -> {
                    val ipAndPort = mList[1]
                    if ("${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}" == ipAndPort){
                        val name = mList[2]
                        val info = mList[3]
                        GroupListener.addLobby(name,info)
                    }
                }
            }
        }
    }
}