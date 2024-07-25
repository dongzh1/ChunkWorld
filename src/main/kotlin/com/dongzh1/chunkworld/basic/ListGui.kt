package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.redis.RedisData
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID

class ListGui(private val p: Player, private val page:Int,private val isTrusted:Boolean) {


    fun build(){
        val basic = PaperBasic(p, Component.text("§f%img_offset_-8%빨".replacePlaceholder(p)))
        basic.rows(6)
        basic.set(2, buildItem(Material.PAPER, builder = {
            customModelData = 300001
            name = "§7设置你的独立世界"
            lore.add("§7传送点、出生点、世界规则等")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 浏览世界设置页面")
        }))
        basic.set(4, buildItem(Material.PLAYER_HEAD, builder = {
            customModelData = 10006
            skullOwner = p.name
            name = "§7你的独立世界"
            lore.add("§7世界可扩展至无限大")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 传送到你的世界")
        }))
        basic.set(6, buildItem(Material.PAPER, builder = {
            customModelData = 300002
            name = "§7与他人共享家园"
            lore.add("§7或者拉黑他人")
            lore.add("")
            lore.add("§f빪 §x§1§9§c§a§a§d➠ 浏览人员设置界面")
        }))
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name ="§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        val friendsAndBanners = RedisData.getFriendsAndBanner(p.uniqueId.toString())!!
        val friends = friendsAndBanners.first.map { UUID.fromString(it) }
        val banners = friendsAndBanners.second.map { UUID.fromString(it) }
        val players = Bukkit.getOnlinePlayers().map { it.name }
        //玩家世界
        val playSlots = listOf(19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43)
        val playerDaos = if (!isTrusted) getPlayerData(playSlots.size)
        else friends.mapNotNull { RedisData.getPlayerDao(it.toString())?:ChunkWorld.db.playerGet(it) }
        for (i in playSlots.indices){
            val playerDao = playerDaos.getOrNull(i)?:break
            basic.set(playSlots[i], buildItem(Material.PLAYER_HEAD, builder = {
                customModelData = 10006
                skullOwner = playerDao.name
                name = "§7${playerDao.name}的独立世界"
                if (players.contains(playerDao.name))
                    lore.add("§3本服世界")
                else lore.add("§7跨服世界")
                if (friends.contains(playerDao.uuid))
                    lore.add("§a共享世界")
                if (banners.contains(playerDao.uuid))
                    lore.add("§c相互拉黑")
                lore.add("§7创建时间:")
                lore.add("§7"+playerDao.createTime)
                lore.add("")
                lore.add("§f빪 §x§1§9§c§a§a§d➠ 传送到${playerDao.name}的世界")
            }))
            basic.onClick(playSlots[i]){
                //玩家执行指令
                p.closeInventory()
                p.performCommand("chunkworld tp ${playerDao.name}")
            }
        }
        basic.set(46, buildItem(Material.PAPER, builder = {
            customModelData = 300004
            name = "§f빪 §x§1§9§c§a§a§d➠ 上一页"
        }))
        basic.set(52, buildItem(Material.PAPER, builder = {
            customModelData = 300005
            name = "§f빪 §x§1§9§c§a§a§d➠ 下一页"
        }))
        if (isTrusted){
            basic.set(49, buildItem(Material.PAPER, builder = {
                customModelData = 300003
                name = "§7目前显示共享世界"
                lore.add("§7点击切换为所有世界")
                lore.add("")
                lore.add("§f빪 §x§1§9§c§a§a§d➠ 切换列表")
            }))
        }else{
            basic.set(49, buildItem(Material.PAPER, builder = {
                customModelData = 300003
                name = "§7目前显示所有世界"
                lore.add("§7点击切换为共享世界")
                lore.add("")
                lore.add("§f빪 §x§1§9§c§a§a§d➠ 切换列表")
            }))
        }
        basic.onClick { it.isCancelled = true }
        basic.onClick(2) {
            //todo
        }
        basic.onClick(4) {
            p.closeInventory()
            p.performCommand("chunkworld tp")
        }
        basic.onClick(6) {
            //todo
        }
        basic.onClick(8) { p.closeInventory() }
        //传送到其他玩家那里已注册
        basic.onClick(46) {
            if (page <= 1) return@onClick
            else ListGui(p,page-1,isTrusted).build()
        }
        basic.onClick(49) {
            ListGui(p,1,!isTrusted).build()
        }
        basic.onClick(52) {
            if (it.view.getItem(43) != null) ListGui(p,page+1,isTrusted).build()
        }
        basic.openAsync()
    }

    /**
     * 根据提供的玩家，获取他信任的所有玩家playerDao列表或
     * 获取当前页面的玩家playerDao，每页可以显示size个玩家
     * 优先显示在线玩家的列表，而后从数据库中获取离线玩家的列表
     * 假如第一页有20个在线玩家，第二页有10个在线玩家，那么第三页就从数据库中获取10个玩家
     * size代表的是每页可显示的playerDao数量
     * 这里的每页不是说数据库的每页，而是一个显示菜单，和数据库无关
     * @param size 获取的数据数量
     * @return 返回玩家数据
     */
    private fun getPlayerData(size:Int): List<PlayerDao> {
        val needSize = page * size
        //排序规则按照本服在线玩家，群组在线玩家和不在线玩家顺序排列，优先世界加载了的玩家
        val players = Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
        if (players.size >= needSize) {
            //如果在线玩家数量大于等于size，那么直接返回
            val daos = players.mapNotNull { RedisData.getPlayerDaoByName(it) }
            //可能有在线但是世界没加载的玩家
            if (daos.size >= needSize) {
                //返回对应页码的数据
                return daos.subList((page-1)*size,page*size)
            }
        }
        RedisManager.getAllNameUuid().keys.forEach { if (!players.contains(it)) players.add(it) }
        //现在加入了群组玩家
        val daos = players.mapNotNull { RedisData.getPlayerDaoByName(it) }
        if (daos.size >= needSize){
            //返回对应页码的数据
            return daos.subList((page-1)*size,page*size)
        }else {
            //返回最后一页的数据
            return daos.subList((page-1)*size,daos.size)
        }
    }




}