package com.dongzh1.chunkworld.basic

import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class VipGui(private val p: Player) {
    fun build() {
        val basic = PaperBasic(p, Component.text("§f赞助我们"))
        basic.rows(6)
        basic.onClick { it.isCancelled = true }
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name = "§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        basic.onClick(8) { p.closeInventory() }
        basic.set(20, buildItem(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, builder = {
            name = "§7成为§8[§b赞助者§8]"
            lore.add("§7权益列表:")
            lore.add("§f专属称号:§8[§b赞助者§8]")
            lore.add("§f全服世界展示")
            lore.add("")
            lore.add("§f请联系群主进行赞助")
            lore.add("§f10RMB/月")
        }))
        basic.openAsync()
    }
}