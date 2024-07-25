package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.spawnLocation
import com.dongzh1.chunkworld.basic.ListGui
import com.dongzh1.chunkworld.redis.RedisData
import com.xbaimiao.easylib.bridge.replacePlaceholder


import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import com.xbaimiao.easylib.util.buildItem
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

@ECommandHeader(command = "chunkworld")
object GroupCommand {
    private val environment = ArgNode(("environment"),
        exec = {
            listOf("NORMAL", "NETHER")
        }, parse = {
            Environment.valueOf(it)
        })
    private val name = ArgNode("name", exec = {
        listOf("玩家名")
    }, parse = {
        it
    })
    @CommandBody
    private val test = command<Player>("test"){
        number {
            exec {
                if (valueOf(it) < 0)
                ListGui(sender,1,false).build()
                else {
                    val item = sender.inventory.itemInMainHand.itemMeta
                    item.setCustomModelData(valueOf(it).toInt())
                    sender.inventory.itemInMainHand.itemMeta = item
                }
            }
        }

    }

    @CommandBody
    private val tp = command<Player>("tp") {
        description = "传送到自己或别人世界，自己没有的话会创建,只有op有权限通过此指令去别人家"
        arg(name, optional = true) {
            exec {
                val name = valueOfOrNull(it)?:sender.name
                launchCoroutine(SynchronizationContext.ASYNC) {
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
                    Tp.to(playerDao.teleportWorldName,
                        playerDao.teleportX,
                        playerDao.teleportY,
                        playerDao.teleportZ,
                        playerDao.teleportYaw,
                        playerDao.teleportPitch,
                        sender,playerDao)
                }
            }
        }
    }
}