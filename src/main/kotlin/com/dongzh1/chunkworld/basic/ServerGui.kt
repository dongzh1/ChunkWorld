package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.command.Tp
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class ServerGui(private val p: Player) {
    fun build() {
        val basic = PaperBasic(p, Component.text("§f服务器选择"))
        basic.rows(6)
        basic.onClick { it.isCancelled = true }
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name = "§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        basic.onClick(8) { p.closeInventory() }
        basic.set(17, buildItem(Material.PAPER, builder = {
            customModelData = 300010
            name = "§f빪 §x§1§9§c§a§a§d➠ 返回上级菜单"
        }))
        basic.onClick(17) {
            MainGui(p).build()
        }
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
            basic.set(slots[i], buildItem(Material.BEDROCK, builder = {
                name = "§f${serverInfo.first}"
                lore.add("§f在线人数: ${serverInfo.second}")
                lore.add("§f点击传送")
            }))
            basic.onClick(slots[i]) {
                p.closeInventory()
                Tp.connect(p, serverInfo.first)
            }
        }
    }
}