package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.basic.Biome.chinese
import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.hasItem
import com.xbaimiao.easylib.util.hasLore
import com.xbaimiao.easylib.util.submit
import com.xbaimiao.easylib.util.takeItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration
import kotlin.random.Random

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
            listOf("§c当前未选择区块!","§a此时确认将扩展空间，按照第1个区块生成"),-1))
        basic.set(32,Item.build(Material.RED_CONCRETE,1,"§c再次抽取",
            listOf("§f消耗 §b1 §f个区块碎片重新组合区块"),-1))
        basic.set(31,Item.build(Material.YELLOW_CONCRETE,1,"§4生成虚空",
            listOf("§4点此将不改变此区块样貌§8(§c虚空§8)","§4但会将§b群系§4修改为","§4选择的区块同款§b群系","§c此方法不会生成宝箱"),-1))
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
                    i13.removeEnchantment(Enchantment.LUCK)
                    it.view.setItem(13,i13)
                }
                val item15 = it.view.getItem(15)
                if (item15 != null) {
                    val i15 = item15.clone()
                    i15.removeEnchantment(Enchantment.LUCK)
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
                    i11.removeEnchantment(Enchantment.LUCK)
                    it.view.setItem(11,i11)
                }
                val item15 = it.view.getItem(15)
                if (item15 != null) {
                    val i15 = item15.clone()
                    i15.removeEnchantment(Enchantment.LUCK)
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
                    i13.removeEnchantment(Enchantment.LUCK)
                    it.view.setItem(13,i13)
                }
                val item11 = it.view.getItem(11)
                if (item11 != null) {
                    val i11 = item11.clone()
                    i11.removeEnchantment(Enchantment.LUCK)
                    it.view.setItem(11,i11)
                }
            }
            it.view.setItem(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
                null,-1))
        }

        basic.onClose{
            if (!canClose) build(chunk1,chunk2,chunk3,it.view.getItem(11)!!,it.view.getItem(13)!!,it.view.getItem(15)!!)
        }

        var n = 0
        val chunkSlot = listOf(11,13,15)
        chunkSlot.forEach { slot ->
            val randomX = (-1000..1000).random()
            val randomZ = (-1000..1000).random()
            val resourceWorld = Bukkit.getWorld(ChunkWorld.inst.config.getString("Resource")!!)!!
            resourceWorld.getChunkAtAsync(randomX,randomZ).thenAccept {
                //获取区块内的某个方块
                val block1 = resourceWorld.getHighestBlockAt(
                    randomX*16+(0..15).random(),randomZ*16+(0..15).random()
                )

                val type = when(block1.type) {
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
                    //确认和重新随机
                    closeAndConfirm(basic,chunk1, chunk2, chunk3)
                    basic.openAsync()
                    p.clearTitle()
                }
            }
        }

    }
    private fun build(chunk1:Chunk, chunk2:Chunk, chunk3:Chunk, item11:ItemStack, item13:ItemStack, item15:ItemStack){
        val basic = PaperBasic(p, Component.text("生成区块"))
        //设置菜单大小为行
        basic.rows(4)
        if (choose != 0) basic.set(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
            null,-1))
        else basic.set(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
            listOf("§c当前未选择区块!","§a此时确认将扩展空间，按照第1个区块生成"),-1))
        basic.set(32,Item.build(Material.RED_CONCRETE,1,"§c再次抽取",
            listOf("§f消耗 §b1 §f个区块碎片重新组合区块"),-1))
        basic.set(31,Item.build(Material.YELLOW_CONCRETE,1,"§4生成虚空",
            listOf("§4点此将不改变此区块样貌§8(§c虚空§8)","§4但会将§b群系§4修改为","§4选择的区块同款§b群系","§4不选择则为第1个","§c此方法不会生成宝箱"),-1))
        basic.set(11,item11)
        basic.set(13,item13)
        basic.set(15,item15)
        basic.onClick { event ->
            event.isCancelled = true
        }

        basic.onClick(11) {
            choose = 11
            item11.addUnsafeEnchantment(Enchantment.LUCK,1)
            item11.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(11,item11)
            item13.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(13,item13)
            item15.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(15,item15)
            it.view.setItem(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
                null,-1))
        }
        basic.onClick(13) {
            choose = 13
            item13.addUnsafeEnchantment(Enchantment.LUCK,1)
            item13.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(13,item13)
            item11.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(11,item11)
            item15.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(15,item15)
            it.view.setItem(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
                null,-1))
        }
        basic.onClick(15) {
            choose = 15
            item15.addUnsafeEnchantment(Enchantment.LUCK,1)
            item15.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            it.view.setItem(15,item15)
            item13.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(13,item13)
            item11.removeEnchantment(Enchantment.LUCK)
            it.view.setItem(11,item11)
            it.view.setItem(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",
                null,-1))
        }
        closeAndConfirm(basic,chunk1,chunk2,chunk3)
        basic.onClose{
            if (!canClose) build(chunk1,chunk2,chunk3,it.view.getItem(11)!!,it.view.getItem(13)!!,it.view.getItem(15)!!)
        }
        basic.openAsync()

    }
    private fun confirm(chunk1: Chunk,chunk2: Chunk,chunk3: Chunk){
        canClose = true
        p.closeInventory()
        //生成区块
        val sourceChunk = when(choose){
            11 -> chunk1
            13 -> chunk2
            15 -> chunk3
            else -> chunk1
        }
        WorldEdit.copyChunk(sourceChunk,chunk,p)
        record()
        p.showTitle(Title.title(Component.text("§e恭喜您"), Component.text("§f区块 ${chunk.x} ${chunk.z} 已掌握,在0层以下已生成§b区块宝箱"),
            Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))))
    }
    private fun confirmVoid(chunk1: Chunk,chunk2: Chunk,chunk3: Chunk){
        canClose = true
        p.closeInventory()
        //生成区块
        val sourceChunk = when(choose){
            11 -> chunk1
            13 -> chunk2
            15 -> chunk3
            else -> chunk1
        }
        WorldEdit.copyChunkBiome(sourceChunk,chunk)
        record()
        p.showTitle(Title.title(Component.text("§e恭喜您"), Component.text("§f区块 ${chunk.x} ${chunk.z} 已掌握,但虚空区块不生成§b宝箱"),
            Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(1))))
    }
    private fun record(){
        Listener.addChunkMap(p,chunk.x to chunk.z)
        val playerDao = Listener.getPlayerDaoMap(p.name)!!
        playerDao.chunkCount += 1
        Listener.setPlayerDaoMap(p.name,playerDao)
        submit(async = true) {
            ChunkWorld.db.chunkCreate(ChunkDao().apply {
                x = chunk.x
                z = chunk.z
                playerID = playerDao.id
            })
            ChunkWorld.db.playerUpdate(playerDao)
        }
        //生成屏障
        WorldEdit.setBarrier(Listener.getChunkMap(p)!!,chunk.x to chunk.z,chunk.world)
    }
    private fun rebuild(){
        val material = Material.valueOf(ChunkWorld.inst.config.getString("item.material")!!)
        if (ChunkWorld.inst.config.getInt("item.customModelData") == -1) {
            //判断有没有
            if (p.inventory.hasItem(matcher = { type == material && hasLore("§c已绑定${p.name}") })){
                //有就扣除并返回true
                if (p.inventory.takeItem(matcher = { type == material && hasLore("§c已绑定${p.name}") })){
                    canClose = true
                    p.closeInventory()
                    ExpandGui(p,chunk).build()
                }else{
                    p.sendMessage("§c你没有足够的物品")
                }
            }else{
                p.sendMessage("§c你没有足够的物品")
            }
            //有就扣除并返回true
        }else{
            if (p.inventory.hasItem(matcher = { type == material && hasLore("§c已绑定${p.name}") && itemMeta?.customModelData == ChunkWorld.inst.config.getInt("item.customModelData") }))
                //有就扣除并返回true
                if (p.inventory.takeItem(matcher = { type == material && hasLore("§c已绑定${p.name}") && itemMeta?.customModelData == ChunkWorld.inst.config.getInt("item.customModelData") })) {
                    canClose = true
                    p.closeInventory()
                    ExpandGui(p, chunk).build()
                }else{
                    p.sendMessage("§c你没有足够的物品")
                }
            else p.sendMessage("§c你没有足够的物品")
        }
    }
    private fun closeAndConfirm(basic: PaperBasic,chunk1: Chunk,chunk2: Chunk,chunk3: Chunk){
        basic.onClick(30) {
            confirm(chunk1,chunk2,chunk3)
        }
        basic.onClick(32) {
            rebuild()
        }
        basic.onClick(31) {
            confirmVoid(chunk1,chunk2,chunk3)
        }
    }


}