package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.plugins.fawe
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.takeItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import java.io.File
import java.time.Duration

class PasteGui(private val p: Player) {
    fun build(schem: File, meta: ItemMeta) {
        val basic = PaperBasic(p, Component.text("拓印画布-§e具现"))
        //设置菜单大小为行
        basic.rows(4)
        //取消全局点击事件
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.set(13, buildItem(Material.PAPER, builder = {
            customModelData = 300011
            name = "§3拓印画布详情"
            lore.add("§f消耗这个已拓印的画布")
            lore.add("§f改变你目前所处区块")
            lore.add("§f将此画布的内容进行具现化")
            lore.add("§c生物抹去")
            lore.add("§c改变后无法恢复")
        }))
        basic.set(30, buildItem(Material.LIME_CONCRETE, builder = {
            name = "§a确认"
            lore.add("§4确认后扣除物品")
        }))
        basic.onClick(30) {
            p.closeInventory()
            if (p.inventory.takeItem(matcher = { itemMeta == meta })) {
                //粘贴区块
                val chunk = p.chunk
                val world = p.world
                val pos1: Location = if (world.environment == World.Environment.NORMAL) {
                    Location(world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
                } else if (world.environment == World.Environment.NETHER) {
                    Location(world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
                } else {
                    p.sendMessage("§c未知的世界环境")
                    return@onClick
                }
                chunk.entities.filter { it !is Player }.forEach {
                    it.remove()
                }
                fawe.placeSchem(schem, pos1)
                p.showTitle(
                    Title.title(
                        Component.text("§a区块拓印成功"), Component.text("§f你的梦境你做主"),
                        Times.times(Duration.ofSeconds(1), Duration.ofSeconds(10), Duration.ofSeconds(1))
                    )
                )
            } else {
                p.sendMessage("§c你没有足够的物品")
            }
        }
        basic.set(32, buildItem(Material.RED_CONCRETE, builder = {
            name = "§c取消"
        }))
        basic.onClick(32) {
            p.closeInventory()
        }
        basic.openAsync()
    }
}