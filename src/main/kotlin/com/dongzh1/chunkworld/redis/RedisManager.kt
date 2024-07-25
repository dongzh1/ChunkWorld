package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.CHANNEL
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import java.util.*

object RedisManager {

    private val jedisPool = ChunkWorld.jedisPool

    fun getPlayerDao(uuidString: String): String? {
        jedisPool.resource.use {
            val daoString = it.hget("ChunkWorld_PlayerDao", uuidString)?:return null
            return "$daoString|,|$uuidString"
        }
    }
    fun getPlayerDaoByName(name: String): String? {
        jedisPool.resource.use {
            val uuidString = it.hget("ChunkWorld_NameUuid", name)?:return null
            val daoString = it.hget("ChunkWorld_PlayerDao", uuidString)?:return null
            return "$daoString|,|$uuidString"
        }
    }
    fun getAllNameUuid(): Map<String,String> {
        jedisPool.resource.use {
            return it.hgetAll("ChunkWorld_NameUuid")
        }
    }

    /**
     * 如果超过1000个玩家，就会导致redis内存占用过大，所以只存最近1000个玩家
     */
    fun setPlayerDao(uuidString: String, playerDaoString: String) {
        jedisPool.resource.use {
            it.hset("ChunkWorld_PlayerDao", uuidString, playerDaoString)
        }
    }
    fun setUuidByName(name: String, uuidString: String) {
        jedisPool.resource.use {
            it.hset("ChunkWorld_NameUuid", name, uuidString)
        }
    }
    fun delPlayerDaoAndUuidByName(uuidString: String) {
        jedisPool.resource.use {
            val name = it.hget("ChunkWorld_PlayerDao", uuidString)?.split("|,|")?.get(1)?:return
            it.hdel("ChunkWorld_PlayerDao", uuidString)
            it.hdel("ChunkWorld_NameUuid", name)
        }
    }

    fun getChunks(uuidString: String, worldType: Byte): String? {
        jedisPool.resource.use {
            val data = it.hget("ChunkWorld_Chunks", uuidString)?:return null
            if (worldType == 0.toByte()){
                return data.split("!!!!!")[0]
            }else{
                return data.split("!!!!!")[1]
            }
        }
    }
    fun getAllChunks(uuidString: String):Pair<String,String>?{
        jedisPool.resource.use {
            val data = it.hget("ChunkWorld_Chunks", uuidString)?:return null
            return data.split("!!!!!")[0] to data.split("!!!!!")[1]
        }
    }

    fun setChunks(uuidString: String, normalChunks: String, netherChunks:String) {
        jedisPool.resource.use {
            it.hset("ChunkWorld_Chunks", uuidString, "$normalChunks!!!!!$netherChunks")
        }
    }
    fun delChunks(uuidString: String) {
        jedisPool.resource.use {
            it.hdel("ChunkWorld_Chunks", uuidString)
        }
    }

    fun getFriends(uuidString: String): String? {
        jedisPool.resource.use {
            val data = it.hget("ChunkWorld_Ship", uuidString)?:return null
            return data.split(",,,,,")[0]
        }
    }

    fun getBanners(uuidString: String): String? {
        jedisPool.resource.use {
            val data = it.hget("ChunkWorld_Ship", uuidString)?:return null
            return data.split(",,,,,")[1]
        }
    }
    fun getFriendsAndBanners(uuidString: String): Pair<String,String>? {
        jedisPool.resource.use {
            val data = it.hget("ChunkWorld_Ship", uuidString)?:return null
            val list = data.split(",,,,,")
            return list[0] to list[1]
        }
    }

    fun setShip(uuidString: String,friends: String,banners: String){
        jedisPool.resource.use {
            it.hset("ChunkWorld_Ship", uuidString, "$friends,,,,,$banners")
        }
    }
    fun shipHas(uuidString: String):Boolean{
        jedisPool.resource.use {
            return it.hexists("ChunkWorld_Ship",uuidString)
        }
    }

    /**
     * 将本服的tps上传到redis,每分钟定时上传
     */
    fun setIP() {
        jedisPool.resource.use {
            it.hset(
                "ChunkWorld_IP",
                ChunkWorld.inst.config.getString("transferIP")!! + ":${Bukkit.getPort()}",
                Bukkit.getTPS().first().toString() + "|" + System.currentTimeMillis()
            )
        }
    }
    fun delIP(){
        jedisPool.resource.use {
            it.hdel("ChunkWorld_IP", ChunkWorld.inst.config.getString("transferIP")!! + ":${Bukkit.getPort()}")
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
                //如果现在时间比存的时间大2分钟，就可以说明这个服死了，把他数据清除
                val list = map.value.split("|")
                val time = list[1].toLong()
                val tps = list[0].toDouble()
                if (System.currentTimeMillis() - time > 120000) {
                    it.hdel("ChunkWorld_IP", map.key)
                    return@forEach
                }
                if (tps > maxTps) {
                    maxTps = tps
                    ip = map.key
                }
            }
            //现在就是最大的
            return ip
        }
    }
    /**
     * 大厅注册ip
     */
    fun setLobbyIP(){
        jedisPool.resource.use {
            it.set("ChunkWorld_LobbyIP", ChunkWorld.inst.config.getString("transferIP")!! + ":${Bukkit.getPort()}")
        }
    }
    fun getLobbyIP(): Pair<String,Int>? {
        jedisPool.resource.use {
            val ip = it.get("ChunkWorld_LobbyIP")?:return null
            val list = ip.split(":")
            return list[0] to list[1].toInt()
        }
    }
}