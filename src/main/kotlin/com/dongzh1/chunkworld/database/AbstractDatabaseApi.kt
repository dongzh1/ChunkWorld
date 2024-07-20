package com.dongzh1.chunkworld.database

import com.dongzh1.chunkworld.database.dao.BanshipDao
import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.database.dao.FriendshipDao
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.stmt.QueryBuilder
import com.xbaimiao.easylib.database.Ormlite
import java.util.UUID

abstract class AbstractDatabaseApi(ormlite: Ormlite) {
    private val playerDao: Dao<PlayerDao, Int> = ormlite.createDao(PlayerDao::class.java)
    private val friendshipDao: Dao<FriendshipDao, Int> = ormlite.createDao(FriendshipDao::class.java)
    private val banshipDao: Dao<BanshipDao, Int> = ormlite.createDao(BanshipDao::class.java)
    private val chunkDao: Dao<ChunkDao, Int> = ormlite.createDao(ChunkDao::class.java)

    // 根据玩家名字获取玩家信息
    fun playerGet(name: String): PlayerDao? =
        playerDao.queryForEq("name", name).firstOrNull()
    fun playerGet(uuid: UUID): PlayerDao? =
        playerDao.queryForEq("uuid", uuid).firstOrNull()
    //获取最近一个月上线的玩家数据
    fun
    // 获取玩家信息，但排除指定玩家列表，根据chunkCount从大到小排序，然后取第n到第m个
    fun playerGet(n: Int, m: Int, exclude: List<Int>): List<PlayerDao> {
        // 构建查询
        val queryBuilder: QueryBuilder<PlayerDao, Int> = playerDao.queryBuilder()

        if (exclude.isNotEmpty()) {
            queryBuilder.where().notIn("id", exclude)
        }

        // 按 chunkCount 从大到小排序
        queryBuilder.orderBy("chunkCount", false)

        // 设置分页
        queryBuilder.offset(n.toLong()).limit((m - n).toLong())

        return playerDao.query(queryBuilder.prepare())
    }
    // 获取玩家信息，根据指定玩家的 UUID 列表
    fun playerGet(uuids: List<UUID>): List<PlayerDao> {
        if (uuids.isEmpty()) return emptyList()
        val queryBuilder: QueryBuilder<PlayerDao, Int> = playerDao.queryBuilder()
        queryBuilder.where().`in`("uuid", uuids)
        return playerDao.query(queryBuilder.prepare())
    }
    fun playerUpdate(dao: PlayerDao) {
        playerDao.update(dao)
    }
    // 根据玩家名字获取玩家占领的区块
    fun chunkGet(name: String): List<Pair<Int, Int>> =
        playerGet(name)?.let { player ->
            chunkDao.queryForEq("playerID", player.id).map { it.x to it.z }
        } ?: emptyList()

    // 根据 playerDao 表 id 获取玩家占领的区块
    fun chunkGet(id: Int): List<Pair<Int, Int>> =
        chunkDao.queryForEq("playerID", id).map { it.x to it.z }

    // 方法：根据UUID获取玩家ID
    fun getPlayerIdByUuid(uuid: UUID): Int? {
        val player = playerDao.queryForEq("uuid", uuid).firstOrNull()
        return player?.id
    }
    private fun getPlayerIdByName(name: String): Int? {
        val player = playerDao.queryForEq("name", name).firstOrNull()
        return player?.id
    }

    // 创建玩家信息
    fun playerCreate(dao: PlayerDao) =
        playerDao.create(dao)

    // 创建区块信息
    fun chunkCreate(dao: ChunkDao) {
        // 如果已经存在就不创建
        if (chunkDao.queryForEq("playerID", dao.playerID).any { it.x == dao.x && it.z == dao.z }) return
        chunkDao.create(dao)
    }

    /**
     * 判断两者关系，
     * @param playerId 玩家1的ID
     * @param otherUuid 玩家2的UUID
     * @param ship false为互相拉黑关系，true为互相好友关系
     */
    fun areShip(playerId: Int, otherUuid: UUID,ship:Boolean): Boolean {
        val otherId = getPlayerIdByUuid(otherUuid) ?: return false
        return areShip(playerId, otherId,ship)
    }
    /**
     * 判断两者关系，
     * @param playerId 玩家1的ID
     * @param otherId 玩家2的ID
     * @param ship false为互相拉黑关系，true为互相好友关系
     */
    fun areShip(playerId: Int, otherId: Int,ship:Boolean): Boolean {
        val (p1, p2) = if (playerId < otherId) playerId to otherId else otherId to playerId
        return if (ship) friendshipDao.queryForEq("playerId", p1).any { it.friendId == p2 }
        else banshipDao.queryForEq("playerId", p1).any { it.bannerId == p2 }
    }
    /**
     * 判断两者关系，
     * @param playerUUID 玩家1的UUID
     * @param otherUuid 玩家2的UUID
     * @param ship false为互相拉黑关系，true为互相好友关系
     */
    fun areShip(playerUUID: UUID, otherUuid: UUID,ship:Boolean): Boolean {
        val playerId = getPlayerIdByUuid(playerUUID) ?: return false
        val otherId = getPlayerIdByUuid(otherUuid) ?: return false
        return areShip(playerId, otherId,ship)
    }
    /**
     * 添加关系，
     * @param playerId 玩家1的ID
     * @param otherId 玩家2的ID
     * @param ship false为互相拉黑关系，true为互相好友关系
     */
    fun addShip(playerId: Int, otherId: Int, ship: Boolean) {
        // 确保 playerId 总是小于 friendId
        val (p1, p2) = if (playerId < otherId) playerId to otherId else otherId to playerId

        // 确保不重复添加
        if (!areShip(p1, p2, ship)) {
            if (ship) {
                val friendship = FriendshipDao().apply {
                    this.playerId = p1
                    this.friendId = p2
                }
                friendshipDao.create(friendship)
            } else {
                val banship = BanshipDao().apply {
                    this.playerId = p1
                    this.bannerId = p2
                }
                banshipDao.create(banship)
            }
        }
    }
    // 方法：查询玩家的所有好友
    fun getShip(playerId: Int,ship: Boolean): List<PlayerDao> {
        if (ship) {
            val p1 = friendshipDao.queryForEq("playerId", playerId).map { it.friendId }
            val p2 = friendshipDao.queryForEq("friendId", playerId).map { it.playerId }
            val allFriendsIds = p1 + p2
            return allFriendsIds.map { playerDao.queryForId(it) }
        } else {
            val p1 = banshipDao.queryForEq("playerId", playerId).map { it.bannerId }
            val p2 = banshipDao.queryForEq("bannerId", playerId).map { it.playerId }
            val allBannerIds = p1 + p2
            return allBannerIds.map { playerDao.queryForId(it) }
        }
    }

    /**
     * 删除关系，
     * @param playerId 玩家1的ID
     * @param otherName 玩家2的名字
     * @param ship false为互相拉黑关系，true为互相好友关系
     */
    fun removeShip(playerId: Int, otherName:String, ship: Boolean) {
        val otherId = getPlayerIdByName(otherName) ?: return
        removeShip(playerId, otherId, ship)
    }
    /**
     * 删除关系，
     * @param playerId 玩家1的ID
     * @param otherUuid 玩家2的UUID
     * @param ship false为互相拉黑关系，true为互相好友关系
     */
    fun removeShip(playerId: Int, otherUuid: UUID, ship: Boolean) {
        val otherId = getPlayerIdByUuid(otherUuid) ?: return
        removeShip(playerId, otherId, ship)
    }
    /**
     * 删除关系，
     * @param playerId 玩家1的ID
     * @param otherId 玩家2的ID
     * @param ship false为互相拉黑关系，true为互相好友关系
     */
    fun removeShip(playerId: Int, otherId: Int, ship: Boolean) {
        // 确保 playerId 总是小于 friendId
        val (p1, p2) = if (playerId < otherId) playerId to otherId else otherId to playerId

        // 确保不重复添加
        if (areShip(p1, p2, ship)) {
            if (ship) {
                friendshipDao.queryForEq("playerId", p1).find { it.friendId == p2 }?.let {
                    friendshipDao.delete(it)
                }
                friendshipDao.queryForEq("playerId", p2).find { it.friendId == p1 }?.let {
                    friendshipDao.delete(it)
                }
            } else {
                banshipDao.queryForEq("playerId", p1).find { it.bannerId == p2 }?.let {
                    banshipDao.delete(it)
                }
                banshipDao.queryForEq("playerId", p2).find { it.bannerId == p1 }?.let {
                    banshipDao.delete(it)
                }
            }
        }
    }
}
