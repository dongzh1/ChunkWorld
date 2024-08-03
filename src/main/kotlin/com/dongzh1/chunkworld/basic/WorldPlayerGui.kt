package com.dongzh1.chunkworld.basic

import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player

class WorldPlayerGui(private val p: Player, private val world: World, private val page: Int) {
    fun build() {
        if (world.name != "chunkworlds/world/${p.uniqueId}" || world.name != "chunkworlds/nether/${p.uniqueId}") return
        val basic = PaperBasic(p, Component.text("世界玩家"))
        basic.rows(6)
        basic.onClick { it.isCancelled = true }
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name = "§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        basic.onClick(8) { p.closeInventory() }
        basic.set(17, buildItem(Material.PAPER, builder = {
            customModelData = 300008
            name = "§f빪 §x§1§9§c§a§a§d➠ 返回上级菜单"
        }))
        basic.onClick(17) {
            SetGui(p).build()
        }
        val playerList =
            if (world.players.size > page * 21) world.players.subList((page - 1) * 21, page * 21)
            else if (world.players.size > (page - 1) * 21) world.players.subList((page - 1) * 21, world.players.size)
            else emptyList()
        val slots = listOf(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)
        for (i in slots.indices) {
            val player = playerList.getOrNull(i) ?: break
            basic.set(slots[i], buildItem(Material.PLAYER_HEAD, builder = {
                skullOwner = player.name
                name = player.name
                customModelData = 10006
                lore.add("§f点击打开护照")
            }))
            basic.onClick(slots[i]) {
                PassportGui(p, player).build()
            }
        }
        basic.set(46, buildItem(Material.PAPER, builder = {
            customModelData = 300004
            name = "§f빪 §x§1§9§c§a§a§d➠ 上一页"
        }))
        basic.onClick(46) {
            if (page <= 1) return@onClick
            else WorldPlayerGui(p, world, page - 1).build()
        }
        basic.set(52, buildItem(Material.PAPER, builder = {
            customModelData = 300005
            name = "§f빪 §x§1§9§c§a§a§d➠ 下一页"
        }))
        basic.onClick(52) {
            if (it.view.getItem(43) != null) WorldPlayerGui(p, world, page + 1).build()
        }
        basic.openAsync()
    }
}