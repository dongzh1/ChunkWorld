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
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.abs
import kotlin.random.Random

object Tp {
    /**
     * 玩家是否可以传送到此世界，仅用于已经加载的世界
     * @return 是否可以传送，不可以传送的理由
     * @param worldName 世界名
     * @param p 被传送的玩家
     */
    private fun couldTP(worldName:String,p: Player):Pair<Boolean,String?> {
        //查看是否是玩家世界
        if (worldName.startsWith(ChunkWorld.inst.config.getString("World")!!)) {
            //是玩家世界，进行权限判断,获取世界
            val list = worldName.split("/")
            val uuidString = list[list.size - 2]
            //本地玩家世界都加载了，如果没有playerDao,说明出了问题
            val playerDao = RedisData.getPlayerDao(uuidString)?:return false to "§c世界数据加载错误，请稍后再试"
            return couldTP(playerDao,p)
        }else{
            //不是玩家世界，可以传送
           return true to null
        }
    }
    /**
     * 玩家是否可以传送此玩家世界,一般适用于菜单传送
     * @return 是否可以传送，不可以传送的理由
     * @param playerDao 玩家世界信息
     * @param p 被传送的玩家
     */
    private fun couldTP(playerDao: PlayerDao,p:Player):Pair<Boolean,String?>{
        val banners = RedisData.getBanners(p.uniqueId.toString())
        val friends = RedisData.getFriends(p.uniqueId.toString())
        //自己的世界怎么都能进入
        if (p.name == playerDao.name){return true to null}
        if (banners == null|| friends == null){
            return false to "§c您的好友数据加载错误，请重新进入游戏"
        }
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
     * 跨服，使用1.20.5以上的转发跨服
     */
    private fun transferServer(ip:String, port:Int, p:Player,location:String){
        //把信息存入玩家cookies
        val time = System.currentTimeMillis()
        //把时间信息存入cookies
        p.storeCookie(NamespacedKey(ChunkWorld.inst,"transferTime"),time.toString().toByteArray(Charsets.UTF_8))
        //把时间信息存入redis进行比对
        RedisPush.transferInfo("$ip:$port",p.name,"$time|||||$location")
        p.transfer(ip,port)
    }
    fun transferLobby(p: Player,ip: String,port: Int){
        val time = System.currentTimeMillis()
        //把时间信息存入cookies
        p.storeCookie(NamespacedKey(ChunkWorld.inst,"transferTime"),time.toString().toByteArray(Charsets.UTF_8))

    }

    /**
     * 把玩家传送到指定的世界的指定坐标
     * @param p 被传送的玩家
     * @param worldName 世界名
     * @param x 传送的x坐标
     * @param y 传送的y坐标
     * @param z 传送的z坐标
     * @param yaw 传送的yaw
     * @param pitch 传送的pitch
     * @param playerDao 有则说明这确定是一个玩家世界，没有则说明去一个加载了的世界，不确定加载的世界不能不写此参数
     */
    fun to(worldName: String, x:Double, y:Double, z:Double, yaw:Float, pitch:Float, p: Player, playerDao: PlayerDao?=null){
        if (playerDao != null){
            couldTP(playerDao,p).let { (canTp, reason) ->
                if (!canTp) {
                    p.sendMessage(reason!!)
                    return
                }
            }
        }else{
            //仅用于已加载的世界
            couldTP(worldName,p).let { (canTp, reason) ->
                if (!canTp) {
                    p.sendMessage(reason!!)
                    return
                }
            }
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
                if (Bukkit.getWorld(worldName) != null){
                    //就在本服，直接传送
                    p.sendMessage("§a已确定世界坐标,正在传送...")
                    p.teleportAsync(Location(Bukkit.getWorld(worldName)!!,x,y,z,yaw,pitch))
                }else{
                    p.sendMessage("§a正在确定世界坐标...")
                    //不在本服。群组搜索
                    RedisPush.teleportWorld(worldName,x,y,z).thenAccept {
                        if (it != null){
                            p.sendMessage("§a已确定世界坐标,正在传送...")
                            //世界找到了，传送过去
                            val ipAndPort = it.split(":")
                            transferServer(ipAndPort[0],ipAndPort[1].toInt(),p,"$worldName,$x,$y,$z,$yaw,$pitch")
                        }else{
                            //创建世界，获取要创建的服务器
                            if (playerDao == null){
                                //为null时为tpa事件，世界应该加载好了已经，所以不传送了
                                p.sendMessage("§c对应世界不存在，请联系管理员")
                                return@thenAccept
                            }
                            //加载玩家世界，有playerDao，说明不是第一次创建
                            val server = RedisManager.getHighestTpsIP()
                            if (server == ChunkWorld.inst.config.getString("transferIP")!!+":"+Bukkit.getPort().toString()){
                                //在本服加载
                                val world = Bukkit.createWorld(WorldCreator(worldName))
                                if (world != null){
                                    //加载成功
                                    p.sendMessage("§a已确定世界坐标,正在传送...")
                                    p.teleportAsync(Location(world,x,y,z,yaw,pitch))
                                }else{
                                    //加载失败
                                    p.sendMessage("§c世界加载失败，请联系管理员")
                                }
                            }else{
                                if (server == null){
                                    p.sendMessage("§c没有可用服务器，请联系管理员")
                                    return@thenAccept
                                }
                                RedisPush.loadWorld(server,worldName,x,y,z).thenAccept { success ->
                                    if (success != null){
                                        p.sendMessage("§a已确定世界坐标,正在传送...")
                                        //世界找到了，传送过去
                                        val ipAndPort = server.split(":")
                                        //跨服
                                        transferServer(ipAndPort[0],ipAndPort[1].toInt(),p,"$worldName,$x,$y,$z,$yaw,$pitch")
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
     */
    fun createTp(p:Player){
        val ipAndPort = RedisManager.getHighestTpsIP()
        if (ipAndPort == null){
            p.sendMessage("§c没有可用服务器，请联系管理员")
            return
        }
        //复制世界
        val file = File(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world")
        val templeFile = File(ChunkWorld.inst.dataFolder, "world")
        //有level.dat_old文件说明是加载过的
        if (File(file,"level.dat_old").exists()) {
            //此世界已被加载过
            p.sendMessage("§c你的世界已被加载过，但没有数据，请联系管理员")
            return
        }
        //协程异步
        launchCoroutine(SynchronizationContext.ASYNC) {
            try {
                templeFile.copyRecursively(file)
            }catch (ex:Exception) {
                //踢出玩家并提示联系管理员
                p.sendMessage("§c你的世界创建失败，原因为复制失败，请尽快联系管理员")
                error("${p.name}玩家的 ${p.uniqueId}/world 世界复制文件失败")
            }
            //复制完毕，加载世界
            if (ipAndPort == ChunkWorld.inst.config.getString("transferIP")!!+":"+Bukkit.getPort().toString()) {
                submit {
                    createWorldLocal(p.uniqueId,p.name).thenAccept {
                        if (it.first){
                            //创建成功
                            p.sendMessage("§a世界创建成功，正在传送...")
                            val l = it.second.split(",")
                            to(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world",
                                l[0].toDouble(),l[1].toDouble(),l[2].toDouble(),l[3].toFloat(),l[4].toFloat(),p)
                        }else {
                            //创建失败
                            p.sendMessage(it.second)
                        }
                    }
                }
            }else{
                //跨服创建
                RedisPush.createWorld(ipAndPort,p.uniqueId,p.name).thenAccept {
                    if (it != null){
                        //创建成功
                        p.sendMessage("§a世界创建成功，正在传送...")
                        val l = it.split(",")
                        to(ChunkWorld.inst.config.getString("World")!!+"/${p.uniqueId}/world",
                            l[0].toDouble(),l[1].toDouble(),l[2].toDouble(),l[3].toFloat(),l[4].toFloat(),p)
                    }else {
                        //创建失败
                        p.sendMessage("§c加载世界失败,请联系管理员")
                    }
                }
            }
        }
    }
    fun createWorldLocal(playerUUID: UUID,playerName:String):CompletableFuture<Pair<Boolean,String>>{
        val future = CompletableFuture<Pair<Boolean,String>>()
        //在本服创建
        val worldName = ChunkWorld.inst.config.getString("World")!!+"/$playerUUID/world"
        val wc = WorldCreator(worldName)
        val world = wc.createWorld() ?: //世界加载失败了
        return future.apply { complete(false to "§c世界加载失败") }
        //设置世界规则等
        world.isAutoSave = true
        world.setGameRule(GameRule.KEEP_INVENTORY,true)
        world.setGameRule(GameRule.SPAWN_CHUNK_RADIUS,0)
        submit(async = true) {
            var playerDao = PlayerDao().apply {
                name = playerName
                uuid = playerUUID
                createTime = java.text.SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒")
                    .format(java.util.Date(System.currentTimeMillis()))
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
            submit {
                //这里是第一次加载，通过worldedit插件复制屏障到占领的区块边缘
                WorldEdit.setBarrier(
                    setOf(chunkDao.x to chunkDao.z),
                    chunkDao.x to chunkDao.z,
                    world
                )
                future.complete(true to "${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}")
            }
        }
        //存储世界
        world.save()
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