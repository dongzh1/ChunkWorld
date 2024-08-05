package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.basic.Biome.chinese
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.time.Duration

class ExpandGui(private val p: Player, private val chunk: Chunk) {
    private var choose = 0
    private var canClose = false
    private val worldisNormal = chunk.world.environment == World.Environment.NORMAL
    private val item30t = buildItem(Material.LIME_CONCRETE, builder = {
        name = "§a确认生成"
        lore.add("§a生成的区块将会有宝箱")
        lore.add("§a宝箱位置为 0 层以下")
    })
    private val item30f = buildItem(Material.LIME_CONCRETE, builder = {
        name = "§c确认生成"
        lore.add("§c当前未选择区块!")
        lore.add("§a此时确认将扩展空间，按照第1个区块生成")
        lore.add("§a生成的区块将会有宝箱")
    })
    private val item31 = buildItem(Material.YELLOW_CONCRETE, builder = {
        name = "§4生成虚空"
        lore.add("§4点此将不改变此区块样貌§8(§c虚空§8)")
        lore.add("§4但会将§b群系§4修改为")
        lore.add("§4选择的区块同款§b群系")
        lore.add("§4不选择则为第1个")
        lore.add("§c此方法不会生成宝箱")
    })
    private val item32 = buildItem(Material.RED_CONCRETE, builder = {
        name = "§c再次抽取"
        lore.add("§f消耗 §b1 §f个区块碎片重新组合区块")
    })

    fun build() {
        val basic = PaperBasic(p, Component.text("生成区块"))
        var chunk1 = p.chunk
        var chunk2 = p.chunk
        var chunk3 = p.chunk
        //设置菜单大小为行
        basic.rows(4)
        basic.set(30, item30f)
        basic.set(31, item31)
        basic.set(32, item32)

        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.onClick(11) {
            choose = 11
            val itemStack = it.view.getItem(11)
            if (itemStack != null) {
                val item = itemStack.clone()
                item.addUnsafeEnchantment(Enchantment.LURE, 1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                it.view.setItem(11, item)
                val item13 = it.view.getItem(13)
                if (item13 != null) {
                    val i13 = item13.clone()
                    i13.removeEnchantment(Enchantment.LURE)
                    it.view.setItem(13, i13)
                }
                val item15 = it.view.getItem(15)
                if (item15 != null) {
                    val i15 = item15.clone()
                    i15.removeEnchantment(Enchantment.LURE)
                    it.view.setItem(15, i15)
                }
            }
            it.view.setItem(30, item30t)
        }
        basic.onClick(13) {
            choose = 13
            val itemStack = it.view.getItem(13)
            if (itemStack != null) {
                val item = itemStack.clone()
                item.addUnsafeEnchantment(Enchantment.LURE, 1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                it.view.setItem(13, item)
                val item11 = it.view.getItem(11)
                if (item11 != null) {
                    val i11 = item11.clone()
                    i11.removeEnchantment(Enchantment.LURE)
                    it.view.setItem(11, i11)
                }
                val item15 = it.view.getItem(15)
                if (item15 != null) {
                    val i15 = item15.clone()
                    i15.removeEnchantment(Enchantment.LURE)
                    it.view.setItem(15, i15)
                }
            }
            it.view.setItem(30, item30t)
        }
        basic.onClick(15) {
            choose = 15
            val itemStack = it.view.getItem(15)
            if (itemStack != null) {
                val item = itemStack.clone()
                item.addUnsafeEnchantment(Enchantment.LURE, 1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                it.view.setItem(15, item)
                val item13 = it.view.getItem(13)
                if (item13 != null) {
                    val i13 = item13.clone()
                    i13.removeEnchantment(Enchantment.LURE)
                    it.view.setItem(13, i13)
                }
                val item11 = it.view.getItem(11)
                if (item11 != null) {
                    val i11 = item11.clone()
                    i11.removeEnchantment(Enchantment.LURE)
                    it.view.setItem(11, i11)
                }
            }
            it.view.setItem(30, item30t)
        }

        basic.onClose {
            if (!canClose) build(
                chunk1,
                chunk2,
                chunk3,
                it.view.getItem(11)!!,
                it.view.getItem(13)!!,
                it.view.getItem(15)!!
            )
        }

        var n = 0
        val chunkSlot = listOf(11, 13, 15)
        chunkSlot.forEach { slot ->
            val randomX = (-1000..1000).random()
            val randomZ = (-1000..1000).random()
            val resourceWorld =
                if (worldisNormal) Bukkit.getWorld("chunkworld")!! else Bukkit.getWorld("world_nether")!!
            resourceWorld.getChunkAtAsync(randomX, randomZ).thenAccept {
                //获取区块内的某个方块
                var block1: Block = resourceWorld.getBlockAt(randomX * 16, 0, randomZ * 16)
                if (worldisNormal) {
                    block1 = resourceWorld.getHighestBlockAt(
                        randomX * 16 + (0..15).random(),
                        randomZ * 16 + (0..15).random()
                    )
                } else {
                    for (y in 64..128) {
                        block1 = resourceWorld.getBlockAt(
                            randomX * 16 + (0..15).random(),
                            y,
                            randomZ * 16 + (0..15).random()
                        )
                        if (block1.type != Material.AIR && block1.type != Material.CAVE_AIR && block1.type != Material.VOID_AIR) {
                            break
                        }
                    }
                }
                val type = when (block1.type) {
                    Material.LAVA -> Material.LAVA_BUCKET
                    Material.WATER -> Material.WATER_BUCKET
                    else -> block1.type
                }
                val biome1 = block1.biome
                val biome2 = resourceWorld.getBiome(
                    randomX * 16 + (0..15).random(), (0..30).random(), randomZ * 16 + randomZ * 16 + (0..15).random()
                )
                val biome3 = if (worldisNormal) resourceWorld.getBiome(
                    randomX * 16 + (0..15).random(),
                    (-40..0).random(),
                    randomZ * 16 + randomZ * 16 + (0..15).random()
                )
                else resourceWorld.getBiome(
                    randomX * 16 + (0..15).random(),
                    (30..64).random(),
                    randomZ * 16 + randomZ * 16 + (0..15).random()
                )

                //300,0,-30各取一个点进行测算
                basic.set(slot, buildItem(type, builder = {
                    name = "§a选择此区块"
                    lore.add("§f此区块初步探明的群系有:")
                    lore.add(biome1.chinese)
                    lore.add(biome2.chinese)
                    lore.add(biome3.chinese)
                    lore.add("§f以上群系仅为三点探测所得")
                    lore.add("§f真实群系还需生成后探索")
                }))
                n++
                when (slot) {
                    11 -> chunk1 = it
                    13 -> chunk2 = it
                    15 -> chunk3 = it
                }
                if (n == 3) {
                    //确认和重新随机
                    closeAndConfirm(basic, chunk1, chunk2, chunk3)
                    basic.openAsync()
                    p.clearTitle()
                }
            }
        }

    }

    private fun build(
        chunk1: Chunk,
        chunk2: Chunk,
        chunk3: Chunk,
        item11: ItemStack,
        item13: ItemStack,
        item15: ItemStack
    ) {
        val basic = PaperBasic(p, Component.text("生成区块"))
        //设置菜单大小为行
        basic.rows(4)
        if (choose != 0)
            basic.set(30, item30t)
        else
            basic.set(30, item30f)
        basic.set(32, item32)
        basic.set(31, item31)
        basic.set(11, item11)
        basic.set(13, item13)
        basic.set(15, item15)
        basic.onClick { event ->
            event.isCancelled = true
        }

        basic.onClick(11) {
            choose = 11
            item11.addUnsafeEnchantment(Enchantment.LURE, 1)
            item11.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(11, item11)
            item13.removeEnchantment(Enchantment.LURE)
            it.view.setItem(13, item13)
            item15.removeEnchantment(Enchantment.LURE)
            it.view.setItem(15, item15)
            it.view.setItem(30, item30f)
        }
        basic.onClick(13) {
            choose = 13
            item13.addUnsafeEnchantment(Enchantment.LURE, 1)
            item13.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(13, item13)
            item11.removeEnchantment(Enchantment.LURE)
            it.view.setItem(11, item11)
            item15.removeEnchantment(Enchantment.LURE)
            it.view.setItem(15, item15)
            it.view.setItem(30, item30f)
        }
        basic.onClick(15) {
            choose = 15
            item15.addUnsafeEnchantment(Enchantment.LURE, 1)
            item15.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(15, item15)
            item13.removeEnchantment(Enchantment.LURE)
            it.view.setItem(13, item13)
            item11.removeEnchantment(Enchantment.LURE)
            it.view.setItem(11, item11)
            it.view.setItem(30, item30f)
        }
        closeAndConfirm(basic, chunk1, chunk2, chunk3)
        basic.onClose {
            if (!canClose) build(
                chunk1,
                chunk2,
                chunk3,
                it.view.getItem(11)!!,
                it.view.getItem(13)!!,
                it.view.getItem(15)!!
            )
        }
        basic.openAsync()

    }

    private fun confirm(chunk1: Chunk, chunk2: Chunk, chunk3: Chunk) {
        canClose = true
        p.closeInventory()
        //生成区块
        val sourceChunk = when (choose) {
            11 -> chunk1
            13 -> chunk2
            15 -> chunk3
            else -> chunk1
        }
        WorldEdit.copyChunk(sourceChunk, chunk, p)
        record()
        if (worldisNormal)
            p.showTitle(
                Title.title(
                    Component.text("§e恭喜您"),
                    Component.text("§f区块 ${chunk.x} ${chunk.z} 已掌握,在0层以下已生成§b区块宝箱"),
                    Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
                )
            )
        else
            p.showTitle(
                Title.title(
                    Component.text("§e恭喜您"), Component.text("§4地狱§f区块 ${chunk.x} ${chunk.z} 已掌握"),
                    Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
                )
            )
    }

    private fun confirmVoid(chunk1: Chunk, chunk2: Chunk, chunk3: Chunk) {
        canClose = true
        p.closeInventory()
        //生成区块
        val sourceChunk = when (choose) {
            11 -> chunk1
            13 -> chunk2
            15 -> chunk3
            else -> chunk1
        }
        WorldEdit.copyChunkBiome(sourceChunk, chunk)
        record()
        if (worldisNormal)
            p.showTitle(
                Title.title(
                    Component.text("§e恭喜您"),
                    Component.text("§f区块 ${chunk.x} ${chunk.z} 已掌握,但虚空区块不生成§b宝箱"),
                    Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
                )
            )
        else
            p.showTitle(
                Title.title(
                    Component.text("§e恭喜您"), Component.text("§4地狱§f区块 ${chunk.x} ${chunk.z} 已掌握"),
                    Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
                )
            )
    }

    private fun record() {
        submit(async = true) {
            val world = chunk.world
            val chunkListString = world.persistentDataContainer.get(
                NamespacedKey.fromString("chunkworld_chunks")!!,
                PersistentDataType.STRING
            )!!
            val oldChunkList = chunkListString.split("|").dropLast(1)
                .map { it.split(",")[0].toInt() to it.split(",")[1].toInt() }.toSet()
            //存入世界数据
            world.persistentDataContainer.set(
                NamespacedKey.fromString("chunkworld_chunks")!!,
                PersistentDataType.STRING, "$chunkListString${chunk.x},${chunk.z}|"
            )
            chunk.persistentDataContainer.set(
                NamespacedKey.fromString("chunkworld_unlock")!!,
                PersistentDataType.BOOLEAN, true
            )
            val worldInfo = RedisManager.getWorldInfo(p.name)!!
            if (worldisNormal) worldInfo.normalChunks += 1
            else worldInfo.netherChunks += 1
            RedisManager.setWorldInfo(p.name, worldInfo)
            if (worldisNormal) {
                submit { WorldEdit.setBarrier(oldChunkList, chunk.x to chunk.z, chunk.world) }
            } else {
                submit { WorldEdit.setBarrier(oldChunkList, chunk.x to chunk.z, chunk.world) }
            }
        }

    }

    private fun rebuild() {
        if (chunk.world.environment == World.Environment.NORMAL) {
            if (p.inventory.hasItem(matcher = {
                    type == Material.PAPER && itemMeta.customModelData == 300008 && hasLore(
                        "§c已绑定${p.name}"
                    )
                })) {
                if (p.inventory.takeItem(matcher = {
                        type == Material.PAPER && itemMeta.customModelData == 300008 && hasLore(
                            "§c已绑定${p.name}"
                        )
                    })) {
                    canClose = true
                    p.closeInventory()
                    ExpandGui(p, chunk).build()
                } else {
                    p.sendMessage("§c你没有足够的物品")
                }
            } else {
                p.sendMessage("§c你没有足够的物品")
            }
        } else {
            if (p.inventory.hasItem(matcher = {
                    type == Material.PAPER && itemMeta.customModelData == 300009 && hasLore(
                        "§c已绑定${p.name}"
                    )
                })) {
                if (p.inventory.takeItem(matcher = {
                        type == Material.PAPER && itemMeta.customModelData == 300009 && hasLore(
                            "§c已绑定${p.name}"
                        )
                    })) {
                    canClose = true
                    p.closeInventory()
                    ExpandGui(p, chunk).build()
                } else {
                    p.sendMessage("§c你没有足够的物品")
                }
            } else {
                p.sendMessage("§c你没有足够的物品")
            }
        }
    }

    private fun closeAndConfirm(basic: PaperBasic, chunk1: Chunk, chunk2: Chunk, chunk3: Chunk) {
        basic.onClick(30) {
            confirm(chunk1, chunk2, chunk3)
        }
        basic.onClick(32) {
            rebuild()
        }
        basic.onClick(31) {
            confirmVoid(chunk1, chunk2, chunk3)
        }
    }


}