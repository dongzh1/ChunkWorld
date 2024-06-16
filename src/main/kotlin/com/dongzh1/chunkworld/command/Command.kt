package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.basic.ConfirmExpandGui
import com.dongzh1.chunkworld.basic.ListGui
import com.dongzh1.chunkworld.basic.SettingGui
import com.google.common.io.ByteStreams
import com.sk89q.worldedit.world.block.BlockTypes
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

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