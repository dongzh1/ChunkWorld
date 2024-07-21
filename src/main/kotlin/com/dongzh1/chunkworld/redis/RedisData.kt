package com.dongzh1.chunkworld.redis

import com.dongzh1.chunkworld.database.dao.PlayerDao
import java.util.*

/**
 * 存入内存的各类信息，用于各种判断
 */
object RedisData {
    /**
     * 根据uuid获取玩家在内存的数据，群组使用
     */
    fun getPlayerDao(uuidString: String): PlayerDao? {
        val stringPlayerDao = RedisManager.getPlayerDao(uuidString) ?: return null
        return PlayerDao().apply {
            id = stringPlayerDao.split("|,|")[0].toInt()
            name = stringPlayerDao.split("|,|")[1]
            uuid = UUID.fromString(uuidString)
            createTime = stringPlayerDao.split("|,|")[2]
            lastTime = stringPlayerDao.split("|,|")[3].toLong()
            spawn = stringPlayerDao.split("|,|")[4]
            netherSpawn = stringPlayerDao.split("|,|")[5]
            worldStatus = stringPlayerDao.split("|,|")[6].toByte()
            teleport = stringPlayerDao.split("|,|")[7]
        }
    }

    /**
     * 把playerDao存入内存，群组使用,
     * 从数据库存入redis应该发生在玩家上线时，
     */
    fun setPlayerDao(dao: PlayerDao) {
        val stringPlayerDao =
            "${dao.id}|,|${dao.name}|,|${dao.createTime}|,|${dao.lastTime}|,|${dao.spawn}|,|${dao.netherSpawn}|,|${dao.worldStatus}|,|${dao.teleport}"
        RedisManager.setPlayerDao(dao.uuid.toString(), stringPlayerDao)
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

    /**
     * 把这个玩家世界的区块信息存入redis,0，1分别代表主世界，地狱
     */
    fun setChunks(uuidString: String, chunks: Set<Pair<Int, Int>>, worldType: Byte) {
        var stringChunks = ""
        chunks.forEach { stringChunks += "${it.first},${it.second}|" }
        RedisManager.setChunks(uuidString, stringChunks, worldType)
    }

    /**
     * 获取此玩家的共享世界玩家列表，全是uuid的字符串
     */
    fun getFriends(uuidString: String): Set<String>? {
        val stringFriends = RedisManager.getFriends(uuidString) ?: return null
        //先根据"|"分割成一对一对去掉最后一个空值，然后把每一对转化为int to int 最后把list变成set
        return stringFriends.split("|").toMutableList().dropLast(1).toSet()
    }

    /**
     * 存入此玩家的共享世界玩家列表，全是uuid的字符串
     */
    fun setFriends(uuidString: String, friends: Set<String>) {
        var stringFriends = ""
        friends.forEach { stringFriends += "$it|" }
        RedisManager.setFriends(uuidString, stringFriends)
    }

    /**
     * 获取此玩家的封禁玩家列表，全是uuid的字符串
     */
    fun getBanners(uuidString: String): Set<String>? {
        val stringBanners = RedisManager.getBanners(uuidString) ?: return null
        //先根据"|"分割成一对一对去掉最后一个空值，然后把每一对转化为int to int 最后把list变成set
        return stringBanners.split("|").toMutableList().dropLast(1).toSet()
    }

    /**
     * 获取此玩家的封禁玩家列表，全是uuid的字符串
     */
    fun setBanners(uuidString: String, banners: Set<String>) {
        var stringBanners = ""
        banners.forEach { stringBanners += "$it|" }
        RedisManager.setBanners(uuidString, stringBanners)
    }

}