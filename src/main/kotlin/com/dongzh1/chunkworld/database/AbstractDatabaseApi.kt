package com.dongzh1.chunkworld.database

import com.dongzh1.chunkworld.database.dao.BanDao
import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.database.dao.TrustDao
import com.j256.ormlite.dao.Dao
import com.xbaimiao.easylib.database.Ormlite
import java.util.UUID

abstract class AbstractDatabaseApi(ormlite: Ormlite) {
    private val playerDao: Dao<PlayerDao, Int> = ormlite.createDao(PlayerDao::class.java)
    private val trustDao: Dao<TrustDao, Int> = ormlite.createDao(TrustDao::class.java)
    private val banDao: Dao<BanDao, Int> = ormlite.createDao(BanDao::class.java)
    private val chunkDao: Dao<ChunkDao, Int> = ormlite.createDao(ChunkDao::class.java)

    //根据玩家名字获取世界信息和玩家信息
    fun playerGet(name:String):PlayerDao? =
        playerDao.queryForEq("name",name).firstOrNull()
    //根据玩家名字获取玩家占领的区块
    fun chunkGet(name: String):List<Pair<Int,Int>> =
        chunkDao.queryForEq("playerID",playerGet(name)!!.id).map { it.x to it.z }
    //根据playerDao表id获取玩家占领的区块
    fun chunkGet(id: Int):List<Pair<Int,Int>> =
        chunkDao.queryForEq("playerID",id).map { it.x to it.z }
    //根据玩家名字获取玩家信任的玩家
    fun trustGet(name: String):List<UUID> =
        trustDao.queryForEq("playerID",playerGet(name)!!.id).map { playerDao.queryForId(it.trustedPlayerID).uuid }
    //根据playerDao表id获取玩家信任的玩家
    fun trustGet(id: Int):List<UUID> =
        trustDao.queryForEq("playerID",id).map { playerDao.queryForId(it.trustedPlayerID).uuid }
    //根据玩家名字获取信任玩家的玩家
    fun beTrustGet(name: String):List<UUID> =
        trustDao.queryForEq("trustedPlayerID",playerGet(name)!!.id).map { playerDao.queryForId(it.playerID).uuid }
    //根据playerDao表id获取信任玩家的玩家
    fun beTrustGet(id: Int):List<UUID> =
        trustDao.queryForEq("trustedPlayerID",id).map { playerDao.queryForId(it.playerID).uuid }
    //根据玩家名字获取玩家拉黑的玩家
    fun banGet(name: String):List<UUID> =
        banDao.queryForEq("playerID",playerGet(name)!!.id).map { playerDao.queryForId(it.bannedPlayerID).uuid }
    //根据playerDao表id获取玩家拉黑的玩家
    fun banGet(id: Int):List<UUID> =
        banDao.queryForEq("playerID",id).map { playerDao.queryForId(it.bannedPlayerID).uuid }
    //根据玩家名字获取拉黑玩家的玩家
    fun beBanGet(name: String):List<UUID> =
        banDao.queryForEq("bannedPlayerID",playerGet(name)!!.id).map { playerDao.queryForId(it.playerID).uuid }
    //根据playerDao表id获取拉黑玩家的玩家
    fun beBanGet(id: Int):List<UUID> =
        banDao.queryForEq("bannedPlayerID",id).map { playerDao.queryForId(it.playerID).uuid }
    //创建玩家信息
    fun playerCreate(dao: PlayerDao) =
        playerDao.create(dao)
    //创建区块信息
    fun chunkCreate(dao: ChunkDao) =
        chunkDao.create(dao)
    //创建信任信息
    fun trustCreate(dao: TrustDao) =
        trustDao.create(dao)
    //创建拉黑信息
    fun banCreate(dao: BanDao) =
        banDao.create(dao)
}