package com.dongzh1.chunkworld.database

import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.database.dao.ShipDao
import com.j256.ormlite.dao.Dao
import com.xbaimiao.easylib.database.Ormlite

abstract class AbstractDatabaseApi(ormlite: Ormlite) {
    private val playerDao: Dao<PlayerDao, Int> = ormlite.createDao(PlayerDao::class.java)
    private val shipDao: Dao<ShipDao, Int> = ormlite.createDao(ShipDao::class.java)

    // 根据玩家名字获取玩家信息
    fun getPlayerDao(name: String): PlayerDao? =
        playerDao.queryForEq("name", name).firstOrNull()
}
