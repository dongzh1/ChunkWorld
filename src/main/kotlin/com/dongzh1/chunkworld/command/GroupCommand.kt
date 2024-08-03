package com.dongzh1.chunkworld.command


import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.ChunkWorld.Companion.repoClient
import com.dongzh1.chunkworld.basic.ServerGui
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.util.CommandBody
import com.xbaimiao.easylib.util.ECommandHeader
import com.xbaimiao.repository.repositoryClient
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

@ECommandHeader(command = "chunkworld")
object GroupCommand {



    @CommandBody
    private val menu = command<Player>("menu") {
        description = "打开跨服传送菜单"
        exec {
            ServerGui(sender).build()
        }
    }
    @CommandBody
    private val upload = command<CommandSender>("upload") {
        description = "上传youwan1的材质包，默认ia材质路径"
        exec {
            sender.sendMessage("正在将youwan1服材质包上传到小白服务器")
            val iaFile = File("/home/pixelServer/youwan1_25580/plugins/ItemsAdder/output/generated.zip")
            val time = System.currentTimeMillis()
            repoClient.upload(iaFile, "dongzh/$time.zip")
            if (sender is Player) {
                val player = sender as Player
                val uuid = player.uniqueId
                val url = repoClient.createPresignedUrl("dongzh/$time.zip", uuid)
                val hash = calculateHash(iaFile)
                //保存下载地址
                ChunkWorld.inst.config.set("resource", "dongzh/$time.zip")
                ChunkWorld.inst.config.set("hash", hash.toList())
                //覆盖原来的下载地址
                ChunkWorld.inst.saveConfig()
                player.sendMessage("资源包已上传，您的专属下载地址为：${url.downloadUrl}")
            } else {
                sender.sendMessage(Component.text("资源包已上传，针对每个玩家下载地址不同，请进入游戏获取"))
            }
        }
    }
    @CommandBody
    private val test = command<Player>("test"){
        description = "测试"
        exec {
            val zip = ChunkWorld.inst.config.getString("resource")!!
            val url = repoClient.createPresignedUrl(zip, sender.uniqueId).downloadUrl
            sender.setResourcePack(url,
                null,
                Component.text("§a请您选择接受资源包以进入像素物语").appendNewline()
                    .append(Component.text("§f只有接受资源包才能进行完整体验")))
        }
    }
    private fun calculateHash(file: File): ByteArray {
        val buffer = ByteArray(1024)
        val sha1 = MessageDigest.getInstance("SHA-1")
        val inputStream = FileInputStream(file)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            sha1.update(buffer, 0, bytesRead)
        }
        inputStream.close()
        return sha1.digest()
    }
}