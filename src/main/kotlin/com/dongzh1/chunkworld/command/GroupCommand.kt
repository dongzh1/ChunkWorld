package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.basic.ListGui
import com.dongzh1.chunkworld.redis.RedisData


import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.World.Environment
import org.bukkit.entity.Player

@ECommandHeader(command = "chunkworld")
object GroupCommand {
    private val trustMap = mutableListOf<Pair<Player,Player>>()
    private val banMap = mutableListOf<Pair<Player,Player>>()
    fun addTrust(player: Player, target: Player){
        trustMap.add(player to target)
        submit(delay = 20*60) {
            if (trustMap.contains(player to target)){
                player.sendMessage("§a你对 ${target.name} 的共享世界邀请已过期")
                trustMap.remove(player to target)
            }
        }
    }
    private fun removeTrust(player: Player, target: Player){
        trustMap.remove(player to target)
    }
    fun isTrust(player: Player, target: Player):Boolean{
        return trustMap.contains(player to target)
    }
    fun addBan(player: Player, target: Player){
        banMap.add(player to target)
        submit(delay = 20*60) {
            if (banMap.contains(player to target)){
                player.sendMessage("§a你对 ${target.name} 的解除拉黑申请已过期")
                banMap.remove(player to target)
            }
        }
    }
    private fun removeBan(player: Player, target: Player){
        banMap.remove(player to target)
    }
    fun isBan(player: Player, target: Player):Boolean{
        return banMap.contains(player to target)
    }
    private val name = ArgNode("name", exec = {
        listOf("玩家名")
    }, parse = {
        it
    })
    @CommandBody
    private val menu = command<Player>("menu"){
        description = "打开菜单"
        exec {
            ListGui(sender,1,false).build()
        }
    }
    @CommandBody
    private val test = command<Player>("test"){
        description = "请勿使用，测试插件专用"
        permission = "chunkworld.admin"
        arg(name){ name ->
            booleans {
                exec {
                    val worldName = valueOf(name)
                    if (valueOf(it)){
                        val world = Bukkit.createWorld(org.bukkit.WorldCreator(worldName))
                        sender.teleportAsync(world!!.spawnLocation)
                    }else {
                        val world = Bukkit.getWorld(worldName)
                        if (world != null) {
                            Bukkit.unloadWorld(world, true)
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    private val tp = command<Player>("tp") {
        description = "传送到自己或别人世界，自己没有的话会创建,只有op有权限通过此指令去别人家"
        arg(name, optional = true) {
            exec {
                launchCoroutine(SynchronizationContext.ASYNC) {
                    val name = valueOfOrNull(it)?:sender.name
                    val playerDao = RedisData.getPlayerDaoByName(name)?:ChunkWorld.db.playerGet(name)
                    if (playerDao == null){
                        if (name == sender.name){
                            //创建独立世界
                            Tp.createTp(sender)
                            return@launchCoroutine
                        }else{
                            sender.sendMessage("§c此玩家还没有自己的独立世界")
                            return@launchCoroutine
                        }
                    }
                    //说明世界创建过，传送就行
                    Tp.toPlayerWorld(sender,playerDao)
                }
            }
        }
    }
    @CommandBody
    private val trust = command<Player>("trust"){
        booleans{ bool ->
            players {
                exec {
                    val result = valueOf(bool)
                    val player = valueOf(it)
                    if (player == null){
                        sender.sendMessage("§c此玩家已不在本服或离线")
                        return@exec
                    }
                    submit(async = true) {
                        if (result){
                            if (isTrust(player,sender)){
                                removeTrust(player , sender)
                                val friends = RedisData.getFriends(sender.uniqueId.toString())!!
                                if (friends.size >= 6){
                                    sender.sendMessage("§c你的共享世界人数已达上限")
                                    return@submit
                                }
                                if (friends.contains(player.uniqueId.toString())){
                                    sender.sendMessage("§c你们已经共享世界了")
                                    return@submit
                                }
                                val playerDao = RedisData.getPlayerDao(sender.uniqueId.toString())?:ChunkWorld.db.playerGet(sender.uniqueId)?:return@submit
                                val targetDao = RedisData.getPlayerDao(player.uniqueId.toString())?:ChunkWorld.db.playerGet(player.uniqueId)?:return@submit
                                RedisData.addFriend(player,sender)
                                ChunkWorld.db.addShip(playerDao.id,targetDao.id,true)
                                sender.sendMessage("§a你们可以共享世界了，§c注意保护好自身世界")
                                player.sendMessage("§c${sender.name}已同意和你共享世界")
                                //如果是在对方世界，应该改为生存模式
                                if (player.world.name == "chunkworlds/world/${sender.uniqueId}" || player.world.name == "chunkworlds/nether/${sender.uniqueId}"){
                                    player.gameMode = org.bukkit.GameMode.SURVIVAL
                                }
                                if (sender.world.name == "chunkworlds/world/${player.uniqueId}" || sender.world.name == "chunkworlds/nether/${player.uniqueId}"){
                                    sender.gameMode = org.bukkit.GameMode.SURVIVAL
                                }
                                return@submit
                            }else{
                                sender.sendMessage("§c此邀请已过期")
                                return@submit
                            }
                        }else{
                            removeTrust(player , sender)
                            sender.sendMessage("§a你拒绝了共享世界邀请")
                            player.sendMessage("§c${sender.name}拒绝了你的共享世界邀请")
                        }
                    }
                }
            }
        }
    }
    @CommandBody
    private val ban = command<Player>("ban"){
        booleans{ bool ->
            players {
                exec {
                    val result = valueOf(bool)
                    val player = valueOf(it)
                    if (player == null){
                        sender.sendMessage("§c此玩家已不在本服或离线")
                        return@exec
                    }
                    submit(async = true) {
                        if (result){
                            val banners = RedisData.getBanners(player.uniqueId.toString())!!
                            if (!banners.contains(sender.uniqueId.toString())){
                                sender.sendMessage("§c你和此玩家已经没有相互拉黑了")
                                return@submit
                            }
                            if (isBan(player,sender)){
                                removeBan(player , sender)
                                val playerDao = RedisData.getPlayerDao(sender.uniqueId.toString())?:ChunkWorld.db.playerGet(sender.uniqueId)?:return@submit
                                val targetDao = RedisData.getPlayerDao(player.uniqueId.toString())?:ChunkWorld.db.playerGet(player.uniqueId)?:return@submit
                                RedisData.removeBanner(player,sender.uniqueId.toString())
                                ChunkWorld.db.removeShip(playerDao.id,targetDao.id,false)
                                sender.sendMessage("§a你们已解除拉黑关系，可以去对方的世界了")
                                player.sendMessage("§c${sender.name}已和你解除拉黑关系")
                                return@submit
                            }else{
                                sender.sendMessage("§c此申请已过期")
                                return@submit
                            }
                        }else{
                            removeBan(player , sender)
                            sender.sendMessage("§a已拒绝解除拉黑的申请")
                            player.sendMessage("§c${sender.name}拒绝了你的解除拉黑的申请")
                        }
                    }
                }
            }
        }
    }
}