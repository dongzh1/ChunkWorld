package com.dongzh1.chunkworld

import com.dongzh1.chunkworld.command.GroupCommand
import com.dongzh1.chunkworld.database.AbstractDatabaseApi
import com.dongzh1.chunkworld.database.MysqlDatabaseApi
import com.dongzh1.chunkworld.database.SQLiteDatabaseApi
import com.dongzh1.chunkworld.listener.GroupListener
import com.dongzh1.chunkworld.redis.RedisConfig
import com.dongzh1.chunkworld.redis.RedisListener
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.command.registerCommand
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.task.EasyLibTask
import com.xbaimiao.easylib.util.plugin
import com.xbaimiao.easylib.util.registerListener
import com.xbaimiao.easylib.util.submit
import de.tr7zw.nbtapi.NBTFile
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import redis.clients.jedis.JedisPool
import java.io.File
import java.time.LocalTime

@Suppress("unused")
class ChunkWorld : EasyPlugin() {
    //写变量
    companion object {
        val inst get() = plugin as ChunkWorld
        lateinit var db: AbstractDatabaseApi
        lateinit var jedisPool: JedisPool
        lateinit var subscribeTask: EasyLibTask
        const val CHANNEL = "ChunkWorld"
        //用于判断玩家是否可以传送到资源世界
        var toZiyuan = true
        lateinit var spawnLocation :Location
    }

    private var redisListener: RedisListener? = null
    private var uploadTpsTask: EasyLibTask? = null

    override fun onLoad() {
        super.onLoad()
        saveDefaultConfig()
        //大厅服不需要
        if (!config.getBoolean("LobbyServer")){
            //准备好资源世界的备份，方便资源世界替换,先删除原来的，再生成新的，再卸载世界
            val chunkWorldBackup = File(Bukkit.getWorldContainer(), "chunkworld_backup")
            val netherBackup = File(Bukkit.getWorldContainer(), "world_nether_backup")
            val endBackup = File(Bukkit.getWorldContainer(), "world_the_end_backup")
            deleteWorld(chunkWorldBackup)
            deleteWorld(netherBackup)
            deleteWorld(endBackup)
        }
    }

    override fun enable() {
        Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f插件已加载")
        //释放世界文件
        loadResource()
        //确定出生点坐标
        val location = config.getString("Location")!!.split(",")
        val world = Bukkit.getWorld(location[0])
        spawnLocation = Location(world,location[1].toDouble(),location[2].toDouble(),location[3].toDouble(),location[4].toFloat(),location[5].toFloat())
        //赋值数据库
        if (config.getBoolean("Mysql.Enable")) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord")
            //使用mysql的同时redis也要启用
            db = MysqlDatabaseApi()
            Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f启用群组模式，链接mysql成功")
            val redisConfig = RedisConfig(config)
            Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f启用群组模式，尝试链接redis库")
            Bukkit.getConsoleSender().sendMessage("RedisInfo: " + redisConfig.host + ":" + redisConfig.port)
            jedisPool = if (redisConfig.password != null) {
                JedisPool(redisConfig, redisConfig.host, redisConfig.port, 1000, redisConfig.password)
            } else {
                JedisPool(redisConfig, redisConfig.host, redisConfig.port)
            }
            redisListener = RedisListener()
            subscribeTask = submit(async = true) {
                jedisPool.resource.use { jedis ->
                    jedis.subscribe(redisListener, CHANNEL)
                }
            }
            Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f链接redis库成功")
            if (!config.getBoolean("LobbyServer")) {
                //说明不是大厅，要上传tps数据,每分钟都上传
                uploadTpsTask = submit(delay = 1, period = 20 * 60) { RedisManager.setServerName() }
            }else{
                //大厅要注册大厅的ip和端口，其他服离线了进入大厅
                RedisManager.setLobbyIP()
            }
            //注册全局监听
            registerListener(GroupListener)
            //注册指令
            registerCommand(GroupCommand)
        } else {
            db = SQLiteDatabaseApi()
            Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f启用单端模式，链接sqldb成功")
        }
        //不是大厅服才加载资源世界和重启
        if (!config.getBoolean("LobbyServer")){
            //把删除的备份世界创建起来
            createBackup()
            //加载资源世界，用于获取区块和探索
            loadWorlds(false)
            val time = config.getString("time")!!.split(",").map { it.toInt() }
            //定时关服.20点刷新资源
            submit(period = 20 * 60) {
                val currentTime = LocalTime.now()
                val targetTime = LocalTime.of(time[0], time[1])
                val warningTime = targetTime.minusMinutes(1)
                val sourceRebornTime = LocalTime.of(20, 0)
                when {
                    currentTime.hour == warningTime.hour && currentTime.minute == warningTime.minute -> {
                        Bukkit.broadcast(Component.text("§c警告: 像素物语将在1分钟后进行每日重启！"))
                    }
                    currentTime.hour == targetTime.hour && currentTime.minute == targetTime.minute -> {
                        Bukkit.broadcast(Component.text("§c警告: 像素物语重启中..."))
                        Bukkit.shutdown()
                    }
                    currentTime.hour == 19&& currentTime.minute == 50 -> {
                        Bukkit.broadcast(Component.text("§c警告: 资源世界将在10分钟后重置,请尽快离开"))
                    }
                    currentTime.hour == 19&& currentTime.minute == 55 -> {
                        Bukkit.broadcast(Component.text("§c警告: 资源世界将在5分钟后重置,请尽快离开"))
                    }
                    currentTime.hour == 19&& currentTime.minute == 59 -> {
                        Bukkit.broadcast(Component.text("§c警告: 资源世界将在1分钟后重置,请尽快离开"))
                    }
                    currentTime.hour == sourceRebornTime.hour && currentTime.minute == sourceRebornTime.minute -> {
                        //现在玩家不能进入资源世界
                        toZiyuan = false
                        //重置资源世界
                        Bukkit.broadcast(Component.text("§c警告: 资源世界开始重置,请尽快离开"))
                        logger.info("已至20:00pm,开始重置资源世界")
                        val baseName = config.getString("Resource") ?: "chunkworld"
                        //先卸载资源世界,第一步，把世界内的玩家转移到大厅服并不允许进入
                        val list = mutableListOf<World?>()
                        val chunkworld = Bukkit.getWorld(baseName)
                        val chunkworldZy = Bukkit.getWorld("${baseName}_zy")
                        val netherWorld = Bukkit.getWorld("world_nether")
                        val netherZy = Bukkit.getWorld("world_nether_zy")
                        val endWorld = Bukkit.getWorld("world_the_end")
                        list.add(chunkworld)
                        list.add(chunkworldZy)
                        list.add(netherWorld)
                        list.add(netherZy)
                        list.add(endWorld)
                        launchCoroutine(SynchronizationContext.SYNC) {
                            list.forEach {
                                if (it != null) {
                                    it.players.forEach { player ->
                                        player.teleport(spawnLocation)
                                        player.sendMessage("§c资源世界正在重置中,请稍后再进入")
                                    }
                                    if (it.players.size != 0){
                                        it.players.forEach { p ->
                                            p.kick(Component.text("§c资源世界正在重置中,请稍后再进入"))
                                        }
                                    }
                                    //玩家清理完毕
                                    Bukkit.unloadWorld(it,false)
                                }
                            }
                            SynchronizationContext.ASYNC
                            //删除文件
                            list.forEach {
                                if (it != null) {
                                    deleteWorld(it.worldFolder)
                                }
                            }
                            try {
                                val chunkBackupFile = File(Bukkit.getWorldContainer(), "chunkworld_backup")
                                val netherBackupFile = File(Bukkit.getWorldContainer(), "world_nether_backup")
                                val endBackupFile = File(Bukkit.getWorldContainer(), "world_the_end_backup")
                                val chunkFile = File(Bukkit.getWorldContainer(), baseName)
                                val netherFile = File(Bukkit.getWorldContainer(), "world_nether")
                                val endFile = File(Bukkit.getWorldContainer(), "world_the_end")
                                val chunkZyFile = File(Bukkit.getWorldContainer(), "${baseName}_zy")
                                val netherZyFile = File(Bukkit.getWorldContainer(), "world_nether_zy")
                                chunkBackupFile.copyRecursively(chunkFile, true).let {if (it) logger.info("复制 $baseName 成功") else logger.info("复制 $baseName 失败")}
                                //复制完毕了，删除uid
                                File(chunkFile, "uid.dat").deleteRecursively()
                                netherBackupFile.copyRecursively(netherFile, true).let {if (it) logger.info("复制 world_nether 成功") else logger.info("复制 world_nether 失败")}
                                File(netherFile, "uid.dat").deleteRecursively()
                                endBackupFile.copyRecursively(endFile, true).let {if (it) logger.info("复制 world_the_end 成功") else logger.info("复制 world_the_end 失败")}
                                File(endFile, "uid.dat").deleteRecursively()
                                chunkBackupFile.copyRecursively(chunkZyFile, true).let {if (it) logger.info("复制 ${baseName}_zy 成功") else logger.info("复制 ${baseName}_zy 失败")}
                                File(chunkZyFile, "uid.dat").deleteRecursively()
                                netherBackupFile.copyRecursively(netherZyFile, true).let {if (it) logger.info("复制 world_nether_zy 成功") else logger.info("复制 world_nether_zy 失败")}
                                File(netherZyFile, "uid.dat").deleteRecursively()
                            }catch (e:Exception){
                                logger.info("复制资源世界出现异常")
                            }
                            //复制完毕，创建世界
                            loadWorlds(true)
                        }
                    }
                }
            }
        }
    }

    private fun loadResource() {
        val worldFolder = File(dataFolder, "world")
        if (worldFolder.exists()) {
            return
        }
        worldFolder.mkdirs()
    }

    override fun onDisable() {
        //关闭redis
        if (config.getBoolean("Mysql.Enable")) {
            uploadTpsTask?.cancel()
            //清除本服的ip数据
            RedisManager.delServerName()
            try {
                redisListener!!.unsubscribe()
                jedisPool.close()
                subscribeTask.cancel()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 加载资源世界,资源世界名为配置文件的世界名加_zy
     * @param reset 是否是重置资源世界
     */
    fun loadWorlds(reset:Boolean) {
        val baseName = config.getString("Resource") ?: "chunkworld"
        //1、如果是刷新的话，5个世界文件内容都是有的，直接加载就行
        //2、如果是第一次开服加载，world_nether和world_the_end是有的，chunkworld、world_nether_zy 和 chunkworld_zy是没有的
        //3、后面开服只需要加载chunkworld和chunkworld_zy、world_nether_zy三个世界
        launchCoroutine(SynchronizationContext.SYNC) {
            if (reset){
                //是刷新世界，那么世界都卸载了，都复制好了，加载5个世界就行
                Bukkit.getConsoleSender().sendMessage("§a资源世界重置中")
                createZyWorld(WorldCreator(baseName))
                createZyWorld(WorldCreator("world_nether").environment(World.Environment.NETHER))
                createZyWorld(WorldCreator("world_the_end").environment(World.Environment.THE_END))
                createZyWorld(WorldCreator("${baseName}_zy"))
                createZyWorld(WorldCreator("world_nether_zy").environment(World.Environment.NETHER))
                Bukkit.getConsoleSender().sendMessage("§a资源世界重置完毕")
            }else{
                //第一次开服没有chunkworld文件
                Bukkit.getConsoleSender().sendMessage("§a查询 $baseName 世界文件是否存在,此世界为资源世界区块提供世界")
                val worldFile = File(Bukkit.getWorldContainer(), "$baseName/region/r.0.0.mca")
                if (!worldFile.exists()) {
                    //第一次开服
                    Bukkit.getConsoleSender().sendMessage("§a $baseName 世界不存在，应该是第一次加载服务器，新创建")
                    //同步
                    val chunkWorld = createZyWorld(WorldCreator(baseName))
                    //加载完毕了，该卸载世界用于复制了
                    Bukkit.unloadWorld(chunkWorld!!, true)
                    //异步
                    SynchronizationContext.ASYNC
                    val zyWorldFile = File(Bukkit.getWorldContainer(), "${baseName}_zy")
                    Bukkit.getConsoleSender().sendMessage("§a正在从 $baseName 复制 到 ${baseName}_zy")
                    val baseFile = File(Bukkit.getWorldContainer(), baseName)
                    try {
                        baseFile.copyRecursively(zyWorldFile, true)
                            .let { if (it) logger.info("复制 ${baseName}_zy 成功") else logger.info("复制 ${baseName}_zy 失败") }
                        //删除uid
                        File(zyWorldFile, "uid.dat").deleteRecursively()
                    } catch (e: Exception) {
                        logger.info("$baseName 复制 到 ${baseName}_zy 出现异常")
                    }
                    //复制完毕了，重新加载chunkworld
                    SynchronizationContext.SYNC
                    createZyWorld(WorldCreator(baseName))
                    //加载zy
                    createZyWorld(WorldCreator("${baseName}_zy"))
                    //卸载地狱并复制
                    Bukkit.unloadWorld(Bukkit.getWorld("world_nether")!!, true)
                    SynchronizationContext.ASYNC
                    val netherZyFile = File(Bukkit.getWorldContainer(), "world_nether_zy")
                    Bukkit.getConsoleSender().sendMessage("§a正在从 world_nether 复制 到 world_nether_zy")
                    val netherFile = File(Bukkit.getWorldContainer(), "world_nether")
                    try {
                        netherFile.copyRecursively(netherZyFile, true)
                            .let { if (it) logger.info("复制 world_nether_zy 成功") else logger.info("复制 world_nether_zy 失败") }
                        //删除uid
                        File(netherZyFile, "uid.dat").deleteRecursively()
                    } catch (e: Exception) {
                        logger.info("$baseName 复制 到 world_nether_zy 出现异常")
                    }
                    SynchronizationContext.SYNC
                    createZyWorld(WorldCreator("world_nether").environment(World.Environment.NETHER))
                    createZyWorld(WorldCreator("world_nether_zy").environment(World.Environment.NETHER))
                }else{
                    //不是第一次开服了，资源世界都存在
                    Bukkit.getConsoleSender().sendMessage("§a $baseName 世界存在，直接加载")
                    createZyWorld(WorldCreator(baseName))
                    createZyWorld(WorldCreator("${baseName}_zy"))
                    createZyWorld(WorldCreator("world_nether_zy").environment(World.Environment.NETHER))
                }
            }
        }
    }

    fun deleteWorld(file: File) {
        if (file.exists()) {
            logger.info("删除 ${file.name} 世界文件夹中...")
            try {
                val succ = file.deleteRecursively()
                if (succ){
                    logger.info("删除${file.name}完成")
                }
                else {
                    logger.info("删除 ${file.name} 世界文件夹失败")
                }
            }catch (e:Exception){
                logger.info("删除 ${file.name} 世界文件夹异常")
            }
        } else {
            logger.info("${file.name}世界文件不存在，不删除")
        }
    }
    private fun createZyWorld(worldCreator: WorldCreator):World?{
        Bukkit.getConsoleSender().sendMessage("§a ${worldCreator.name()} 世界加载并初始化规则中")
        val world = worldCreator.keepSpawnLoaded(TriState.FALSE).createWorld()
        if (world == null) {
            Bukkit.getConsoleSender().sendMessage("§c加载 ${worldCreator.name()} 世界失败，自动关闭服务器")
            Bukkit.shutdown()
            return null
        }
        world.setGameRule(GameRule.KEEP_INVENTORY, true)
        world.setGameRule(GameRule.SPAWN_CHUNK_RADIUS,0)
        world.setGameRule(GameRule.DO_FIRE_TICK,false)
        Bukkit.getConsoleSender().sendMessage("§a ${worldCreator.name()} 世界加载并初始化规则完毕")
        return world
    }

    /**
     * 玩家世界从根目录存储
     */
    fun changeWorldName(name:String,isPlayerWorld:Boolean){
        val file:File = if (isPlayerWorld){
            File(name,"level.dat")
        }else{
            File(Bukkit.getWorldContainer(),"$name/level.dat")
        }
        val nbt = NBTFile(file)
        //获取世界名
        nbt.getCompound("Data")!!.setString("LevelName",name)
        nbt.save()
    }
    private fun createBackup(){
        Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f资源世界备用世界开始生成")
        //删除完成后，生成新的
        val chunkWorld = createZyWorld(WorldCreator("chunkworld_backup"))
        Bukkit.unloadWorld(chunkWorld!!,true)
        val nether = createZyWorld(WorldCreator("world_nether_backup").environment(World.Environment.NETHER))
        Bukkit.unloadWorld(nether!!,true)
        val end  = createZyWorld(WorldCreator("world_the_end_backup").environment(World.Environment.THE_END))
        Bukkit.unloadWorld(end!!,true)
        Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f资源世界备用世界生成完毕")
    }
}
