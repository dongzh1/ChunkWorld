package com.dongzh1.chunkworld.command

import com.xbaimiao.easylib.bridge.player.parseECommand
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission

object FakeOPUtil {

    /**
     * 使用玩家为op执行一段命令，op是假的
     */
    fun execCommand(player: Player, commands: List<String>) {
        commands.parseECommand(player).exec(object : Player by player {
            override fun hasPermission(name: String): Boolean {
                return true
            }

            override fun hasPermission(perm: Permission): Boolean {
                return true
            }

            override fun isOp(): Boolean {
                return true
            }
        })
    }

}