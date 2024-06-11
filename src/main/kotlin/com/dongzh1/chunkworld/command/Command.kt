package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.google.common.io.ByteStreams
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@ECommandHeader(command = "chunkworld")
object Command {
    /**
     * 重载
     */
    @CommandBody
    val reload = command<CommandSender>("reload") {
        description = "重载插件"
        permission = "chunkworld.reload"
        exec {
            WorldCreator("world1").createWorld()
            WorldCreator("world1").createWorld()
            WorldCreator("world1").createWorld()
        }
    }

}