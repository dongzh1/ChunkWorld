package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.command.GroupCommand
import com.dongzh1.chunkworld.redis.RedisData
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.entity.Player

class PassportGui(private val p:Player,private val targetPlayer:Player) {
    fun build(){
        val basic = PaperBasic(p, Component.text("像素护照"))
        basic.rows(4)
        basic.onClick { it.isCancelled = true }
        basic.set(10, buildItem(Material.PLAYER_HEAD, builder = {
            skullOwner = targetPlayer.name
            customModelData = 10007
            name = "§f${targetPlayer.name}"
        }))
        basic.set(listOf(11,19,20), buildItem(Material.PAPER, builder = {
            customModelData = 300007
            name = "§f${targetPlayer.name}"
        }))
        basic.set(22, buildItem(Material.CAMPFIRE, builder = {
            name = "§f共享世界"
            lore.add("§f点击申请共享世界")
        }))
        basic.onClick(22) {
            p.closeInventory()
            if (GroupCommand.isTrust(p,targetPlayer)){
                p.sendMessage("§c你已经发送过邀请了")
                return@onClick
            }
            submit(async = true) {
                val pDao = RedisData.getPlayerDao(p.uniqueId.toString())?: ChunkWorld.db.playerGet(p.uniqueId)
                val targetDao = RedisData.getPlayerDao(targetPlayer.uniqueId.toString())?: ChunkWorld.db.playerGet(targetPlayer.uniqueId)
                if (pDao == null){
                    p.sendMessage("§c你还没有自己的独立世界")
                    return@submit
                }
                if (targetDao == null){
                    p.sendMessage("§c对方还没有自己的独立世界")
                    return@submit
                }
                val friends = RedisData.getFriends(p.uniqueId.toString())!!
                if (friends.size >= 6){
                    p.sendMessage("§c你的共享世界人数已达上限")
                    return@submit
                }
                if (friends.contains(targetPlayer.uniqueId.toString())){
                    p.sendMessage("§c你们已经共享世界了")
                    return@submit
                }
                GroupCommand.addTrust(p,targetPlayer)
                p.sendMessage("§a已向${targetPlayer.name}发送共享世界申请")
                //发送邀请
                TellrawJson()
                    .append("§a${p.name}希望和你共享世界,共享世界后你们可以在对方世界§c破坏§a和建造")
                    .newLine()
                    .append("§a同意").hoverText("点击同意共享世界").runCommand("/chunkworld trust true ${p.name}")
                    .append("          ")
                    .append("§c拒绝").hoverText("点击拒绝共享世界").runCommand("/chunkworld trust false ${p.name}")
                    .sendTo(targetPlayer)
            }
        }
        basic.set(24, buildItem(Material.TNT, builder = {
            name = "§c拉黑玩家"
            lore.add("§f点击拉黑此玩家")
            lore.add("§c拉黑后你们无法进入对方的世界")
            if (targetPlayer.world.name.contains(p.uniqueId.toString())){
                lore.add("§c也会将其踢出你的世界")
            }
        }))
        basic.onClick(24) {
            p.closeInventory()
            submit(async = true) {
                val pDao = RedisData.getPlayerDao(p.uniqueId.toString())?: ChunkWorld.db.playerGet(p.uniqueId)
                val targetDao = RedisData.getPlayerDao(targetPlayer.uniqueId.toString())?: ChunkWorld.db.playerGet(targetPlayer.uniqueId)
                if (pDao == null){
                    p.sendMessage("§c你还没有自己的独立世界")
                    return@submit
                }
                if (targetDao == null){
                    p.sendMessage("§c对方还没有自己的独立世界")
                    return@submit
                }
                val banners = RedisData.getBanners(p.uniqueId.toString())!!
                if (banners.contains(targetPlayer.uniqueId.toString())){
                    p.sendMessage("§c你已经拉黑了这个玩家")
                    return@submit
                }
                p.sendMessage("§c已拉黑${targetPlayer.name}")
                //发送邀请
                targetPlayer.sendMessage("§c${p.name}已和你相互拉黑")
                if (targetPlayer.world.name == "chunkworlds/world/${p.uniqueId}" || targetPlayer.world.name == "chunkworlds/nether/${p.uniqueId}"){
                    targetPlayer.teleportAsync(ChunkWorld.spawnLocation)
                    targetPlayer.sendMessage("§c你已被踢出对方世界")
                }
                RedisData.addBanner(p,targetPlayer)
                submit(async = true) {
                    ChunkWorld.db.addShip(pDao.id,targetDao.id,false)
                }
            }
        }
        basic.openAsync()
    }
}