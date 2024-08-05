package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.database.dao.ServerInfo
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


    fun getServerInfo(): List<ServerInfo> {
        jedisPool.resource.use {
            val infoString = it.get("ChunkWorld_Server")?:return emptyList()
            val infoList = infoString.split(",,,,,").drop(1)
            val list = mutableListOf<ServerInfo>()
            infoList.forEach { info ->
                val stringList = info.split("|")
                val serverInfo = ServerInfo(
                    serverName = stringList[0],
                    serverTps = stringList[1].toDouble(),
                    serverplayers = stringList[2].toInt()
                )
                list.add(serverInfo)
            }
            return list
        }
    }
}