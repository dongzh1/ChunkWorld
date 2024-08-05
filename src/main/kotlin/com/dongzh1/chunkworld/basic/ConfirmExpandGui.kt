package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.hasItem
import com.xbaimiao.easylib.util.hasLore
import com.xbaimiao.easylib.util.takeItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max

class ConfirmExpandGui(private val p: Player, private val chunk: Chunk) {
    fun build() {
        val basic = PaperBasic(p, Component.text("生成区块"))
        val chunkLevel = max(abs(chunk.x), abs(chunk.z))

        //设置菜单大小为行
        basic.rows(4)
        //取消全局点击事件
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.set(13, buildItem(Material.PAPER, builder = {
            customModelData = if (chunk.world.environment == World.Environment.NORMAL)
                300008
            else
                300009
            amount = chunkLevel
            name = "§3生成区块详情"
            lore.add("§f距离出生区块越远的区块等级越高")
            lore.add("§f这是 §f$chunkLevel 级区块")
            lore.add("§f消耗 §b$chunkLevel §f个区块碎片来生成区块")
            lore.add("§f请勿在选择区块时离线")
            lore.add("§4选择 §a确认 §4后碎片将无法退回")
        }))
        basic.set(30, buildItem(Material.LIME_CONCRETE, builder = {
            name = "§a确认"
            lore.add("§4确认后扣除碎片")
        }))
        basic.onClick(30) {
            p.closeInventory()
            if (deduct()) {
                //扣除成功
                p.showTitle(
                    Title.title(
                        Component.text("§a请稍等"), Component.text("§f正在对区块进行排列组合"),
                        Times.times(Duration.ofSeconds(1), Duration.ofMinutes(1), Duration.ofSeconds(1))
                    )
                )
                ExpandGui(p, chunk).build()
            } else {
                //没有足够的物品
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

    /**
     * 从玩家背包判断是否有足够的指定物品，如果有就扣除并返回true,没有就返回false
     */
    private fun deduct(): Boolean {
        val chunkLevel = max(abs(chunk.x), abs(chunk.z))
        if (chunk.world.environment == World.Environment.NORMAL) {
            if (p.inventory.hasItem(
                    amount = chunkLevel,
                    matcher = { type == Material.PAPER && itemMeta.customModelData == 300008 && hasLore("§c已绑定${p.name}") })
            ) {
                p.inventory.takeItem(
                    amount = chunkLevel,
                    matcher = { type == Material.PAPER && itemMeta.customModelData == 300008 && hasLore("§c已绑定${p.name}") })
                return true
            } else {
                return false
            }
        } else {
            if (p.inventory.hasItem(
                    amount = chunkLevel,
                    matcher = { type == Material.PAPER && itemMeta.customModelData == 300009 && hasLore("§c已绑定${p.name}") })
            ) {
                p.inventory.takeItem(
                    amount = chunkLevel,
                    matcher = { type == Material.PAPER && itemMeta.customModelData == 300009 && hasLore("§c已绑定${p.name}") })
                return true
            } else {
                return false
            }
        }
    }
}