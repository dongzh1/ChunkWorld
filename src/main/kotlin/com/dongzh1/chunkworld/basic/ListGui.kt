package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.xbaimiao.easylib.ui.PaperBasic
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class ListGui(private val p: Player, private val page:Int,private val isTrusted:Boolean) {
    fun build(){
        val basic = PaperBasic(p, Component.text("列表"))
        basic.rows(6)
        basic.set(0, Item.build(Material.COMMAND_BLOCK,1,"§a世界设置", listOf("§f点击打开世界设置页面"),-1))
        basic.set(4, Item.head(p.name,"§a我的世界", listOf("§f点击回到自己的世界"),-1))
        basic.set(8, Item.build(Material.BARRIER,1,"§c关闭界面", listOf("§f点击关闭此界面"),-1))
        //玩家世界
        val playSlots = listOf(19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43)
        val playerDaos = getPlayerData(playSlots.size)
        for (i in playSlots.indices){
            val playerDao = playerDaos.getOrNull(i)
            if (playerDao != null){
                basic.set(playSlots[i],Item.head(playerDao.name,"§a${playerDao.name}", listOf(
                    "§f解锁区块数: ${playerDao.chunkCount}","§f创建时间:", playerDao.createTime,"","§b点击传送到玩家世界"),-1))
                basic.onClick(playSlots[i]){
                    //玩家执行指令
                    p.closeInventory()
                    p.performCommand("chunkworld tp ${playerDao.name}")
                }
            }
        }
        basic.set(45, Item.build(Material.ARROW,1,"§a上一页", listOf("§f点击翻页"),-1))
        if (isTrusted) basic.set(49,Item.build(Material.NAME_TAG,1,"§a切换列表", listOf("§f点击切换到全部家园"),-1))
        else basic.set(49,Item.build(Material.NAME_TAG,1,"§a切换列表", listOf("§f点击切换到共享家园"),-1))
        basic.set(53, Item.build(Material.ARROW,1,"§a下一页", listOf("§f点击翻页"),-1))

        basic.onClick { it.isCancelled = true }
        basic.onClick(0) {
            SettingGui(p,1).build()
        }
        basic.onClick(4) {
            p.closeInventory()
            p.performCommand("chunkworld tp")
        }
        basic.onClick(8) { p.closeInventory() }
        //传送到其他玩家那里已注册
        basic.onClick(45) {
            if (page <= 1) return@onClick
            else ListGui(p,page-1,isTrusted).build()
        }
        basic.onClick(49) {
            p.closeInventory()
            ListGui(p,1,!isTrusted).build()
        }
        basic.onClick(53) {
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
        //在线玩家列表,按照占领区块数量排序
        val onlinePlayers = Listener.getPlayerDaosMap().sortedByDescending { it.chunkCount }
        if (isTrusted){
            //被信任的玩家
            val list = Listener.getTrustMap(p)?.toList()
            return ChunkWorld.db.playerGet(list?: emptyList())
        }else{
            // 获取所有玩家，优先显示在线玩家
            val start = (page - 1) * size
            val end = page * size
            if (onlinePlayers.size >= end){
                return onlinePlayers.subList(start,end)
            }else if (onlinePlayers.size > start) {
                //在线玩家不够，从数据库中获取
                val num = end - onlinePlayers.size
                //在线玩家列表
                val onlinePlayerDaos = onlinePlayers.subList(start,onlinePlayers.size)
                val playerDaos = ChunkWorld.db.playerGet(0,num,onlinePlayers.map { it.id })
                return onlinePlayerDaos + playerDaos
            }else{
                //在线玩家都不够，从数据库中获取,先看获取哪部分
                val begin = start - onlinePlayers.size
                return ChunkWorld.db.playerGet(begin,begin+size,onlinePlayers.map { it.id })
            }
        }
    }
}