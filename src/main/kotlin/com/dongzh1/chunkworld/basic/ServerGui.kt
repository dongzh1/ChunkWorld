package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.repoClient
import com.dongzh1.chunkworld.command.Tp
import com.dongzh1.chunkworld.listener.GroupListener
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class ServerGui(private val p: Player) {
    fun build() {
        if (Tp.isCooldown(p)) {
            p.sendMessage("§c世界正在加载中...")
            return
        }
        val playerDao = ChunkWorld.db.getPlayerDao(p.name)
        if (playerDao == null) {
            p.kick(Component.text("§c您的数据未正常加载，请重试或请联系腐竹，错误代码Lobby001"))
            return
        }
        val worldInfo = RedisManager.getWorldInfo(p.name)
        if (worldInfo != null) {
            p.sendMessage("§c您的独立世界尚未完整保存,无法切换服务器加载世界")
            p.sendMessage("§c准备将您再次传送到服务器:${worldInfo.serverName}")
            Tp.toSelfWorld(p, worldInfo.serverName, playerDao.id)
            return
        }
        val basic = PaperBasic(p, Component.text("§f服务器选择"))
        basic.rows(6)
        basic.onClick { it.isCancelled = true }
        basic.set(4, buildItem(Material.PAPER, builder = {
            name = "§f服务器说明"
            lore.add("§f所有服务器数据互通")
            lore.add("§f你的独立世界也可在其他服务器访问")
            lore.add("§f选择服务器后也可在游戏内再次跨服")
            lore.add("§f同一个服务器内的玩家可以更好的互动")
        }))
        val slots = listOf(
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            19,
            20,
            21,
            22,
            23,
            24,
            25,
            28,
            29,
            30,
            31,
            32,
            33,
            34,
            37,
            38,
            39,
            40,
            41,
            42,
            43,
            46,
            47,
            48,
            49,
            50,
            51,
            52
        )
        val serverList = RedisManager.getServerInfo()
        for (i in serverList.indices) {
            val serverInfo = serverList[i]
            if (i >= slots.size) break
            basic.set(slots[i], buildItem(Material.BEDROCK, builder = {
                name = "§f${serverInfo.first}"
                lore.add("§f在线人数: ${serverInfo.second}/20")
                lore.add("§f点击传送")
            }))
            basic.onClick(slots[i]) {
                p.closeInventory()
                GroupListener.addServerMap(p, serverInfo.first, playerDao.id)
                val zip = ChunkWorld.inst.config.getString("resource")!!
                val url = repoClient.createPresignedUrl(zip, p.uniqueId).downloadUrl
                val hash = ChunkWorld.inst.config.getByteList("hash").toByteArray()
                p.setResourcePack(
                    url,
                    hash,
                    Component.text("§a请您选择接受资源包以进入像素物语").appendNewline()
                        .append(Component.text("§f只有接受资源包才能进行完整体验"))
                )

            }
        }
        basic.openAsync()
    }
}