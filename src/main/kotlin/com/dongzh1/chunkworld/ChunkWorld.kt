package com.dongzh1.chunkworld

import com.dongzh1.chunkworld.database.AbstractDatabaseApi
import com.dongzh1.chunkworld.database.MysqlDatabaseApi
import com.dongzh1.chunkworld.database.SQLiteDatabaseApi
import com.dongzh1.chunkworld.listener.GroupListener
import com.dongzh1.chunkworld.redis.RedisConfig
import com.dongzh1.chunkworld.redis.RedisListener
import com.dongzh1.chunkworld.redis.RedisManager
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.command.registerCommand
import com.xbaimiao.easylib.task.EasyLibTask
import com.xbaimiao.easylib.util.plugin
import com.xbaimiao.easylib.util.registerListener
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.WorldCreator
import redis.clients.jedis.JedisPool
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Suppress("unused")
class ChunkWorld : EasyPlugin() {
    //写变量
    companion object {
        val inst get() = plugin as ChunkWorld
        lateinit var db: AbstractDatabaseApi
        lateinit var jedisPool: JedisPool
        lateinit var subscribeTask: EasyLibTask
        const val CHANNEL = "ChunkWorld"
    }

    private var redisListener: RedisListener? = null
    private var uploadTpsTask: EasyLibTask? = null

    override fun onLoad() {
        super.onLoad()
        saveDefaultConfig()
        //删除世界
        if (time3am()) {
            logger.info("已至第二天3:00am,开始重置资源世界")
            val baseName = config.getString("Resource") ?: "chunkworld"
            val resouceWorld = File(Bukkit.getWorldContainer(), baseName)
            //第二天凌晨3点后,删除世界重新生成
            val netherFile = File(Bukkit.getWorldContainer(), "world_nether")
            val endFile = File(Bukkit.getWorldContainer(), "world_the_end")
            val worldzyFile = File(Bukkit.getWorldContainer(), "${baseName}_zy")
            deleteWorld(netherFile)
            deleteWorld(endFile)
            deleteWorld(resouceWorld)
            deleteWorld(worldzyFile)
            // 更新配置文件中的 lastRunDate 为今天
            config.set("date", LocalDateTime.now().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            saveConfig()
        }
    }

    override fun enable() {

        Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f插件已加载")
        //释放世界文件
        loadResource()
        //赋值数据库
        if (config.getBoolean("Mysql.Enable")) {
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
            if (!config.getBoolean("LobbyServer")) {
                //说明不是大厅，要上传tps数据,每分钟都上传
                uploadTpsTask = submit(delay = 1, period = 20 * 60) { RedisManager.setIP() }
            }
            //注册全局监听
            registerListener(GroupListener)
            //注册指令
            registerCommand(GroupListener)
        } else {
            db = SQLiteDatabaseApi()
            Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f启用单端模式，链接sqldb成功")
        }
        //加载资源世界，用于获取区块和探索
        loadWorlds(config.getString("Resource") ?: "chunkworld")
        //每晚3点关服
        submit(period = 20 * 60) {
            val currentTime = LocalTime.now()
            val targetTime = LocalTime.of(3, 0)
            val warningTime = targetTime.minusMinutes(1)

            when {
                currentTime.hour == warningTime.hour && currentTime.minute == warningTime.minute -> {
                    Bukkit.broadcast(Component.text("警告: 像素物语将在1分钟后进行每日重启！"))
                }

                currentTime.hour == targetTime.hour && currentTime.minute == targetTime.minute -> {
                    Bukkit.broadcast(Component.text("警告: 像素物语重启中..."))
                    Bukkit.shutdown()
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
        //存储所有世界
        Bukkit.getWorlds().forEach { it.save() }
        uploadTpsTask?.cancel()
        //关闭redis
        if (config.getBoolean("Mysql.Enable")) {
            try {
                jedisPool.close()
                subscribeTask.cancel()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 加载资源世界,资源世界名为配置文件的世界名加_zy
     */
    private fun loadWorlds(baseName: String) {
        Bukkit.getConsoleSender().sendMessage("§a加载 $baseName 世界中,此世界为资源世界的备用区块世界")
        val baseWorld = Bukkit.createWorld(WorldCreator(baseName))
        if (baseWorld == null) {
            Bukkit.getConsoleSender().sendMessage("§c加载 $baseName 世界失败，自动关闭服务器")
            Bukkit.shutdown()
        }
        baseWorld!!.setGameRule(GameRule.KEEP_INVENTORY, true)
        Bukkit.getWorld("world_nether")!!.setGameRule(GameRule.KEEP_INVENTORY, true)
        Bukkit.getWorld("world_the_end")!!.setGameRule(GameRule.KEEP_INVENTORY, true)
        Bukkit.getConsoleSender().sendMessage("§a加载 ${baseName}_zy 世界中,此世界为资源世界")
        val zyWorld = Bukkit.createWorld(WorldCreator(baseName + "_zy").copy(baseWorld))
        if (zyWorld == null) {
            Bukkit.getConsoleSender().sendMessage("§c加载 ${baseName}_zy 世界失败，自动关闭服务器")
            Bukkit.shutdown()
        }
        zyWorld!!.setGameRule(GameRule.KEEP_INVENTORY, true)
    }

    //是否到了第二天3am后
    private fun time3am(): Boolean {
        val lastRunDateStr = config.getString("date") ?: return true
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val lastRunDate = LocalDate.parse(lastRunDateStr, formatter)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val threeAMToday = today.atTime(LocalTime.of(3, 0))
        return (now.isAfter(threeAMToday) && today.isAfter(lastRunDate))

        // 更新配置文件中的 lastRunDate 为今天
        //config.set("date", today.format(formatter))
        //saveConfig()

    }

    private fun deleteWorld(file: File) {
        if (file.exists()) {
            logger.info("删除 ${file.name} 世界文件夹中...")
            file.deleteRecursively()
            logger.info("删除完成,此世界会自动在稍后进行重建 ")
        } else {
            logger.info("${file.name}世界文件不存在，不删除")
        }
    }


}
