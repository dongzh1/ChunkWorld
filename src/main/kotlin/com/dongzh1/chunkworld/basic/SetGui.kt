package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.persistence.PersistentDataType

class SetGui(val p: Player) {
    fun build() {
        val basic = PaperBasic(p, Component.text("世界设置"))
        val world = p.world
        if (world.name != "chunkworlds/world/${p.uniqueId}" && world.name != "chunkworlds/nether/${p.uniqueId}") {
            p.sendMessage("§c请在自己的世界设置")
            return
        }
        basic.rows(6)
        val worldInfo = RedisManager.getWorldInfo(p.name)!!
        basic.set(4, buildItem(Material.PAINTING, builder = {
            name = "世界信息"
            lore.add("")
            lore.add("§f已解锁主世界区块: §a${worldInfo.normalChunks}")
            lore.add("§f已解锁地狱区块: §4${worldInfo.netherChunks}")
        }))
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name = "§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        basic.onClick(8) { p.closeInventory() }
        basic.set(17, buildItem(Material.PAPER, builder = {
            customModelData = 300010
            name = "§f빪 §x§1§9§c§a§a§d➠ 返回上级菜单"
        }))
        basic.onClick(17) {
            MainGui(p).build()
        }
        basic.set(21, buildItem(Material.RED_BED, builder = {
            name = "§f设置此位置为出生点"
            lore.add("§f传送时也会传送到这里")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 设置出生点")
        }))
        basic.onClick(21) {
            //在自己世界，可以设置
            world.spawnLocation = p.location
            p.closeInventory()
            p.sendMessage("§a主世界出生点设置成功")

        }
        basic.set(23, buildItem(Material.VERDANT_FROGLIGHT, builder = {
            when (worldInfo.state) {
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
        basic.onClick(23) {
            worldInfo.state = when (worldInfo.state) {
                0.toByte() -> 1.toByte()
                1.toByte() -> 2.toByte()
                2.toByte() -> 0.toByte()
                else -> 0.toByte()
            }
            world.persistentDataContainer.set(
                NamespacedKey.fromString("chunkworld_state")!!,
                PersistentDataType.BYTE, worldInfo.state
            )
            submit(async = true) {
                RedisManager.setWorldInfo(p.name, worldInfo)
            }
            p.closeInventory()
            p.sendMessage(
                "§a世界状态已修改为${
                    when (worldInfo.state) {
                        0.toByte() -> "所有人可进入"
                        1.toByte() -> "共享世界玩家可进入"
                        2.toByte() -> "仅自己可进入"
                        else -> "所有人可进入"
                    }
                }"
            )
        }
        basic.set(31, buildItem(Material.NAME_TAG, builder = {
            name = "人员列表"
            lore.add("§f可以查看你的世界中的玩家")
            lore.add("§f只能查看身处的世界")
        }))
        basic.onClick(31) {
            p.closeInventory()
            WorldPlayerGui(p, world, 1).build()
        }
        basic.set(27, buildItem(Material.PHANTOM_SPAWN_EGG, builder = {
            name = "§a生成幻翼"
            lore.addAll(listOf("§f对应规则:", "§7doInsomnia", "§f幻翼是否会在夜间生成"))
            if (world.getGameRuleValue(GameRule.DO_INSOMNIA) == true) lore.add("§f当前状态:§a允许生成")
            else lore.add("§f当前状态:§c禁止生成")
        }))
        basic.onClick(27) {
            p.closeInventory()
            p.sendMessage("§a世界规则已修改")
            world.setGameRule(GameRule.DO_INSOMNIA, !world.getGameRuleValue(GameRule.DO_INSOMNIA)!!)
        }
        basic.set(28, buildItem(Material.CLOCK, builder = {
            name = "§a昼夜更替"
            lore.addAll(listOf("§f对应规则:", "§7doDaylightCycle", "§f是否进行昼夜更替和月相变化"))
            if (world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE) == true) lore.add("§f当前状态:§a时间走动")
            else lore.add("§f当前状态:§c时间停止")
        }))
        basic.onClick(28) {
            p.closeInventory()
            p.sendMessage("§a世界规则已修改")
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, !world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)!!)
        }
        basic.set(29, buildItem(Material.ELYTRA, builder = {
            name = "§a天气定格"
            lore.addAll(listOf("§f对应规则:", "§7doWeatherCycle", "§f天气是否变化"))
            if (world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE) == true) lore.add("§f当前状态:§a天气变化")
            else lore.add("§f当前状态:§c天气定格")
        }))
        basic.onClick(29) {
            p.closeInventory()
            p.sendMessage("§a世界规则已修改")
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, !world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)!!)
        }
        basic.set(30, buildItem(Material.BLUE_ICE, builder = {
            name = "§a液体流动"
            lore.addAll(listOf("§f对应规则:", "§8dong自定义的规则", "§f世界的液体是否流动"))
            //先看世界有没有存这个key，没有说明没有添加规则，可以流动
            if (world.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_fluid")!!)) {
                if (world.persistentDataContainer.get(
                        NamespacedKey.fromString("chunkworld_fluid")!!,
                        PersistentDataType.BOOLEAN
                    ) == true
                ) {
                    //有标记，且可以流动
                    lore.add("§f当前状态:§a液体流动")
                } else lore.add("§f当前状态:§c不再流动")
            } else lore.add("§f当前状态:§a液体流动")
        }))
        basic.onClick(30) {
            p.closeInventory()
            p.sendMessage("§a世界规则已修改")
            //先看世界有没有存这个key，没有说明没有添加规则，可以流动
            if (world.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_fluid")!!)) {
                //有标记
                if (world.persistentDataContainer.get(
                        NamespacedKey.fromString("chunkworld_fluid")!!,
                        PersistentDataType.BOOLEAN
                    ) == true
                ) {
                    //标记是可以流动，改为不行
                    world.persistentDataContainer.set(
                        NamespacedKey.fromString("chunkworld_fluid")!!,
                        PersistentDataType.BOOLEAN,
                        false
                    )
                } else world.persistentDataContainer.set(
                    NamespacedKey.fromString("chunkworld_fluid")!!,
                    PersistentDataType.BOOLEAN,
                    true
                )
            } else world.persistentDataContainer.set(
                NamespacedKey.fromString("chunkworld_fluid")!!,
                PersistentDataType.BOOLEAN,
                false
            )
        }
        basic.set(32, buildItem(Material.HONEY_BOTTLE, builder = {
            name = "§a游戏难度"
            lore.addAll(
                listOf(
                    "§f对应规则:",
                    "§7difficulty",
                    "§f游戏困难度",
                    "§f设置为§b和平",
                    "§c和平困难下所有怪物会被清空",
                    "§c请慎重决定"
                )
            )
        }))
        basic.set(33, buildItem(Material.GLASS_BOTTLE, builder = {
            name = "§a游戏难度"
            lore.addAll(listOf("§f对应规则:", "§7difficulty", "§f游戏困难度", "§f设置为§a简单", "§f比较简单的怪物强度"))
        }))
        basic.set(34, buildItem(Material.EXPERIENCE_BOTTLE, builder = {
            name = "§a游戏难度"
            lore.addAll(
                listOf(
                    "§f对应规则:",
                    "§7difficulty",
                    "§f游戏困难度",
                    "§f设置为§6普通",
                    "§f有一定难度的怪物水平"
                )
            )
        }))
        basic.set(35, buildItem(Material.DRAGON_BREATH, builder = {
            name = "§a游戏难度"
            lore.addAll(
                listOf(
                    "§f对应规则:",
                    "§7difficulty",
                    "§f游戏困难度",
                    "§f设置为§c困难",
                    "§c最高的难度",
                    "§f有些事件只能在这个难度触发"
                )
            )
        }))
        when (world.difficulty) {
            Difficulty.PEACEFUL -> {
                val item = buildItem(Material.HONEY_BOTTLE, builder = {
                    name = "§a游戏难度"
                    lore.addAll(
                        listOf(
                            "§f对应规则:",
                            "§7difficulty",
                            "§f游戏困难度",
                            "§f设置为§b和平",
                            "§c和平困难下所有怪物会被清空",
                            "§c请慎重决定"
                        )
                    )
                })
                item.addUnsafeEnchantment(Enchantment.LURE, 1)
                item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                basic.set(32, item)
            }

            Difficulty.EASY -> {
                val item = buildItem(Material.GLASS_BOTTLE, builder = {
                    name = "§a游戏难度"
                    lore.addAll(
                        listOf(
                            "§f对应规则:",
                            "§7difficulty",
                            "§f游戏困难度",
                            "§f设置为§a简单",
                            "§f比较简单的怪物强度"
                        )
                    )
                })
                item.addUnsafeEnchantment(Enchantment.LURE, 1)
                item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                basic.set(33, item)
            }

            Difficulty.NORMAL -> {
                val item = buildItem(Material.EXPERIENCE_BOTTLE, builder = {
                    name = "§a游戏难度"
                    lore.addAll(
                        listOf(
                            "§f对应规则:",
                            "§7difficulty",
                            "§f游戏困难度",
                            "§f设置为§6普通",
                            "§f有一定难度的怪物水平"
                        )
                    )
                })
                item.addUnsafeEnchantment(Enchantment.LURE, 1)
                item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                basic.set(34, item)
            }

            Difficulty.HARD -> {
                val item = buildItem(Material.DRAGON_BREATH, builder = {
                    name = "§a游戏难度"
                    lore.addAll(
                        listOf(
                            "§f对应规则:",
                            "§7difficulty",
                            "§f游戏困难度",
                            "§f设置为§c困难",
                            "§c最高的难度",
                            "§f有些事件只能在这个难度触发"
                        )
                    )
                })
                item.addUnsafeEnchantment(Enchantment.LURE, 1)
                item.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                basic.set(35, item)
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
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.openAsync()
    }
}