package com.dongzh1.chunkworld.command

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.Listener
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.basic.Item
import com.dongzh1.chunkworld.basic.ListGui
import com.xbaimiao.easylib.chat.TellrawJson
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.util.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.WorldCreator
import org.bukkit.block.Container
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.abs

@ECommandHeader(command = "chunkworld")
object Command {
    private val reply = ArgNode(("Mode"),
        exec = {
            listOf("invite", "unban")
        }, parse = {
            it
        }
    )
    private val yaw = ArgNode(("yaw"),
        exec = {
            listOf("0","45","90")
        }, parse = {
            it.toFloat()
        }
    )
    private val pitch = ArgNode(("pitch"),
        exec = {
            listOf("0","45","90")
        }, parse = {
            it.toFloat()
        }
    )
    val invite = command<Player>("invite") {
        // 要求传入一个玩家参数
        players { playerArg ->
            exec {
                val player = valueOfOrNull(playerArg)
                if (player == sender) {
                    sender.sendMessage("§c你不能邀请你自己")
                    return@exec
                }
                if (player == null) {
                    sender.sendMessage("§c玩家不在线")
                    return@exec
                }
                //是否已信任
                if (Listener.isBeTrust(sender,player.uniqueId)){
                    sender.sendMessage("§c你已和 ${player.name} 世界共享")
                    return@exec
                }
                if (Listener.getTrustMap(sender)!!.size >= 8){
                    sender.sendMessage("§c最多只能和8个世界共享")
                    return@exec
                }
                if (Listener.getTrustMap(player)!!.size >= 8){
                    sender.sendMessage("§c ${player.name} 已经没有更多共享位置了")
                    return@exec
                }
                sender.sendMessage("§a已向 ${player.name} 发出世界共享请求")
                //发送邀请
                TellrawJson()
                    .append("§b${sender.name} §f邀请你进行世界共享")
                    .newLine()
                    .append("§a同意").hoverText("点击同意这个邀请").runCommand("/chunkworld accept invite ${sender.name}")
                    .append("          ")
                    .append("§c拒绝").hoverText("点击拒绝这个邀请").runCommand("/chunkworld deny invite ${sender.name}")
                    .sendTo(player)
                //把邀请信息存入内存,只有一分钟
                Listener.setCommand("${sender.name} invite ${player.name}")
            }
        }
    }
    val wban = command<Player>("wban"){
        // 要求传入一个玩家参数
        players { playerArg ->
            exec {
                val player = valueOfOrNull(playerArg)
                if (player == null) {
                    sender.sendMessage("§c玩家不在线")
                    return@exec
                }
                if (player == sender) {
                    sender.sendMessage("§c你不能拉黑你自己")
                    return@exec
                }
                //查看是否已拉黑
                val banMap1 = Listener.getBanMap(sender)!!.toMutableSet()
                if (banMap1.contains(player.uniqueId)){
                    sender.sendMessage("§c你已经把 ${player.name} 拉黑了")
                    return@exec
                }
                sender.sendMessage("§a已对 ${player.name} 进行双向拉黑")
                sender.sendMessage("§a你们之间将无法传送到对方世界")
                TellrawJson()
                    .append("§a是否现在要将对方驱逐出世界")
                    .newLine()
                    .append("§a是的").hoverText("点击进行驱逐").runCommand("/chunkworld kick ${player.name}")
                    .append("          ")
                    .append("§c不用").hoverText("不用可以不点击")
                    .sendTo(sender)
                //发送邀请
                TellrawJson()
                    .append("§c${sender.name} 已把你从世界拉黑")
                    .newLine()
                    .append("§a现在你们已双向拉黑")
                    .sendTo(player)
                TellrawJson()
                    .append("§a是否现在要将对方驱逐出世界")
                    .newLine()
                    .append("§a是的").hoverText("点击进行驱逐").runCommand("/chunkworld kick ${sender.name}")
                    .append("          ")
                    .append("§c不用").hoverText("不用可以不点击")
                    .sendTo(player)
                val banMap2 = Listener.getBanMap(player)!!.toMutableSet()
                banMap1.add(player.uniqueId)
                banMap2.add(sender.uniqueId)
                Listener.setBanMap(sender,banMap1)
                Listener.setBanMap(player,banMap2)
                val senderId = Listener.getPlayerDaoMap(sender.uniqueId)!!.id
                val playerId = Listener.getPlayerDaoMap(player.uniqueId)!!.id
                //数据库中记录拉黑关系
                submit(async = true) {
                    ChunkWorld.db.addShip(senderId,playerId,false)
                }
            }
        }
    }
    /**
     * 接受邀请
     */
    @CommandBody
    val accept = command<Player>("accept"){
        description = "接受共享世界或取消黑名单"
        arg(reply){
            offlinePlayers { playerArg ->
                exec {
                    val name = valueOf(playerArg)
                    val player = Bukkit.getPlayerExact(name)
                    if (player == null){
                        sender.sendMessage("§c$name 已离线")
                        return@exec
                    }
                    when(valueOf(it)){
                        "invite" -> {
                            if (Listener.hasCommand("$name invite ${sender.name}")){
                                if (Listener.getTrustMap(sender)!!.size >= 8){
                                    sender.sendMessage("§c最多只能和8个世界共享")
                                    Listener.removeCommand("$name invite ${sender.name}")
                                    return@exec
                                }
                                val trustMap1 = Listener.getTrustMap(sender)!!.toMutableSet()
                                val trustMap2 = Listener.getTrustMap(player)!!.toMutableSet()
                                trustMap1.add(player.uniqueId)
                                trustMap2.add(sender.uniqueId)
                                Listener.setTrustMap(sender,trustMap1)
                                Listener.setTrustMap(player,trustMap2)
                                val senderId = Listener.getPlayerDaoMap(sender.uniqueId)!!.id
                                val playerId = Listener.getPlayerDaoMap(player.uniqueId)!!.id
                                submit(async = true) { ChunkWorld.db.addShip(senderId,playerId,true) }
                                Listener.removeCommand("$name invite ${sender.name}")
                                //共享之后，如果就在别人世界，要可以挖掘那些
                                sender.sendMessage("§a现在你和 ${player.name} 已共享世界")
                                sender.sendMessage("§a重新传送至 ${player.name} 的世界开始共建")
                                player.sendMessage("§a${sender.name} 已同意你的共享世界请求")
                                player.sendMessage("§a重新传送至 ${sender.name} 的世界开始共建")
                            }else sender.sendMessage("§c此邀请已过期或不存在")
                        }
                        "unban" -> {
                            if (Listener.hasCommand("$name unban ${sender.name}")){
                                val banMap1 = Listener.getBanMap(sender)!!.toMutableSet()
                                val banMap2 = Listener.getBanMap(player)!!.toMutableSet()
                                banMap1.remove(player.uniqueId)
                                banMap2.remove(sender.uniqueId)
                                Listener.setBanMap(sender,banMap1)
                                Listener.setBanMap(player,banMap2)
                                val senderId = Listener.getPlayerDaoMap(sender.uniqueId)!!.id
                                val playerId = Listener.getPlayerDaoMap(player.uniqueId)!!.id
                                submit(async = true) { ChunkWorld.db.removeShip(senderId,playerId,false) }
                                Listener.removeCommand("$name unban ${sender.name}")
                                sender.sendMessage("§a现在你和 ${player.name} 已不再相互拉黑")
                                player.sendMessage("§a${sender.name} 已同意和你解除相互拉黑关系")
                            }else sender.sendMessage("§c此申请已过期或不存在")
                        }
                        else -> return@exec
                    }
                }
            }
        }
    }
    /**
     * 拒绝邀请或解除黑名单
     */
    @CommandBody
    val deny = command<Player>("deny"){
        description = "拒绝共享世界或取消黑名单"
        arg(reply){
            offlinePlayers { playerArg ->
                exec {
                    val name = valueOf(playerArg)
                    val player = Bukkit.getPlayerExact(name)
                    when(valueOf(it)){
                        "invite" -> {
                            if (Listener.hasCommand("$name invite ${sender.name}")){
                                Listener.removeCommand("$name invite ${sender.name}")
                                sender.sendMessage("§a已拒绝 $name 的共享世界请求")
                                player?.sendMessage("§c${sender.name} 已拒绝你的共享世界请求")
                            }else sender.sendMessage("§c此邀请已过期或不存在")
                        }
                        "unban" -> {
                            if (Listener.hasCommand("$name unban ${sender.name}")){
                                Listener.removeCommand("$name unban ${sender.name}")
                                sender.sendMessage("§a已拒绝 $name 的解除相互拉黑请求")
                                player?.sendMessage("§c${sender.name} 已拒绝和你解除相互拉黑关系")
                            }else sender.sendMessage("§c此申请已过期或不存在")
                        }
                        else -> return@exec
                    }
                }
            }
        }
    }
    /**
     * 打开菜单
     */
    @CommandBody
    val menu = command<Player>("menu") {
        description = "打开菜单"
        permission = "chunkworld.menu"
        exec {
            ListGui(sender,1,false).build()
        }
    }

    /**
     * 驱逐对方
     */
    @CommandBody
    val kick = command<Player>("kick"){
        description = "驱逐对方"
        players {
            exec {
                val player = valueOfOrNull(it)
                if (player == null) {
                    sender.sendMessage("§c玩家不在线")
                    return@exec
                }
                if (player == sender) {
                    sender.sendMessage("§c你不能驱逐你自己")
                    return@exec
                }
                if (player.world.name != "${ChunkWorld.inst.config.getString("World")!!}/${sender.uniqueId}"){
                    sender.sendMessage("§c${player.name} 不在你的世界")
                    return@exec
                }
                val dao = Listener.getPlayerDaoMap(player.name)
                val location = Location(Bukkit.getWorld("${ChunkWorld.inst.config.getString("World")!!}/${dao!!.uuid}"),dao.x(),dao.y(),dao.z(),dao.yaw(),dao.pitch())
                player.teleportAsync(location)
            }
        }
    }

    @CommandBody
    val tp = command<Player>("tp") {
        description = "传送到世界"
        permission = "chunkworld.tp"
        offlinePlayers(optional = true) { playerArg ->
            exec {
                Tp.to(playerArg.valueOrNull()?:sender.name, sender)
            }
        }
    }

    /**
     * 怪物攻城
     */
    @CommandBody
    val monster = command<CommandSender>("monster"){
        description = "对一个玩家世界强行发起怪物攻城"
        permission = "chunkworld.admin"
        players {
            exec {

            }
        }
    }

    /**
     * 把玩家所在区块改为虚空
     */
    @CommandBody
    val voiditem = command<CommandSender>("voiditem"){
        description = "给人一个虚空道具"
        permission = "chunkworld.admin"
        players {
            exec {
                val p = valueOf(it)
                if (p == null){
                    sender.sendMessage("§c此玩家不在线")
                    return@exec
                }
                p.giveItem(Item.voidItem(),true)
            }
        }
    }
    @CommandBody
    val netheritem = command<CommandSender>("netheritem"){
        description = "给人一个地狱邀请函"
        permission = "chunkworld.admin"
        players {
            exec {
                val p = valueOf(it)
                if (p == null){
                    sender.sendMessage("§c此玩家不在线")
                    return@exec
                }
                p.giveItem(Item.netherItem(),true)
            }
        }
    }
    @CommandBody
    val enditem = command<CommandSender>("enditem"){
        description = "给人一个末地邀请函"
        permission = "chunkworld.admin"
        players {
            exec {
                val p = valueOf(it)
                if (p == null){
                    sender.sendMessage("§c此玩家不在线")
                    return@exec
                }
                p.giveItem(Item.endItem(),true)
            }
        }
    }
    @CommandBody
    val unload = command<CommandSender>("unload"){
        description = "极度危险指令，强制卸载世界，请勿使用，后果严重"
        permission = "chunkworld.admin"
        worlds {
            exec {
                Bukkit.unloadWorld(valueOf(it),true)
            }
        }
    }
    /**
     * 从老版本继承世界
     */
    @CommandBody
    val extendold = command<Player>("extendold"){
        description = "极度危险指令，继承老世界，请勿使用，后果严重"
        permission = "chunkworld.admin"
        offlinePlayers {
            exec {
                val name = valueOf(it)
                submit(async = true) {
                    val playerDao = ChunkWorld.db.playerGet(name)
                    if (playerDao == null){
                        sender.sendMessage("§c查无此人")
                        return@submit
                    }
                    val chunkdao = ChunkWorld.db.chunkGet(playerDao.id)
                    submit {
                        val world = Bukkit.createWorld(WorldCreator(ChunkWorld.inst.config.getString("World")!!+"/${playerDao.uuid}"))
                        if (world != null){
                            var n = 0
                            for (chunk in chunkdao){
                                world.getChunkAtAsync(chunk.first,chunk.second).thenAccept { n+=1 }
                            }
                            submit(period = 20, maxRunningNum = 100) {
                                if (n != chunkdao.size) return@submit
                                else{
                                    WorldEdit.setBarrier(chunkdao.toSet(),world)
                                    cancel()
                                    sender.sendMessage("生成完毕")
                                }
                            }

                        }else{
                            sender.sendMessage("§c世界不存在")
                        }
                    }
                }
            }
        }
    }
    @CommandBody
    val clearinv = command<Player>("clearinv"){
        description = "极度危险指令，清空容器，请勿使用，后果严重"
        permission = "chunkworld.admin"
        x { x1Arg->
            x { x2Arg->
                z { z1Arg->
                    z {z2Arg->
                        exec {
                                //扫描这个范围的所有容器、例如箱子，木桶，大箱子，熔炉，烟熏炉，熔炉，炼药锅，漏斗，发射器，投掷器、陷阱箱、末地箱
                                for (x in valueOf(x1Arg).toInt()..valueOf(x2Arg).toInt()){
                                    for (z in valueOf(z1Arg).toInt()..valueOf(z2Arg).toInt()){
                                        sender.sendMessage("$x $z")
                                        for (y in -64..320){
                                            val state = sender.world.getBlockState(x,y,z)
                                            if (state is Container){
                                                state.inventory.clear()
                                                state.inventory.contents.forEach { if (it != null) sender.sendMessage(it.displayName) }
                                                sender.sendMessage("clear $x $y $z")
                                            }
                                        }
                                    }
                                }
                                sender.sendMessage("finash")
                        }
                    }
                }

            }

        }
    }
    /**
     * 世界传送
     */
    @CommandBody
    val world = command<Player>("world"){
        description = "传送到指定世界的指定坐标"
        permission = "chunkworld.world"
        worlds { world ->
            x { x ->
                y { y ->
                    z { z->
                        arg(yaw){ yaw ->
                            arg(pitch){ pitch ->
                                booleans {
                                    exec {
                                        val location = Location(valueOf(world),valueOf(x),valueOf(y),valueOf(z),valueOf(yaw),valueOf(pitch))
                                        if (valueOf(it)){
                                            //要读秒
                                            //计时器，3秒后传送
                                            var n = 0
                                            //玩家禁止时的坐标
                                            val stop = sender.location
                                            val task = submit(delay = 1,period = 20, maxRunningNum = 4) {
                                                //如果玩家移动了，取消传送,判断距离为0.1
                                                if (abs(sender.location.x - stop.x) > 0.1 || abs(sender.location.y - stop.y) > 0.1 || abs(sender.location.z - stop.z) > 0.1){
                                                    cancel()
                                                    sender.sendMessage("§c你移动了,传送取消")
                                                    return@submit
                                                }
                                                if (n == 3) {
                                                    //如果坐标加载了，就传送，不管传不传送，这个任务都取消
                                                    sender.teleportAsync(location)
                                                }
                                                if(n < 3)
                                                    sender.sendMessage("§a ${3-n} 秒后进行传送，请不要移动!")
                                                if (n == 3) sender.sendMessage("§a正在传送...")
                                                n++
                                            }
                                        }else{
                                            sender.teleportAsync(location)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}