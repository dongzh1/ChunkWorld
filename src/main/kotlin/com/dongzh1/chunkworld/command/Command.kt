package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.basic.ConfirmExpandGui
import com.google.common.io.ByteStreams
import com.sk89q.worldedit.world.block.BlockTypes
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import org.bukkit.Location
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@ECommandHeader(command = "chunkworld")
object Command {
    /**
     * 重载
     */
    @CommandBody
    val reload = command<Player>("reload") {
        description = "重载插件"
        permission = "chunkworld.reload"
        exec {
            WorldEdit.copyChunk(sender.chunk, sender.world.getChunkAt(sender.chunk.x + 1, sender.chunk.z + 1))
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