package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.dongzh1.chunkworld.database.dao.PlayerDao
import org.bukkit.entity.Player
import java.util.*

/**
 * 存入内存的各类信息，用于各种判断
 */
object RedisData {
    private fun stringToPlayerDao(stringPlayerDao:String):PlayerDao{
        return PlayerDao().apply {
            id = stringPlayerDao.split("|,|")[0].toInt()
            name = stringPlayerDao.split("|,|")[1]
            uuid = UUID.fromString(stringPlayerDao.split("|,|")[8])
            createTime = stringPlayerDao.split("|,|")[2]
            lastTime = stringPlayerDao.split("|,|")[3].toLong()
            spawn = stringPlayerDao.split("|,|")[4]
            netherSpawn = stringPlayerDao.split("|,|")[5]
            worldStatus = stringPlayerDao.split("|,|")[6].toByte()
            teleport = stringPlayerDao.split("|,|")[7]
        }
    }
    /**
     * 根据uuid获取玩家在内存的数据，群组使用
     */
    fun getPlayerDao(uuidString: String): PlayerDao? {
        val stringPlayerDao = RedisManager.getPlayerDao(uuidString) ?: return null
        return stringToPlayerDao(stringPlayerDao)
    }
    /**
     * 根据名字获取playerDAO
     */
    fun getPlayerDaoByName(name: String): PlayerDao? {
        val stringPlayerDao = RedisManager.getPlayerDaoByName(name)?: return null
        return stringToPlayerDao(stringPlayerDao)
    }
    /**
     * 把playerDao存入内存，群组使用,
     * 从数据库存入redis应该发生在玩家上线时，
     */
    fun setPlayerDao(dao: PlayerDao) {
        val stringPlayerDao =
            "${dao.id}|,|${dao.name}|,|${dao.createTime}|,|${dao.lastTime}|,|${dao.spawn}|,|${dao.netherSpawn}|,|${dao.worldStatus}|,|${dao.teleport}"
        RedisManager.setUuidByName(dao.name, dao.uuid.toString())
        RedisManager.setPlayerDao(dao.uuid.toString(), stringPlayerDao)
    }
    /**
     * 删除玩家的内存数据，群组使用
     */
    fun delPlayerDao(uuidString: String) {
        RedisManager.delPlayerDaoAndUuidByName(uuidString)
    }

    /**
     * 从redis获取这个玩家的区块信息,0，1分别代表主世界，地狱
     */
    fun getChunks(uuidString: String, worldType: Byte): Set<Pair<Int, Int>>? {
        val stringChunks = RedisManager.getChunks(uuidString, worldType) ?: return null
        //先根据"|"分割成一对一对去掉最后一个空值，然后把每一对转化为int to int 最后把list变成set
        return stringChunks.split("|").toMutableList().dropLast(1)
            .map { it.split(",")[0].toInt() to it.split(",")[1].toInt() }.toSet()
    }
    fun getAllChunksNum(uuidString: String):Pair<Int,Int>?{
        val stringChunks = RedisManager.getAllChunks(uuidString) ?: return null
        val normalNum = stringChunks.first.split("|").toMutableList().dropLast(1).size
        val netherNum = stringChunks.second.split("|").toMutableList().dropLast(1).size
        return normalNum to netherNum
    }

    /**
     * 把这个玩家世界的区块信息存入redis,0，1分别代表主世界，地狱
     * 一次只能存入一个玩家的
     */
    fun setChunks(uuidString: String,chunks:List<ChunkDao>) {
        //根据worldType分开
        val map = chunks.groupBy { it.worldType }
        var normalChunks = ""
        var netherChunks = ""
        map.forEach { (worldType, chunkList) ->
            if (worldType == 0.toByte()){
                chunkList.forEach { normalChunks += "${it.x},${it.z}|" }
            }else{
                chunkList.forEach { netherChunks += "${it.x},${it.z}|" }
            }
        }
        RedisManager.setChunks(uuidString, normalChunks, netherChunks)
    }
    fun addChunk(uuidString: String,chunk:Pair<Int,Int>){

    }
    /**
     * 删除这个玩家的区块信息
     */
    fun delChunks(uuidString: String) {
        RedisManager.delChunks(uuidString)
    }

    /**
     * 获取此玩家的共享世界玩家列表，全是uuid的字符串
     */
    fun getFriends(uuidString: String): Set<String>? {
        val stringFriends = RedisManager.getFriends(uuidString) ?: return null
        //先根据"|"分割成一对一对去掉最后一个空值，然后把每一对转化为int to int 最后把list变成set
        return stringFriends.split("|").toMutableList().dropLast(1).toSet()
    }
    fun removeFriend(player:Player,traget:Player){
        //todo
        val ship1 = getFriendsAndBanner(player.uniqueId.toString())!!
        val ship2 = getFriendsAndBanner(traget.uniqueId.toString())!!
        val friends1 = ship1.first.toMutableSet()
        val friends2 = ship2.first.toMutableSet()
        friends1.remove(traget.uniqueId.toString())
        friends2.remove(player.uniqueId.toString())
        setShip(player.uniqueId.toString(),friends1.map { UUID.fromString(it) }.toSet(),ship1.second.map { UUID.fromString(it) }.toSet())
        setShip(traget.uniqueId.toString(),friends2.map { UUID.fromString(it) }.toSet(),ship2.second.map { UUID.fromString(it) }.toSet())
    }
    fun removeFriend(player:Player,traget:String){
        val ship1 = getFriendsAndBanner(player.uniqueId.toString())!!
        val friends1 = ship1.first.toMutableSet()
        friends1.remove(traget)
        setShip(player.uniqueId.toString(),friends1.map { UUID.fromString(it) }.toSet(),ship1.second.map { UUID.fromString(it) }.toSet())
        val ship2 = getFriendsAndBanner(traget) ?: return
        val friends2 = ship2.first.toMutableSet()
        friends2.remove(player.uniqueId.toString())
        setShip(traget,friends2.map { UUID.fromString(it) }.toSet(),ship2.second.map { UUID.fromString(it) }.toSet())
    }
    fun removeBanner(player:Player,traget:String){
        val ship1 = getFriendsAndBanner(player.uniqueId.toString())!!
        val banners1 = ship1.second.toMutableSet()
        banners1.remove(traget)
        setShip(player.uniqueId.toString(),ship1.first.map { UUID.fromString(it) }.toSet(),banners1.map { UUID.fromString(it) }.toSet())
        val ship2 = getFriendsAndBanner(traget) ?: return
        val banners2 = ship2.second.toMutableSet()
        banners2.remove(player.uniqueId.toString())
        setShip(traget,ship2.first.map { UUID.fromString(it) }.toSet(),banners2.map { UUID.fromString(it) }.toSet())
    }

    /**
     * 存入此玩家的共享世界玩家列表和封禁玩家列表
     */
    fun setShip(uuidString: String, friends: Set<UUID>, banners: Set<UUID>){
        var stringFriends = ""
        friends.forEach { stringFriends += "$it|" }
        var stringBanners = ""
        banners.forEach { stringBanners += "$it|" }
        RedisManager.setShip(uuidString, stringFriends, stringBanners)
    }
    fun getFriendsAndBanner(uuidString: String): Pair<Set<String>,Set<String>>? {
        val stringFriendsAndBanners = RedisManager.getFriendsAndBanners(uuidString) ?: return null
        val friends = stringFriendsAndBanners.first.split("|").toMutableList().dropLast(1).toSet()
        val banners = stringFriendsAndBanners.second.split("|").toMutableList().dropLast(1).toSet()
        return friends to banners
    }
    fun addFriend(player:Player,traget:Player){
        val ship1 = getFriendsAndBanner(player.uniqueId.toString())!!
        val ship2 = getFriendsAndBanner(traget.uniqueId.toString())!!
        val friends1 = ship1.first.toMutableSet()
        val friends2 = ship2.first.toMutableSet()
        friends1.add(traget.uniqueId.toString())
        friends2.add(player.uniqueId.toString())
        setShip(player.uniqueId.toString(),friends1.map { UUID.fromString(it) }.toSet(),ship1.second.map { UUID.fromString(it) }.toSet())
        setShip(traget.uniqueId.toString(),friends2.map { UUID.fromString(it) }.toSet(),ship2.second.map { UUID.fromString(it) }.toSet())
    }
    fun addBanner(player:Player,traget:Player){
        val ship1 = getFriendsAndBanner(player.uniqueId.toString())!!
        val ship2 = getFriendsAndBanner(traget.uniqueId.toString())!!
        val banners1 = ship1.second.toMutableSet()
        val banners2 = ship2.second.toMutableSet()
        banners1.add(traget.uniqueId.toString())
        banners2.add(player.uniqueId.toString())
        setShip(player.uniqueId.toString(),ship1.first.map { UUID.fromString(it) }.toSet(),banners1.map { UUID.fromString(it) }.toSet())
        setShip(traget.uniqueId.toString(),ship2.first.map { UUID.fromString(it) }.toSet(),banners2.map { UUID.fromString(it) }.toSet())
    }

    /**
     * 获取此玩家的封禁玩家列表，全是uuid的字符串
     */
    fun getBanners(uuidString: String): Set<String>? {
        val stringBanners = RedisManager.getBanners(uuidString) ?: return null
        //先根据"|"分割成一对一对去掉最后一个空值，然后把每一对转化为int to int 最后把list变成set
        return stringBanners.split("|").toMutableList().dropLast(1).toSet()
    }

}