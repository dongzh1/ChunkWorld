package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.basic.Biome.chinese
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.ui.Basic
import com.xbaimiao.easylib.ui.PaperBasic
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import kotlin.math.max

class ExpandGui(private val p: Player,private val chunk: Chunk) {
    private var choose = 0
    private var canClose = false
    fun build() {
        val basic = PaperBasic(p, Component.text("生成区块"))
        var chunk1 = p.chunk
        var chunk2 = p.chunk
        var chunk3 = p.chunk
        //设置菜单大小为行
        basic.rows(4)
        basic.set(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
            listOf("§c当前未选择区块!","§c此时确认将扩展空间，不生成区块"),-1))
        basic.set(32,Item.build(Material.RED_CONCRETE,1,"§c再次抽取",null,-1))

        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.onClick(11) {
            choose = 11
            val itemStack = it.view.getItem(11)
            if (itemStack != null) {
                val item = itemStack.clone()
                item.addUnsafeEnchantment(Enchantment.LUCK,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                it.view.setItem(11,item)
                val item13 = it.view.getItem(13)
                if (item13 != null) {
                    val i13 = item13.clone()
                    i13.removeEnchantments()
                    it.view.setItem(13,i13)
                }
                val item15 = it.view.getItem(15)
                if (item15 != null) {
                    val i15 = item15.clone()
                    i15.removeEnchantments()
                    it.view.setItem(15,i15)
                }
            }
            it.view.setItem(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
                null,-1))
        }
        basic.onClick(13) {
            choose = 13
            val itemStack = it.view.getItem(13)
            if (itemStack != null) {
                val item = itemStack.clone()
                item.addUnsafeEnchantment(Enchantment.LUCK,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                it.view.setItem(13,item)
                val item11 = it.view.getItem(11)
                if (item11 != null) {
                    val i11 = item11.clone()
                    i11.removeEnchantments()
                    it.view.setItem(11,i11)
                }
                val item15 = it.view.getItem(15)
                if (item15 != null) {
                    val i15 = item15.clone()
                    i15.removeEnchantments()
                    it.view.setItem(15,i15)
                }
            }
            it.view.setItem(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
                null,-1))
        }
        basic.onClick(15) {
            choose = 15
            val itemStack = it.view.getItem(15)
            if (itemStack != null) {
                val item = itemStack.clone()
                item.addUnsafeEnchantment(Enchantment.LUCK,1)
                item.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
                it.view.setItem(15,item)
                val item13 = it.view.getItem(13)
                if (item13 != null) {
                    val i13 = item13.clone()
                    i13.removeEnchantments()
                    it.view.setItem(13,i13)
                }
                val item11 = it.view.getItem(11)
                if (item11 != null) {
                    val i11 = item11.clone()
                    i11.removeEnchantments()
                    it.view.setItem(11,i11)
                }
            }
            it.view.setItem(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
                null,-1))
        }
        basic.onClick(30) {
            canClose = true
            p.closeInventory()

            //生成区块
            val sourceChunk = when(choose){
                11 -> chunk1
                13 -> chunk2
                15 -> chunk3
                else -> null
            }
            if (sourceChunk != null)
            WorldEdit.copyChunk(sourceChunk,chunk)
            //生成屏障
            WorldEdit.setBarrier(Listener.getChunkMap(p)!!,chunk.x to chunk.z,chunk.world)
        }
        basic.onClick(32) {
            canClose = true
            p.closeInventory()
            ExpandGui(p,chunk).build()
        }
        basic.onClose(once = true) {
            choose = 0
            if (!canClose) basic.openAsync()
        }

        var n = 0
        val chunkSlot = listOf(11,13,15)
        chunkSlot.forEach { slot ->
            val randomX = (0..500).random()
            val randomZ = (0..500).random()
            val resourceWorld = Bukkit.getWorld(ChunkWorld.inst.config.getString("Resource")!!)!!
            resourceWorld.getChunkAtAsync(randomX,randomZ).thenAccept {
                //获取区块内的某个方块
                val block1 = resourceWorld.getHighestBlockAt(
                    randomX*16+(0..15).random(),randomZ*16+(0..15).random()
                )
                val type = when(block1.type){
                    Material.WATER -> Material.WATER_BUCKET
                    Material.LAVA -> Material.LAVA_BUCKET
                    else -> block1.type
                }
                val biome1 = block1.biome
                val biome2 = resourceWorld.getBiome(
                    randomX*16+(0..15).random(),(0..30).random(),randomZ*16+randomZ*16+(0..15).random()
                )
                val biome3 = resourceWorld.getBiome(
                    randomX*16+(0..15).random(),(-40..0).random(),randomZ*16+randomZ*16+(0..15).random()
                )
            //300,0,-30各取一个点进行测算
                basic.set(slot, Item.build(type,1,"§a选择此区块",
                    listOf("§f此区块初步探明的群系有:", biome1.chinese, biome2.chinese, biome3.chinese,
                        "§f以上群系仅为三点探测所得", "§f真实群系还需生成后探索"),-1)
                )
                n++
                when(slot){
                    11 -> chunk1 = it
                    13 -> chunk2 = it
                    15 -> chunk3 = it
                }
                if (n == 3) {
                    basic.openAsync()
                    p.clearTitle()
                }
            }
        }

    }


}