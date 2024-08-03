package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import redis.clients.jedis.JedisPubSub

class RedisListener : JedisPubSub() {
    override fun onMessage(channel: String?, message: String?) {
        if (channel == ChunkWorld.CHANNEL) {
            val mList = message!!.split("|,|")
            when (mList[0]) {
                "createWorldResult" -> {
                    val serverName = mList[2]
                    val uuid = mList[1]
                    val isSuccess = mList[3].toBoolean()
                    //返回目标服务器ip和端口
                    RedisPush.getFuture("createWorld$uuid")?.complete(if (isSuccess) "true" else null)
                    RedisPush.removeFuture("createWorld$uuid")
                }
            }
        }
    }
}