package com.dongzh1.chunkworld.basic

import com.cryptomorin.xseries.XItemStack
import com.cryptomorin.xseries.XMaterial
import com.xbaimiao.easylib.util.ItemBuilder
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
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
}