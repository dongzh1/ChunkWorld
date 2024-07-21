package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import redis.clients.jedis.JedisPubSub

class RedisListener : JedisPubSub() {
    override fun onMessage(channel: String?, message: String?) {
        if (channel == ChunkWorld.CHANNEL) {
            when (message!!.split("|,|")[0]) {
                "ServerOnline" -> {
                    val serverName = message.split("|,|")[1]
                }
            }
        }
    }
}