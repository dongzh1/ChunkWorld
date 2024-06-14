package com.dongzh1.chunkworld.basic

import com.cryptomorin.xseries.XItemStack
import com.cryptomorin.xseries.XMaterial
import net.kyori.adventure.text.Component
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
    fun build(material: Material,number: Int,displayName: String?,lore: List<String>?,customModelData : Int):ItemStack{
        val item = ItemStack(material,number)
        val meta = item.itemMeta
        if (displayName != null) meta.displayName(Component.text(displayName))
        if (lore != null) meta.lore(lore.map { Component.text(it) })
        if (customModelData != -1) meta.setCustomModelData(customModelData)
        item.itemMeta = meta
        return item
    }
}