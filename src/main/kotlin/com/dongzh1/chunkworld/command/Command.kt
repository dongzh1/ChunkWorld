package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.basic.ListGui
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@ECommandHeader(command = "chunkworld")
object Command {
    private val reply = ArgNode(("Mode"),
        exec = {
            listOf("invite", "unban")
        }, parse = {
            it
        }
    )
    val invite = command<Player>("invite") {
        // 要求传入一个玩家参数
        players { playerArg ->
            exec {
                val player = valueOfOrNull(playerArg)
                if (player == sender) {
                    sender.sendMessage("§c你不能邀请你自己")
                    return@exec
                }
                if (player == null) {
                    sender.sendMessage("§c玩家不在线")
                    return@exec
                }
                //是否已信任
                if (Listener.isBeTrust(sender,player.uniqueId)){
                    sender.sendMessage("§c你已和 ${player.name} 家园共享")
                    return@exec
                }
                if (Listener.getTrustMap(sender)!!.size >= 8){
                    sender.sendMessage("§c最多只能和8个家园共享")
                    return@exec
                }
                if (Listener.getTrustMap(player)!!.size >= 8){
                    sender.sendMessage("§c ${player.name} 已经没有更多共享位置了")
                    return@exec
                }
                sender.sendMessage("§a已向 ${player.name} 发出家园共享请求")
                //发送邀请
                TellrawJson()
                    .append("§b${sender.name} §f邀请你进行家园共享")
                    .newLine()
                    .append("§a同意").hoverText("点击同意这个邀请").runCommand("/chunkworld accept invite ${sender.name}")
                    .append("          ")
                    .append("§c拒绝").hoverText("点击拒绝这个邀请").runCommand("/chunkworld deny invite ${sender.name}")
                    .sendTo(player)
                //把邀请信息存入内存,只有一分钟
                Listener.setCommand("${sender.name} invite ${player.name}")
            }
        }
    }
    val ban = command<Player>("ban"){
        // 要求传入一个玩家参数
        players { playerArg ->
            exec {
                val player = valueOfOrNull(playerArg)
                if (player == null) {
                    sender.sendMessage("§c玩家不在线")
                    return@exec
                }
                if (player == sender) {
                    sender.sendMessage("§c你不能拉黑你自己")
                    return@exec
                }
                //查看是否已拉黑
                val banMap1 = Listener.getBanMap(sender)!!.toMutableSet()
                if (banMap1.contains(player.uniqueId)){
                    sender.sendMessage("§c你已经把 ${player.name} 拉黑了")
                    return@exec
                }
                sender.sendMessage("§a已对 ${player.name} 进行双向拉黑")
                //发送邀请
                TellrawJson()
                    .append("§c${sender.name} 已把你从家园拉黑")
                    .newLine()
                    .append("§a现在你们已双向拉黑")
                    .sendTo(player)
                val banMap2 = Listener.getBanMap(player)!!.toMutableSet()
                banMap1.add(player.uniqueId)
                banMap2.add(sender.uniqueId)
                Listener.setBanMap(sender,banMap1)
                Listener.setBanMap(player,banMap2)
                val senderId = Listener.getPlayerDaoMap(sender.uniqueId)!!.id
                val playerId = Listener.getPlayerDaoMap(player.uniqueId)!!.id
                //数据库中记录拉黑关系
                submit(async = true) {
                    ChunkWorld.db.addShip(senderId,playerId,false)
                }
            }
        }
    }
    /**
     * 接受邀请
     */
    @CommandBody
    val accept = command<Player>("accept"){
        description = "接受共享家园或取消黑名单"
        arg(reply){
            offlinePlayers { playerArg ->
                exec {
                    val name = valueOf(playerArg)
                    val player = Bukkit.getPlayerExact(name)
                    if (player == null){
                        sender.sendMessage("§c$name 已离线")
                        return@exec
                    }
                    when(valueOf(it)){
                        "invite" -> {
                            if (Listener.hasCommand("$name invite ${sender.name}")){
                                if (Listener.getTrustMap(sender)!!.size >= 8){
                                    sender.sendMessage("§c最多只能和8个家园共享")
                                    Listener.removeCommand("$name invite ${sender.name}")
                                    return@exec
                                }
                                val trustMap1 = Listener.getTrustMap(sender)!!.toMutableSet()
                                val trustMap2 = Listener.getTrustMap(player)!!.toMutableSet()
                                trustMap1.add(player.uniqueId)
                                trustMap2.add(sender.uniqueId)
                                Listener.setTrustMap(sender,trustMap1)
                                Listener.setTrustMap(player,trustMap2)
                                val senderId = Listener.getPlayerDaoMap(sender.uniqueId)!!.id
                                val playerId = Listener.getPlayerDaoMap(player.uniqueId)!!.id
                                submit(async = true) { ChunkWorld.db.addShip(senderId,playerId,true) }
                                Listener.removeCommand("$name invite ${sender.name}")
                                sender.sendMessage("§a现在你和 ${player.name} 已共享家园")
                                player.sendMessage("§a${sender.name} 已同意你的共享家园请求")
                            }else sender.sendMessage("§c此邀请已过期或不存在")
                        }
                        "unban" -> {
                            if (Listener.hasCommand("$name unban ${sender.name}")){
                                val banMap1 = Listener.getBanMap(sender)!!.toMutableSet()
                                val banMap2 = Listener.getBanMap(player)!!.toMutableSet()
                                banMap1.remove(player.uniqueId)
                                banMap2.remove(sender.uniqueId)
                                Listener.setBanMap(sender,banMap1)
                                Listener.setBanMap(player,banMap2)
                                val senderId = Listener.getPlayerDaoMap(sender.uniqueId)!!.id
                                val playerId = Listener.getPlayerDaoMap(player.uniqueId)!!.id
                                submit(async = true) { ChunkWorld.db.removeShip(senderId,playerId,false) }
                                Listener.removeCommand("$name unban ${sender.name}")
                                sender.sendMessage("§a现在你和 ${player.name} 已不再相互拉黑")
                                player.sendMessage("§a${sender.name} 已同意和你解除相互拉黑关系")
                            }else sender.sendMessage("§c此申请已过期或不存在")
                        }
                        else -> return@exec
                    }
                }
            }
        }
    }
    /**
     * 拒绝邀请或解除黑名单
     */
    @CommandBody
    val deny = command<Player>("deny"){
        description = "拒绝共享家园或取消黑名单"
        arg(reply){
            offlinePlayers { playerArg ->
                exec {
                    val name = valueOf(playerArg)
                    val player = Bukkit.getPlayerExact(name)
                    when(valueOf(it)){
                        "invite" -> {
                            if (Listener.hasCommand("$name invite ${sender.name}")){
                                Listener.removeCommand("$name invite ${sender.name}")
                                sender.sendMessage("§a已拒绝 $name 的共享家园请求")
                                player?.sendMessage("§c${sender.name} 已拒绝你的共享家园请求")
                            }else sender.sendMessage("§c此邀请已过期或不存在")
                        }
                        "unban" -> {
                            if (Listener.hasCommand("$name unban ${sender.name}")){
                                Listener.removeCommand("$name unban ${sender.name}")
                                sender.sendMessage("§a已拒绝 $name 的解除相互拉黑请求")
                                player?.sendMessage("§c${sender.name} 已拒绝和你解除相互拉黑关系")
                            }else sender.sendMessage("§c此申请已过期或不存在")
                        }
                        else -> return@exec
                    }
                }
            }
        }
    }
    /**
     * 重载
     */
    @CommandBody
    val reload = command<Player>("reload") {
        description = "重载插件"
        permission = "chunkworld.reload"
        exec {
            ListGui(sender,1,false).build()
        }
    }
    @CommandBody
    val tp = command<Player>("tp") {
        description = "传送到家园"
        permission = "chunkworld.tp"
        offlinePlayers(optional = true) { playerArg ->
            exec {
                Tp.to(playerArg.valueOrNull()?:sender.name, sender)
            }
        }
    }
}