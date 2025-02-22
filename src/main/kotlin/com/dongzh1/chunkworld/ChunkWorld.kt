package com.dongzh1.chunkworld

import com.cryptomorin.xseries.XMaterial
import com.dongzh1.chunkworld.command.Command
import com.dongzh1.chunkworld.database.AbstractDatabaseApi
import com.dongzh1.chunkworld.database.MysqlDatabaseApi
import com.dongzh1.chunkworld.database.SQLiteDatabaseApi
import com.fastasyncworldedit.core.Fawe
import com.google.common.io.ByteStreams
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.command.registerCommand
import com.xbaimiao.easylib.database.MysqlHikariDatabase
import com.xbaimiao.easylib.database.Ormlite
import com.xbaimiao.easylib.util.ShortUUID
import com.xbaimiao.easylib.util.plugin
import com.xbaimiao.easylib.util.registerListener
import com.xbaimiao.easylib.util.submit
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.messaging.PluginMessageListener
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
        lateinit var db : AbstractDatabaseApi
    }

    override fun onLoad() {
        super.onLoad()
        saveDefaultConfig()
        //删除世界
        if (time3am()) {
            logger.info("已至第二天3:00am,开始重置资源世界")
            val baseName = config.getString("Resource")?:"chunkworld"
            val resouceWorld = File(Bukkit.getWorldContainer(),baseName)
            //第二天凌晨3点后,删除世界重新生成
            val netherFile = File(Bukkit.getWorldContainer(),"world_nether")
            val endFile = File(Bukkit.getWorldContainer(),"world_the_end")
            val worldzyFile = File(Bukkit.getWorldContainer(),"${baseName}_zy")
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
        db = if (config.getBoolean("Mysql.Enable")) {
            MysqlDatabaseApi()
        } else {
            SQLiteDatabaseApi()
        }
        //注册指令
        registerCommand(Command)
        //注册监听
        registerListener(Listener)
        Command.invite.register()
        Command.wban.register()
        //加载资源世界，用于获取区块和探索
        loadWorlds(config.getString("Resource")?:"chunkworld")
        //每晚3点关服
        submit(period = 20*60) {
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
    private fun loadResource(){
        val worldFolder = File(dataFolder, "world")
        if (worldFolder.exists()) {
            return
        }
        worldFolder.mkdirs()
    }

    override fun onDisable() {
        Bukkit.getWorlds().forEach { it.save() }
    }

    /**
     * 加载资源世界,资源世界名为配置文件的世界名加_zy
     */
    private fun loadWorlds(baseName:String){
        Bukkit.getConsoleSender().sendMessage("§a加载 $baseName 世界中,此世界为资源世界的备用区块世界")
        val baseWorld = Bukkit.createWorld(WorldCreator(baseName))
        if (baseWorld == null){
            Bukkit.getConsoleSender().sendMessage("§c加载 $baseName 世界失败，自动关闭服务器")
            Bukkit.shutdown()
        }
        baseWorld!!.setGameRule(GameRule.KEEP_INVENTORY,true)
        Bukkit.getWorld("world_nether")!!.setGameRule(GameRule.KEEP_INVENTORY,true)
        Bukkit.getWorld("world_the_end")!!.setGameRule(GameRule.KEEP_INVENTORY,true)
        Bukkit.getConsoleSender().sendMessage("§a加载 ${baseName}_zy 世界中,此世界为资源世界")
        val zyWorld = Bukkit.createWorld(WorldCreator(baseName+"_zy").copy(baseWorld))
        if (zyWorld == null){
            Bukkit.getConsoleSender().sendMessage("§c加载 ${baseName}_zy 世界失败，自动关闭服务器")
            Bukkit.shutdown()
        }
        zyWorld!!.setGameRule(GameRule.KEEP_INVENTORY,true)
    }
    //是否到了第二天3am后
    private fun time3am():Boolean{
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
    private fun deleteWorld(file: File){
        if (file.exists()) {
            logger.info("删除 ${file.name} 世界文件夹中...")
            file.deleteRecursively()
            logger.info("删除完成,此世界会自动在稍后进行重建 ")
        } else {
            logger.info("${file.name}世界文件不存在，不删除")
        }
    }


}
