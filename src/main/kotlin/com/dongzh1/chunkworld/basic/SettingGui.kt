package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import java.util.UUID

class SettingGui(private val p: Player,private val banPage:Int) {
    fun build() {
        val basic = PaperBasic(p, Component.text("世界设置"))
        basic.rows(4)
        val playerDao = Listener.getPlayerDaoMap(p.uniqueId)!!
        basic.set(4,Item.build(Material.PAINTING,1,"世界信息",
            listOf("",
                "§f已解锁区块数量: §a${playerDao.chunkCount}",
                "§f创建日期: §7${playerDao.createTime}"),-1))
        basic.set(8,Item.build(Material.BARRIER,1,"返回上级", null,-1))
        basic.set(10,Item.build(Material.END_PORTAL_FRAME,1,"设置出生点",
            listOf("",
                "§f当前出生点: " +
                        "§a${String.format("%.2f",playerDao.x())}§b," +
                        "§a${String.format("%.2f",playerDao.y())}§b," +
                        "§a${String.format("%.2f",playerDao.z())}",
                "§f点击设置当前位置为出生点"),-1))
        basic.set(12,Item.build(Material.VERDANT_FROGLIGHT,1,"开放世界", listOf("",
            "§f此模式下所有玩家都可进入你的世界"),-1))
        basic.set(14,Item.build(Material.OCHRE_FROGLIGHT,1,"部分开放", listOf("",
            "§f此模式下只有被信任的玩家可进入你的世界"),-1))
        basic.set(16,Item.build(Material.PEARLESCENT_FROGLIGHT,1,"关闭世界", listOf("",
            "§f此模式下只有你可以进入你的世界"),-1))
        when(playerDao.worldStatus){
            0.toByte() -> {
                val item = Item.build(Material.VERDANT_FROGLIGHT,1,"开放世界", listOf("",
                    "§f此模式下所有玩家都可进入你的世界"),-1)
                item.addUnsafeEnchantment(Enchantment.LUCK,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                basic.set(12,item)
            }
            1.toByte() -> {
                val item = Item.build(Material.OCHRE_FROGLIGHT,1,"部分开放", listOf("",
                    "§f此模式下只有被信任的玩家可进入你的世界"),-1)
                item.addUnsafeEnchantment(Enchantment.LUCK,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                basic.set(14,item)
            }
            2.toByte() -> {
                val item = Item.build(Material.PEARLESCENT_FROGLIGHT,1,"关闭世界", listOf("",
                    "§f此模式下只有你可以进入你的世界"),-1)
                item.addUnsafeEnchantment(Enchantment.LUCK,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                basic.set(16,item)
            }
        }
        basic.set(18,Item.build(Material.SKELETON_SKULL,1,"信任玩家", listOf("",
            "§f右侧是你的信任的玩家",
            "§f被信任的玩家可以在你的家园建造和破坏"),-1))
        basic.set(27,Item.build(Material.WITHER_SKELETON_SKULL,1,"拉黑玩家", listOf("",
            "§f右侧是被你拉黑的玩家",
            "§f被拉黑的玩家无法进入你的世界"),-1))
        basic.set(35,Item.build(Material.ARROW,1,"§a下一页",
            listOf("§f点击查看更多被拉黑的玩家"),-1))
        //团队玩家只能8人
        for (i in 19..26){
            basic.set(i,Item.build(Material.LIME_STAINED_GLASS_PANE,1,"§a已解锁",
                listOf("§f使用指令 /invite 玩家","§f即可信任该玩家"),-1))
        }
        for (i in 28..34){
            basic.set(i,Item.build(Material.LIME_STAINED_GLASS_PANE,1,"§a已解锁",
                listOf("§f使用指令 /ban 玩家","§f即可拉黑该玩家"),-1))
        }
        val trustList = Listener.getTrustMap(p)!!.toList()
        val banList :List<UUID> = if (banPage == 1) Listener.getBanMap(p)!!.toList().take(banPage*7-1).toList()
        else Listener.getBanMap(p)!!.toList().drop((banPage-1)*7-1).take(banPage*7-1).toList()

        for (i in trustList.indices){
            val offlinePlayer = Bukkit.getOfflinePlayer(trustList[i])
            basic.set(19+i,Item.head(offlinePlayer.name?:playerDao.name,"§a${offlinePlayer.name}", listOf("§b点击不再信任该玩家"),-1))
        }
        for (i in banList.indices){
            val offlinePlayer = Bukkit.getOfflinePlayer(banList[i])
            basic.set(28+i,Item.head(offlinePlayer.name?:playerDao.name,"§a${offlinePlayer.name}", listOf("§b点击申请解除相互拉黑关系"),-1))
        }
        basic.onClick { event ->
            event.isCancelled = true
        }

        basic.onClick(8) { ListGui(p,1,false).build() }

        basic.onClick(10) {
            if (p.location.world.name == ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}"){
                //在自己世界，可以设置
                playerDao.spawn = "${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                submit(async = true) {
                    ChunkWorld.db.playerUpdate(playerDao)
                //重新获取数据
                    Listener.setPlayerDaoMap(p.name,ChunkWorld.db.playerGet(p.name)!!)
                }
                p.closeInventory()
                p.sendMessage("§a出生点设置成功")
            }else{
                p.closeInventory()
                p.sendMessage("§c请在自己的世界设置出生点")
            }
        }

        basic.onClick(12) {
            val item = it.view.getItem(12)
            item!!.addUnsafeEnchantment(Enchantment.LUCK, 1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(12, item)
            val item14 = it.view.getItem(14)
            item14!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(14, item14)
            val item16 = it.view.getItem(16)
            item16!!.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(16, item16)
            playerDao.worldStatus = 0
            Listener.setPlayerDaoMap(p.name, playerDao)
            //数据库
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
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
            playerDao.worldStatus = 1
            Listener.setPlayerDaoMap(p.name, playerDao)
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
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
            playerDao.worldStatus = 2
            Listener.setPlayerDaoMap(p.name, playerDao)
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
        }
        basic.onClick(listOf(19,20,21,22,23,24,25,26)) {
            //不是玻璃板
            if (it.view.getItem(it.slot)!!.type == Material.LIME_STAINED_GLASS_PANE) return@onClick
            //根据列表对应获取玩家
            val uuid = trustList[it.slot-19]
            //解除信任关系
            val trustMap = Listener.getTrustMap(p)!!.toMutableSet()
            trustMap.remove(uuid)
            Listener.setTrustMap(p,trustMap)
            val id1 = playerDao.id
            val targetPlayer = Bukkit.getPlayer(uuid)
            if (targetPlayer != null){
                targetPlayer.sendMessage("§c${p.name} 已和你解除信任关系")
                val trustDao1 = Listener.getTrustMap(targetPlayer)!!.toMutableSet()
                trustDao1.remove(p.uniqueId)
                Listener.setTrustMap(targetPlayer, trustDao1)
            }
            submit(async = true) { ChunkWorld.db.removeShip(id1,uuid,true) }
            it.view.setItem(it.slot,Item.build(Material.LIME_STAINED_GLASS_PANE,1,"§a已解锁",
                listOf("§f使用指令 /invite 玩家","§f即可信任该玩家"),-1))
        }
        basic.onClick(listOf(28,29,30,31,32,33,34)) {
            //不是玻璃板
            if (it.view.getItem(it.slot)!!.type == Material.LIME_STAINED_GLASS_PANE) return@onClick
            //根据列表对应获取玩家
            val uuid = banList[it.slot-28]

            val targetPlayer = Bukkit.getPlayer(uuid)
            //关闭菜单
            p.closeInventory()
            if (targetPlayer != null){
                Listener.setCommand("${p.name} unban ${targetPlayer.name}")
                TellrawJson()
                    .append("§c${p.name} 申请解除相互拉黑关系")
                    .newLine()
                    .append("§a同意").hoverText("点击同意这个申请").runCommand("/chunkworld accept unban ${p.name}")
                    .append("          ")
                    .append("§c拒绝").hoverText("点击拒绝这个申请").runCommand("/chunkworld deny unban ${p.name}")
                    .sendTo(targetPlayer)
            }else p.sendMessage("§a该玩家不在线，暂时无法解除拉黑关系")
        }
        basic.openAsync()
    }
}