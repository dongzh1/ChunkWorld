package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.CHANNEL
import org.bukkit.Bukkit
import java.util.*

object RedisManager {

    private val jedisPool = ChunkWorld.jedisPool
    fun push(message: String) {
        jedisPool.resource.use { jedis -> jedis.publish(CHANNEL, message) }
    }

    fun getPlayerDao(uuidString: String): String? {
        jedisPool.resource.use {
            return it.hget("ChunkWorld_PlayerDao", uuidString)
        }
    }

    /**
     * 如果超过1000个玩家，就会导致redis内存占用过大，所以只存最近1000个玩家
     */
    fun setPlayerDao(uuidString: String, playerDaoString: String) {
        jedisPool.resource.use {
            it.hset("ChunkWorld_PlayerDao", uuidString, playerDaoString)
            //如果超过1000个玩家，就删除最早的那个
            if (it.hlen("ChunkWorld_PlayerDao") > 1000) {
                val oldest = it.hkeys("ChunkWorld_PlayerDao").first()
                it.hdel("ChunkWorld_PlayerDao", oldest)
            }
        }
    }

    fun getChunks(uuidString: String, worldType: Byte): String? {
        jedisPool.resource.use {
            return it.hget("ChunkWorld_Chunks_$worldType", uuidString)
        }
    }

    fun setChunks(uuidString: String, chunks: String, worldType: Byte) {
        jedisPool.resource.use {
            it.hset("ChunkWorld_Chunks_$worldType", uuidString, chunks)
        }
    }

    fun getFriends(uuidString: String): String? {
        jedisPool.resource.use {
            return it.hget("ChunkWorld_Friends", uuidString)
        }
    }

    fun setFriends(uuidString: String, friends: String) {
        jedisPool.resource.use {
            it.hset("ChunkWorld_Friends", uuidString, friends)
        }
    }

    fun getBanners(uuidString: String): String? {
        jedisPool.resource.use {
            return it.hget("ChunkWorld_Banners", uuidString)
        }
    }

    fun setBanners(uuidString: String, banners: String) {
        jedisPool.resource.use {
            it.hset("ChunkWorld_Banners", uuidString, banners)
        }
    }

    /**
     * 将本服的tps上传到redis,每分钟定时上传
     */
    fun setIP() {
        jedisPool.resource.use {
            it.hset(
                "ChunkWorld_IP",
                Bukkit.getIp() + ":${Bukkit.getPort()}",
                Bukkit.getTPS().first().toString() + "|" + System.currentTimeMillis()
            )
        }
    }

    /**
     * 获取tps最高的服务器ip，方便转发过去
     */
    fun getHighestTpsIP(): String? {
        jedisPool.resource.use {
            val ipTPS = it.hgetAll("ChunkWorld_IP")
            var ip: String? = null
            var maxTps = 0.0
            ipTPS.forEach { map ->
                if (map.value.split("|")[0].toDouble() > maxTps) {
                    maxTps = map.value.toDouble()
                    ip = map.key
                }
            }
            //现在就是最大的
            return ip
        }
    }


    fun setSeed(uuid: UUID, seed: String) {
        push("setSeed|,|${uuid}|,|${seed}")
    }
}