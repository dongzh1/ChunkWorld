package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.util.MapItem
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.hasItem
import com.xbaimiao.easylib.util.takeItem
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Item {
    val menuItem = menuItem()
    /**
     * 区块清除道具
     */
    fun voidItem(): ItemStack {
        return buildItem(Material.SCULK, builder = {
            name = "§c虚空生成器"
            lore.add("§4非常的危险")
            lore.add("§a按住§8[§bShift§8]§a可以右键放置")
            lore.add("§a只能用于自己的世界")
            lore.add("§c放置过后会在10s内将区块化为虚空")
            lore.add("§a群系不会改变")
            lore.add("§d10s内取消,生成器不退回")
            lore.add("§d一次性消耗品")
        })
    }

    /**
     * 地狱邀请函
     */
    fun netherItem(p: Player? = null): ItemStack {
        return buildItem(Material.PAPER, builder = {
            name = "§4地狱邀请函"
            if (p != null) lore.add("§c已绑定${p.name}")
            lore.add("§f一次性门票")
            lore.add("§f进入资源地狱会消耗邀请函")
            lore.add("§f在主城进行传送")
            lore.add("§f被其他玩家邀请传送到地狱也会消耗")
        })
    }

    /**
     * 末地邀请函
     */
    fun endItem(p: Player? = null): ItemStack {
        return buildItem(Material.PAPER, builder = {
            name = "§5末地邀请函"
            if (p != null) lore.add("§c已绑定${p.name}")
            lore.add("§f一次性门票")
            lore.add("§f进入资源末地会消耗邀请函")
            lore.add("§f在主城进行传送")
            lore.add("§f被其他玩家邀请传送到末地也会消耗")
        })
    }
    /**
     * 菜单
     */
    private fun menuItem(): ItemStack {
        return buildItem(Material.COMPASS, builder = {
            name = "§6物语菜单"
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 打开菜单")
        })
    }
    /**
     * 拓印画布
     */
    fun copyItemUsed(schemName:String,p:Player): ItemStack {
        // 获取当前时间
        val currentTime = LocalDateTime.now()
        // 定义格式化样式
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
        // 格式化当前时间
        val formattedTime = currentTime.format(formatter)
        val i = buildItem(Material.PAPER, builder = {
            amount = 1
            name = "§6拓印画布"
            lore.add("§c已绑定%player_name%".replacePlaceholder(p))
            lore.add("§7已在§b神明梦境§7拓印区块")
            lore.add("§7在你的§b梦境世界§7中右键使用")
            lore.add("§7画布中区块数据仅保留§c7§7天")
            lore.add("§7拓印时间:§b$formattedTime")
            lore.add("§c具现化后原区块无法找回")
            customModelData = 300011
        })
        val meta = i.itemMeta
        val pdc = meta.persistentDataContainer
        pdc.set(NamespacedKey(ChunkWorld.inst, "schem"), PersistentDataType.STRING,schemName)
        i.itemMeta = meta
        return i

    }

    /**
     * 从玩家背包判断是否有足够的指定物品，如果有就扣除并返回true,没有就返回false
     */
    fun deduct(item: ItemStack, p: Player, num: Int): Boolean {
        return if (p.inventory.hasItem(item, num)) {
            (p.inventory.takeItem(num, matcher = { itemMeta == item.itemMeta }))
        } else false
    }

}