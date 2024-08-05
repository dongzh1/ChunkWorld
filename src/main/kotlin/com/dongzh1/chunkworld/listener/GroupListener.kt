package com.dongzh1.chunkworld.listener

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.basic.Item
import com.dongzh1.chunkworld.basic.ServerGui
import com.dongzh1.chunkworld.command.Tp
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.*
import org.spigotmc.event.player.PlayerSpawnLocationEvent

object GroupListener : Listener {
    private val delinePlayers = mutableListOf<String>()
    private val serverMap = mutableMapOf<Player, Pair<String, Int>>()
    fun addServerMap(player: Player, serverName: String, id: Int) {
        serverMap[player] = serverName to id
        submit(async = true, delay = 20 * 300) { serverMap.remove(player) }
    }
    fun removeServerMap(player: Player) {
        serverMap.remove(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSpawn(e: PlayerSpawnLocationEvent) {
        //确定出生点
        e.player.respawnLocation = ChunkWorld.spawnLocation
        e.spawnLocation = ChunkWorld.spawnLocation
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun respawn(e: PlayerRespawnEvent) {
        e.respawnLocation = ChunkWorld.spawnLocation
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        val item8 = p.inventory.getItem(8)
        if (item8 == null){
            p.inventory.setItem(8, Item.menuItem)
        }else{
            if (item8 != Item.menuItem){
                p.inventory.setItem(8,Item.menuItem)
                p.world.dropItem(p.location,item8)
            }
        }
    }
    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        if (e.item == Item.menuItem) {
            e.isCancelled = true
            ServerGui(e.player).build()
        }
    }
    @EventHandler
    fun drop(e: PlayerDropItemEvent){
        if (e.itemDrop.itemStack == Item.menuItem){
            e.isCancelled = true
        }
    }
    @EventHandler
    fun click(e: InventoryClickEvent){
        if (e.currentItem == Item.menuItem){
            e.isCancelled = true
        }
        if (e.cursor == Item.menuItem){
            e.isCancelled = true
        }
    }
    @EventHandler
    fun drag(e: InventoryDragEvent){
        if (e.oldCursor == Item.menuItem){
            e.isCancelled = true
        }
        if (e.cursor == Item.menuItem){
            e.isCancelled = true
        }
        if (e.newItems.values.contains(Item.menuItem)){
            e.isCancelled = true
        }

    }
    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        e.quitMessage(null)
    }

    @EventHandler
    fun resource(e: PlayerResourcePackStatusEvent) {
        val serverInfo = serverMap[e.player] ?: return
        if (e.status == PlayerResourcePackStatusEvent.Status.DECLINED) {
            if (!delinePlayers.contains(e.player.name)) {
                delinePlayers.add(e.player.name)
                e.player.kick(
                    Component.text("§c请您接受资源包，否则游玩效果会差很多哦").appendNewline()
                        .append(Component.text("§f请在[§b多人游戏§f]界面 选中 §e像素物语§f 点击 §e编辑§f 然后将 §e服务器资源包§f调整为 §e启用§f"))
                        .appendNewline()
                        .append(Component.text("§7如果您实在不想启用资源包，可以再次进入服务器，不会再次弹出提示"))
                )
            } else {
                Tp.addCooldown(e.player)
                submit(async = true) {
                    Tp.toSelfWorld(e.player, serverInfo.first, serverInfo.second)
                }
                serverMap.remove(e.player)
            }
            return
        }
        if (e.status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            if (delinePlayers.contains(e.player.name)) {
                delinePlayers.remove(e.player.name)
            }
            Tp.addCooldown(e.player)
            submit(async = true) {
                Tp.toSelfWorld(e.player, serverInfo.first, serverInfo.second)
            }
            serverMap.remove(e.player)
            return
        }
        if (e.status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            if (!delinePlayers.contains(e.player.name)) {
                delinePlayers.add(e.player.name)
                e.player.kick(
                    Component.text("§c资源包下载失败，请重新进入服务器").appendNewline()
                        .append(Component.text("§f再次进入时你可以跳过资源包进行游玩，请告诉管理员资源包下载失败"))
                )
            } else {
                Tp.addCooldown(e.player)
                submit(async = true) {
                    Tp.toSelfWorld(e.player, serverInfo.first, serverInfo.second)
                }
                serverMap.remove(e.player)
            }
            return
        }
        if (e.status == PlayerResourcePackStatusEvent.Status.FAILED_RELOAD) {
            if (!delinePlayers.contains(e.player.name)) {
                delinePlayers.add(e.player.name)
                e.player.kick(
                    Component.text("§c资源包加载失败，请重新进入服务器").appendNewline()
                        .append(Component.text("§f再次进入时你可以跳过资源包进行游玩，请告诉管理员资源包加载失败"))
                )
            } else {
                Tp.addCooldown(e.player)
                submit(async = true) {
                    Tp.toSelfWorld(e.player, serverInfo.first, serverInfo.second)
                }
                serverMap.remove(e.player)
            }
            return
        }
    }
}