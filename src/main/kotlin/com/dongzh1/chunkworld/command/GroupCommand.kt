package com.dongzh1.chunkworld.command


import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.basic.MainGui
import com.dongzh1.chunkworld.plugins.fawe
import com.dongzh1.chunkworld.redis.RedisManager
import com.dongzh1.chunkworld.redis.RedisPush
import com.xbaimiao.easylib.bridge.economy.Vault
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File


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
    private val worldName = ArgNode("world", exec = {
        Bukkit.getWorlds().map { it.name }
    }, parse = {
        it
    })
    private val temples = ArgNode("temples", exec = {
        listOf("文件名，拒绝大写，中文")
    }, parse = {
        it
    })

    private fun findSchemFilesWithRelativePaths(folderPath: String): List<String> {
        val schemFiles = mutableListOf<String>()
        val folder = File(folderPath)
        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        val relativePath = file.name
                        val subFolderSchemFiles = findSchemFilesWithRelativePaths(file.absolutePath)
                        subFolderSchemFiles.forEach { schemFile ->
                            schemFiles.add("$relativePath/$schemFile")
                        }
                    } else if (file.name.endsWith(".schem")) {
                        schemFiles.add(file.name)
                    }
                }
            }
        }
        return schemFiles
    }

    private val schem = ArgNode("schem", exec = {
        findSchemFilesWithRelativePaths("/home/pixelServer/temples")
    }, parse = {
        File("/home/pixelServer/temples/$it")
    })
    private val rare = ArgNode("rare", exec = {
        listOf("普通", "稀有", "史诗", "传说", "唯一")
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

    /**
     * 突发奇想，购买菜单指令
     */
    @CommandBody
    private val buymenu = command<Player>("buymenu") {
        description = "购买菜单"
        exec {
            //根据玩家的经济情况进行扣款
            var money = (Vault()[sender] * 0.01).toInt()
            if (money < 1) {
                money = 0
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mythicmobs items give -ds ${sender.name} 梦境菜单")
            Vault().take(sender, money.toDouble())
            sender.sendMessage("§a你已花费${money}元购买§6梦境菜单")

        }
    }

    @CommandBody
    private val world = command<Player>("world") {
        permission = "chunkworld.admin"
        description = "传送到对应世界出生点"
        arg(worldName) { name ->
            exec {
                sender.teleportAsync(Bukkit.getWorld(valueOf(name))!!.spawnLocation)
            }
        }
    }

    @CommandBody
    private val chunk = command<Player>("chunk") {
        permission = "chunkworld.admin"
        description = "在创世神内选择玩家所在区块"
        arg(temples) { temples ->
            arg(rare) { rare ->
                exec {
                    val rareString =
                        when (valueOf(rare)) {
                            "普通" -> "putong"
                            "稀有" -> "xiyou"
                            "史诗" -> "shishi"
                            "传说" -> "chuanshuo"
                            "唯一" -> "weiyi"
                            else -> "其他"
                        }
                    val chunk = sender.chunk
                    if (fawe.saveSchem(chunk, valueOf(temples), rareString)) {
                        sender.sendMessage("§a已保存区块")
                    } else {
                        sender.sendMessage("§c保存区块失败")
                    }
                }
            }
        }
    }

    @CommandBody
    private val test = command<Player>("test") {
        description = "dong专用指令，用于测试效果不明的代码，目前用于测试保存的区块"
        permission = "chunkworld.admin"
        arg(schem) { schem ->
            exec {
                val pos1: Location
                val world = sender.world
                val chunk = sender.chunk
                if (world.environment == World.Environment.NORMAL) {
                    pos1 = Location(world, chunk.x * 16.toDouble(), -64.0, chunk.z * 16.toDouble())
                } else if (sender.world.environment == World.Environment.NETHER) {
                    pos1 = Location(world, chunk.x * 16.toDouble(), 0.0, chunk.z * 16.toDouble())
                } else return@exec
                sender.chunk.entities.filter { it !is Player }.forEach {
                    it.remove()
                }
                fawe.placeSchem(valueOf(schem), pos1)
            }
        }
        /*
            exec {
                        /*
                        val manager: HologramManager = FancyHologramsPlugin.get().hologramManager
                        val hologramData = TextHologramData("hologram_name", sender.location)
                        hologramData.setBackground(Color.fromRGB(255, 65, 125))
                        hologramData.setBillboard(Display.Billboard.FIXED)
                        val hologram: Hologram = manager.create(hologramData)
                        manager.addHologram(hologram)

                        val loc = sender.location
                        fawe.placeSchem("baozang.schem",loc)
                        */
                    //测试石化区块
                //fawe.replaceEx(sender.chunk)
            }

         */
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

    @CommandBody
    private val worldload = command<Player>("worldload") {
        permission = "chunkworld.admin"
        booleans { bool ->
            arg("世界名") { worldname ->
                exec {
                    if (valueOf(bool)) {
                        val world = Bukkit.createWorld(WorldCreator(valueOf(worldname)))
                        if (world == null) {
                            sender.sendMessage("§c世界加载失败")
                            return@exec
                        }
                        sender.teleportAsync(world.spawnLocation)
                    } else {
                        //卸载
                        val world = Bukkit.getWorld(valueOf(worldname))
                        if (world == null) {
                            sender.sendMessage("§c世界没加载")
                            return@exec
                        }
                        world.players.forEach {
                            it.teleport(ChunkWorld.spawnLocation)
                        }
                        Bukkit.unloadWorld(world, true)
                    }
                }
            }
        }
    }

    @CommandBody
    val reload = command<CommandSender>("reload") {
        permission = "chunkworld.admin"
        exec {
            ChunkWorld.inst.reloadConfig()
            sender.sendMessage("§a重载配置文件成功,仅针对宝箱配置可重载")
        }
    }
}