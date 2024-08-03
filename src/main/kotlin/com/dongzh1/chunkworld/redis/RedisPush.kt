package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.CHANNEL
import com.dongzh1.chunkworld.ChunkWorld.Companion.jedisPool
import com.xbaimiao.easylib.util.submit
import java.util.*
import java.util.concurrent.CompletableFuture

object RedisPush {
    private val futureMap = mutableMapOf<String, CompletableFuture<String?>>()
    private fun push(message: String) {
        jedisPool.resource.use { jedis -> jedis.publish(CHANNEL, message) }
    }

    private fun addFuture(key: String, future: CompletableFuture<String?>) {
        futureMap[key] = future
    }

    fun removeFuture(key: String) {
        futureMap.remove(key)
    }

    fun getFuture(key: String): CompletableFuture<String?>? {
        return futureMap[key]
    }

    fun getAllFuture(): Map<String, CompletableFuture<String?>> {
        return futureMap
    }

    /**
     * 向所有服务器查找对应世界
     */
    fun teleportWorld(playerName: String, worldName: String, serverName: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        addFuture("teleportWorld$worldName$playerName", future)
        submit(delay = 60) {
            if (!future.isDone) {
                removeFuture("teleportWorld$worldName$playerName")
                future.complete(null)
            }
        }
        //世界名,本服的ip和端口
        push("teleportWorld|,|${worldName}|,|$serverName|,|${ChunkWorld.serverName}|,|$playerName")
        return future
    }

    /**
     * 向指定服务器发送世界已被本服务器找到信息并邀请传送过来
     */
    fun teleportWorldFound(targetServerName: String, worldNameAndPlayerName: String, result: String) {
        //正在寻找世界的服务器ip和端口，世界名，本服的ip和端口
        push("teleportWorldFound|,|$targetServerName|,|${worldNameAndPlayerName}|,|${result}")
    }

    fun worldSetPersistent(worldName: String, serverName: String, key: String, value: String) {
        push("worldSetPersistent|,|$worldName|,|$serverName|,|$key|,|$value")
    }

    /**
     * 告诉指定服务器，玩家世界创建成功或者失败
     */
    fun createWorldResult(serverName: String, uuid: String, result: Boolean) {
        //玩家uuid,本服的ip和端口
        push("createWorldResult|,|$uuid|,|$serverName|,|${result}")
    }

    fun cancelFriend(targetName: String, playerName: String, playerUUID: UUID) {
        push("cancelFriend|,|$targetName|,|$playerName|,|${playerUUID}")
    }
}