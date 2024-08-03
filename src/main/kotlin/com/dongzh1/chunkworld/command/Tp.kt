package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.redis.RedisPush
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.time.Duration

object Tp {

    private val cooldown = mutableListOf<String>()
    fun addCooldown(player: Player) {
        cooldown.add("${player.uniqueId}")
        submit(delay = 20 * 20) {
            cooldown.remove(player.name)
        }
    }

    fun isCooldown(player: Player): Boolean {
        return cooldown.contains("${player.uniqueId}")
    }

    private fun removeCooldown(player: Player) {
        cooldown.remove("${player.uniqueId}")
    }

    private fun connect(player: Player, server: String) {
        val byteArray = ByteArrayOutputStream()
        val out = DataOutputStream(byteArray)
        try {
            out.writeUTF("Connect")
            out.writeUTF(server)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        player.sendPluginMessage(ChunkWorld.inst, "BungeeCord", byteArray.toByteArray())
    }

    fun toSelfWorld(p: Player, serverName: String, id: Int) {
        p.showTitle(
            Title.title(
                Component.text("§d构建世界"), Component.text("§f请您稍等，正在构建您的专属世界"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(13), Duration.ofSeconds(1))
            )
        )
        val perm = p.hasPermission("chunkworld.vip")
        RedisPush.createWorld(serverName, p.uniqueId, p.name, id, perm).thenAccept {
            removeCooldown(p)
            if (it != null) {
                connect(p, serverName)
            } else {
                p.showTitle(
                    Title.title(
                        Component.text("§c构建失败"), Component.text("§f请更换服务器,或联系管理员"),
                        Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1))
                    )
                )
                p.teleportAsync(ChunkWorld.spawnLocation)
            }
        }
    }

}