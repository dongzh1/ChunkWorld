package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.CHANNEL
import com.dongzh1.chunkworld.ChunkWorld.Companion.jedisPool
import com.sun.net.httpserver.Authenticator.Success
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
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
    fun teleportWorld(worldName:String,x:Double,y:Double,z:Double): CompletableFuture<String?> {
        val existFuture = getFuture("teleportWorld$worldName")
        if (existFuture !=null) return existFuture
        val future = CompletableFuture<String?>()
        submit(delay = 60) {
            if (!future.isDone){
                removeFuture("teleportWorld$worldName")
                future.complete(null)
            }
        }
        addFuture("teleportWorld$worldName", future)
        //世界名,本服的ip和端口
        push("teleportWorld|,|${worldName}|,|$x,$y,$z|,|${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}")
        return future
    }
    /**
     * 向指定服务器发送世界已被本服务器找到信息并邀请传送过来
     */
    fun teleportWorldFound(ipAndPort:String,worldName: String) {
        //正在寻找世界的服务器ip和端口，世界名，本服的ip和端口
       push("teleportWorldFound|,|$ipAndPort|,|${worldName}|,|${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}")
    }
    /**
     * 告诉指定的服务器加载指定世界
     */
    fun loadWorld(ipAndPort: String, worldName:String,x:Double,y:Double,z:Double): CompletableFuture<String?> {
        val existFuture = getFuture("loadWorld$worldName")
        if (existFuture != null) return existFuture
        val future = CompletableFuture<String?>()
        submit(delay = 80) {
            if (!future.isDone) {
                removeFuture("loadWorld$worldName")
                future.complete(null)
            }
        }
        addFuture("loadWorld$worldName", future)
        //世界名,本服的ip和端口
        push("loadWorld|,|${worldName}|,|$ipAndPort|,|$x,$y,$z|,|${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}")
        return future
    }
    /**
     * 告诉指定服务器，世界加载好了或者加载失败了
     */
    fun loadWorldResult(ipAndPort: String, worldName: String,result:String?) {
        //世界名，本服的ip和端口
        push("loadWorldResult|,|${worldName}|,|$ipAndPort|,|$result")
    }
    /**
     * 在指定服务器创建玩家世界
     */
    fun createWorld(ipAndPort: String, uuid: UUID,playerName:String): CompletableFuture<String?> {
        val existFuture = getFuture("createWorld$uuid")
        if (existFuture != null) return existFuture
        val future = CompletableFuture<String?>()
        submit(delay = 100) {
            if (!future.isDone) {
                removeFuture("createWorld$uuid")
                future.complete(null)
            }
        }
        addFuture("createWorld$uuid", future)
        //玩家uuid,玩家名字,本服的ip和端口
        push("createWorld|,|$uuid|,|$playerName|,|$ipAndPort|,|${ChunkWorld.inst.config.getString("transferIP")!!}:${Bukkit.getPort()}")
        return future
    }
    /**
     * 告诉指定服务器，玩家世界创建成功或者失败
     */
    fun createWorldResult(ipAndPort: String, uuid: String,result:Pair<Boolean,String>) {
        //玩家uuid,本服的ip和端口
        push("createWorldResult|,|$uuid|,|$ipAndPort|,|${result.first}|,|${result.second}")
    }
    /**
     * 向指定服务器发送玩家跨服信息，用于跨服接收
     */
    fun transferInfo(ipAndPort: String,name:String,info:String){
        push("transferInfo|,|$ipAndPort|,|$name|,|$info")
    }
    /**
     * 像大厅发送跨服信息
     */
    fun transferLobby(ipAndPort: String,name:String,info:String){
        push("transferLobby|,|$ipAndPort|,|$name|,|$info")
    }
}