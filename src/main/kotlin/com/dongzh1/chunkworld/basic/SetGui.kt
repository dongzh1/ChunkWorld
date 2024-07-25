package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.redis.RedisData
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.ItemBuilder
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType
import java.util.*

class SetGui(val p:Player,private val banPage:Int) {
    fun build(){
        val basic = PaperBasic(p, Component.text("世界设置"))
        val world = p.world
        basic.rows(6)
        val playerDao = RedisData.getPlayerDao(p.uniqueId.toString())
        if (playerDao == null){
            p.closeInventory()
            p.sendMessage("§c你的独立世界还没加载过")
            return
        }
        val chunkDao = RedisData.getAllChunksNum(p.uniqueId.toString())!!
        basic.set(4, buildItem(Material.PAINTING, builder = {
            name = "世界信息"
            lore.add("")
            lore.add("§f已解锁主世界区块: §a${chunkDao.first}")
            lore.add("§f已解锁地狱区块: §4${chunkDao.second}")
            lore.add("§f创建日期: §7${playerDao.createTime}")
        }))
        basic.set(8, buildItem(Material.BARRIER, builder = {
            name = "返回上级"
        }))
        basic.set(9, buildItem(Material.SOUL_CAMPFIRE, builder = {
            name = "设置世界传送点"
            lore.add("§f当传送到你的世界时")
            lore.add("§f会传送到这个位置")
            lore.add("§f当前传送点: §a${String.format("%.2f",playerDao.teleportX)}§b,§a${String.format("%.2f",playerDao.teleportY)}§b,§a${String.format("%.2f",playerDao.teleportZ)}")
            lore.add("§f点击设置当前位置为传送点")
        }))
        basic.set(10, buildItem(Material.RED_BED, builder = {
            name = "设置出生点"
            lore.add("§f当通过传送门到主世界时")
            lore.add("§f会传送到这个位置")
            lore.add("§f当前出生点: §a${String.format("%.2f",playerDao.normalX)}§b,§a${String.format("%.2f",playerDao.normalY)}§b,§a${String.format("%.2f",playerDao.normalZ)}")
            lore.add("§f点击设置当前位置为出生点")
        }))
        basic.set(11, buildItem(Material.RESPAWN_ANCHOR, builder = {
            name = "设置地狱出生点"
            lore.add("§f当通过传送门到地狱时")
            lore.add("§f会传送到这个位置")
            lore.add("§f当前地狱出生点: §a${String.format("%.2f",playerDao.netherX)}§b,§a${String.format("%.2f",playerDao.netherY)}§b,§a${String.format("%.2f",playerDao.netherZ)}")
            lore.add("§f点击设置当前位置为地狱出生点")
        }))
        basic.set(13, buildItem(Material.VERDANT_FROGLIGHT, builder = {
            name = "开放世界"
            lore.add("")
            lore.add("§f此模式下所有玩家都可进入你的世界")
        }))
        basic.set(15, buildItem(Material.OCHRE_FROGLIGHT, builder = {
            name = "部分开放"
            lore.add("")
            lore.add("§f此模式下只有被信任的玩家可进入你的世界")
        }))
        basic.set(17, buildItem(Material.PEARLESCENT_FROGLIGHT, builder = {
            name = "关闭世界"
            lore.add("")
            lore.add("§f此模式下只有你可以进入你的世界")
        }))
        when(playerDao.worldStatus){
            0.toByte() -> {
                val item = buildItem(Material.VERDANT_FROGLIGHT, builder = {
                    name = "开放世界"
                    lore.add("")
                    lore.add("§f此模式下所有玩家都可进入你的世界")
                    enchants[Enchantment.LURE] = 1
                    flags.add(ItemBuilder.ItemBuilderFlag.HIDE_ENCHANTS)
                })
                basic.set(13,item)
            }
            1.toByte() -> {
                val item = buildItem(Material.OCHRE_FROGLIGHT, builder = {
                    name = "部分开放"
                    lore.add("")
                    lore.add("§f此模式下只有被信任的玩家可进入你的世界")
                    enchants[Enchantment.LURE] = 1
                    flags.add(ItemBuilder.ItemBuilderFlag.HIDE_ENCHANTS)
                })
                basic.set(15,item)
            }
            2.toByte() -> {
                val item = buildItem(Material.PEARLESCENT_FROGLIGHT, builder = {
                    name = "关闭世界"
                    lore.add("")
                    lore.add("§f此模式下只有你可以进入你的世界")
                    enchants[Enchantment.LURE] = 1
                    flags.add(ItemBuilder.ItemBuilderFlag.HIDE_ENCHANTS)
                })
                basic.set(17,item)
            }
        }
        basic.set(18, buildItem(Material.SKELETON_SKULL, builder = {
            name = "信任玩家"
            lore.add("")
            lore.add("§f右侧是你的信任的玩家")
            lore.add("§f被信任的玩家可以在你的世界建造和破坏")
        }))
        basic.set(27, buildItem(Material.WITHER_SKELETON_SKULL, builder = {
            name = "拉黑玩家"
            lore.add("")
            lore.add("§f右侧是被你拉黑的玩家")
            lore.add("§f被拉黑的玩家无法进入你的世界")
        }))
        basic.set(35, buildItem(Material.ARROW, builder = {
            name = "§a下一页"
            lore.add("§f点击查看更多被拉黑的玩家")
        }))
        //团队玩家只能8人
        for (i in 19..26){
            basic.set(i,buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                name = "§a已解锁"
                lore.add("§f使用指令 /invite 玩家")
                lore.add("§f即可信任该玩家")
            }))
        }
        for (i in 28..34){
            basic.set(i,buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                name = "§a已解锁"
                lore.add("§f使用指令 /wban 玩家")
                lore.add("§f即可拉黑该玩家")
            }))
        }
        val ship = RedisData.getFriendsAndBanner(p.uniqueId.toString())!!
        val trustList = ship.first.toList()
        val banList = if (banPage == 1) ship.second.take(banPage*7-1).toList()
        else ship.second.drop((banPage-1)*7-1).take(banPage*7-1).toList()

        for (i in trustList.indices){
            val pname = Bukkit.getOfflinePlayer(UUID.fromString(trustList[i])).name?:RedisData.getPlayerDao(trustList[i])?.name?:ChunkWorld.db.playerGet(UUID.fromString(trustList[i]))?.name!!
            basic.set(19+i, buildItem(Material.PLAYER_HEAD, builder = {
                name = "§a$pname"
                skullOwner = pname
                lore.add("§b点击不再信任该玩家")
            }))
        }
        for (i in banList.indices){
            val pname = Bukkit.getOfflinePlayer(UUID.fromString(banList[i])).name?:RedisData.getPlayerDao(banList[i])?.name?:ChunkWorld.db.playerGet(UUID.fromString(banList[i]))?.name!!
            basic.set(28+i, buildItem(Material.PLAYER_HEAD, builder = {
                name = "§a$pname"
                skullOwner = pname
                lore.add("§b点击申请解除相互拉黑关系")
            }))
        }

        if (world.name == ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world" || world.name == ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/nether"){
            basic.set(36, buildItem(Material.PAPER, builder = {
                name = "世界规则"
                lore.add("§f可以修改你的世界的规则")
                lore.add("§f并附带有较为详细的说明")
            }))
            basic.set(45, buildItem(Material.PHANTOM_SPAWN_EGG, builder = {
                name = "§a生成幻翼"
                lore.addAll(listOf("§f对应规则:","§7doInsomnia","§f幻翼是否会在夜间生成",))
                if (world.getGameRuleValue(GameRule.DO_INSOMNIA) == true) lore.add("§f当前状态:§a允许生成")
                else lore.add("§f当前状态:§c禁止生成")
            }))
            basic.set(46, buildItem(Material.CLOCK, builder = {
                name = "§a昼夜更替"
                lore.addAll(listOf("§f对应规则:","§7doDaylightCycle","§f是否进行昼夜更替和月相变化",))
                if (world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE) == true) lore.add("§f当前状态:§a时间走动")
                else lore.add("§f当前状态:§c时间停止")
            }))
            basic.set(47, buildItem(Material.ELYTRA, builder = {
                name = "§a天气定格"
                lore.addAll(listOf("§f对应规则:","§7doWeatherCycle","§f天气是否变化",))
                if (world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE) == true) lore.add("§f当前状态:§a天气变化")
                else lore.add("§f当前状态:§c天气定格")
            }))
            basic.set(48, buildItem(Material.BLUE_ICE, builder = {
                name = "§a液体流动"
                lore.addAll(listOf("§f对应规则:","§8dong自定义的规则","§f世界的液体是否流动",))

                //先看世界有没有存这个key，没有说明没有添加规则，可以流动
                if (world.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_fluid")!!)){
                    if (world.persistentDataContainer.get(NamespacedKey.fromString("chunkworld_fluid")!!,
                            PersistentDataType.BOOLEAN) == true){
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
                world.setGameRule(GameRule.DO_INSOMNIA,!world.getGameRuleValue(GameRule.DO_INSOMNIA)!!)
            }
            basic.onClick(46) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,!world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)!!)
            }
            basic.onClick(47) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_WEATHER_CYCLE,!world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)!!)
            }
            basic.onClick(48) {
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
        basic.onClick(9) {
            when (world.name) {
                ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world" -> playerDao.teleport = "world,${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/nether" -> playerDao.teleport = "nether,${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                else -> {
                    p.closeInventory()
                    p.sendMessage("§c请在自己的主世界或地狱设置传送点")
                    return@onClick
                }
            }
            //在自己世界，可以设置
            submit(async = true) {
                ChunkWorld.db.playerUpdate(playerDao)
                //重新获取数据
                RedisData.setPlayerDao(playerDao)
            }
            p.closeInventory()
            p.sendMessage("§a世界传送点设置成功")
        }
        basic.onClick(10) {
            if (world.name == ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world"){
                //在自己世界，可以设置
                playerDao.spawn = "${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                p.world.spawnLocation = p.location
                submit(async = true) {
                    ChunkWorld.db.playerUpdate(playerDao)
                    //重新获取数据
                    RedisData.setPlayerDao(playerDao)
                }
                p.closeInventory()
                p.sendMessage("§a主世界出生点设置成功")
            }else{
                p.closeInventory()
                p.sendMessage("§c请在自己的主世界设置出生点")
            }
        }
        basic.onClick(11) {
            if (world.name == ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/nether"){
                //在自己世界，可以设置
                playerDao.netherSpawn = "${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                submit(async = true) {
                    ChunkWorld.db.playerUpdate(playerDao)
                    //重新获取数据
                    RedisData.setPlayerDao(playerDao)
                }
                p.closeInventory()
                p.sendMessage("§a地狱出生点设置成功")
            }else{
                p.closeInventory()
                p.sendMessage("§c请在自己的地狱设置出生点")
            }
        }
        basic.onClick(13) {
            val item = it.view.getItem(13)
            item!!.addUnsafeEnchantment(Enchantment.LURE, 1)
            item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(13, item)
            val item14 = it.view.getItem(15)
            item14!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(15, item14)
            val item16 = it.view.getItem(17)
            item16!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(17, item16)
            playerDao.worldStatus = 0
            RedisData.setPlayerDao(playerDao)
            //数据库
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
        }
        basic.onClick(15) {
            val item = it.view.getItem(15)
            item!!.addUnsafeEnchantment(Enchantment.LURE,1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(15,item)
            val item12 = it.view.getItem(13)
            item12!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(13,item12)
            val item16 = it.view.getItem(17)
            item16!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(17,item16)
            playerDao.worldStatus = 1
            RedisData.setPlayerDao(playerDao)
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
        }
        basic.onClick(17) {
            val item = it.view.getItem(17)
            item!!.addUnsafeEnchantment(Enchantment.LURE,1)
            item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(17,item)
            val item14 = it.view.getItem(15)
            item14!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(15,item14)
            val item12 = it.view.getItem(13)
            item12!!.removeEnchantment(Enchantment.LURE)
            it.view.setItem(13,item12)
            playerDao.worldStatus = 2
            RedisData.setPlayerDao(playerDao)
            submit(async = true) { ChunkWorld.db.playerUpdate(playerDao) }
        }
        basic.onClick(listOf(19,20,21,22,23,24,25,26)) {
            //不是玻璃板
            //todo
            //从redis调取双方数据，数据库也要，然后删除，然后跨服发送消息
            if (it.view.getItem(it.slot)!!.type == Material.LIME_STAINED_GLASS_PANE) return@onClick
            //根据列表对应获取玩家
            val uuid = trustList[it.slot-19]
            submit(async = true) {
                ChunkWorld.db.removeShip(playerDao.id,uuid,true)
                RedisData.setShip(p.uniqueId.toString(),ChunkWorld.db.getShip(playerDao.id,true).map { it.uuid }.toSet(),ChunkWorld.db.getShip(playerDao.id,false).map { it.uuid }.toSet())
                if (RedisManager.shipHas(uuid)){
                    val targetDao = RedisData.getPlayerDao(uuid)?:ChunkWorld.db.playerGet(UUID.fromString(uuid))
                }
            }
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