package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.Listener.getPlayerDaoMap
import com.dongzh1.chunkworld.command.Tp
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max

class PortalGui(private val p: Player) {
    fun build() {
        val dao = Listener.getPlayerDaoMap(p.name)
        val basic = PaperBasic(p, Component.text("世界传送"))
        //设置菜单大小为行
        basic.rows(1)
        basic.set(0,Item.build(Material.BLACK_CONCRETE_POWDER
            ,1,"§e小黑屋"
            , listOf("§f传送到小黑屋","§f未来的主城...")
            ,-1))
        basic.set(1, buildItem(Material.PINK_BED, builder = {
            name = "§b我的世界"
            lore.add("§f传送到你的世界的主世界出生点")
            lore.add("§8你的世界，我的世界，好像不一样")
        }))
        /*
        basic.set(2, buildItem(Material.RED_BED, builder = {
            name = "§b我的地狱"
            lore.add("§f传送到你的世界的地狱出生点")
            if (dao!!.chunkCount >= 50){
                if ()
                lore.add("§f解锁区块§a${dao.chunkCount}§f/50")
                lore.add("§f你已符合条件,可以传送到地狱")
            }else{
                lore.add("§f解锁区块§c${dao.chunkCount}§f/50")
                lore.add("§f达到条件后即可解锁你的地狱世界")
            }
        }))

         */
        basic.set(2, buildItem(Material.GRASS_BLOCK, builder = {
            name = "§a资源世界"
            lore.add("§f随机传送到资源世界")
            lore.add("§f使用条件:")
            if (dao!!.chunkCount >= 9){
                lore.add("§f解锁区块§a${dao.chunkCount}§f/9")
                lore.add("§f你已符合条件,可以传送")
            }else{
                lore.add("§f解锁区块§c${dao.chunkCount}§f/9")
                lore.add("§f你还不符合条件,请加油解锁区块")
            }
            lore.add("§f资源世界每日§a3:00§f更新")
            lore.add("§f一旦死亡将会回到你的世界")
            lore.add("§a探索不易,苟命要紧")
        }))
        basic.set(3, buildItem(Material.NETHER_BRICK, builder = {
            name = "§4资源地狱"
            lore.add("§f随机传送到资源地狱世界")
            lore.add("§f使用条件:")
            lore.add("§f消耗1张§4地狱邀请函")
            lore.add("§f邀请函可通过扩展区块获得")
            lore.add("§f资源地狱每日§a3:00§f更新")
            lore.add("§f一旦死亡将会回到你的世界")
            lore.add("§a探索不易,苟命要紧")
            lore.add("§7建议携带大量建材,毕竟你可能出生在岩浆湖")
        }))
        basic.set(4, buildItem(Material.END_STONE, builder = {
            name = "§5资源末地"
            lore.add("§f随机传送到资源地狱世界")
            lore.add("§f使用条件:")
            lore.add("§f消耗1张§5末地邀请函")
            lore.add("§f邀请函可通过扩展区块获得")
            lore.add("§f资源地狱每日§a3:00§f更新")
            lore.add("§f一旦死亡将会回到你的世界")
            lore.add("§a探索不易,苟命要紧")
            lore.add("§7建议携带大量建材,毕竟你可能出生在虚空")
        }))
        basic.set(8,Item.build(Material.BARRIER
            ,1,"§c取消传送"
            , listOf("§f哪儿也不去")
            ,-1))
        //取消全局点击事件
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.onClick(0){
            p.closeInventory()
            val locationString = ChunkWorld.inst.config.getString("Location")!!
            val worldName = locationString.split(",")[0]
            val x = locationString.split(",")[1].toDouble()
            val y = locationString.split(",")[2].toDouble()
            val z = locationString.split(",")[3].toDouble()
            val yaw = locationString.split(",")[4].toFloat()
            val pitch = locationString.split(",")[5].toFloat()
            val location = Location(Bukkit.getWorld(worldName),x, y, z, yaw, pitch)
            p.teleportAsync(location)
        }
        basic.onClick(1) {
            p.closeInventory()
            val location = Location(Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}"),
                dao!!.x(),dao.y(),dao.z(),dao.yaw(),dao.pitch())
            p.teleportAsync(location)

        }
        basic.onClick(2) {
            p.closeInventory()
            if (dao!!.chunkCount >= 9){
                val world = Bukkit.getWorld((ChunkWorld.inst.config.getString("Resource")?:"chunkworld")+"_zy")
                if (world == null){
                    p.sendMessage("§c资源世界出现问题，请联系腐竹")
                    return@onClick
                }
                p.sendMessage("§a正在为您搜寻合适的传送点...")
                Tp.randomTp(p,world,10000)
            }else{
                p.sendMessage("§f在你的世界解锁更多区块才能传送")
            }
        }
        basic.onClick(3) {
            p.closeInventory()
            if (p.inventory.hasItem(Item.netherItem(p),1) || p.inventory.hasItem(Item.netherItem(),1)){
                p.sendMessage("§a正在为您搜寻合适的传送点...")
                Tp.randomTp(p,Bukkit.getWorld("world_nether")!!,10000)
            }else{
                p.sendMessage("§f您没有§4地狱邀请函§f,每次拓展区块都可以在宝箱中获得邀请函")
            }
        }
        basic.onClick(4) {
            p.closeInventory()
            if (p.inventory.hasItem(Item.endItem(p),1) || p.inventory.hasItem(Item.endItem(),1)){
                p.sendMessage("§a正在为您搜寻合适的传送点...")
                Tp.randomTp(p,Bukkit.getWorld("world_the_end")!!,100)
            }else{
                p.sendMessage("§f您没有§5末地邀请函§f,每次拓展区块都可以在宝箱中获得邀请函")
            }
        }

        basic.onClick(8) { p.closeInventory() }
        basic.openAsync()
    }

}