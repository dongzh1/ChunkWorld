package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import org.bukkit.World.Environment
import org.bukkit.command.CommandSender
import java.io.File

@ECommandHeader(command = "chunkworld")
object GroupCommand {
    private val environment = ArgNode(("environment"),
        exec = {
            listOf("NORMAL","NETHER","THE_END")
        }, parse = {
            Environment.valueOf(it)
        })
    private val yaw = ArgNode(("yaw"),
        exec = {
            listOf("0","45","90")
        }, parse = {
            it.toFloat()
        }
    )
    @CommandBody
    val create = command<CommandSender>("create"){
        description = "创建此玩家世界并决定是否创建完毕后传送玩家过去"
        permission = "chunkworld.admin"
        arg(environment){ environmentArg ->
            players { playersArg ->
                booleans {
                    exec {
                        val envir = valueOf(environmentArg)
                        val p = valueOfOrNull(playersArg)
                        if (p == null){
                            sender.sendMessage("§c此玩家不在线，无法创建")
                            return@exec
                        }
                        //要创建的世界的位置
                        val file :File
                        when(envir){
                            Environment.NORMAL -> {
                                file = File(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world")
                                //有entities文件夹说明是加载过的
                                if (File(file,"entities").exists()) {
                                    //说明这个世界已经创建了
                                    sender.sendMessage("§c此玩家主世界已创建,不能重复创建")
                                    return@exec
                                }
                            }
                            Environment.NETHER -> {
                                file = File(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/nether")
                                //地狱世界格式和主世界不一样
                                if (File(file,"DIM-1/entities").exists()) {
                                    //说明这个世界已经创建了
                                    sender.sendMessage("§c此玩家地狱世界已创建,不能重复创建")
                                    return@exec
                                }
                            }
                            Environment.THE_END -> {
                                file = File(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/end")
                                //末地世界格式和主世界不一样
                                if (File(file,"DIM1/entities").exists()) {
                                    //说明这个世界已经创建了
                                    sender.sendMessage("§c此玩家末地世界已创建,不能重复创建")
                                    return@exec
                                }
                            }
                            else -> {
                                sender.sendMessage("§c世界类型错误")
                                return@exec
                            }
                        }

                    }
                }
            }
        }
    }
}