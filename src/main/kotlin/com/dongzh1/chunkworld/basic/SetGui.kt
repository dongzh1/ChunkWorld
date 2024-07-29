package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.redis.RedisData
import com.dongzh1.chunkworld.redis.RedisManager
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

class SetGui(val p:Player) {
    fun build(){
        val basic = PaperBasic(p, Component.text("世界设置"))
        val world = p.world
        basic.rows(4)
        val playerDao = RedisData.getPlayerDao(p.uniqueId.toString())?: ChunkWorld.db.playerGet(p.uniqueId)
        if (playerDao == null){
            p.closeInventory()
            p.sendMessage("§c你还没有自己的独立世界")
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
        basic.onClick(8) {
            p.closeInventory()
            ListGui(p,1,false).build()
        }
        basic.set(10, buildItem(Material.SOUL_CAMPFIRE, builder = {
            name = "设置世界传送点"
            lore.add("§f当传送到你的世界时")
            lore.add("§f会传送到这个位置")
            lore.add("§f当前传送点: §a${String.format("%.2f",playerDao.tX)}§b,§a${String.format("%.2f",playerDao.tY)}§b,§a${String.format("%.2f",playerDao.tZ)}")
            lore.add("§f点击设置当前位置为传送点")
        }))
        basic.onClick(10) {
            when (world.name) {
                "chunkworlds/world/${playerDao.uuid}" -> playerDao.teleport = "world,${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                "chunkworlds/nether/${playerDao.uuid}" -> playerDao.teleport = "nether,${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
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
        basic.set(12, buildItem(Material.RED_BED, builder = {
            name = "设置出生点"
            lore.add("§f当通过传送门到主世界时")
            lore.add("§f会传送到这个位置")
            lore.add("§f当前出生点: §a${String.format("%.2f",playerDao.wX)}§b,§a${String.format("%.2f",playerDao.wY)}§b,§a${String.format("%.2f",playerDao.wZ)}")
            lore.add("§f点击设置当前位置为出生点")
        }))
        basic.onClick(12) {
            if (world.name == "chunkworlds/world/${playerDao.uuid}") {
                //在自己世界，可以设置
                playerDao.spawn =
                    "${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
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
        if (playerDao.netherSpawn == "null"){
            basic.set(14, buildItem(Material.BEDROCK, builder = {
                name = "设置地狱出生点"
                lore.add("§f当通过传送门到地狱时")
                lore.add("§f会传送到这个位置")
                lore.add("§c独立地狱未解锁")
            }))
        }else{
            basic.set(14, buildItem(Material.RESPAWN_ANCHOR, builder = {
                name = "设置地狱出生点"
                lore.add("§f当通过传送门到地狱时")
                lore.add("§f会传送到这个位置")
                lore.add("§f当前地狱出生点: §a${String.format("%.2f",playerDao.nX)}§b,§a${String.format("%.2f",playerDao.nY)}§b,§a${String.format("%.2f",playerDao.nZ)}")
                lore.add("§f点击设置当前位置为地狱出生点")
            }))
            basic.onClick(14) {
                if (world.name == "chunkworlds/nether/${playerDao.uuid}") {
                    //在自己世界，可以设置
                    playerDao.netherSpawn =
                        "${p.location.x},${p.location.y},${p.location.z},${p.location.yaw},${p.location.pitch}"
                    submit(async = true) {
                        ChunkWorld.db.playerUpdate(playerDao)
                        //重新获取数据
                        RedisData.setPlayerDao(playerDao)
                    }
                    p.closeInventory()
                    p.sendMessage("§a地狱出生点设置成功")
                }else{
                    p.closeInventory()
                    p.sendMessage("§c请在自己的独立地狱设置出生点")
                }
            }
        }

        basic.set(16, buildItem(Material.VERDANT_FROGLIGHT, builder = {
            when(playerDao.worldStatus){
                0.toByte() -> {
                    name = "世界状态: §a公开世界"
                    lore.add("§f所有玩家都可以进入你的世界")
                    lore.add("§f点击切换为仅共享玩家可进入")
                }
                1.toByte() -> {
                    name = "世界状态: §e共享开放"
                    lore.add("§f只有共享世界的玩家可以进入你的世界")
                    lore.add("§f点击切换为仅自己可进入")
                }
                2.toByte() -> {
                    name = "世界状态: §c自己可进"
                    lore.add("§f只有你自己可以进入你的世界")
                    lore.add("§f点击切换为所有人可进入")
                }
            }
        }))
        basic.onClick(16) {
            playerDao.worldStatus = when(playerDao.worldStatus){
                0.toByte() -> 1.toByte()
                1.toByte() -> 2.toByte()
                2.toByte() -> 0.toByte()
                else -> 0.toByte()
            }
            submit(async = true) {
                ChunkWorld.db.playerUpdate(playerDao)
                //重新获取数据
                RedisData.setPlayerDao(playerDao)
            }
            p.closeInventory()
            p.sendMessage("§a世界状态已修改为${when(playerDao.worldStatus){
                0.toByte() -> "所有人可进入"
                1.toByte() -> "共享世界玩家可进入"
                2.toByte() -> "仅自己可进入"
                else -> "所有人可进入"
            }}")
        }
        if (world.name == "chunkworlds/world/${playerDao.uuid}" || world.name == "chunkworlds/nether/${playerDao.uuid}"){
            basic.set(22, buildItem(Material.PAPER, builder = {
                name = "人员列表"
                lore.add("§f可以查看你的世界中的玩家")
                lore.add("§f只能查看身处的世界")
            }))
            basic.onClick(22) {
                p.closeInventory()
                if (world.name == "chunkworlds/world/${playerDao.uuid}" || world.name == "chunkworlds/nether/${playerDao.uuid}") {
                    WorldPlayerGui(p,world,1).build()
                }else{
                    p.sendMessage("§c请在自己的世界查看")
                }
            }
            basic.set(27, buildItem(Material.PHANTOM_SPAWN_EGG, builder = {
                name = "§a生成幻翼"
                lore.addAll(listOf("§f对应规则:","§7doInsomnia","§f幻翼是否会在夜间生成",))
                if (world.getGameRuleValue(GameRule.DO_INSOMNIA) == true) lore.add("§f当前状态:§a允许生成")
                else lore.add("§f当前状态:§c禁止生成")
            }))
            basic.onClick(27) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_INSOMNIA,!world.getGameRuleValue(GameRule.DO_INSOMNIA)!!)
            }

            basic.set(28, buildItem(Material.CLOCK, builder = {
                name = "§a昼夜更替"
                lore.addAll(listOf("§f对应规则:","§7doDaylightCycle","§f是否进行昼夜更替和月相变化",))
                if (world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE) == true) lore.add("§f当前状态:§a时间走动")
                else lore.add("§f当前状态:§c时间停止")
            }))
            basic.onClick(28) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,!world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)!!)
            }
            basic.set(29, buildItem(Material.ELYTRA, builder = {
                name = "§a天气定格"
                lore.addAll(listOf("§f对应规则:","§7doWeatherCycle","§f天气是否变化",))
                if (world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE) == true) lore.add("§f当前状态:§a天气变化")
                else lore.add("§f当前状态:§c天气定格")
            }))
            basic.onClick(29) {
                p.closeInventory()
                p.sendMessage("§a世界规则已修改")
                world.setGameRule(GameRule.DO_WEATHER_CYCLE,!world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)!!)
            }
            basic.set(30, buildItem(Material.BLUE_ICE, builder = {
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
            basic.onClick(30) {
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
            basic.set(32, buildItem(Material.HONEY_BOTTLE, builder = {
                name = "§a游戏难度"
                lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§b和平","§c和平困难下所有怪物会被清空","§c请慎重决定"))
            }))
            basic.set(33, buildItem(Material.GLASS_BOTTLE, builder = {
                name = "§a游戏难度"
                lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§a简单","§f比较简单的怪物强度"))
            }))
            basic.set(34, buildItem(Material.EXPERIENCE_BOTTLE, builder = {
                name = "§a游戏难度"
                lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§6普通","§f有一定难度的怪物水平"))
            }))
            basic.set(35, buildItem(Material.DRAGON_BREATH, builder = {
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
                    basic.set(32,item)
                }
                Difficulty.EASY ->{
                    val item = buildItem(Material.GLASS_BOTTLE, builder = {
                        name = "§a游戏难度"
                        lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§a简单","§f比较简单的怪物强度"))
                    })
                    item.addUnsafeEnchantment(Enchantment.LURE,1)
                    item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    basic.set(33,item)
                }
                Difficulty.NORMAL ->{
                    val item = buildItem(Material.EXPERIENCE_BOTTLE, builder = {
                        name = "§a游戏难度"
                        lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§6普通","§f有一定难度的怪物水平"))
                    })
                    item.addUnsafeEnchantment(Enchantment.LURE,1)
                    item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    basic.set(34,item)
                }
                Difficulty.HARD ->{
                    val item = buildItem(Material.DRAGON_BREATH, builder = {
                        name = "§a游戏难度"
                        lore.addAll(listOf("§f对应规则:","§7difficulty","§f游戏困难度","§f设置为§c困难","§c最高的难度","§f有些事件只能在这个难度触发"))
                    })
                    item.addUnsafeEnchantment(Enchantment.LURE,1)
                    item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    basic.set(35,item)
                }
            }
            basic.onClick(32) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.PEACEFUL
            }
            basic.onClick(33) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.EASY
            }
            basic.onClick(34) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.NORMAL
            }
            basic.onClick(35) {
                p.closeInventory()
                p.sendMessage("§a世界难度已修改")
                world.difficulty = Difficulty.HARD
            }
        }
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.openAsync()
    }
}