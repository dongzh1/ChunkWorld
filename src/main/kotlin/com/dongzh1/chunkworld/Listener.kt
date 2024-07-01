package com.dongzh1.chunkworld

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.dongzh1.chunkworld.basic.ConfirmExpandGui
import com.dongzh1.chunkworld.database.dao.ChunkDao
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.xbaimiao.easylib.util.submit
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.io.File
import java.time.Duration
import java.util.UUID
import kotlin.math.max

object Listener:Listener {
    private val playerDaoMap = mutableMapOf<String,PlayerDao>()
    private val chunkMap = mutableMapOf<Player,Set<Pair<Int,Int>>>()
    private val trustMap = mutableMapOf<Player,Set<UUID>>()
    private val banMap = mutableMapOf<Player,Set<UUID>>()
    private val uuidToNameMap = mutableMapOf<UUID,String>()
    private val command = mutableListOf<String>()
    private val reSpawn = mutableSetOf<Player>()
    fun setCommand(string: String) {
        command.add(string)
        submit(delay = 60*20) { removeCommand(string) }
    }
    fun removeCommand(string: String) = command.remove(string)
    fun hasCommand(string: String):Boolean = command.contains(string)
    fun setPlayerDaoMap(name:String, playerDao: PlayerDao) = playerDaoMap.set(name,playerDao)
    fun getPlayerDaoMap(name:String):PlayerDao? = playerDaoMap[name]
    fun getPlayerDaosMap():List<PlayerDao> = playerDaoMap.values.toList()
    fun getPlayerDaoMap(uuid: UUID):PlayerDao? = playerDaoMap[uuidToNameMap[uuid]]
    fun setChunkMap(player: Player,chunk:Set<Pair<Int,Int>>) = chunkMap.set(player,chunk)
    fun addChunkMap(player: Player,chunk:Pair<Int,Int>) {
        val list = chunkMap[player]!!.toMutableSet()
        list.add(chunk)
        chunkMap[player] = list
    }
    fun getChunkMap(player: Player):Set<Pair<Int,Int>>? = chunkMap[player]
    fun getChunkMaps():Map<Player,Set<Pair<Int,Int>>> = chunkMap
    fun setTrustMap(player: Player,trust:Set<UUID>) = trustMap.set(player,trust)
    fun getTrustMap(player: Player):Set<UUID>? = trustMap[player]
    fun setBanMap(player: Player,ban:Set<UUID>) = banMap.set(player,ban)
    fun getBanMap(player: Player):Set<UUID>? = banMap[player]
    fun setUUIDtoName(uuid: UUID,name: String) = uuidToNameMap.set(uuid,name)
    fun getUUIDtoName(uuid: UUID) = uuidToNameMap[uuid]
    //删除所有的关于这个玩家存在内存的信息
    fun removeData(player: Player) {
        playerDaoMap.remove(player.name)
        uuidToNameMap.remove(player.uniqueId)
        chunkMap.remove(player)
        trustMap.remove(player)
        banMap.remove(player)
    }
    //仅删除playerDao和uuidToName
    fun removePlayerData(uuid: UUID) {
        val name = uuidToNameMap[uuid]
        if (name != null) {
            playerDaoMap.remove(name)
            uuidToNameMap.remove(uuid)
        }
    }
    //获取这个玩家是否被世界主人信任
    private fun isBeTrust(player: Player,world:World):Boolean {
        val uuid = UUID.fromString(world.name.split("/").last())
        return getTrustMap(player)!!.contains(uuid)
    }
    fun isBeTrust(player: Player,uuid: UUID):Boolean {
        return getTrustMap(player)!!.contains(uuid)
    }
    //这个玩家是否在被信任的世界，包括自己的世界
    private fun isInTrustedWorld(player: Player):Boolean{
        //如果是自己的世界，那也没啥好取消的
        if (player.world.name == ChunkWorld.inst.config.getString("World")!!+"/${player.uniqueId}") return true
        //信任也不需要取消
        if (isBeTrust(player,player.world)) return true
        return false
    }
    //获取这个玩家是否被世界主人拉黑
    private fun isBeBan(player: Player,world:World):Boolean {
        val uuid = UUID.fromString(world.name.split("/").last())
        return getBanMap(player)!!.contains(uuid)
    }
    fun isBeBan(player: Player,uuid: UUID):Boolean {
        return getBanMap(player)!!.contains(uuid)
    }


    @EventHandler
    fun onLogin(e:PlayerLoginEvent) {
    }
    @EventHandler
    fun onSpawn(e:PlayerSpawnLocationEvent){

    }
    @EventHandler
    fun onJoin(e:PlayerJoinEvent){

        //给玩家致盲效果
        e.player.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS,20*60,3))
        //提示title
        e.player.showTitle(Title.title(Component.text("§a欢迎回家"), Component.text("§f正在将您传送至家园..."),
            Times.times(Duration.ofSeconds(1), Duration.ofMinutes(1), Duration.ofSeconds(1))))
        //开启协程，会在异步和同步之间来回使用,现在是异步
        launchCoroutine(SynchronizationContext.ASYNC) {
            //查询玩家信息并存入内存
            var playerDao = ChunkWorld.db.playerGet(e.player.name)
            if (playerDao == null) {
                //说明第一次进服，先创建世界
                //说明世界还没创建过,创建家园世界,先复制模板文件
                val worldFolder = File(ChunkWorld.inst.dataFolder, "world")
                try {
                    worldFolder.copyRecursively(File(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"))
                }catch (ex:Exception) {
                    //踢出玩家并提示联系管理员
                    switchContext(SynchronizationContext.SYNC)
                    e.player.kick(Component.text("世界文件复制失败，请联系管理员"))
                    error("世界文件复制失败")
                }
            }
            //后面的加载确定世界文件是否成功存在
            if (playerDao != null){
                if (!File(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}/poi/r.0.0.mca").exists()) {
                    //根据玩家进度进行处理
                    switchContext(SynchronizationContext.SYNC)
                    e.player.kick(Component.text("世界文件不完整或第一次加载未正确保留，请联系管理员"))
                    error("世界文件不完整")
                }
            }
            //现在是同步
            switchContext(SynchronizationContext.SYNC)
            //复制完毕，加载世界
            val world = Bukkit.createWorld(WorldCreator(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"))
            if (world == null){
                //世界加载失败了
                e.player.kick(Component.text("世界加载失败，请联系管理员"))
                error("世界加载失败")
            }else {
                //现在是异步
                switchContext(SynchronizationContext.ASYNC)
                val spawnLocation:Location
                //第一次加载世界完毕，建立玩家信息
                if (playerDao == null) {
                    playerDao = PlayerDao().apply {
                        name = e.player.name
                        uuid = e.player.uniqueId
                        createTime = java.text.SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒")
                            .format(java.util.Date(System.currentTimeMillis()))
                        spawn =
                            "${world.spawnLocation.x},${world.spawnLocation.y},${world.spawnLocation.z},${world.spawnLocation.yaw},${world.spawnLocation.pitch}"
                        worldStatus = 0
                        chunkCount = 1
                    }
                    spawnLocation = world.spawnLocation
                    //玩家数据存入数据库
                    ChunkWorld.db.playerCreate(playerDao)
                    //取出玩家数据，获取id
                    playerDao = ChunkWorld.db.playerGet(e.player.name)!!
                    //出生的区块也存入
                    val chunkDao = ChunkDao().apply {
                        playerID = playerDao.id
                        x = world.spawnLocation.chunk.x
                        z = world.spawnLocation.chunk.z
                    }
                    //区块数据存入数据库
                    ChunkWorld.db.chunkCreate(chunkDao)
                    //新建了玩家数据，可以存入内存
                    setPlayerDaoMap(e.player.name,playerDao)
                    setUUIDtoName(e.player.uniqueId,e.player.name)
                    setChunkMap(e.player, setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z))
                    setTrustMap(e.player, emptySet())
                    setBanMap(e.player, emptySet())
                    //现在是同步
                    switchContext(SynchronizationContext.SYNC)
                    //这里是第一次加载，通过worldedit插件复制屏障到占领的区块边缘
                    WorldEdit.setBarrier(
                        setOf( world.spawnLocation.chunk.x to world.spawnLocation.chunk.z),
                        world.spawnLocation.chunk.x to world.spawnLocation.chunk.z ,world)
                    //存储世界
                    world.save()
                } else {
                    //有玩家数据,导入所有到内存
                    spawnLocation = Location(world,playerDao.x(),playerDao.y(),playerDao.z(),playerDao.yaw(),playerDao.pitch())
                    setPlayerDaoMap(e.player.name,playerDao)
                    setUUIDtoName(e.player.uniqueId,e.player.name)
                    val chunList = ChunkWorld.db.chunkGet(playerDao.id)
                    if (chunList.isEmpty()){
                        //说明玩家上次区块信息没存入，重新存
                        val chunkDao = ChunkDao().apply {
                            playerID = playerDao.id
                            x = world.spawnLocation.chunk.x
                            z = world.spawnLocation.chunk.z
                        }
                        //区块数据存入数据库
                        ChunkWorld.db.chunkCreate(chunkDao)
                        chunkMap[e.player] = setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z)
                        //创建第一次的屏障
                        //现在是同步
                        switchContext(SynchronizationContext.SYNC)
                        WorldEdit.setBarrier(
                            setOf(world.spawnLocation.chunk.x to world.spawnLocation.chunk.z),
                            world.spawnLocation.chunk.x to world.spawnLocation.chunk.z,world)
                    }else{
                        //有区块信息，存入内存
                        setChunkMap(e.player,chunList.toSet())
                    }
                    SynchronizationContext.ASYNC
                    setTrustMap(e.player, ChunkWorld.db.getShip(playerDao.id,true).map { it.uuid }.toSet())
                    setBanMap(e.player, ChunkWorld.db.getShip(playerDao.id,false).map { it.uuid }.toSet())
                }
                //切换主线程，加载区块并传送玩家过去
                switchContext(SynchronizationContext.SYNC)
                //传送玩家
                e.player.teleportAsync(spawnLocation).thenAccept {
                    e.player.clearTitle()
                    e.player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
                }
            }
        }
    }
    @EventHandler
    fun onInteract(e:PlayerInteractEvent){
        //如果是拓展物品，也要取消
        var isItem = false
        if (e.item?.type == Material.valueOf(ChunkWorld.inst.config.getString("item.material")!!)){
            if (e.item!!.itemMeta.hasCustomModelData()){
                //物品有模型值，看能不能和配置的模型值相等
                if (e.item!!.itemMeta.customModelData == ChunkWorld.inst.config.getInt("item.customModelData")){
                    isItem = true
                    //是指定物品，取消
                    e.isCancelled = true
                }
            }else{
                //物品没有模型值，看配置的模型值是不是-1
                if (ChunkWorld.inst.config.getInt("item.customModelData") == -1){
                    //配置不需要模型值，手上物品也没有模型值,说明是指定物品，取消
                    isItem = true
                    e.isCancelled = true
                }
            }
        }

        //玩家不在家园世界就不管
        if (!e.player.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) return

        //吃东西也没事
        if (e.hasItem() && e.item!!.type.isEdible){
            if (e.action == Action.RIGHT_CLICK_BLOCK || e.action == Action.RIGHT_CLICK_AIR){
                return
            }
        }

        //在家园世界和屏障交互
        if (e.clickedBlock?.type == org.bukkit.Material.BARRIER){
            //如果是世界主人，可以拓展世界
            e.isCancelled = true
            if (e.player.world.name == ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}" && isItem){
                ConfirmExpandGui(e.player, e.clickedBlock!!.chunk).build()
            }
            return
        }

        if (e.player.isOp) return
        //不是自己的世界或被信任的世界就取消
        e.isCancelled = !isInTrustedWorld(e.player)
    }
    @EventHandler
    //阻止玩家被实体锁定目标
    fun target(e: EntityTargetLivingEntityEvent) {
        //不是玩家世界就不管
        if (!e.entity.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) return
        //如果目标是玩家且不是成员，则取消事件
        if (e.target is Player) {
            val target = e.target as Player
            //不是自己的世界或被信任的世界就取消
            e.isCancelled = !isInTrustedWorld(target)
        }
    }
    @EventHandler
    //阻止访客玩家被实体伤害以及玩家伤害实体
    fun damage(e: EntityDamageByEntityEvent) {
        //不是玩家世界就不管
        if (!e.entity.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) return
        //如果攻击者是玩家且不是成员，则取消事件
        if (e.damager is Player) {
            val damager = e.damager as Player
            if (damager.isOp)
                return
            //不是自己的世界或被信任的世界就取消
            e.isCancelled = !isInTrustedWorld(damager)
        }
        //如果被攻击者是玩家且不是成员，则取消事件
        if (e.entity is Player) {
            val entity = e.entity as Player
            //不是自己的世界或被信任的世界就取消
            e.isCancelled = !isInTrustedWorld(entity)
        }
    }
    @EventHandler
    //不和实体交互
    fun rightClickEntity(e: PlayerInteractEntityEvent) {
        if (e.player.isOp)
            return
        //不是玩家世界就不管
        if (!e.player.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) return
        //不是自己的世界或被信任的世界就取消
        e.isCancelled = !isInTrustedWorld(e.player)
    }
    @EventHandler
    //不和悬挂实体交互
    fun hangEntity(e:HangingBreakByEntityEvent){
        if (e.remover is Player) {
            val player = e.remover as Player
            if (player.isOp)
                return
            //不是玩家世界就不管
            if (!player.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) return
            //不是自己的世界或被信任的世界就取消
            e.isCancelled = !isInTrustedWorld(player)
        }
    }
    /**
     * 禁止载具被游客打击
     */
    @EventHandler
    fun vehicle(e: VehicleDamageEvent){
        if (e.attacker is Player) {
            val player = e.attacker as Player
            if (player.isOp)
                return
            //不是玩家世界就不管
            if (!player.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) return
            //不是自己的世界或被信任的世界就取消
            e.isCancelled = !isInTrustedWorld(player)
        }
    }
    /**传送到家园世界的事件，统一先阻止、判断权限后放行，并根据情况给与玩家相关权限,到没有权限的世界变为冒险模式
     * @param e 玩家传送事件
     */
    @EventHandler
    fun worldTp(e: PlayerTeleportEvent) {
        //传送不涉及世界切换就返回
        if (e.to.world == e.from.world) return
        val player = e.player
        //不是玩家世界就不管
        if (!e.to.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) return
        //如果是自己的世界，那也没啥好取消的
        if (e.to.world.name == ChunkWorld.inst.config.getString("World")!!+"/${player.uniqueId}") return
        //现在确定是别人的家园世界,获取对应世界玩家的uuid
        val uuid = UUID.fromString(e.to.world.name.split("/").last())
        val playerDao = getPlayerDaoMap(uuid)
        //查看是否能传送
        when(playerDao!!.worldStatus) {
            //玩家世界开放
            0.toByte() -> {
                //如果是黑名单，也无法进入
                if (isBeBan(player,uuid)) {
                    //取消传送任务
                    player.sendMessage("§c此家园禁止你访问")
                    e.isCancelled = true
                    return
                }
            }
            1.toByte() -> {
                //部分开放，看看是否被信任
                if (!isBeTrust(player,uuid)) {
                    player.sendMessage("§c此家园只允许共享家园的玩家访问")
                    e.isCancelled = true
                    return
                }
            }
            //玩家世界仅对自己开放
            2.toByte() -> {
                //取消传送任务
                player.sendMessage("§c此家园禁止他人访问")
                e.isCancelled = true
                return
            }
        }
    }
    /**
    * 玩家切换世界的时候注意游戏模式
    */
    @EventHandler
    fun worldChange(e: PlayerChangedWorldEvent){
        //world世界和家园世界为冒险模式，主人和信任者除外
        val player = e.player
        //玩家去主城，就改为冒险模式
        if (player.world.name == "world"){
            player.gameMode = GameMode.ADVENTURE
            player.showTitle(Title.title(Component.text("§b神奇小黑屋"),
                Component.text("§f不会建筑的dong默默路过"),
                Times.times(Duration.ofSeconds(1),
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(1))))
            return
        }
        //不是玩家世界就生存
        if (!player.world.name.contains(ChunkWorld.inst.config.getString("World")!!)) {
            player.gameMode = GameMode.SURVIVAL
            return
        }
        //现在肯定是家园世界，玩家去家园世界，是被信任的就生存，不是就冒险
        if (!isInTrustedWorld(player)) {
            player.gameMode = GameMode.ADVENTURE
            return
        }else {
            player.gameMode = GameMode.SURVIVAL
            return
        }
    }
    /**
     * 监听聊天，主要用于指令回应
     */
    @EventHandler
    fun chat(e:AsyncChatEvent){

    }
    /**
     * 家园穿过地狱门
     */
    @EventHandler
    fun portal(e:PlayerPortalEvent){
        if (e.player.world.name.contains(ChunkWorld.inst.config.getString("World")!!)){
            e.isCancelled = true
            val locationString = ChunkWorld.inst.config.getString("Location")!!
            val worldName = locationString.split(",")[0]
            val x = locationString.split(",")[1].toDouble()
            val y = locationString.split(",")[2].toDouble()
            val z = locationString.split(",")[3].toDouble()
            val yaw = locationString.split(",")[4].toFloat()
            val pitch = locationString.split(",")[5].toFloat()
            val location = Location(Bukkit.getWorld(worldName),x, y, z, yaw, pitch)
            e.player.teleportAsync(location)
        }
        if (e.player.world.name == "world"){
            e.isCancelled = true
            val playerDao = getPlayerDaoMap(e.player.uniqueId)!!
            val location = Location(Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"),
                playerDao.x(),playerDao.y(),playerDao.z(),playerDao.yaw(),playerDao.pitch())
            e.player.teleportAsync(location)
        }
    }
    /**
     * 玩家死亡重生,在自己家园死就在自己家园生
     */
    @EventHandler
    fun death(e:PlayerDeathEvent){
        if (e.player.world.name == ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"){
            reSpawn.add(e.player)
            submit(delay = 20*10) { reSpawn.remove(e.player) }
        }
    }
    /**
     * 玩家重生事件
     */
    @EventHandler
    fun reborn(e:PlayerRespawnEvent){
        if (reSpawn.contains(e.player)){
            //出生在自己家园
            val dao = getPlayerDaoMap(e.player.uniqueId)!!
            e.respawnLocation = Location(Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"),
                dao.x(),dao.y(),dao.z(),dao.yaw(),dao.pitch())
            reSpawn.remove(e.player)
        }
    }
    /**
     *
     */
    @EventHandler
    fun respawn(e:PlayerPostRespawnEvent){

    }
}