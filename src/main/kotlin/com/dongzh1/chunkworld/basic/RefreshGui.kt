package com.dongzh1.chunkworld.basic

import ParticleEffect
import com.dongzh1.chunkworld.plugins.fawe
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.takeItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.time.Duration
import java.util.*

class RefreshGui(private val p: Player) {
    val chunk = p.chunk
    private val worldisNormal = chunk.world.environment == World.Environment.NORMAL
    fun build(meta: ItemMeta) {
        val basic = PaperBasic(p, Component.text("一方梦境"))
        //设置菜单大小为行
        basic.rows(4)
        //取消全局点击事件
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.set(13, buildItem(Material.PAPER, builder = {
            customModelData = 300010
            name = "§3一方梦境详情"
            lore.add("§f消耗一个此物品")
            lore.add("§f将你所处的区块进行改变")
            lore.add("§f改变为一个随机的区块")
            lore.add("§f生物、群系、方块均会改变")
            lore.add("§c此改变不可逆")
        }))
        basic.set(30, buildItem(Material.LIME_CONCRETE, builder = {
            name = "§a确认"
            lore.add("§4确认后扣除物品")
        }))
        basic.onClick(30) {
            p.closeInventory()
            if (p.inventory.takeItem(matcher = { itemMeta == meta })) {
                //清除宝藏粒子效果
                val pdc = chunk.persistentDataContainer
                if (pdc.has(NamespacedKey.fromString("baozang_location")!!)) {
                    val loc = pdc.get(NamespacedKey.fromString("baozang_location")!!, PersistentDataType.STRING)
                    if (loc == null) {
                        pdc.remove(NamespacedKey.fromString("baozang_location")!!)
                    } else {
                        val x = loc.split(",")[0].toInt()
                        val y = loc.split(",")[1].toInt()
                        val z = loc.split(",")[2].toInt()
                        val block = chunk.world.getBlockAt(x, y, z)
                        val blockState = block.state
                        if (blockState is Skull) {
                            val sPDC = blockState.persistentDataContainer
                            if (sPDC.has(NamespacedKey.fromString("baozang")!!)) {
                                //删除粒子效果
                                val oldID = sPDC.get(NamespacedKey.fromString("baozang")!!, PersistentDataType.STRING)
                                if (oldID != null) ParticleEffect.stopEffect(UUID.fromString(oldID))
                                sPDC.remove(NamespacedKey.fromString("baozang")!!)
                                pdc.remove(NamespacedKey.fromString("baozang_location")!!)
                            } else {
                                pdc.remove(NamespacedKey.fromString("baozang_location")!!)
                            }
                        } else {
                            pdc.remove(NamespacedKey.fromString("baozang_location")!!)
                        }
                    }
                }
                //杀死生物
                chunk.entities.filter { it !is Player }.forEach {
                    it.remove()
                }
                //选择模板文件
                val schemPair = fawe.getRandomSchem(worldisNormal)
                val xiyoudu = schemPair.first
                val schem = schemPair.second ?: return@onClick
                //将模板粘贴到玩家世界,确定粘贴点，最小点
                val pos1: Location
                if (worldisNormal) {
                    pos1 = Location(chunk.world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
                } else {
                    pos1 = Location(chunk.world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
                }
                //复制模板
                fawe.placeSchem(schem, pos1)
                val schemShow = removeNumbersFromString(schem.name).replace(".schem", "")
                p.showTitle(
                    Title.title(
                        Component.text("§a梦境具现化"), Component.text("§f此梦境区块为 $xiyoudu $schemShow 区块"),
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

    fun removeNumbersFromString(input: String): String {
        // 使用正则表达式匹配数字并替换为空字符串
        return input.replace(Regex("\\d"), "")
    }
}