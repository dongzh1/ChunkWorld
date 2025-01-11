package com.dongzh1.chunkworld.basic

import ParticleEffect
import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.plugins.WorldEdit
import com.dongzh1.chunkworld.plugins.fawe
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.*
import me.arcaniax.hdb.api.HeadDatabaseAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.*
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class ConfirmExpandGui(private val p: Player, private val chunk: Chunk) {
    private val worldisNormal = chunk.world.environment == World.Environment.NORMAL
    fun build() {
        val basic = PaperBasic(p, Component.text("生成梦境区块"))
        val chunkLevel = max(abs(chunk.x), abs(chunk.z))


        //设置菜单大小为行
        basic.rows(4)
        //取消全局点击事件
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.set(13, buildItem(Material.PAPER, builder = {
            amount = chunkLevel
            name = "§3生成梦境区块详情"
            lore.add("§f距离初始区块越远的区块等级越高")
            lore.add("§f这是 §f$chunkLevel 级区块")
            if (chunk.world.environment == World.Environment.NORMAL) {
                customModelData = 300008
                lore.add("§f消耗 §b$chunkLevel §f个§6世界碎片§f来生成区块")
            } else {
                customModelData = 300009
                lore.add("§f消耗 §b$chunkLevel §f个§6地狱碎片§f来生成区块")
            }
            lore.add("§4选择 §a确认 §4后碎片将无法退回")
        }))
        basic.set(30, buildItem(Material.LIME_CONCRETE, builder = {
            name = "§a确认"
            lore.add("§4确认后扣除碎片")
        }))
        basic.onClick(30) {
            p.closeInventory()
            if (deduct()) {
                //扣除成功
                p.showTitle(
                    Title.title(
                        Component.text("§a请稍等"), Component.text("§f正在释放能量凝聚区块"),
                        Times.times(Duration.ofSeconds(1), Duration.ofSeconds(10), Duration.ofSeconds(1))
                    )
                )
                //选择模板文件
                val schemPair = fawe.getRandomSchem(worldisNormal)
                val schem = schemPair.second!!
                val xiyoudu = schemPair.first
                val schemShow = removeNumbersFromString(schem.name).replace(".schem", "")
                //将模板粘贴到玩家世界,确定粘贴点，最小点
                val pos1: Location
                if (worldisNormal) {
                    pos1 = Location(chunk.world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
                } else {
                    pos1 = Location(chunk.world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
                }
                //复制模板
                fawe.placeSchem(schem, pos1)
                //生成宝藏
                submit {
                    val config =
                        if (worldisNormal) {
                            ChunkWorld.inst.config.getConfigurationSection("Baoxiang.world")!!
                        } else {
                            ChunkWorld.inst.config.getConfigurationSection("Baoxiang.nether")!!
                        }
                    val block = chunk.getBlock(
                        Random.nextInt(3, 13),
                        Random.nextInt(config.getInt("Ymin"), config.getInt("Ymax")),
                        Random.nextInt(3, 13)
                    )
                    //清理周围的方块，形成一个3*3*3的空间
                    for (x in -1..1) {
                        for (y in 0..2) {
                            for (z in -1..1) {
                                val block1 = block.getRelative(x, y, z)
                                block1.type = Material.AIR
                            }
                        }
                    }
                    val light = block.getRelative(0, -1, 0)
                    light.type = Material.SEA_LANTERN
                    block.type = Material.PLAYER_HEAD
                    //指定玩家头颅
                    val meta = block.state as Skull
                    val hdb = HeadDatabaseAPI()
                    val head = hdb.getItemHead("59124").itemMeta as SkullMeta
                    meta.ownerProfile = head.playerProfile
                    //把头颅信息保存好，便于监听,并播放粒子
                    val pUUID = ParticleEffect.startCircleEffect(block.location, 1.0, 5, Particle.WITCH)
                    meta.persistentDataContainer.set(
                        NamespacedKey.fromString("baozang")!!,
                        PersistentDataType.STRING, pUUID.toString()
                    )
                    block.chunk.persistentDataContainer.set(
                        NamespacedKey.fromString("baozang_location")!!,
                        PersistentDataType.STRING, "${block.x},${block.y},${block.z}"
                    )
                    meta.update()
                    //发送宝箱的某个坐标
                    var x = "???"
                    var y = "???"
                    var z = "???"
                    when (Random.nextInt(3)) {
                        0 -> x = block.x.toString()
                        1 -> y = block.y.toString()
                        2 -> z = block.z.toString()
                    }
                    val msg = Component.text("§m--------------------------").appendNewline()
                        .append(Component.text("§e恭喜你,世界又变大了许多!")).appendNewline()
                        .append(Component.text("§e新区块的梦境宝箱坐标就在:")).appendNewline()
                        .append(Component.text("§e$x,$y,$z")).appendNewline()
                        .append(Component.text("§e好像只获得了部分坐标")).appendNewline()
                        .append(Component.text("§e快去找找看吧!")).appendNewline()
                        .append(Component.text("§m--------------------------"))
                    p.sendMessage(msg)
                    if (worldisNormal)
                        p.showTitle(
                            Title.title(
                                Component.text("§e梦境世界已拓展"),
                                Component.text("§f此梦境区块为 $xiyoudu $schemShow 区块"),
                                Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
                            )
                        )
                    else
                        p.showTitle(
                            Title.title(
                                Component.text("§e梦境地狱已拓展"),
                                Component.text("§f此地狱区块为 $xiyoudu $schemShow 区块"),
                                Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))
                            )
                        )
                }
                //进行数据操作并设置屏障
                record()
            } else {
                p.sendMessage("§c你没有足够的物品")
            }
        }
        basic.set(32, buildItem(Material.RED_CONCRETE, builder = {
            name = "§c取消"
        }))
        basic.onClick(32) {
            p.closeInventory()
        }
        basic.openAsync()
    }

    /**
     * 从玩家背包判断是否有足够的指定物品，如果有就扣除并返回true,没有就返回false
     */
    private fun deduct(): Boolean {
        val chunkLevel = max(abs(chunk.x), abs(chunk.z))
        if (chunk.world.environment == World.Environment.NORMAL) {
            if (p.inventory.hasItem(
                    amount = chunkLevel,
                    matcher = {
                        type == Material.PAPER
                                && itemMeta.hasCustomModelData()
                                && itemMeta.customModelData == 300008
                                && hasLore("§c已绑定${p.name}")
                    })
            ) {
                p.inventory.takeItem(
                    amount = chunkLevel,
                    matcher = {
                        type == Material.PAPER
                                && itemMeta.hasCustomModelData()
                                && itemMeta.customModelData == 300008
                                && hasLore("§c已绑定${p.name}")
                    })
                return true
            } else {
                return false
            }
        } else {
            if (p.inventory.hasItem(
                    amount = chunkLevel,
                    matcher = {
                        type == Material.PAPER
                                && itemMeta.hasCustomModelData()
                                && itemMeta.customModelData == 300009
                                && hasLore("§c已绑定${p.name}")
                    })
            ) {
                p.inventory.takeItem(
                    amount = chunkLevel,
                    matcher = {
                        type == Material.PAPER
                                && itemMeta.hasCustomModelData()
                                && itemMeta.customModelData == 300009
                                && hasLore("§c已绑定${p.name}")
                    })
                return true
            } else {
                return false
            }
        }
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

    fun removeNumbersFromString(input: String): String {
        // 使用正则表达式匹配数字并替换为空字符串
        return input.replace(Regex("\\d"), "")
    }
}