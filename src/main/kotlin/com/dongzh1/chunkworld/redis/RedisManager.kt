package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.database.dao.WorldInfo
import redis.clients.jedis.params.SetParams

object RedisManager {

    private val jedisPool = ChunkWorld.jedisPool

    fun getWorldInfo(name: String): WorldInfo? {
        jedisPool.resource.use {
            val daoString = it.get("ChunkWorld_WorldInfo_$name") ?: return null
            val list = daoString.split(",")
            return WorldInfo(
                state = list[0].toByte(),
                normalChunks = list[1].toInt(),
                netherChunks = list[2].toInt(),
                serverName = list[3]
            )
        }
    }

    fun setServerInfo(info: String) {
        jedisPool.resource.use {
            it.set("ChunkWorld_Server", info, SetParams.setParams().ex(10))
        }
    }

    fun getServerInfo(): List<Pair<String, Int>> {
        jedisPool.resource.use {
            val daoString = it.get("ChunkWorld_Server") ?: return emptyList()
            val list = daoString.split(",")
            return list.map { it.split("|") }.map { it[0] to it[1].toInt() }
        }
    }
}