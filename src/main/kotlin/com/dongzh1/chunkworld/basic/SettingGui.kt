package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.xbaimiao.easylib.ui.PaperBasic
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.ObjectUtils.Null
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SettingGui(private val p: Player,private val invitePage:Int,private val banPage:Int) {
    fun build() {
        val basic = PaperBasic(p, Component.text("世界设置"))
        basic.rows(4)
        val playerDao = Listener.getPlayerDaoMap(p.name)!!
        basic.set(4,Item.build(Material.PAINTING,1,"世界信息",
            listOf("",
                "§f已解锁区块数量: §a${playerDao.chunkCount}",
                "§f创建日期: §7${playerDao.createTime}"),-1))
        basic.set(8,Item.build(Material.BARRIER,1,"返回上级", null,-1))
        basic.set(10,Item.build(Material.END_PORTAL_FRAME,1,"设置出生点",
            listOf("",
                "§f当前出生点: " +
                        "§a${playerDao.spawn.split(",")[0]}§b," +
                        "§a${playerDao.spawn.split(",")[1]}§b," +
                        "§a${playerDao.spawn.split(",")[2]}",
                "&f点击设置当前位置为出生点"),-1))
        basic.set(12,Item.build(Material.VERDANT_FROGLIGHT,1,"开放世界", listOf("",
            "§f此模式下所有玩家都可进入你的世界"),-1))
        basic.set(14,Item.build(Material.OCHRE_FROGLIGHT,1,"部分开放", listOf("",
            "§f此模式下只有被信任的玩家可进入你的世界"),-1))
        basic.set(16,Item.build(Material.PEARLESCENT_FROGLIGHT,1,"关闭世界", listOf("",
            "§f此模式下只有你可以进入你的世界"),-1))
        basic.set(18,Item.build(Material.SKELETON_SKULL,1,"信任玩家", listOf("",
            "§f右侧是你的信任的玩家",
            "§f被信任的玩家可以在你的家园建造和破坏"),-1))
        basic.set(27,Item.build(Material.WITHER_SKELETON_SKULL,1,"拉黑玩家", listOf("",
            "§f右侧是被你拉黑的玩家",
            "§f被拉黑的玩家无法进入你的世界"),-1))
        for (i in 19..25){
            basic.set(i,Item.build(Material.LIME_STAINED_GLASS_PANE,1,"§a已解锁",
                listOf("§f使用指令 /invite 玩家","§f即可信任该玩家"),-1))
        }
        for (i in 28..34){
            basic.set(i,Item.build(Material.LIME_STAINED_GLASS_PANE,1,"§a已解锁",
                listOf("§f使用指令 /ban 玩家","§f即可拉黑该玩家"),-1))
        }

        basic.onClick(12) {
            val item = it.view.getItem(12)
            item!!.addUnsafeEnchantment(Enchantment.LUCK,1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(12,item)
            val item14 = it.view.getItem(14)
            item14!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(14,item14)
            val item16 = it.view.getItem(16)
            item16!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(16,item16)
        }
        basic.onClick(14) {
            val item = it.view.getItem(14)
            item!!.addUnsafeEnchantment(Enchantment.LUCK,1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(14,item)
            val item12 = it.view.getItem(12)
            item12!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(12,item12)
            val item16 = it.view.getItem(16)
            item16!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(16,item16)
        }
        basic.onClick(16) {
            val item = it.view.getItem(16)
            item!!.addUnsafeEnchantment(Enchantment.LUCK,1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(16,item)
            val item14 = it.view.getItem(14)
            item14!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(14,item14)
            val item12 = it.view.getItem(12)
            item12!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(12,item12)
        }
        basic.openAsync()
    }
}