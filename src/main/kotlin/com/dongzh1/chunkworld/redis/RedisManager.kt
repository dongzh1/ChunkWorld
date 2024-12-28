package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.database.dao.ServerInfo
import com.dongzh1.chunkworld.database.dao.WorldInfo
import redis.clients.jedis.params.SetParams

object RedisManager {

    private val jedisPool = ChunkWorld.jedisPool

    fun setWorldInfo(name: String, i: WorldInfo,forever:Boolean= false) {
        jedisPool.resource.use {
            //信息保存1小时，期间世界会自动刷新信息
            if (!forever)
            it.set(
                "ChunkWorld_WorldInfo_$name",
                "${i.state},${i.normalChunks},${i.netherChunks},${i.serverName},${i.showWorld}",
                SetParams.setParams().ex(60 * 10)
            )
            else
                it.set(
                    "ChunkWorld_WorldInfo_$name",
                    "${i.state},${i.normalChunks},${i.netherChunks},${i.serverName},${i.showWorld}"
                )
        }
    }

    fun getWorldInfo(name: String): WorldInfo? {
        jedisPool.resource.use {
            val daoString = it.get("ChunkWorld_WorldInfo_$name") ?: return null
            val list = daoString.split(",")
            return WorldInfo(
                state = list[0].toByte(),
                normalChunks = list[1].toInt(),
                netherChunks = list[2].toInt(),
                serverName = list[3],
                showWorld = list[4].toBoolean()
            )
        }
    }

    fun getShowWorldInfo(): List<Pair<String, WorldInfo>> {
        jedisPool.resource.use {
            val data = it.keys("ChunkWorld_WorldInfo_*")
            val worldInfoList = mutableListOf<Pair<String, WorldInfo>>()
            data.forEach { name ->
                val daoString = it.get(name) ?: return@forEach
                val list = daoString.split(",")
                if (list[4].toBoolean()) {
                    worldInfoList.add(
                        name.removePrefix("ChunkWorld_WorldInfo_") to
                                WorldInfo(
                                    state = list[0].toByte(),
                                    normalChunks = list[1].toInt(),
                                    netherChunks = list[2].toInt(),
                                    serverName = list[3],
                                    showWorld = list[4].toBoolean()
                                )
                    )
                }
            }
            return worldInfoList
        }
    }

    fun removeWorldInfo(name: String) {
        jedisPool.resource.use {
            it.del("ChunkWorld_WorldInfo_$name")
        }
    }

    fun getServerInfo(): List<ServerInfo> {
        jedisPool.resource.use {
            val infoString = it.get("ChunkWorld_Server")?:return emptyList()
            val infoList = infoString.split(",,,,,").dropLast(1)
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