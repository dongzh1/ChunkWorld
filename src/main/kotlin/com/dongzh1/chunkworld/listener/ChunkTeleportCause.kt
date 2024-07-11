package com.dongzh1.chunkworld.listener

import org.bukkit.event.player.PlayerTeleportEvent
import java.lang.invoke.MethodHandles

/**
 * 用于处理玩家加入游戏后创建或加载家园世界并传送到指定世界
 */
object ChunkTeleportCause {
    val MENU_TELEPORT by lazy { create("MENU_TELEPORT", 12) }

    private fun create(name: String, index: Int): PlayerTeleportEvent.TeleportCause {
        val c = PlayerTeleportEvent.TeleportCause::class.java.getDeclaredConstructor(
            String::class.java,
            Int::class.javaPrimitiveType
        )
        c.isAccessible = true
        val h = MethodHandles.lookup().unreflectConstructor(c)
        return try {
            h.invokeExact(name, index) as PlayerTeleportEvent.TeleportCause
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }
}