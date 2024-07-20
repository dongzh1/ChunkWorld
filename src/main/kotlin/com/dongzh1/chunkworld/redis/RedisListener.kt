package com.dongzh1.chunkworld.redis

import redis.clients.jedis.JedisPubSub

class RedisListener : JedisPubSub() {

    private val onlineServer = mutableListOf(PixelWorldPro.instance.config.getString("ServerName")!!)
    private val newOnlineServer = mutableListOf(PixelWorldPro.instance.config.getString("ServerName")!!)
    private val push = submit(async = true, period = 20L) {
        RedisManager.push("ServerOnline|,|${PixelWorldPro.instance.config.getString("ServerName")}")
    }
    fun stop(){
        push.cancel()
    }

    override fun onMessage(channel: String?, message: String?) {
        if (channel == PixelWorldPro.channel) {
            when(message!!.split("|,|")[0]){
                "ServerOnline" ->{
                    val serverName = message.split("|,|")[1]
                    if (!newOnlineServer.contains(serverName)){
                        newOnlineServer.add(serverName)
                    }
                    if (newOnlineServer.size > onlineServer.size){
                        onlineServer.clear()
                        onlineServer.addAll(newOnlineServer)
                    }
                }
                "createWorld" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val template = message.split("|,|")[2]
                    val name = message.split("|,|")[3]
                    val server = message.split("|,|")[4]
                    val serverData = Server.getLocalServer()
                    if (server == serverData.realName) {
                        submit {
                            if (WorldImpl.createWorldLocal(uuid, template, name)) {
                                RedisManager.push("createWorldSuccess|,|${uuid}")
                            }
                        }
                    }
                }
                "createWorldSuccess" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    World.createWorldList.remove(uuid)
                }
                "loadWorldGroup" ->{
                    //获取最低mspt服务器
                    val serverName = message.split("|,|")[2]
                    //如果当前服务器为最低mspt服务器则加载世界
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        WorldImpl.loadWorldLocal(uuid)
                    }

                }
                "loadWorldGroupTp" -> {
                    val serverName = message.split("|,|")[3]
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        if (WorldImpl.loadWorldLocal(uuid)){
                            val playerUuid = UUID.fromString(message.split("|,|")[2])
                            TeleportApi.Factory.teleportApi!!.teleport(playerUuid,uuid)
                        }
                    }
                }
                "loadWorldServer" ->{
                    val serverName = message.split("|,|")[2]
                    if (serverName != Server.getLocalServer().realName){
                        return
                    }
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    submit {
                        if (WorldImpl.loadWorldLocal(uuid)){
                            RedisManager.push("loadWorldSuccess|,|${uuid}")
                        }
                    }
                }
                "loadWorldSuccess" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    if (WorldImpl.getLoadWorldList().contains(uuid)){
                        WorldImpl.removeLoadWorldList(uuid)
                    }
                }
                "teleportUUID" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val serverName = message.split("|,|")[2]
                    if (Bukkit.getPlayer(uuid) == null){
                        return
                    }
                    TeleportImpl().connect(Bukkit.getPlayer(uuid)!!,serverName)
                }
                "teleportLocation" ->{
                    val location = Serialize.deserializeLocation(message.split("|,|")[1])
                    val serverName = message.split("|,|")[2]
                    val uuid = UUID.fromString(message.split("|,|")[3])
                    if (serverName == PixelWorldPro.instance.config.getString("ServerName")){
                        TeleportApi.Factory.teleportApi!!.teleport(uuid,location)
                    }
                }
                "teleportWorld" ->{
                    //找到加载了对应世界的服务器
                    val world = Bukkit.getWorld(message.split("|,|")[2]) ?: return
                    //让玩家传送过来
                    TeleportApi.Factory.teleportApi!!.teleport(UUID.fromString(message.split("|,|")[1]),world.spawnLocation)
                }
                "sendMessage" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val msg = message.split("|,|")[2]
                    if (Bukkit.getPlayer(uuid) == null){
                        return
                    }
                    Bukkit.getPlayer(uuid)!!.sendMessage(msg)
                }
                "setTime" ->{
                    val serverName = message.split("|,|")[1]
                    if (serverName != PixelWorldPro.instance.config.getString("ServerName")){
                        return
                    }
                    val timeWorld = Bukkit.getWorld(message.split("|,|")[2])
                    if (timeWorld == null){
                        Bukkit.getConsoleSender().sendMessage(lang("WorldTimeWorldNotExist"))
                        return
                    }
                    RedisManager.push("setTimeBack|,|${timeWorld.time}|,|${message.split("|,|")[3]}")
                }
                "setTimeBack" ->{
                    val time = message.split("|,|")[1].toLong()
                    val worldName = message.split("|,|")[2]
                    val world = Bukkit.getWorld(worldName)
                    if (world == null){
                        Bukkit.getConsoleSender().sendMessage(lang("WorldTimeWorldNotExist"))
                        return
                    }
                    world.time = time
                }
                "updateWorldLevel" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    val world = Bukkit.getWorld(worldData!!.worldName)
                    if (world == null){
                        return
                    }else{
                        WorldImpl.setWorldBorder(world,worldData.worldLevel)
                    }
                }
                "unloadWorld" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    val world = Bukkit.getWorld(worldData!!.worldName)
                    if (world == null){
                        return
                    }else{
                        if (WorldImpl.unloadWorld(world)){
                            RedisManager.push("unloadWorldBack|,|${uuid}")
                        }
                    }
                }
                "unloadWorldBack" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    //获取要卸载是世界uuid，并移除
                    if (WorldImpl.getUnloadWorldList().contains(uuid)){
                        WorldImpl.removeUnloadWorldList(uuid)
                    }
                }
                "setSeed" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val seed = message.split("|,|")[2]
                    WorldImpl.setSeed(uuid, seed)
                }
                "getServerData" ->{
                    val id = message.split("|,|")[1].toInt()
                    val serverData = Server.getLocalServer()
                    val msg = if (serverData.type == null){
                        "returnServerData|,|${id}|,|${serverData.showName}|,|${serverData.realName}|,|${serverData.mode}|,|${serverData.tps}|,|none"
                    } else {
                        "returnServerData|,|${id}|,|${serverData.showName}|,|${serverData.realName}|,|${serverData.mode}|,|${serverData.tps}|,|${serverData.type}"
                    }
                    RedisManager.push(msg)
                }
                "returnServerData" ->{
                    val id = message.split("|,|")[1].toInt()
                    val showName = message.split("|,|")[2]
                    val realName = message.split("|,|")[3]
                    val mode = message.split("|,|")[4]
                    val tps = message.split("|,|")[5].toDouble()
                    val type = message.split("|,|")[6]
                    val serverData = if (type == "none"){
                        ServerData(
                            showName,
                            realName,
                            mode,
                            tps,
                            null
                        )
                    } else {
                        ServerData(
                            showName,
                            realName,
                            mode,
                            tps,
                            type
                        )
                    }
                    Server.setServerData(id, serverData)
                }
                "BungeeWorldTp" ->{
                    val uuid = UUID.fromString(message.split("|,|")[1])
                    val worldName = message.split("|,|")[2]
                    val server = message.split("|,|")[3]
                    val serverData = Server.getLocalServer()
                    if (server != serverData.realName){
                        return
                    }
                    Thread {
                        var times = 0
                        var world = Bukkit.getWorld("$worldName/world")
                        if (world == null) {
                            while (times < 1000) {
                                times += 1
                                world = Bukkit.getWorld("$worldName/world")
                                if (world != null) {
                                    break
                                }
                                sleep(500)
                            }
                            if (world == null) {
                                Bukkit.getConsoleSender()
                                    .sendMessage("§aPixelWorldPro Bungee传送出错啦：无法找到世界 $worldName/world")
                                return@Thread
                            }
                        }
                        val location = world.spawnLocation
                        val worldData = PixelWorldPro.databaseApi.getWorldData(world.name)!!
                        times = 0
                        while (times < 1000) {
                            val player = Bukkit.getPlayer(uuid)
                            if (player != null) {
                                submit {
                                    try {
                                        player.teleport(Location(world, worldData.location["x"]!!, worldData.location["y"]!!, worldData.location["z"]!!))
                                    }catch (_:Exception) {
                                        player.teleport(location)
                                    }
                                }
                                return@Thread
                            }
                            sleep(500)
                            times += 1
                        }
                    }.start()
                }
                else ->{
                    Bukkit.getLogger().warning("未知的redis消息类型")
                }
            }
        }
    }