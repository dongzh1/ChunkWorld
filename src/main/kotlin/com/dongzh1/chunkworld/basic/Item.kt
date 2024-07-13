package com.dongzh1.chunkworld.basic

import com.cryptomorin.xseries.XItemStack
import com.cryptomorin.xseries.XMaterial
import com.dongzh1.chunkworld.ChunkWorld
import com.xbaimiao.easylib.util.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object Item {
    /**
     * 构建物品
     * @param material 材质
     * @param number 数量
     * @param displayName 显示名称
     * @param lore 介绍
     * @param customModelData 自定义模型数据,没有为-1
     */
    fun build(itemMaterial: Material,itemNumber: Int,itemName: String?,itemLore: List<String>?,itemCustomModelData : Int):ItemStack{
        return buildItem(XMaterial.matchXMaterial(itemMaterial), builder = {
            amount = itemNumber
            name = itemName
            itemLore?.let{ lore.addAll(it)}
            customModelData = itemCustomModelData
        })

    }
    fun head(playerName: String, itemName: String?, itemLore: List<String>?, itemCustomModelData: Int):ItemStack{
        return buildItem(XMaterial.matchXMaterial(Material.PLAYER_HEAD), builder = {
            skullOwner = playerName
            name = itemName
            itemLore?.let { lore.addAll(it) }
            customModelData = itemCustomModelData
        })
    }
    /**
     * 区块清除道具
     */
    fun voidItem():ItemStack{
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
    fun netherItem(p:Player?=null):ItemStack{
        return buildItem(Material.PAPER, builder = {
            name = "§4地狱邀请函"
            if (p != null) lore.add("§c已绑定${p.name}")
            lore.add("§f进入资源地狱会消耗邀请函")
            lore.add("§f右键传送门打开传送菜单")
            lore.add("§f被其他玩家邀请传送到地狱也会消耗")
        })
    }
    /**
     * 末地邀请函
     */
    fun endItem(p:Player?= null):ItemStack{
        return buildItem(Material.PAPER, builder = {
            name = "§5末地邀请函"
            if (p != null) lore.add("§c已绑定${p.name}")
            lore.add("§f进入资源末地会消耗邀请函")
            lore.add("§f右键传送门打开传送菜单")
            lore.add("§f被其他玩家邀请传送到末地也会消耗")
        })
    }
    /**
     * 从玩家背包判断是否有足够的指定物品，如果有就扣除并返回true,没有就返回false
     */
    fun deduct(item:ItemStack,p:Player,num:Int):Boolean{
        return if (p.inventory.hasItem(item,num)){
            (p.inventory.takeItem(num, matcher = {itemMeta == item.itemMeta}))
        }else false
    }

}