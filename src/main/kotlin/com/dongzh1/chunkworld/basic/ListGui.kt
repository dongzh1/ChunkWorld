package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class ListGui(private val p: Player) {
    private val playSlots = listOf(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
    private val worldInfoList = RedisManager.getShowWorldInfo()

    fun buildLocal() {
        val basic = build()
        basic.set(49, buildItem(Material.PAPER, builder = {
            customModelData = 300003
            name = "§7目前显示本服的玩家世界"
            lore.add("§7点击切换为相互信任的世界")
            lore.add(" ")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 切换列表")
        }))
        basic.onClick(49) {
            p.closeInventory()
            buildTrust()
        }
        val names = Bukkit.getWorlds().filter { it.name.startsWith("chunkworlds/world") }.map {
            it.persistentDataContainer.get(
                NamespacedKey.fromString("chunkworld_owner")!!,
                PersistentDataType.STRING
            )
        }.filterNotNull()
        for (i in playSlots.indices) {
            val playerName = names.getOrNull(i) ?: break
            val worldInfo = RedisManager.getWorldInfo(playerName) ?: continue
            basic.set(playSlots[i], buildItem(Material.PLAYER_HEAD, builder = {
                customModelData = 10006
                skullOwner = playerName
                name = "§7${playerName}的独立世界"
                lore.add("§7区块数量:")
                lore.add("§a主世界§f${worldInfo.normalChunks}")
                lore.add("§c地狱世界§f${worldInfo.netherChunks}")
                when (worldInfo.state) {
                    0.toByte() -> lore.add("§f世界状态:§a放开访问")
                    1.toByte() -> lore.add("§f世界状态:§d仅共享玩家访问")
                    2.toByte() -> lore.add("§f世界状态:§c不可访问")
                }
                lore.add("§f빪 §x§1§9§c§a§a§d➠ 传送到${playerName}的世界")
            }))
            basic.onClick(playSlots[i]) {
                //玩家执行指令
                p.closeInventory()
                p.performCommand("chunkworld tp $playerName")
            }
        }
        basic.openAsync()
    }

    private fun buildTrust() {
        val basic = build()
        basic.set(49, buildItem(Material.PAPER, builder = {
            customModelData = 300003
            name = "§7目前显示相互信任的世界"
            lore.add("§7点击切换为全服展示世界")
            lore.add(" ")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 切换列表")
        }))
        basic.onClick(49) {
            p.closeInventory()
            buildRedis(1)
        }
        val playerDao = ChunkWorld.db.getPlayerDao(p.name)!!
        val trusts = ChunkWorld.db.getTrustNames(playerDao.id)
        for (i in playSlots.indices) {
            val playerName = trusts.getOrNull(i) ?: break
            val worldInfo = RedisManager.getWorldInfo(playerName)
            basic.set(playSlots[i], buildItem(Material.PLAYER_HEAD, builder = {
                customModelData = 10006
                skullOwner = playerName
                name = "§7${playerName}的独立世界"
                if (worldInfo == null) {
                    lore.add("§c世界主人未在线")
                } else {
                    if (worldInfo.serverName == ChunkWorld.serverName) lore.add("§b此服世界")
                    lore.add("§7区块数量:")
                    lore.add("§a主世界§f${worldInfo.normalChunks}")
                    lore.add("§c地狱世界§f${worldInfo.netherChunks}")
                    when (worldInfo.state) {
                        0.toByte() -> lore.add("§f世界状态:§a放开访问")
                        1.toByte() -> lore.add("§f世界状态:§d仅共享玩家访问")
                        2.toByte() -> lore.add("§f世界状态:§c不可访问")
                    }
                    lore.add("§f빪 §x§1§9§c§a§a§d➠ 传送到${playerName}的世界")
                    basic.onClick(playSlots[i]) {
                        //玩家执行指令
                        p.closeInventory()
                        p.performCommand("chunkworld tp $playerName")
                    }
                }
            }))
        }
        basic.openAsync()
    }

    fun buildRedis(page: Int) {
        val basic = build()
        basic.set(49, buildItem(Material.PAPER, builder = {
            customModelData = 300003
            name = "§7目前显示全服展示世界"
            lore.add("§7点击切换为本服玩家世界")
            lore.add(" ")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 切换列表")
        }))
        basic.onClick(49) {
            p.closeInventory()
            buildLocal()
        }
        basic.set(46, buildItem(Material.PAPER, builder = {
            customModelData = 300004
            name = "§f빪 §x§1§9§c§a§a§d➠ 上一页"
        }))
        basic.onClick(46) {
            if (page <= 1) return@onClick
            else {
                p.closeInventory()
                buildRedis(page - 1)
            }
        }
        basic.set(52, buildItem(Material.PAPER, builder = {
            customModelData = 300005
            name = "§f빪 §x§1§9§c§a§a§d➠ 下一页"
        }))
        basic.onClick(52) {
            if (it.view.getItem(43) != null) {
                p.closeInventory()
                buildRedis(page + 1)
            }
        }
        var worldInfoList1 = worldInfoList
        worldInfoList1 = if (worldInfoList1.size > playSlots.size * page) {
            worldInfoList1.subList(playSlots.size * (page - 1), playSlots.size * page)
        } else if (worldInfoList1.size > playSlots.size * (page - 1)) {
            worldInfoList1.subList(playSlots.size * (page - 1), worldInfoList1.size)
        } else {
            emptyList()
        }
        for (i in playSlots.indices) {
            val worldInfo = worldInfoList1.getOrNull(i) ?: break
            basic.set(playSlots[i], buildItem(Material.PLAYER_HEAD, builder = {
                customModelData = 10006
                skullOwner = worldInfo.first
                name = "§7${worldInfo.first}的独立世界"
                lore.add("§7区块数量:")
                lore.add("§a主世界§f${worldInfo.second.normalChunks}")
                lore.add("§c地狱世界§f${worldInfo.second.netherChunks}")
                when (worldInfo.second.state) {
                    0.toByte() -> lore.add("§f世界状态:§a放开访问")
                    1.toByte() -> lore.add("§f世界状态:§d仅共享玩家访问")
                    2.toByte() -> lore.add("§f世界状态:§c不可访问")
                }
                lore.add("§f빪 §x§1§9§c§a§a§d➠ 传送到${worldInfo.first}的世界")
            }))
            basic.onClick(playSlots[i]) {
                //玩家执行指令
                p.closeInventory()
                p.performCommand("chunkworld tp ${worldInfo.first}")
            }
        }
        basic.openAsync()
    }

    private fun build(): PaperBasic {
        val basic = PaperBasic(p, Component.text("§f世界列表"))
        basic.rows(6)
        basic.onClick { it.isCancelled = true }
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name = "§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        basic.onClick(8) { p.closeInventory() }
        basic.set(17, buildItem(Material.PAPER, builder = {
            customModelData = 300012
            name = "§f빪 §x§1§9§c§a§a§d➠ 返回上级菜单"
        }))
        basic.onClick(17) {
            MainGui(p).build()
        }
        basic.set(2, buildItem(Material.TOTEM_OF_UNDYING, builder = {
            name = "§7成为§b赞助者"
            lore.add("§7即可向全服展示你的世界")
            lore.add(" ")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 查看赞助者权益")
        }))
        basic.onClick(2) {
            p.closeInventory()
            VipGui(p).build()
        }
        basic.set(4, buildItem(Material.PAPER, builder = {
            customModelData = 300007
            name = "§7活动专属空位"
            lore.add("§7暂未开放")
        }))
        return basic
    }
}