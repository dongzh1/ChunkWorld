package com.dongzh1.chunkworld.redis

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

    /**
     * 在指定服务器创建玩家世界
     */
    fun createWorld(serverName: String, uuid: UUID, name: String, id: Int, p: Boolean): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        addFuture("createWorld$uuid", future)
        submit(delay = 300) {
            if (!future.isDone) {
                removeFuture("createWorld$uuid")
                future.complete(null)
            }
        }
        //玩家uuid,玩家名字,本服的ip和端口
        push("createWorld|,|$uuid|,|$name|,|$serverName|,|$id|,|$p")
        return future
    }
}