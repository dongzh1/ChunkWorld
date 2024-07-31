package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.redis.RedisData
import com.dongzh1.chunkworld.redis.RedisManager
import com.dongzh1.chunkworld.redis.RedisPush
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.util.TriState
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.abs
import kotlin.random.Random

object Tp {

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

    private fun couldTp(p:Player, playerDao: PlayerDao):Pair<Boolean,String?>{
        if (p.name == playerDao.name){return true to null}
        val ship = RedisData.getFriendsAndBanner(p.uniqueId.toString()) ?: return false to "§c你的关系数据还未加载完毕，请稍后..."
        val banners = ship.second
        val friends = ship.first
        if (banners.contains(playerDao.uuid.toString())){
            //互相拉黑状态
            return false to "§c目标世界和你处于拉黑状态,无法传送"
        }
        when(playerDao.worldStatus){
            0.toByte() -> {}
            1.toByte() -> {
                //共享玩家才能进
                if (!friends.contains(playerDao.uuid.toString())){
                    return false to "§c目标世界只对共享玩家开放,无法传送"
                }
            }
            2.toByte() -> {
                //关闭状态
                return false to "§c目标世界仅允许世界主人进入,无法传送"
            }
        }
        return true to null
    }

    /**
     * 把玩家传送到指定的世界的指定坐标
     */
    fun toPlayerWorld(p: Player, playerDao: PlayerDao){

        //经过判断，不能去这个玩家世界
        val result = couldTp(p,playerDao)
        if (!result.first){
            p.sendMessage(result.second!!)
            return
        }
        //确实世界是可以传送的，接下来世界方面有没有问题
        //计时器，3秒后传送
        var n = 0
        //玩家禁止时的坐标
        val stop = p.location
        submit(delay = 1,period = 20, maxRunningNum = 4) {
            //如果玩家移动了，取消传送,判断距离为0.1
            if (abs(p.location.x - stop.x) > 0.1 || abs(p.location.y - stop.y) > 0.1 || abs(p.location.z - stop.z) > 0.1){
                cancel()
                p.sendMessage("§c你移动了,传送取消")
                return@submit
            }
            if (n == 3) {
                var world = Bukkit.getWorld(playerDao.tName)
                if (world != null){
                    //就在本服，直接传送
                    p.sendMessage("§a已确定世界坐标,正在传送...")
                    p.teleportAsync(Location(world,playerDao.tX,playerDao.tY,playerDao.tZ,playerDao.tYaw,playerDao.tPitch))
                }else{
                    p.sendMessage("§a正在确定世界坐标...")
                    //不在本服。群组搜索
                    RedisPush.teleportWorld(p.name,playerDao.tName,playerDao.tX,playerDao.tY,playerDao.tZ).thenAccept {
                        if (it != null){
                            p.sendMessage("§a已确定世界坐标,正在传送...")
                            //世界找到了，传送过去
                            connect(p,it)
                        }else{
                            //加载玩家世界，有playerDao，说明不是第一次创建
                            val server = RedisManager.getHighestTpsServerName()
                            if (server == null){
                                p.sendMessage("§c没有可用服务器，请联系管理员")
                                return@thenAccept
                            }
                            if (server == ChunkWorld.inst.config.getString("serverName")!!){
                                //在本服加载
                                val worldCreator = WorldCreator(playerDao.tName).keepSpawnLoaded(TriState.FALSE)
                                world = worldCreator.createWorld()
                                if (world != null){
                                    //加载成功
                                    p.sendMessage("§a已确定世界坐标,正在传送...")
                                    p.teleportAsync(Location(world,playerDao.tX,playerDao.tY,playerDao.tZ,playerDao.tYaw,playerDao.tPitch))
                                }else{
                                    //加载失败
                                    p.sendMessage("§c世界加载失败，请联系管理员")
                                }
                            }else{
                                //其他服加载
                                RedisPush.loadWorldTeleport(server,playerDao.tName,playerDao.tX,playerDao.tY,playerDao.tZ,p.name).thenAccept { success ->
                                    if (success != null){
                                        p.sendMessage("§a已确定世界坐标,正在传送...")
                                        //跨服
                                        connect(p,server)
                                    }else{
                                        //创建世界失败
                                        p.sendMessage("§c世界加载失败，请联系管理员")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(n < 3)
                p.sendMessage("§a ${3-n} 秒后进行传送，请不要移动!")
            n++
        }
    }

    /**
     * 创建玩家世界并传送
     * 这个方法只会被异步调用
     */
    fun createTp(p:Player){
        launchCoroutine(SynchronizationContext.ASYNC) {
            val serverName = RedisManager.getHighestTpsServerName()
            if (serverName == null){
                p.sendMessage("§c没有可用服务器，请联系管理员")
                return@launchCoroutine
            }
            //复制世界
            val file = File("chunkworlds/world/${p.uniqueId}")
            val templeFile = File(ChunkWorld.inst.dataFolder, "world")
            //有level.dat_old文件说明是加载过的
            if (File(file,"level.dat_old").exists()) {
                 //此世界已被加载过
                p.sendMessage("§c你的世界已被加载过，但没有数据，请联系管理员")
                return@launchCoroutine
            }
            try {
                templeFile.copyRecursively(file)
            }catch (ex:Exception) {
                //踢出玩家并提示联系管理员
                p.sendMessage("§c你的世界创建失败，原因为复制失败，请尽快联系管理员")
                error("${p.name}玩家的 chunkworlds/world/${p.uniqueId} 世界复制文件失败")
            }
            //复制完毕，加载世界
            if (serverName == ChunkWorld.inst.config.getString("serverName")!!) {
                createWorldLocal(p.uniqueId,p.name).thenAccept {
                    if (it.first){
                        //创建成功
                        p.sendMessage("§a世界创建成功，正在传送...")
                        val playerDao = it.second!!
                        toPlayerWorld(p,playerDao)
                    }else {
                        //创建失败
                        p.sendMessage("§c加载世界失败,请联系管理员,错误原因，创建失败")
                    }
                }
            }else{

                RedisPush.createWorld(serverName,p.uniqueId,p.name).thenAccept {
                    if (it != null){
                        //创建成功
                        p.sendMessage("§a世界创建成功，正在传送...")
                        connect(p,serverName)
                    }else {
                        //创建失败
                        p.sendMessage("§c加载世界失败,请联系管理员,在${serverName}创建失败")
                    }
                }
            }
        }
    }
    fun createWorldLocal(playerUUID: UUID,playerName:String):CompletableFuture<Pair<Boolean,PlayerDao?>>{
        val future = CompletableFuture<Pair<Boolean,PlayerDao?>>()
        launchCoroutine(SynchronizationContext.SYNC) {
            //在本服创建
            val worldName = "chunkworlds/world/$playerUUID"
            val wc = WorldCreator(worldName).keepSpawnLoaded(TriState.FALSE)
            val world = wc.createWorld()
            if (world == null){
                future.apply { complete(false to null) }
                return@launchCoroutine
            }
            //设置世界规则等
            world.isAutoSave = true
            world.setGameRule(GameRule.KEEP_INVENTORY,true)
            world.setGameRule(GameRule.SPAWN_CHUNK_RADIUS,0)
            world.setGameRule(GameRule.DO_FIRE_TICK,false)
            world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS,false)
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,true)
            //这里是第一次加载，通过worldedit插件复制屏障到占领的区块边缘
            WorldEdit.setBarrier(
                setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z),
                world.spawnLocation.chunk.x to world.spawnLocation.chunk.z,
                world
            )
            //存储世界
            world.save()
            SynchronizationContext.ASYNC
            var playerDao = PlayerDao().apply {
                name = playerName
                uuid = playerUUID
                createTime = java.text.SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒").format(java.util.Date(System.currentTimeMillis()))
                spawn = "${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}"
                netherSpawn = "null"
                worldStatus = 0
                lastTime = System.currentTimeMillis()
                teleport = "world,${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}"
            }
            //玩家数据存入数据库
            ChunkWorld.db.playerCreate(playerDao)
            //取出玩家数据，获取id
            playerDao = ChunkWorld.db.playerGet(playerName)!!
            //出生的区块也存入
            val chunkDao = ChunkDao().apply {
                playerID = playerDao.id
                x = world.spawnLocation.chunk.x
                z = world.spawnLocation.chunk.z
                worldType = 0
            }
            //区块数据存入数据库
            ChunkWorld.db.chunkCreate(chunkDao)
            //新建了玩家数据，可以存入内存
            RedisData.setPlayerDao(playerDao)
            RedisData.setChunks(playerUUID.toString(), listOf(chunkDao))
            future.complete(true to playerDao)
        }
        return future
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