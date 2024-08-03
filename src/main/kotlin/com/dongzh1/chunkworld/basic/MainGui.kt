package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.command.Tp
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class MainGui(private val p: Player) {
    fun build() {
        val basic = PaperBasic(p, Component.text("§f像素物语"))
        basic.rows(6)
        basic.onClick { it.isCancelled = true }
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name = "§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        basic.onClick(8) { p.closeInventory() }
        basic.set(11, buildItem(Material.END_CRYSTAL, builder = {
            name = "§7回到主城"
            lore.add("§7主城是提升自己的好地方")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 传送到主城")
        }))
        basic.onClick(11) {
            p.closeInventory()
            p.teleportAsync(ChunkWorld.spawnLocation)
        }
        basic.set(13, buildItem(Material.PLAYER_HEAD, builder = {
            customModelData = 10006
            skullOwner = p.name
            name = "§7你的私人世界"
            lore.add("§7世界可扩展至无限大")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 传送到你的世界")
        }))
        basic.onClick(13) {
            p.closeInventory()
            p.performCommand("chunkworld tp")
        }
        basic.set(15, buildItem(Material.PAINTING, builder = {
            name = "§7世界展示列表"
            lore.add("§7查看他人展示的世界")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 查看世界展示")
        }))
        basic.onClick(15) {
            p.closeInventory()
            ListGui(p).buildRedis(1)
        }
        basic.set(21, buildItem(Material.PAPER, builder = {
            customModelData = 300001
            name = "§7设置你的独立世界"
            lore.add("§7传送点、出生点、世界规则等")
            lore.add("")
            if (p.world.name == "chunkworlds/world/${p.uniqueId}" || p.world.name == "chunkworlds/world/${p.uniqueId}_nether") {
                lore.add("§f빪 §x§1§9§c§a§a§d➠ 浏览世界设置页面")
                basic.onClick(21) {
                    p.closeInventory()
                    SetGui(p).build()
                }
            } else {
                lore.add("§c只有在自己的世界才能设置")
            }
        }))
        basic.set(23, buildItem(Material.PAPER, builder = {
            customModelData = 300002
            name = "§7与他人共享家园"
            lore.add("§7或者拉黑他人")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 浏览人员设置界面")
        }))
        basic.onClick(23) {
            p.closeInventory()
            PlayerGui(p, 1).build()
        }
        basic.set(29, buildItem(Material.GRASS_BLOCK, builder = {
            name = "§a资源世界"
            lore.add("§f随机传送到资源世界")
            lore.add("§f使用条件:")
            lore.add("§c暂不可用,等待后续更新")
        }))
        basic.set(31, buildItem(Material.MAGMA_BLOCK, builder = {
            name = "§4资源地狱"
            lore.add("§f随机传送到资源地狱世界")
            lore.add("§f使用条件:")
            lore.add("§c暂不可用,等待后续更新")
        }))
        basic.set(33, buildItem(Material.END_STONE, builder = {
            name = "§5资源末地"
            lore.add("§f随机传送到资源地狱世界")
            lore.add("§f使用条件:")
            lore.add("§c暂不可用,等待后续更新")
        }))
        basic.set(48, buildItem(Material.SCULK_SENSOR, builder = {
            name = "§7切换服务器"
            lore.add("§7如果感到卡顿或想和朋友一起玩")
            lore.add("§7可以尝试切换服务器")
            lore.add("§7当前服务器:${ChunkWorld.serverName}")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 切换服务器")
        }))
        basic.onClick(48) {
            ServerGui(p).build()
        }
        basic.set(50, buildItem(Material.SCULK_SHRIEKER, builder = {
            name = "§7返回登录大厅"
            lore.add("§7回到登录大厅")
            lore.add("§7在登录大厅重新选择服务器")
            lore.add("§7可将你的世界在新选择的服务器加载")
            lore.add("§7当前服务器:${ChunkWorld.serverName}")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 返回登录大厅")
        }))
        basic.onClick(50) {
            Tp.connect(p, "lobby")
        }
    }
}