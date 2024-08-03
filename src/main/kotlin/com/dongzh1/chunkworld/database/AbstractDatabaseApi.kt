package com.dongzh1.chunkworld.database

import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.database.dao.ShipDao
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.QueryBuilder
import com.xbaimiao.easylib.database.Ormlite

abstract class AbstractDatabaseApi(ormlite: Ormlite) {
    private val playerDao: Dao<PlayerDao, Int> = ormlite.createDao(PlayerDao::class.java)
    private val shipDao: Dao<ShipDao, Int> = ormlite.createDao(ShipDao::class.java)

    // 根据玩家名字获取玩家信息
    fun getPlayerDao(name: String): PlayerDao? =
        playerDao.queryForEq("name", name).firstOrNull()

    fun getTrustNames(playerId: Int): List<String> {
        val queryBuilder: QueryBuilder<ShipDao, Int> = shipDao.queryBuilder()
        val where = queryBuilder.where()
        where.and(
            where.eq("type", true),
            where.or(
                where.eq("player1Id", playerId),
                where.eq("player2Id", playerId)
            )
        )
        val daoList = shipDao.query(queryBuilder.prepare())
        return daoList.map {
            if (it.player1Id == playerId) {
                playerDao.queryForId(it.player2Id).name
            } else {
                playerDao.queryForId(it.player1Id).name
            }
        }
    }

    fun getBanNames(playerId: Int): List<String> {
        val queryBuilder: QueryBuilder<ShipDao, Int> = shipDao.queryBuilder()
        val where = queryBuilder.where()
        where.and(
            where.eq("type", false),
            where.or(
                where.eq("player1Id", playerId),
                where.eq("player2Id", playerId)
            )
        )
        val daoList = shipDao.query(queryBuilder.prepare())
        return daoList.map {
            if (it.player1Id == playerId) {
                playerDao.queryForId(it.player2Id).name
            } else {
                playerDao.queryForId(it.player1Id).name
            }
        }
    }

    fun isTrust(player1Id: Int, player2Id: Int): Boolean {
        val queryBuilder: QueryBuilder<ShipDao, Int> = shipDao.queryBuilder()
        val where = queryBuilder.where()
        where.and(
            where.eq("type", true),
            where.or(
                where.and(
                    where.eq("player1Id", player1Id),
                    where.eq("player2Id", player2Id)
                ),
                where.and(
                    where.eq("player1Id", player2Id),
                    where.eq("player2Id", player1Id)
                )
            )
        )
        return shipDao.query(queryBuilder.prepare()).isNotEmpty()
    }

    fun isBan(player1Id: Int, player2Id: Int): Boolean {
        val queryBuilder: QueryBuilder<ShipDao, Int> = shipDao.queryBuilder()
        val where = queryBuilder.where()
        where.and(
            where.eq("type", false),
            where.or(
                where.and(
                    where.eq("player1Id", player1Id),
                    where.eq("player2Id", player2Id)
                ),
                where.and(
                    where.eq("player1Id", player2Id),
                    where.eq("player2Id", player1Id)
                )
            )
        )
        return shipDao.query(queryBuilder.prepare()).isNotEmpty()
    }

    fun setShip(player1Id: Int, player2Id: Int, type: Boolean) {
        val shipDao = ShipDao()
        shipDao.player1Id = player1Id
        shipDao.player2Id = player2Id
        shipDao.type = type
        this.shipDao.create(shipDao)
    }

    fun delShip(player1Id: Int, player2Id: Int, type: Boolean) {
        val queryBuilder: QueryBuilder<ShipDao, Int> = shipDao.queryBuilder()
        val where = queryBuilder.where()
        where.and(
            where.eq("type", type),
            where.or(
                where.and(
                    where.eq("player1Id", player1Id),
                    where.eq("player2Id", player2Id)
                ),
                where.and(
                    where.eq("player1Id", player2Id),
                    where.eq("player2Id", player1Id)
                )
            )
        )
        val daoList = shipDao.query(queryBuilder.prepare())
        daoList.forEach {
            shipDao.delete(it)
        }
    }
}
