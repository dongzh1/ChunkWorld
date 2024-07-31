package com.dongzh1.chunkworld.listener

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.basic.PassportGui
import com.dongzh1.chunkworld.basic.PlayerGui
import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.dongzh1.chunkworld.redis.RedisData
import com.dongzh1.chunkworld.redis.RedisManager
import com.google.common.base.Utf8
import com.xbaimiao.easylib.util.hasLore
import com.xbaimiao.easylib.util.submit
import io.papermc.paper.event.entity.EntityPortalReadyEvent
import io.papermc.paper.event.player.PlayerFailMoveEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.block.Chest
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.bukkit.event.world.ChunkPopulateEvent
import org.bukkit.event.world.WorldInitEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.time.Duration
import java.util.*

object GroupListener : Listener {
    //玩家名对应世界，用于跨服传送
    private val locationMap = mutableMapOf<String,Location>()
    fun addLocation(name:String,location:Location){
        locationMap[name] = location
        submit(delay = 300) { locationMap.remove(name) }
    }
    private fun removeLocation(name: String){
        locationMap.remove(name)
    }
    private fun getLocation(name: String):Location?{
        return locationMap[name]
    }
    private val respawn = mutableMapOf<Player,Location>()
    private fun isInTrustedWorld(player: Player):Boolean{
        if (player.world.name == "chunkworlds/world/${player.uniqueId}"
            || player.world.name == "chunkworlds/world/${player.uniqueId}")
            return true
        val trustList = RedisData.getFriends(player.uniqueId.toString()) ?: return false
        return trustList.contains(player.world.name.split("/").last())
    }
    private fun isBeBan(player: Player,uuid: String):Boolean{
        val banList = RedisData.getBanners(player.uniqueId.toString())!!
        return banList.contains(uuid)
    }
    private fun isBeTrust(player: Player,uuid: String):Boolean{
        val trustList = RedisData.getFriends(player.uniqueId.toString())!!
        return trustList.contains(uuid)
    }

    @EventHandler
    fun onWorldInit(e:WorldInitEvent){
        //判断是不是玩家世界
        val worldName = e.world.name
        if (worldName.startsWith("chunkworlds/")){
            //将数据从数据库存入redis
            submit(async = true) {
                val list = worldName.split("/")
                val uuidString = list.last()
                val playerDao: PlayerDao = ChunkWorld.db.playerGet(UUID.fromString(uuidString)) ?: return@submit
                val chunkDaoList = ChunkWorld.db.chunkGet(playerDao.id)
                //存入redis
                RedisData.setPlayerDao(playerDao)
                RedisData.setChunks(playerDao.uuid.toString(),chunkDaoList)
            }
        }
    }
    @EventHandler
    fun onSpawn(e:PlayerSpawnLocationEvent){
        e.spawnLocation = getLocation(e.player.name) ?: ChunkWorld.spawnLocation
    }
    @EventHandler
    fun explode(e:EntityExplodeEvent){
        if (e.entityType != EntityType.TNT) e.isCancelled = true
    }

    @EventHandler
    fun onJoin(e:PlayerJoinEvent){ e.joinMessage(null) }
    @EventHandler
    fun onQuit(e:PlayerQuitEvent){e.quitMessage(null)}
    @EventHandler
    fun waterFlow(e: BlockFromToEvent){
        val world = e.block.world
        if (e.block.type == Material.WATER ||
            e.block.type == Material.LAVA ||
            e.block.type == Material.WATER_CAULDRON ||
            e.block.type == Material.LAVA_CAULDRON) {
            //先看世界有没有存这个key，没有说明没有添加规则，可以流动
            if (world.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_fluid")!!)){
                if (world.persistentDataContainer.get(NamespacedKey.fromString("chunkworld_fluid")!!,
                        PersistentDataType.BOOLEAN) == true){
                    //有标记，且可以流动
                    return
                }else {
                    //不能流动
                    e.isCancelled = true
                    return
                }
            } else {
                return
            }
        }
    }
    @EventHandler
    fun onBlockPlaced(e: BlockPlaceEvent){
        if (e.itemInHand.itemMeta == Item.voidItem().itemMeta){
            //判断所放位置在不在已拓展的区块内
            val chunks =
            if (e.blockPlaced.world.environment == World.Environment.NORMAL){
                RedisData.getChunks(e.player.uniqueId.toString(),0.toByte())!!
            }else{
                RedisData.getChunks(e.player.uniqueId.toString(),1.toByte())!!
            }
            var isIN = false
            for (c in chunks){
                val chunk = e.blockPlaced.chunk
                if ((chunk.x to chunk.z) == c){
                    isIN = true
                    break
                }
            }
            if (!isIN){
                //在外界
                e.isCancelled = true
                return
            }
            //放置下虚空生成器了
            val blockState = e.blockPlaced.state
            val location = e.blockPlaced.location
            var n = 0
            val task = submit(delay = 1,period = 20, maxRunningNum = 11) {
                //道具被毁
                if (location.block.state != blockState){
                    cancel()
                    e.player.showTitle(
                        Title.title(Component.text("§d生成器破坏"),
                        Component.text("§a虚空化改造已停止"),
                        Title.Times.times(
                            Duration.ofSeconds(1),
                            Duration.ofSeconds(2),
                            Duration.ofSeconds(1))))
                    return@submit
                }
                if(n < 10){
                    e.player.showTitle(Title.title(Component.text("§4\uD83D\uDCA5§c虚空吞噬一切§4\uD83D\uDCA5"),
                        Component.text("§a ${10-n} 秒后缔造虚空,若想取消请用锄头§4破坏§a生成器!"),
                        Title.Times.times(Duration.ofSeconds(1),
                            Duration.ofSeconds(2),
                            Duration.ofSeconds(1))))
                }
                if (n == 10){
                    e.player.showTitle(Title.title(Component.text("§c结束了"),
                        Component.text("§a虚空已吞噬区块"),
                        Title.Times.times(Duration.ofSeconds(1),
                            Duration.ofSeconds(2),
                            Duration.ofSeconds(1))))
                    WorldEdit.setVoid(e.blockPlaced.chunk)
                }
                n++
            }
            //放置虚空生成器

        }



    }
    @EventHandler
    fun onInteract(e: PlayerInteractEvent){

        //地狱邀请函和末地邀请函
        if (e.item?.hasLore("§f在传送菜单进行传送") == true){
            e.isCancelled = true
            return
        }
        //虚空生成器
        if (e.item?.itemMeta == Item.voidItem().itemMeta){
            e.isCancelled = true
            if (e.player.world.name != "chunkworlds/world/${e.player.uniqueId}" && e.player.world.name != "chunkworlds/nether/${e.player.uniqueId}"){
                e.player.sendMessage("§c只能在自己的世界使用")
                return
            }
            if (e.action == Action.RIGHT_CLICK_BLOCK && e.player.isSneaking){
                //右键放置
                val block = e.clickedBlock
                if (block != null){
                    if (block.type != Material.BARRIER){
                        //判断所放位置在不在已拓展的区块内
                        val chunks = if (e.player.world.environment == World.Environment.NORMAL){
                            RedisData.getChunks(e.player.uniqueId.toString(),0.toByte())!!
                        }else{
                            RedisData.getChunks(e.player.uniqueId.toString(),1.toByte())!!
                        }
                        var isIN = false
                        for (c in chunks){
                            val chunk = block.chunk
                            if ((chunk.x to chunk.z) == c){
                                isIN = true
                                break
                            }
                        }
                        if (isIN){
                            //不是屏障
                            e.isCancelled = false
                        }
                    }else{
                        return
                    }
                }
            }else{
                e.player.sendMessage("§c请在潜行状态右键你想改变的区块")
                return
            }
        }
        //如果是拓展物品，也要取消
        var isItem = false
        if (e.item?.type == Material.PAPER){
            if (e.item!!.itemMeta.hasCustomModelData()){
                //物品有模型值，看能不能和配置的模型值相等
                if (e.item!!.itemMeta.customModelData == 300008 || e.item!!.itemMeta.customModelData == 300009){
                    isItem = true
                    //是指定物品，取消
                    e.isCancelled = true
                }
            }
        }

        //玩家不在家园世界就不管
        if (!e.player.world.name.contains("chunkworlds/")) return

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
            if (isItem){
                if (e.player.world.name == "chunkworlds/world/${e.player.uniqueId}" || e.player.world.name == "chunkworlds/nether/${e.player.uniqueId}"){
                    if (e.item!!.hasLore("§c已绑定${e.player.name}")){
                        ConfirmExpandGui(e.player, e.clickedBlock!!.chunk).build()
                    }else{
                        e.player.sendMessage("§c请勿使用他人物品")
                    }
                }
            }
            return
        }

        if (e.player.isOp) return

        //不是自己的世界或被信任的世界就取消
        if (!isInTrustedWorld(e.player)){
            e.isCancelled = true
            return
        }

        //世界外面交互
        val chunk1 = e.clickedBlock?.chunk
        //玩家必在家园世界，所以看看这个世界的区块来决定能不能放置
        val ownerUUID = e.player.world.name.split("/").last()
        val chunks =
        if (e.player.world.environment == World.Environment.NORMAL){
            RedisData.getChunks(ownerUUID,0.toByte())!!
        }else{
            RedisData.getChunks(ownerUUID,1.toByte())!!
        }
        var isIn = false
        if (chunk1 != null){
            for (c in chunks){
                if ((chunk1.x to chunk1.z) == c){
                    isIn = true
                    break
                }
            }
            if (!isIn ){
                //在边界外
                e.isCancelled = true
                return
            }
        }
    }
    @EventHandler
    //阻止玩家被实体锁定目标
    fun target(e: EntityTargetLivingEntityEvent) {
        //不是玩家世界就不管
        if (e.target == null) return
        if (!e.target!!.world.name.contains("chunkworlds/")) return
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
        if (!e.entity.world.name.contains("chunkworlds/")) return
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
        if (e.rightClicked.type == EntityType.PLAYER && e.player.isSneaking){
            PassportGui(e.player,e.rightClicked as Player).build()
        }
        if (e.player.isOp)
            return
        //不是玩家世界就不管
        if (!e.player.world.name.contains("chunkworlds/")) return
        //不是自己的世界或被信任的世界就取消
        e.isCancelled = !isInTrustedWorld(e.player)
    }
    @EventHandler
    //不和悬挂实体交互
    fun hangEntity(e: HangingBreakByEntityEvent){
        if (e.remover is Player) {
            val player = e.remover as Player
            if (player.isOp) return
            //不是玩家世界就不管
            if (!player.world.name.contains("chunkworlds/")) return
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
            if (!player.world.name.contains("chunkworlds/")) return
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
        val world = e.to.world
        if (world == e.from.world) return
        val player = e.player
        //从其他世界到资源世界
        if (world.name == ("chunkworld_zy")){
            //判断区块等级
            val chunks = RedisData.getChunks(player.uniqueId.toString(),0.toByte())
            if (chunks!!.size >=9){
                player.sendMessage("§a您成功传送到资源世界,请尽可能活下去")
                return
            }else{
                e.isCancelled = true
                player.sendMessage("§c传送取消,去到资源世界需要解锁至少9个区块")
                return
            }
        }
        if (world.name == "world_nether_zy"){
            //消耗邀请函并传送
            if (Item.deduct(Item.netherItem(player),player,1)){
                player.sendMessage("§a你跨世界传送到了§4资源地狱§a,消耗 §4地狱邀请函§a x1")
                return
            }else{
                //公共邀请函判断
                if (Item.deduct(Item.netherItem(),player,1)){
                    player.sendMessage("§a你跨世界传送到了§4资源地狱§a,消耗 §4地狱邀请函§a x1")
                    return
                }else{
                    player.sendMessage("§c你没有§4地狱邀请函，无法跨世界传送到资源地狱")
                    e.isCancelled = true
                    return
                }
            }
        }
        if (world.name == "world_the_end"){
            //消耗邀请函并传送
            if (Item.deduct(Item.endItem(player),player,1)){
                player.sendMessage("§a你跨世界传送到了§5资源末地§a,消耗 §5末地邀请函§a x1")
                return
            }else{
                if (Item.deduct(Item.endItem(),player,1)){
                    player.sendMessage("§a你跨世界传送到了§5资源末地§a,消耗 §5末地邀请函§a x1")
                    return
                }else{
                    player.sendMessage("§c你没有§5末地邀请函，无法跨世界传送到资源末地")
                    e.isCancelled = true
                    return
                }
            }
        }
        //不是玩家世界就不管
        if (!world.name.contains("chunkworlds/")) return
        //如果是自己的世界，那也没啥好取消的
        if (world.name == "chunkworlds/world/${player.uniqueId}" || world.name == "chunkworlds/nether/${player.uniqueId}") return
        //现在确定是别人的家园世界,获取对应世界玩家的uuid
        val uuid = world.name.split("/").last()
        val playerDao = RedisData.getPlayerDao(uuid)
        //查看是否能传送
        if (!e.player.isOp){
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
            return
        }
        //不是玩家世界就生存
        if (!player.world.name.contains("chunkworlds/")) {
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
     * 家园穿过地狱门
     */
    @EventHandler
    fun portal(e:EntityPortalEnterEvent){
        e.isCancelled = true
        //todo
        /*
        e.player.showTitle(Title.title(Component.text("§d跨界传送"), Component.text("§f请退出传送门范围,并§8[§b右键§8]§f传送门选择目标"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))))
        val playerDao = RedisData.getPlayerDao(p.uniqueId.toString())
        if (p.world.name == "chunkworlds/world/${p.uniqueId}"){
            p.teleportAsync(Location(Bukkit.getWorld("chunk"),playerDao!!.nX,playerDao.nY,playerDao.nZ,playerDao.nYaw,playerDao.nPitch))
        }

        if (e.player.world.name.contains(ChunkWorld.inst.config.getString("World")!!)){
            val locationString = ChunkWorld.inst.config.getString("Location")!!
            val worldName = locationString.split(",")[0]
            val x = locationString.split(",")[1].toDouble()
            val y = locationString.split(",")[2].toDouble()
            val z = locationString.split(",")[3].toDouble()
            val yaw = locationString.split(",")[4].toFloat()
            val pitch = locationString.split(",")[5].toFloat()
            val location = Location(Bukkit.getWorld(worldName),x, y, z, yaw, pitch)
            e.player.teleportAsync(location)
            return
        }
        if (e.player.world.name == "world"){
            val playerDao = getPlayerDaoMap(e.player.name)!!
            val location = Location(Bukkit.getWorld(ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"),
                playerDao.x(),playerDao.y(),playerDao.z(),playerDao.yaw(),playerDao.pitch())
            e.player.teleportAsync(location)
        }

         */


    }
    /**
     * 玩家死亡重生,在自己家园死就在自己家园生
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun death(e: PlayerDeathEvent){
        val p = e.player
        val dao = RedisData.getPlayerDao(p.uniqueId.toString())
        if (p.world.name == "chunkworlds/world/${p.uniqueId}"){
            val world = p.world
            val location = Location(world,dao!!.wX,dao.wY,dao.wZ,dao.wYaw,dao.wPitch)
            respawn[p] = location
        }
        if (p.world.name == "chunkworlds/nether/${p.uniqueId}"){
            val world = p.world
            val location = Location(world,dao!!.nX,dao.nY,dao.nZ,dao.nYaw,dao.nPitch)
            respawn[p] = location
        }
        submit(delay = 60) { respawn.remove(p) }


        /*
        if (e.player.world.name == ChunkWorld.inst.config.getString("World")!!+"/${e.player.uniqueId}"){
            reSpawn.add(e.player)
            submit(delay = 20*10) { reSpawn.remove(e.player) }
        }

         */
    }
    /**
     * 玩家重生事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun reborn(e:PlayerRespawnEvent){
        val location = respawn[e.player]
        if (location != null){
            e.respawnLocation = location
            respawn.remove(e.player)
        }









        //出生在自己家园
        //val dao = RedisData.getPlayerDao(e.player.uniqueId.toString())

    }
    /**
     * 加载区块的时候添加宝箱物品
     */
    @EventHandler
    fun onChunkLoad(event: ChunkPopulateEvent) {
        val chunk = event.chunk
        submit(async = true) {
            for (x in 0..15) {
                for (z in 0..15) {
                    for (y in chunk.world.minHeight until chunk.world.maxHeight) {
                        val block = chunk.getBlock(x, y, z)
                        if (block.type == Material.CHEST) {
                            submit {
                                val chest = block.state as Chest
                                if (Random().nextInt(20)>18){
                                    chest.inventory.addItem(Item.endItem())
                                }
                                if (Random().nextInt(20)>18){
                                    chest.inventory.addItem(Item.netherItem())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}