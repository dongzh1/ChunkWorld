package com.dongzh1.chunkworld.basic

import com.cryptomorin.xseries.XMaterial
import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class SettingGui(private val p: Player,private val banPage:Int) {
    fun build() {
        val world = Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}")
        val basic = PaperBasic(p, Component.text("世界设置"))
        basic.rows(6)
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
                item.addUnsafeEnchantment(Enchantment.LURE,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                basic.set(12,item)
            }
            1.toByte() -> {
                val item = Item.build(Material.OCHRE_FROGLIGHT,1,"部分开放", listOf("",
                    "§f此模式下只有被信任的玩家可进入你的世界"),-1)
                item.addUnsafeEnchantment(Enchantment.LURE,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                basic.set(14,item)
            }
            2.toByte() -> {
                val item = Item.build(Material.PEARLESCENT_FROGLIGHT,1,"关闭世界", listOf("",
                    "§f此模式下只有你可以进入你的世界"),-1)
                item.addUnsafeEnchantment(Enchantment.LURE,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                basic.set(16,item)
            }
        }
        basic.set(18,Item.build(Material.SKELETON_SKULL,1,"信任玩家", listOf("",
            "§f右侧是你的信任的玩家",
            "§f被信任的玩家可以在你的世界建造和破坏"),-1))
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
                listOf("§f使用指令 /wban 玩家","§f即可拉黑该玩家"),-1))
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

        if (world != null){
            basic.set(40,Item.build(Material.MOJANG_BANNER_PATTERN,1,"世界规则",
                listOf("§f可以修改你的世界的规则","§f并附带有较为详细的说明"),-1))
            basic.set(45, buildItem(Material.CREEPER_HEAD, builder = {
                name = "§a生物破坏"
                lore.addAll(listOf("§f对应规则:","§7mobGriefing","§f所有生物都不能破坏地形","§f包括苦力怕爆炸或村民收割作物",))
                if (world.getGameRuleValue(GameRule.MOB_GRIEFING) == true) lore.add("§f当前状态:§a允许破坏")
                else lore.add("§f当前状态:§c禁止破坏")
            }))
            basic.set(46, buildItem(Material.PHANTOM_SPAWN_EGG, builder = {
                name = "§a生成幻翼"
                lore.addAll(listOf("§f对应规则:","§7doInsomnia","§f幻翼是否会在夜间生成",))
                if (world.getGameRuleValue(GameRule.DO_INSOMNIA) == true) lore.add("§f当前状态:§a允许生成")
                else lore.add("§f当前状态:§c禁止生成")
            }))
            basic.set(47, buildItem(Material.CLOCK, builder = {
                name = "§a昼夜更替"
                lore.addAll(listOf("§f对应规则:","§7doDaylightCycle","§f是否进行昼夜更替和月相变化",))
                if (world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE) == true) lore.add("§f当前状态:§a时间走动")
                else lore.add("§f当前状态:§c时间停止")
            }))
            basic.set(48, buildItem(Material.ELYTRA, builder = {
                name = "§a天气定格"
                lore.addAll(listOf("§f对应规则:","§7doWeatherCycle","§f天气是否变化",))
                if (world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE) == true) lore.add("§f当前状态:§a天气变化")
                else lore.add("§f当前状态:§c天气定格")
            }))
            basic.set(49, buildItem(Material.BLUE_ICE, builder = {
                name = "§a液体流动"
                lore.addAll(listOf("§f对应规则:","§8dong自定义的规则","§f世界的液体是否流动",))

                //先看世界有没有存这个key，没有说明没有添加规则，可以流动
                if (world.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_fluid")!!)){
                    if (world.persistentDataContainer.get(NamespacedKey.fromString("chunkworld_fluid")!!,PersistentDataType.BOOLEAN) == true){
                        //有标记，且可以流动
                        lore.add("§f当前状态:§a液体流动")
                    }else lore.add("§f当前状态:§c不再流动")
                } else lore.add("§f当前状态:§a液体流动")

            }))
            basic.set(50, buildItem(Material.HONEY_BOTTLE, builder = {
                name = "§a游戏难度"
                lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§b和平","§c和平困难下所有怪物会被清空","§c请慎重决定"))
            }))
            basic.set(51, buildItem(Material.GLASS_BOTTLE, builder = {
                name = "§a游戏难度"
                lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§a简单","§f比较简单的怪物强度"))
            }))
            basic.set(52, buildItem(Material.EXPERIENCE_BOTTLE, builder = {
                name = "§a游戏难度"
                lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§6普通","§f有一定难度的怪物水平"))
            }))
            basic.set(53, buildItem(Material.DRAGON_BREATH, builder = {
                name = "§a游戏难度"
                lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§c困难","§c最高的难度","§f有些事件只能在这个难度触发"))
            }))
            when(world.difficulty){
                Difficulty.PEACEFUL ->{
                    val item = buildItem(Material.HONEY_BOTTLE, builder = {
                        name = "§a游戏难度"
                        lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§b和平","§c和平困难下所有怪物会被清空","§c请慎重决定"))
                    })
                    item.addUnsafeEnchantment(Enchantment.LURE,1)
                    item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    basic.set(50,item)
                }
                Difficulty.EASY ->{
                    val item = buildItem(Material.GLASS_BOTTLE, builder = {
                        name = "§a游戏难度"
                        lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§a简单","§f比较简单的怪物强度"))
                    })
                    item.addUnsafeEnchantment(Enchantment.LURE,1)
                    item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    basic.set(51,item)
                }
                Difficulty.NORMAL ->{
                    val item = buildItem(Material.EXPERIENCE_BOTTLE, builder = {
                        name = "§a游戏难度"
                        lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§6普通","§f有一定难度的怪物水平"))
                    })
                    item.addUnsafeEnchantment(Enchantment.LURE,1)
                    item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    basic.set(52,item)
                }
                Difficulty.HARD ->{
                    val item = buildItem(Material.DRAGON_BREATH, builder = {
                        name = "§a游戏难度"
                        lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§c困难","§c最高的难度","§f有些事件只能在这个难度触发"))
                    })
                    item.addUnsafeEnchantment(Enchantment.LURE,1)
                    item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    basic.set(53,item)
                }
            }
            basic.onClick(45) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.MOB_GRIEFING,!world.getGameRuleValue(GameRule.MOB_GRIEFING)!!)
            }
            basic.onClick(46) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_INSOMNIA,!world.getGameRuleValue(GameRule.DO_INSOMNIA)!!)
            }
            basic.onClick(47) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,!world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)!!)
            }
            basic.onClick(48) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_WEATHER_CYCLE,!world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)!!)
            }
            basic.onClick(49) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                //先看世界有没有存这个key，没有说明没有添加规则，可以流动
                if (world.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_fluid")!!)){
                    //有标记
                    if (world.persistentDataContainer.get(NamespacedKey.fromString("chunkworld_fluid")!!,PersistentDataType.BOOLEAN) == true){
                        //标记是可以流动，改为不行
                        world.persistentDataContainer.set(NamespacedKey.fromString("chunkworld_fluid")!!,PersistentDataType.BOOLEAN,false)
                    }else world.persistentDataContainer.set(NamespacedKey.fromString("chunkworld_fluid")!!,PersistentDataType.BOOLEAN,true)
                } else world.persistentDataContainer.set(NamespacedKey.fromString("chunkworld_fluid")!!,PersistentDataType.BOOLEAN,false)
            }
            basic.onClick(50) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.PEACEFUL
            }
            basic.onClick(51) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.EASY
            }
            basic.onClick(52) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.NORMAL
            }
            basic.onClick(53) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.HARD
            }
        }
        basic.onClick { event ->
            event.isCancelled = true
        }

        basic.onClick(8) { ListGui(p,1,false).build() }

        basic.onClick(10) {
            if (p.location.world.name == ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}"){
                //在自己世界，可以设置
                playerDao.spawn = "${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                p.world.spawnLocation = p.location
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
            item!!.addUnsafeEnchantment(Enchantment.LURE, 1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(12, item)
            val item14 = it.view.getItem(14)
            item14!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(14, item14)
            val item16 = it.view.getItem(16)
            item16!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(16, item16)
            playerDao.worldStatus = 0
            Listener.setPlayerDaoMap(p.name, playerDao)
            //数据库
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
        }
        basic.onClick(14) {
            val item = it.view.getItem(14)
            item!!.addUnsafeEnchantment(Enchantment.LURE,1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(14,item)
            val item12 = it.view.getItem(12)
            item12!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(12,item12)
            val item16 = it.view.getItem(16)
            item16!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(16,item16)
            playerDao.worldStatus = 1
            Listener.setPlayerDaoMap(p.name, playerDao)
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
        }
        basic.onClick(16) {
            val item = it.view.getItem(16)
            item!!.addUnsafeEnchantment(Enchantment.LURE,1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(16,item)
            val item14 = it.view.getItem(14)
            item14!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(14,item14)
            val item12 = it.view.getItem(12)
            item12!!.removeEnchantment(Enchantment.LURE)
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