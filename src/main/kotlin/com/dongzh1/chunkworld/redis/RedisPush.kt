package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.ChunkWorld
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
    fun getAllFuture(): Map<String, CompletableFuture<String?>> {
        return futureMap
    }

    /**
     * 向所有服务器查找对应世界
     */
    fun teleportWorld(playerName: String,worldName:String,x:Double,y:Double,z:Double): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        addFuture("teleportWorld$worldName$playerName",future)
        submit(delay = 100) {
            if (!future.isDone){
                removeFuture("teleportWorld$worldName$playerName")
                future.complete(null)
            }
        }
        //世界名,本服的ip和端口
        push("teleportWorld|,|${worldName}|,|$x,$y,$z|,|${ChunkWorld.inst.config.getString("serverName")!!}|,|$playerName")
        return future
    }
    /**
     * 向指定服务器发送世界已被本服务器找到信息并邀请传送过来
     */
    fun teleportWorldFound(serverName:String, worldNameAndPlayerName: String) {
        //正在寻找世界的服务器ip和端口，世界名，本服的ip和端口
       push("teleportWorldFound|,|$serverName|,|${worldNameAndPlayerName}|,|${ChunkWorld.inst.config.getString("serverName")!!}")
    }
    /**
     * 告诉指定的服务器加载指定世界
     */
    fun loadWorldTeleport(serverName: String, worldName:String, x:Double, y:Double, z:Double, playerName: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        addFuture("loadWorldTeleport$worldName$playerName", future)
        submit(delay = 300) {
            if (!future.isDone) {
                removeFuture("loadWorldTeleport$worldName$playerName")
                future.complete(null)
            }
        }
        //世界名,本服的ip和端口
        push("loadWorldTeleport|,|${worldName}|,|$serverName|,|$x,$y,$z|,|${ChunkWorld.inst.config.getString("serverName")!!}|,|$playerName")
        return future
    }
    /**
     * 告诉指定服务器，世界加载好了或者加载失败了
     */
    fun loadWorldResult(serverName: String, worldNameAndPlayerName: String, result:String?) {
        //世界名，本服的ip和端口
        push("loadWorldResult|,|${worldNameAndPlayerName}|,|$serverName|,|$result")
    }
    /**
     * 在指定服务器创建玩家世界
     */
    fun createWorld(serverName: String, uuid: UUID, playerName:String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        addFuture("createWorld$uuid", future)
        submit(delay = 120) {
            if (!future.isDone) {
                removeFuture("createWorld$uuid")
                future.complete(null)
            }
        }
        //玩家uuid,玩家名字,本服的ip和端口
        push("createWorld|,|$uuid|,|$playerName|,|$serverName|,|${ChunkWorld.inst.config.getString("serverName")!!}")
        return future
    }
    /**
     * 告诉指定服务器，玩家世界创建成功或者失败
     */
    fun createWorldResult(serverName: String, uuid: String, result:Boolean) {
        //玩家uuid,本服的ip和端口
        push("createWorldResult|,|$uuid|,|$serverName|,|${result}")
    }
    fun cancelFriend(targetUUID:String,playerName:String){
        push("cancelFriend|,|$targetUUID|,|$playerName")
    }
}