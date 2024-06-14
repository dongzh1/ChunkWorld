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
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.File

@Suppress("unused")
class ChunkWorld : EasyPlugin() {
    //写变量
    companion object {
        val inst get() = plugin as ChunkWorld
        lateinit var db : AbstractDatabaseApi
    }

    override fun enable() {

        Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f插件已加载")
        saveDefaultConfig()
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
        submit(period = 1) {
            if (Bukkit.isTickingWorlds())
            Bukkit.getConsoleSender().sendMessage("§a[ChunkWorld] §f服务器正在运行")
        }


    }
    private fun loadResource(){
        val worldFolder = File(dataFolder, "world")
        if (worldFolder.exists()) {
            return
        }
        worldFolder.mkdirs()
    }


}
