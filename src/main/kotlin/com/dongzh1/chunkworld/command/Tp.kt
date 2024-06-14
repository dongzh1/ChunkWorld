package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.math.abs

object Tp {
    //去指定玩家的世界
    fun to(playerName: String,player: Player) {
        //计时器，3秒后传送
        var n = 0
        //玩家要传送到的坐标
        var target :Location?= null
        //玩家禁止时的坐标
        val stop = player.location
        val task = submit(delay = 1,period = 20, maxRunningNum = 4) {
            //如果玩家移动了，取消传送,判断距离为0.1
            if (abs(player.location.x - stop.x) > 0.1 || abs(player.location.y - stop.y) > 0.1 || abs(player.location.z - stop.z) > 0.1){
                cancel()
                player.sendMessage("§c你移动了,传送取消")
                return@submit
            }
            if (n == 3) {
                //如果坐标加载了，就传送，不管传不传送，这个任务都取消
                if (target != null) player.teleportAsync(target!!)
            }
            if(n < 3)
            player.sendMessage("§a ${3-n} 秒后进行传送，请不要移动!")
            if (n == 3) player.sendMessage("§a正在传送...")
            n++
        }
        //如果是回自己世界
        if (player.name == playerName){
            val playerDao = Listener.getPlayerDaoMap(playerName)!!
            //获取世界
            var world = Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${playerDao.uuid}")
            if (world == null) {
                //世界没加载，加载一下，一般不会出现这种情况
               world = Bukkit.createWorld(WorldCreator(ChunkWorld.inst.config.getString("World")!!+"/${playerDao.uuid}"))
            }
            target = Location(world,playerDao.x(),playerDao.y(),playerDao.z(),playerDao.yaw(),playerDao.pitch())
            return
        }
        //从内存获取这个玩家的数据
        var playerDao = Listener.getPlayerDaoMap(playerName)
        //不在线或不存在的玩家，从数据库调取看看
        launchCoroutine(SynchronizationContext.ASYNC){
            //说明不是在线玩家，从数据库调取
            if (playerDao == null) playerDao = ChunkWorld.db.playerGet(playerName)
            //玩家不存在
            if (playerDao == null) {
                //取消传送任务
                task.cancel()
                player.sendMessage("§c玩家未找到")
                return@launchCoroutine
            }
            //玩家离线，查看世界能否传送,先看世界状态
            when(playerDao!!.worldStatus) {
                //玩家世界开放
                0.toShort() -> {
                    //如果是黑名单，也无法进入
                    val beBanList = Listener.getBeBanMap(player)!!
                    if (beBanList.contains(playerDao!!.uuid)) {
                        //取消传送任务
                        task.cancel()
                        player.sendMessage("§c此玩家禁止你访问")
                        return@launchCoroutine
                    }
                }
                1.toShort() -> {
                    //部分开放，看看是否被信任
                    val beTrustList = Listener.getBeTrustMap(player)!!
                    if (!beTrustList.contains(playerDao!!.uuid)) {
                        //取消传送任务
                        task.cancel()
                        player.sendMessage("§c此玩家只允许信任的玩家访问")
                        return@launchCoroutine
                    }
                }
                //玩家世界仅对自己开放
                2.toShort() -> {
                    //取消传送任务
                    task.cancel()
                    player.sendMessage("§c此玩家禁止他人访问")
                    return@launchCoroutine
                }
            }
            //现在应该都是符合访问条件的
            switchContext(SynchronizationContext.SYNC)
            //先获取，如果没有加载就加载
            val world = Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${playerDao!!.uuid}")?:
                Bukkit.createWorld(WorldCreator(ChunkWorld.inst.config.getString("World")!!+"/${playerDao!!.uuid}"))
            //世界已经加载了，如果内存中没有这个世界的信息就加入一下
            if (Listener.getPlayerDaoMap(playerName) == null){
                Listener.setPlayerDaoMap(playerName,playerDao!!)
                Listener.setUUIDtoName(playerDao!!.uuid,playerName)
            }
            target = Location(world, playerDao!!.x(), playerDao!!.y(), playerDao!!.z(), playerDao!!.yaw(), playerDao!!.pitch())
        }
    }
}