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
        lateinit var spawnLocation :Location
    }

    private var redisListener: RedisListener? = null
    private var uploadTpsTask: EasyLibTask? = null

    override fun onLoad() {
        super.onLoad()
        saveDefaultConfig()
    }

    override fun enable() {
        Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld大厅专用] §f插件已加载")
        //释放世界文件
        loadResource()
        //确定出生点坐标
        val location = config.getString("Location")!!.split(",")
        val world = Bukkit.getWorld(location[0])
        spawnLocation = Location(world,location[1].toDouble(),location[2].toDouble(),location[3].toDouble(),location[4].toFloat(),location[5].toFloat())
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
            //注册全局监听
            registerListener(GroupListener)
            //注册指令
            registerCommand(GroupCommand)
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
