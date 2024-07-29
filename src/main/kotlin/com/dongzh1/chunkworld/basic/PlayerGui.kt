package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.command.GroupCommand
import com.dongzh1.chunkworld.redis.RedisData
import com.dongzh1.chunkworld.redis.RedisPush
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

class PlayerGui(private val p:Player, private val banPage:Int) {
    fun build(){
        val basic = PaperBasic(p, Component.text("人员管理"))
        basic.rows(6)
        val playerDao = RedisData.getPlayerDao(p.uniqueId.toString())?: ChunkWorld.db.playerGet(p.uniqueId)
        if (playerDao == null){
            p.sendMessage("§c你还没有自己的独立世界")
            return
        }
        basic.set(8, buildItem(Material.BARRIER, builder = {
            name = "返回上级"
        }))
        basic.onClick(8) {
            p.closeInventory()
            ListGui(p,1,false).build()
        }
        basic.set(9, buildItem(Material.SKELETON_SKULL, builder = {
            name = "信任玩家"
            lore.add("")
            lore.add("§f右侧是和你共享世界的玩家")
            lore.add("§f你们可以相互在对方世界破坏和建造")
        }))
        basic.set(22, buildItem(Material.WITHER_SKELETON_SKULL, builder = {
            name = "拉黑玩家"
            lore.add("")
            lore.add("§f下方是和你双向拉黑的玩家")
            lore.add("§f你们无法进入对方的世界")
        }))
        for (i in 10..17){
            basic.set(i, buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                name = "§a空位置"
                lore.add("§f右键玩家可打开护照")
                lore.add("§f护照内可申请共享世界")
            }))
        }
        val banSlots = listOf(28,29,30,31,32,33,34,37,38,39,40,41,42,43,46,47,48,49,50,51,52)
        banSlots.forEach {
            basic.set(it, buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                name = "§a空位置"
                lore.add("§f蹲下右键玩家可打开护照")
                lore.add("§f护照内可拉黑玩家")
            }))
        }
        val ship = RedisData.getFriendsAndBanner(p.uniqueId.toString())!!
        val trustList = ship.first.toList()
        val banList = if (banPage == 1) ship.second.take(banPage*7-1).toList()
        else ship.second.drop((banPage-1)*7-1).take(banPage*7-1).toList()

        for (i in trustList.indices){
            val pname = RedisData.getPlayerDao(trustList[i])?.name?: ChunkWorld.db.playerGet(UUID.fromString(trustList[i]))?.name!!
            basic.set(10+i, buildItem(Material.PLAYER_HEAD, builder = {
                name = "§a$pname"
                skullOwner = pname
                lore.add("§b点击不再和该玩家共享世界")
            }))
            basic.onClick(10+i) {
                //改变对应物品
                it.view.setItem(10+i, buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                    name = "§a空位置"
                    lore.add("§f蹲下右键玩家可打开护照")
                    lore.add("§f护照内可申请共享世界")
                }))
                RedisData.removeFriend(p,trustList[i])
                submit(async = true) {
                    ChunkWorld.db.removeShip(playerDao.id, UUID.fromString(trustList[i]),true)
                }
                val target = Bukkit.getPlayer(UUID.fromString(trustList[i]))
                if (target != null){
                    target.sendMessage("§c ${p.name} 已取消和你共享世界")
                }else{
                    RedisPush.cancelFriend(trustList[i],p.name)
                }
            }
        }
        for (i in banList.indices){
            val pname = RedisData.getPlayerDao(banList[i])?.name?: ChunkWorld.db.playerGet(UUID.fromString(banList[i]))?.name!!
            basic.set(banSlots[i], buildItem(Material.PLAYER_HEAD, builder = {
                name = "§a$pname"
                skullOwner = pname
                lore.add("§b点击申请解除相互拉黑关系")
            }))
            basic.onClick(banSlots[i]) {
                p.closeInventory()
                val targetPlayer = Bukkit.getPlayer(UUID.fromString(banList[i]))
                if (targetPlayer == null){
                    p.sendMessage("§c此玩家不在本服或离线")
                    return@onClick
                }
                if (GroupCommand.isBan(p,targetPlayer)){
                    p.sendMessage("§c你已经发送过申请了")
                    return@onClick
                }
                submit(async = true) {
                    val pDao = RedisData.getPlayerDao(p.uniqueId.toString())?: ChunkWorld.db.playerGet(p.uniqueId)
                    val targetDao = RedisData.getPlayerDao(banList[i])?: ChunkWorld.db.playerGet(UUID.fromString(banList[i]))
                    if (pDao == null){
                        p.sendMessage("§c你还没有自己的独立世界")
                        return@submit
                    }
                    if (targetDao == null){
                        p.sendMessage("§c对方还没有自己的独立世界")
                        return@submit
                    }
                    val banners = RedisData.getBanners(p.uniqueId.toString())!!
                    if (!banners.contains(banList[i])){
                        p.sendMessage("§c你们已经不处于拉黑关系了")
                        return@submit
                    }
                    GroupCommand.addBan(p,targetPlayer)
                    p.sendMessage("§a已向${targetPlayer.name}发送解除相互拉黑的请求")
                    //发送邀请
                    TellrawJson()
                        .append("§a${p.name}希望和你解除相互拉黑关系,解除后你们可以传送到对方世界")
                        .newLine()
                        .append("§a同意").hoverText("点击同意解除拉黑").runCommand("/chunkworld ban true ${p.name}")
                        .append("          ")
                        .append("§c拒绝").hoverText("点击拒绝解除拉黑").runCommand("/chunkworld ban false ${p.name}")
                        .sendTo(targetPlayer)
                }
            }
        }
        basic.set(36, buildItem(Material.PAPER, builder = {
            customModelData = 300004
            name = "§f빪 §x§1§9§c§a§a§d➠ 上一页"
        }))
        basic.onClick(36) {
            if (banPage >1){
                p.closeInventory()
                PlayerGui(p,banPage-1)
            }
        }
        basic.set(44, buildItem(Material.PAPER, builder = {
            customModelData = 300005
            name = "§f빪 §x§1§9§c§a§a§d➠ 下一页"
        }))
        basic.onClick(44) {
            if (it.view.getItem(52)?.type == Material.PLAYER_HEAD) PlayerGui(p,banPage+1)
        }
        basic.openAsync()
    }
}