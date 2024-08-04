package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.command.Tp
import com.dongzh1.chunkworld.listener.GroupListener
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.persistence.PersistentDataType
import redis.clients.jedis.JedisPubSub
import java.util.*

class RedisListener : JedisPubSub() {
    //处于异步任务下
    override fun onMessage(channel: String?, message: String?) {
        if (channel == ChunkWorld.CHANNEL) {
            val mList = message!!.split("|,|")
            when (mList[0]) {
                "teleportWorld" -> {
                    val serverName = mList[2]
                    if (ChunkWorld.serverName == serverName) {
                        val worldName = mList[1]
                        val playerName = mList[4]
                        val targetServerName = mList[3]
                        //看看这个世界在本服是否已加载
                        val world = Bukkit.getWorld(worldName)
                        if (world != null) {
                            //预加载区块，等待传送
                            world.getChunkAtAsync(world.spawnLocation)
                            GroupListener.addLocation(playerName, world.spawnLocation)
                            RedisPush.teleportWorldFound(targetServerName, "$worldName$playerName", "true")
                        } else {
                            //这个世界已经卸载了
                            RedisPush.teleportWorldFound(targetServerName, "$worldName$playerName", "false")
                        }
                    }
                }

                "teleportWorldFound" -> {
                    val serverName = mList[1]
                    //找到发出请求的服务器
                    if (ChunkWorld.serverName == serverName) {
                        val worldNameAndPlayerName = mList[2]
                        val result = mList[3]
                        if (result == "true")
                            RedisPush.getFuture("teleportWorld$worldNameAndPlayerName")?.complete("true")
                        else
                            RedisPush.getFuture("teleportWorld$worldNameAndPlayerName")?.complete("false")
                        RedisPush.removeFuture("teleportWorld$worldNameAndPlayerName")
                    }
                }

                "worldSetPersistent" -> {
                    val serverName = mList[2]
                    if (ChunkWorld.serverName == serverName) {
                        val worldName = mList[1]
                        val key = mList[3]
                        val value = mList[4]
                        val world = Bukkit.getWorld(worldName) ?: return
                        world.persistentDataContainer.set(
                            NamespacedKey.fromString(key)!!,
                            PersistentDataType.STRING,
                            value
                        )
                    }
                }

                "createWorld" -> {
                    val serverName = mList[3]
                    //找到发出请求的服务器
                    if (ChunkWorld.serverName == serverName) {
                        val uuid = UUID.fromString(mList[1])
                        val name = mList[2]
                        val id = mList[4].toInt()
                        val hasPerm = mList[5].toBoolean()
                        //创建世界
                        Tp.createWorldLocal(uuid, name, id, hasPerm).thenAccept {
                            RedisPush.createWorldResult(serverName, uuid.toString(), it)
                        }
                    }
                }

                "createWorldResult" -> {
                    val serverName = mList[2]
                    val uuid = mList[1]
                    val isSuccess = mList[3].toBoolean()
                    //找到发出请求的服务器
                    if (ChunkWorld.serverName == serverName) {
                        //返回目标服务器ip和端口
                        RedisPush.getFuture("createWorld$uuid")?.complete(if (isSuccess) "true" else null)
                        RedisPush.removeFuture("createWorld$uuid")
                    }
                }

                "cancelFriend" -> {
                    val targetName = mList[1]
                    val playerName = mList[2]
                    val playerUUID = mList[3]
                    val target = Bukkit.getPlayerExact(targetName) ?: return
                    target.sendMessage("§c $playerName 已取消和你共享世界")
                    if (target.world.name == "chunkworlds/world/$playerUUID" || target.world.name == "chunkworlds/nether/$playerUUID") {
                        target.gameMode = GameMode.ADVENTURE
                    }
                }

                "unloadWorld" -> {
                    val serverName = mList[2]
                    val name = mList[3]
                    if (ChunkWorld.serverName == serverName) {
                        submit {
                            val uuidString = mList[1]
                            val worldNromal = Bukkit.getWorld("chunkworlds/world/$uuidString")
                            var unloadNormal = false
                            var unloadNether = false
                            val worldNether = Bukkit.getWorld("chunkworlds/nether/$uuidString")
                            if (worldNether != null) {
                                if (unloadWorld(worldNether)) unloadNether = true
                            } else unloadNether = true
                            if (worldNromal != null) {
                                if (unloadWorld(worldNromal)) unloadNormal = true
                            } else unloadNormal = true
                            if (unloadNormal && unloadNether) {
                                //删除redis数据
                                Tp.removeWorldInfo("chunkworlds/world/$uuidString")
                                RedisManager.removeWorldInfo(name)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun unloadWorld(world: World): Boolean {
        //禁止其他玩家进入这个世界
        GroupListener.addUnloadWorld(world)
        if (world.players.isNotEmpty()) {
            world.players.forEach {
                 it.teleport(ChunkWorld.spawnLocation)
                if (it.uniqueId.toString() != world.name.split("/").last())
                it.sendMessage("§c世界主人已离线，世界关闭")
            }
        }
        //卸载世界
        val success = Bukkit.unloadWorld(world, true)
        GroupListener.removeUnloadWorld(world)
        return success
    }
}