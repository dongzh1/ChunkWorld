package com.dongzh1.chunkworld.command


import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.basic.MainGui
import com.dongzh1.chunkworld.redis.RedisManager
import com.dongzh1.chunkworld.redis.RedisPush
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import com.xbaimiao.easylib.util.submit
import org.bukkit.entity.Player

@ECommandHeader(command = "chunkworld")
object GroupCommand {
    private val trustMap = mutableListOf<Pair<Player, Player>>()
    private val banMap = mutableListOf<Pair<Player, Player>>()
    fun addTrust(player: Player, target: Player) {
        trustMap.add(player to target)
        submit(delay = 20 * 60) {
            if (trustMap.contains(player to target)) {
                player.sendMessage("§a你对 ${target.name} 的共享世界邀请已过期")
                trustMap.remove(player to target)
            }
        }
    }

    private fun removeTrust(player: Player, target: Player) {
        trustMap.remove(player to target)
    }

    fun isTrust(player: Player, target: Player): Boolean {
        return trustMap.contains(player to target)
    }

    fun addBan(player: Player, target: Player) {
        banMap.add(player to target)
        submit(delay = 20 * 60) {
            if (banMap.contains(player to target)) {
                player.sendMessage("§a你对 ${target.name} 的解除拉黑申请已过期")
                banMap.remove(player to target)
            }
        }
    }

    private fun removeBan(player: Player, target: Player) {
        banMap.remove(player to target)
    }

    fun isBan(player: Player, target: Player): Boolean {
        return banMap.contains(player to target)
    }

    private val name = ArgNode("name", exec = {
        listOf("玩家名")
    }, parse = {
        it
    })

    @CommandBody
    private val menu = command<Player>("menu") {
        description = "打开菜单"
        exec {
            MainGui(sender).build()
        }
    }

    @CommandBody
    private val tp = command<Player>("tp") {
        description = "传送到自己或别人世界"
        permission = "chunkworld.tp"
        arg(name, optional = true) {
            exec {
                launchCoroutine(SynchronizationContext.ASYNC) {
                    val name = valueOfOrNull(it) ?: sender.name
                    val worldInfo = RedisManager.getWorldInfo(name)
                    if (worldInfo == null) {
                        sender.sendMessage("§c此世界主人不在线")
                        return@launchCoroutine
                    }
                    Tp.teleportPlayerWorld(sender, name, worldInfo.state, worldInfo.serverName)
                }
            }
        }
    }

    @CommandBody
    private val trust = command<Player>("trust") {
        booleans { bool ->
            players {
                exec {
                    val result = valueOf(bool)
                    val player = valueOf(it)
                    if (player == null) {
                        sender.sendMessage("§c此玩家已不在本服或离线")
                        return@exec
                    }
                    submit(async = true) {
                        if (result) {
                            if (isTrust(player, sender)) {
                                val senderDao = ChunkWorld.db.getPlayerDao(sender.name)!!
                                val friends = ChunkWorld.db.getTrustNames(senderDao.id)
                                if (friends.size >= 6) {
                                    sender.sendMessage("§c你的共享世界人数已达上限")
                                    return@submit
                                }
                                if (friends.contains(player.name)) {
                                    sender.sendMessage("§c你们已经共享世界了")
                                    return@submit
                                }
                                val playerDao = ChunkWorld.db.getPlayerDao(player.name)!!
                                removeTrust(player, sender)
                                ChunkWorld.db.setShip(playerDao.id, senderDao.id, true)
                                val senderTrustsString = ChunkWorld.db.getTrustNames(senderDao.id).joinToString("|,;|")
                                val playerTrustsString = ChunkWorld.db.getTrustNames(playerDao.id).joinToString("|,;|")
                                //世界也该存储相关信息
                                val senderWorldInfo = RedisManager.getWorldInfo(sender.name)!!
                                val playerWorldInfo = RedisManager.getWorldInfo(player.name)!!
                                RedisPush.worldSetPersistent(
                                    "chunkworlds/world/${sender.uniqueId}",
                                    senderWorldInfo.serverName, "chunkworld_trust", senderTrustsString
                                )
                                RedisPush.worldSetPersistent(
                                    "chunkworlds/world/${player.uniqueId}",
                                    playerWorldInfo.serverName, "chunkworld_trust", playerTrustsString
                                )
                                sender.sendMessage("§a你们可以共享世界了，§c注意保护好自身世界")
                                player.sendMessage("§c${sender.name}已同意和你共享世界")
                                //如果是在对方世界，应该改为生存模式
                                if (player.world.name == "chunkworlds/world/${sender.uniqueId}" || player.world.name == "chunkworlds/nether/${sender.uniqueId}") {
                                    player.gameMode = org.bukkit.GameMode.SURVIVAL
                                }
                                if (sender.world.name == "chunkworlds/world/${player.uniqueId}" || sender.world.name == "chunkworlds/nether/${player.uniqueId}") {
                                    sender.gameMode = org.bukkit.GameMode.SURVIVAL
                                }
                                return@submit
                            } else {
                                sender.sendMessage("§c此邀请已过期")
                                return@submit
                            }
                        } else {
                            removeTrust(player, sender)
                            sender.sendMessage("§a你拒绝了共享世界邀请")
                            player.sendMessage("§c${sender.name}拒绝了你的共享世界邀请")
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    private val ban = command<Player>("ban") {
        booleans { bool ->
            players {
                exec {
                    val result = valueOf(bool)
                    val player = valueOf(it)
                    if (player == null) {
                        sender.sendMessage("§c此玩家已不在本服或离线")
                        return@exec
                    }
                    submit(async = true) {
                        if (result) {
                            val senderDao = ChunkWorld.db.getPlayerDao(sender.name)!!
                            val banners = ChunkWorld.db.getBanNames(senderDao.id)
                            if (!banners.contains(player.name)) {
                                sender.sendMessage("§c你和此玩家已经没有相互拉黑了")
                                return@submit
                            }
                            if (isBan(player, sender)) {
                                removeBan(player, sender)
                                val playerDao = ChunkWorld.db.getPlayerDao(player.name)!!
                                val senderBans = ChunkWorld.db.getBanNames(senderDao.id).joinToString("|,;|")
                                val playerBans = ChunkWorld.db.getBanNames(playerDao.id).joinToString("|,;|")
                                val senderWorldInfo = RedisManager.getWorldInfo(sender.name)!!
                                val playerWorldInfo = RedisManager.getWorldInfo(player.name)!!
                                RedisPush.worldSetPersistent(
                                    "chunkworlds/world/${sender.uniqueId}",
                                    senderWorldInfo.serverName, "chunkworld_ban", senderBans
                                )
                                RedisPush.worldSetPersistent(
                                    "chunkworlds/world/${player.uniqueId}",
                                    playerWorldInfo.serverName, "chunkworld_ban", playerBans
                                )
                                ChunkWorld.db.delShip(senderDao.id, playerDao.id, false)
                                sender.sendMessage("§a你们已解除拉黑关系，可以去对方的世界了")
                                player.sendMessage("§c${sender.name}已和你解除拉黑关系")
                                return@submit
                            } else {
                                sender.sendMessage("§c此申请已过期")
                                return@submit
                            }
                        } else {
                            removeBan(player, sender)
                            sender.sendMessage("§a已拒绝解除拉黑的申请")
                            player.sendMessage("§c${sender.name}拒绝了你的解除拉黑的申请")
                        }
                    }
                }
            }
        }
    }
}