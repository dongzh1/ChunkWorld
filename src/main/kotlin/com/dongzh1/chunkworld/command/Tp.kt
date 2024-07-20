package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.listener.SingleListener
import com.dongzh1.chunkworld.listener.SingleListener.isBeBan
import com.dongzh1.chunkworld.listener.SingleListener.isBeTrust
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import kotlin.math.abs
import kotlin.random.Random

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
            val playerDao = SingleListener.getPlayerDaoMap(playerName)!!
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
        var playerDao = SingleListener.getPlayerDaoMap(playerName)
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
            if (!player.isOp){
                when(playerDao!!.worldStatus) {
                    //玩家世界开放
                    0.toByte() -> {
                        //如果是黑名单，也无法进入
                        if (isBeBan(player,playerDao!!.uuid)) {
                            //取消传送任务
                            task.cancel()
                            player.sendMessage("§c此家园禁止你访问")
                            return@launchCoroutine
                        }
                    }
                    1.toByte() -> {
                        //部分开放，看看是否被信任
                        if (!isBeTrust(player,playerDao!!.uuid)) {
                            task.cancel()
                            player.sendMessage("§c此家园只允许共享家园的玩家访问")
                            return@launchCoroutine
                        }
                    }
                    //玩家世界仅对自己开放
                    2.toByte() -> {
                        //取消传送任务
                        player.sendMessage("§c此家园禁止他人访问")
                        task.cancel()
                        return@launchCoroutine
                    }
                }
            }
            //现在应该都是符合访问条件的
            switchContext(SynchronizationContext.SYNC)
            //先获取，如果没有加载就加载
            val world = Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${playerDao!!.uuid}")?:
                Bukkit.createWorld(WorldCreator(ChunkWorld.inst.config.getString("World")!!+"/${playerDao!!.uuid}"))
            //世界已经加载了，如果内存中没有这个世界的信息就加入一下
            if (SingleListener.getPlayerDaoMap(playerName) == null){
                SingleListener.setPlayerDaoMap(playerName,playerDao!!)
                SingleListener.setUUIDtoName(playerDao!!.uuid,playerName)
            }
            target = Location(world, playerDao!!.x(), playerDao!!.y(), playerDao!!.z(), playerDao!!.yaw(), playerDao!!.pitch())
        }
    }

    fun randomTp(p:Player,world: World,range:Int){
        p.sendMessage("")
        val x = Random.nextInt(-range,range)
        val z = Random.nextInt(-range,range)
        submit(async = true) {
            //异步获取对应的信息，主线程再传送和修改
            when(world.environment){
                World.Environment.NETHER -> {
                    var locY:Int = 121
                    for (y in 120 downTo 32) {
                        if (isSafeLocation(world,x,y,z)){
                            locY = y
                            break
                        }
                    }
                    if (locY == 121) locY = 64
                    submit {
                        if (locY == 64 && !isSafeLocation(world,x,locY,z)){
                            world.getBlockAt(x,locY,z).type = Material.NETHERRACK
                            world.getBlockAt(x,locY+1,z).type = Material.AIR
                            world.getBlockAt(x,locY+2,z).type = Material.AIR
                        }
                        p.teleportAsync(Location(world,x+0.5,locY+1.0,z+0.5))
                    }
                }
                World.Environment.THE_END -> {
                    var locY:Int = 71
                    for (y in 70 downTo 32) {
                        if (isSafeLocation(world,x,y,z)){
                            locY = y
                            break
                        }
                    }
                    if (locY == 71) locY = 64
                    submit {
                        if (locY == 64 && !isSafeLocation(world,x,locY,z)){
                            world.getBlockAt(x,locY,z).type = Material.END_STONE
                            world.getBlockAt(x,locY+1,z).type = Material.AIR
                            world.getBlockAt(x,locY+2,z).type = Material.AIR
                        }
                        p.teleportAsync(Location(world,x+0.5,locY+1.0,z+0.5))
                    }
                }
                else -> {
                    //只考虑主世界了
                    val y = world.getHighestBlockYAt(x,z)
                    submit {
                        if (!isSafeLocation(world,x,y,z)){
                            world.getBlockAt(x,y,z).type = Material.STONE
                            world.getBlockAt(x,y+1,z).type = Material.AIR
                            world.getBlockAt(x,y+2,z).type = Material.AIR
                        }
                        p.teleportAsync(Location(world,x+0.5,y+1.0,z+0.5))
                    }
                }
            }
        }

    }
    private fun isSafeLocation(world: World, x: Int, y: Int, z: Int): Boolean {
        val block = world.getBlockAt(x, y, z).type
        val blockAbove = world.getBlockAt(x, y+1, z).type
        val blockAbove2 = world.getBlockAt(x, y + 2, z).type

        // 检查传送位置是否安全（例如，方块下方是固体，上方是空气）
        return (!block.isAir && block != Material.WATER && block != Material.LAVA
                && blockAbove.isAir && blockAbove2.isAir)
    }
}