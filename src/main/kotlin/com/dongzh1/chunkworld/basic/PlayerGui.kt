package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.command.GroupCommand
import com.dongzh1.chunkworld.redis.RedisManager
import com.dongzh1.chunkworld.redis.RedisPush
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player

class PlayerGui(private val p: Player, private val banPage: Int) {
    fun build() {
        val basic = PaperBasic(p, Component.text("人员管理"))
        basic.rows(6)
        basic.set(8, buildItem(Material.PAPER, builder = {
            customModelData = 300006
            name = "§f빪 §x§1§9§c§a§a§d➠ 关闭菜单"
        }))
        basic.onClick(8) { p.closeInventory() }
        basic.set(17, buildItem(Material.PAPER, builder = {
            customModelData = 300012
            name = "§f빪 §x§1§9§c§a§a§d➠ 返回上级菜单"
        }))
        basic.onClick(17) {
            MainGui(p).build()
        }
        basic.set(10, buildItem(Material.SKELETON_SKULL, builder = {
            name = "信任玩家"
            lore.add(" ")
            lore.add("§f右侧是和你共享世界的玩家")
            lore.add("§f你们可以相互在对方世界破坏和建造")
        }))
        basic.set(22, buildItem(Material.WITHER_SKELETON_SKULL, builder = {
            name = "拉黑玩家"
            lore.add(" ")
            lore.add("§f下方是和你双向拉黑的玩家")
            lore.add("§f你们无法进入对方的世界")
        }))
        for (i in 11..16) {
            basic.set(i, buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                name = "§a空位置"
                lore.add("§f右键玩家可打开护照")
                lore.add("§f护照内可申请共享世界")
            }))
        }
        val banSlots = listOf(28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43, 46, 47, 48, 49, 50, 51, 52)
        banSlots.forEach {
            basic.set(it, buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                name = "§a空位置"
                lore.add("§f蹲下右键玩家可打开护照")
                lore.add("§f护照内可拉黑玩家")
            }))
        }
        val playerDao = ChunkWorld.db.getPlayerDao(p.name)!!
        val trusts = ChunkWorld.db.getTrustNames(playerDao.id)
        val bans = ChunkWorld.db.getBanNames(playerDao.id)
        val banList =
            if (bans.size <= (banPage - 1) * banSlots.size) {
                emptyList()
            } else if (bans.size <= banPage * banSlots.size) {
                bans.subList((banPage - 1) * banSlots.size, bans.size)
            } else {
                bans.subList((banPage - 1) * banSlots.size, banPage * banSlots.size)
            }

        for (i in trusts.indices) {
            val pname = trusts[i]
            basic.set(11 + i, buildItem(Material.PLAYER_HEAD, builder = {
                name = "§a$pname"
                skullOwner = pname
                lore.add("§b点击不再和该玩家共享世界")
            }))
            basic.onClick(11 + i) {
                //改变对应物品
                it.view.setItem(11 + i, buildItem(Material.LIME_STAINED_GLASS_PANE, builder = {
                    name = "§a空位置"
                    lore.add("§f蹲下右键玩家可打开护照")
                    lore.add("§f护照内可申请共享世界")
                }))
                submit(async = true) {
                    val targetDao = ChunkWorld.db.getPlayerDao(pname)!!
                    ChunkWorld.db.delShip(playerDao.id, targetDao.id, true)
                    val playerTrusts = ChunkWorld.db.getTrustNames(playerDao.id).joinToString("|,;|")
                    val playerWorldInfo = RedisManager.getWorldInfo(p.name)!!
                    RedisPush.cancelFriend(pname, p.name, p.uniqueId)
                    if (p.world.name == "chunkworlds/world/${targetDao.uuid}" || p.world.name == "chunkworlds/nether/${targetDao.uuid}") {
                        p.sendMessage("§c你和${pname}的共享世界已解除")
                        p.gameMode = GameMode.ADVENTURE
                    }
                    //对应的世界key也要改变
                    RedisPush.worldSetPersistent(
                        "chunkworlds/world/${p.uniqueId}",
                        playerWorldInfo.serverName, "chunkworld_trust", playerTrusts
                    )
                    val targetWorldInfo = RedisManager.getWorldInfo(pname) ?: return@submit
                    val targetTrusts = ChunkWorld.db.getTrustNames(targetDao.id).joinToString("|,;|")
                    RedisPush.worldSetPersistent(
                        "chunkworlds/world/${targetDao.uuid}",
                        targetWorldInfo.serverName, "chunkworld_trust", targetTrusts
                    )
                }
            }
        }
        for (i in banList.indices) {
            val pname = banList[i]
            basic.set(banSlots[i], buildItem(Material.PLAYER_HEAD, builder = {
                name = "§a$pname"
                skullOwner = pname
                lore.add("§b点击申请解除相互拉黑关系")
            }))
            basic.onClick(banSlots[i]) {
                p.closeInventory()
                val targetPlayer = Bukkit.getPlayerExact(pname)
                if (targetPlayer == null) {
                    p.sendMessage("§c此玩家不在本服或离线")
                    return@onClick
                }
                if (GroupCommand.isBan(p, targetPlayer)) {
                    p.sendMessage("§c你已经发送过申请了")
                    return@onClick
                }
                submit(async = true) {
                    val banners = ChunkWorld.db.getBanNames(playerDao.id)
                    if (!banners.contains(banList[i])) {
                        p.sendMessage("§c你们已经不处于拉黑关系了")
                        return@submit
                    }
                    GroupCommand.addBan(p, targetPlayer)
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
            if (banPage > 1) {
                p.closeInventory()
                PlayerGui(p, banPage - 1)
            }
        }
        basic.set(44, buildItem(Material.PAPER, builder = {
            customModelData = 300005
            name = "§f빪 §x§1§9§c§a§a§d➠ 下一页"
        }))
        basic.onClick(44) {
            if (it.view.getItem(52)?.type == Material.PLAYER_HEAD) PlayerGui(p, banPage + 1)
        }
        basic.openAsync()
    }
}