package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.channel
import org.bukkit.Bukkit
import org.bukkit.Server
import java.util.*

object RedisManager{

    private val jedisPool = ChunkWorld.jedisPool
    fun push(message: String) {
        jedisPool.resource.use { jedis -> jedis.publish(channel, message) }
    }

    fun getPlayerDao(uuidString: String):String?{
        jedisPool.resource.use {
            return it.hget("ChunkWorld_PlayerDao",uuidString)
        }
    }
    fun setPlayerDao(uuidString: String,playerDaoString: String){
        jedisPool.resource.use {
            it.hset("ChunkWorld_PlayerDao",uuidString,playerDaoString)
        }
    }
    fun getChunks(uuidString: String,worldType:Byte):String?{
        jedisPool.resource.use {
            return it.hget("ChunkWorld_Chunks_$worldType", uuidString)
        }
    }
    fun setChunks(uuidString: String,chunks:String,worldType: Byte){
        jedisPool.resource.use {
            it.hset("ChunkWorld_Chunks_$worldType",uuidString,chunks)
        }
    }
    fun getFriends(uuidString: String):String?{
        jedisPool.resource.use {
            return it.hget("ChunkWorld_Friends",uuidString)
        }
    }
    fun setFriends(uuidString: String,friends:String){
        jedisPool.resource.use {
            it.hset("ChunkWorld_Friends",uuidString,friends)
        }
    }
    fun getBanners(uuidString: String):String?{
        jedisPool.resource.use {
            return it.hget("ChunkWorld_Banners",uuidString)
        }
    }
    fun setBanners(uuidString: String,banners:String){
        jedisPool.resource.use {
            it.hset("ChunkWorld_Banners",uuidString,banners)
        }
    }
    fun setIP(){
        jedisPool.resource.use {
            it.hset("ChunkWorld_IP",Bukkit.getIp(),Bukkit.getTPS().first().toString())
        }
    }


    fun setSeed(uuid: UUID, seed:String){
        push("setSeed|,|${uuid}|,|${seed}")
    }
}