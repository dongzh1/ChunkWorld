package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.plugins.WorldEdit
import com.dongzh1.chunkworld.plugins.fawe
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.*
import me.arcaniax.hdb.api.HeadDatabaseAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.*
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class CopyGui(private val p: Player) {
    fun build(meta: ItemMeta) {
        val basic = PaperBasic(p, Component.text("拓印画布-§3拓印"))
        //设置菜单大小为行
        basic.rows(4)
        //取消全局点击事件
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.set(13, buildItem(Material.PAPER, builder = {
            customModelData = 300011
            name = "§3拓印画布详情"
            lore.add("§f消耗一个此物品")
            lore.add("§f拓印你所处的区块")
            lore.add("§f包括群系和大部分方块")
            lore.add("§c区块容器消失,生物抹去")
            lore.add("§c拓印后此区块将石化")
        }))
        basic.set(30, buildItem(Material.LIME_CONCRETE, builder = {
            name = "§a确认"
            lore.add("§4确认后扣除物品")
        }))
        basic.onClick(30) {
            p.closeInventory()
            if (p.inventory.takeItem(matcher = {itemMeta == meta})){
                //把区块所有的容器替换为空气
                fawe.replaceExclude(p.chunk)
                //保存为模板文件
                val fileName = fawe.savePlayerSchem(p.chunk,p)
                if (fileName == null){
                    p.sendMessage("§c保存失败")
                    //返还物品
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "neigeitems giveSilent ${p.name} 拓印画布 1")
                    return@onClick
                }
                //石化
                fawe.replaceEx(p.chunk)
                //给玩家复制后的物品
                p.inventory.addItem(Item.copyItemUsed(fileName,p))
                p.showTitle(
                    Title.title(
                        Component.text("§a已成功拓印"), Component.text("§f请回到你的梦境世界使用"),
                        Times.times(Duration.ofSeconds(1), Duration.ofSeconds(10), Duration.ofSeconds(1))
                    )
                )
            }
            else {
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