package com.dongzh1.chunkworld.listener

import com.dongzh1.chunkworld.ChunkWorld
import com.dongzh1.chunkworld.WorldEdit
import com.dongzh1.chunkworld.basic.*
import com.dongzh1.chunkworld.command.Tp
import com.xbaimiao.easylib.util.hasLore
import com.xbaimiao.easylib.util.submit
import com.xbaimiao.invsync.api.events.PlayerDataSyncDoneEvent
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent
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
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.bukkit.event.world.ChunkPopulateEvent
import org.bukkit.persistence.PersistentDataType
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import java.time.Duration
import java.util.*

object GroupListener : Listener {
    //玩家名对应世界，用于跨服传送
    private val locationMap = mutableMapOf<String, Location>()
    private val unloadWorld = mutableListOf<World>()
    fun addUnloadWorld(world: World) = unloadWorld.add(world)
    fun removeUnloadWorld(world: World) = unloadWorld.remove(world)
    fun isUnloadWorld(world: World) = unloadWorld.contains(world)
    fun addLocation(name: String, location: Location) {
        locationMap[name] = location
        submit(delay = 300) { locationMap.remove(name) }
    }

    private fun removeLocation(name: String) {
        locationMap.remove(name)
    }

    private fun getLocation(name: String): Location? {
        return locationMap[name]
    }

    private val respawn = mutableMapOf<Player, Location>()
    private fun isInTrustedWorld(player: Player): Boolean {
        if (player.world.name == "chunkworlds/world/${player.uniqueId}" || player.world.name == "chunkworlds/nether/${player.uniqueId}") return true
        val world = if (player.world.name.contains("/nether/")) Bukkit.getWorld(
            player.world.name.replace(
                "/nether/",
                "/world/"
            )
        )!! else player.world
        val trusts = world.persistentDataContainer.get(
            NamespacedKey.fromString("chunkworld_trust")!!,
            PersistentDataType.STRING
        )!!.split("|,;|")
        return trusts.contains(player.name)
    }

    private fun isBeTrust(player: Player, world: World): Boolean {
        val world1 =
            if (world.name.contains("/nether/")) Bukkit.getWorld(world.name.replace
                ("/nether/", "/world/"))!! else world
        val trustList = world1.persistentDataContainer.get(
            NamespacedKey.fromString("chunkworld_trust")!!,
            PersistentDataType.STRING
        )!!.split("|,;|")
        return trustList.contains(player.name)
    }

    private fun isBeBan(player: Player, world: World): Boolean {
        val world1 =
            if (world.name.contains("/nether/")) Bukkit.getWorld(world.name.replace
                ("/nether/", "/world/"))!! else world
        val banList = world1.persistentDataContainer.get(
            NamespacedKey.fromString("chunkworld_ban")!!,
            PersistentDataType.STRING
        )!!.split("|,;|")
        return banList.contains(player.name)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSpawn(e: PlayerSpawnLocationEvent) {
        e.spawnLocation = getLocation(e.player.name) ?: ChunkWorld.spawnLocation
        if (!e.spawnLocation.isWorldLoaded) e.spawnLocation = ChunkWorld.spawnLocation
    }

    @EventHandler
    fun explode(e: EntityExplodeEvent) {
        if (e.entityType != EntityType.TNT) e.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(e: PlayerJoinEvent) {
        e.joinMessage(null)
        val p = e.player
        val world = p.world
        if (world.name == "world"){
            p.gameMode = GameMode.ADVENTURE
            return
        }
        if (!world.name.startsWith("chunkworlds/")){
            p.gameMode = GameMode.SURVIVAL
            return
        }
        if (isInTrustedWorld(e.player)){
            p.gameMode = GameMode.SURVIVAL
        }else{
            p.gameMode = GameMode.ADVENTURE
        }
    }
    @EventHandler
    fun invSyncDone(e:PlayerDataSyncDoneEvent){
        val p = e.player
        val item8 = p.inventory.getItem(8)
        if (item8 == null){
            p.inventory.setItem(8,Item.menuItem)
        }else{
            if (item8 != Item.menuItem){
                p.inventory.setItem(8,Item.menuItem)
                p.world.dropItem(p.location,item8)
            }
        }
    }
    @EventHandler
    fun drop(e:PlayerDropItemEvent){
        if (e.itemDrop.itemStack == Item.menuItem){
            e.isCancelled = true
        }
    }
    @EventHandler
    fun click(e:InventoryClickEvent){
        if (e.currentItem == Item.menuItem){
            e.isCancelled = true
        }
        if (e.cursor == Item.menuItem){
            e.isCancelled = true
        }
    }
    @EventHandler
    fun drag(e:InventoryDragEvent){
        if (e.oldCursor == Item.menuItem){
            e.isCancelled = true
        }
        if (e.cursor == Item.menuItem){
            e.isCancelled = true
        }
        if (e.newItems.values.contains(Item.menuItem)){
            e.isCancelled = true
        }

    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        e.quitMessage(null)
    }

    @EventHandler
    fun waterFlow(e: BlockFromToEvent) {
        val world = e.block.world
        if (e.block.type == Material.WATER ||
            e.block.type == Material.LAVA ||
            e.block.type == Material.WATER_CAULDRON ||
            e.block.type == Material.LAVA_CAULDRON
        ) {
            //先看世界有没有存这个key，没有说明没有添加规则，可以流动
            if (world.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_fluid")!!)) {
                if (world.persistentDataContainer.get(
                        NamespacedKey.fromString("chunkworld_fluid")!!,
                        PersistentDataType.BOOLEAN
                    ) == true
                ) {
                    //有标记，且可以流动
                    return
                } else {
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
    fun onBlockPlaced(e: BlockPlaceEvent) {
        if (e.itemInHand.itemMeta == Item.voidItem().itemMeta) {
            //判断所放位置在不在已拓展的区块内
            val isInChunks =
                e.blockPlaced.chunk.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_unlock")!!)
            if (!isInChunks) {
                e.isCancelled = true
                e.player.sendMessage("§c请在已拓展的区块内放置")
                return
            }
            //放置下虚空生成器了
            val blockState = e.blockPlaced.state
            val location = e.blockPlaced.location
            var n = 0
            submit(delay = 1, period = 20, maxRunningNum = 11) {
                //道具被毁
                if (location.block.state != blockState) {
                    cancel()
                    e.player.showTitle(
                        Title.title(
                            Component.text("§d生成器破坏"),
                            Component.text("§a虚空化改造已停止"),
                            Title.Times.times(
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2),
                                Duration.ofSeconds(1)
                            )
                        )
                    )
                    return@submit
                }
                if (n < 10) {
                    e.player.showTitle(
                        Title.title(
                            Component.text("§4\uD83D\uDCA5§c虚空吞噬一切§4\uD83D\uDCA5"),
                            Component.text("§a ${10 - n} 秒后缔造虚空,若想取消请用锄头§4破坏§a生成器!"),
                            Title.Times.times(
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2),
                                Duration.ofSeconds(1)
                            )
                        )
                    )
                }
                if (n == 10) {
                    e.player.showTitle(
                        Title.title(
                            Component.text("§c结束了"),
                            Component.text("§a虚空已吞噬区块"),
                            Title.Times.times(
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2),
                                Duration.ofSeconds(1)
                            )
                        )
                    )
                    WorldEdit.setVoid(e.blockPlaced.chunk)
                }
                n++
            }
            //放置虚空生成器

        }

    }

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        if (e.item == Item.menuItem) {
            e.isCancelled = true
            MainGui(e.player).build()
            return
        }

        //地狱邀请函和末地邀请函
        if (e.item?.hasLore("§f一次性门票") == true) {
            e.isCancelled = true
            return
        }
        //虚空生成器
        if (e.item?.itemMeta == Item.voidItem().itemMeta) {
            e.isCancelled = true
            if (e.player.world.name != "chunkworlds/world/${e.player.uniqueId}" && e.player.world.name != "chunkworlds/nether/${e.player.uniqueId}") {
                e.player.sendMessage("§c只能在自己的世界使用")
                return
            }
            if (e.action == Action.RIGHT_CLICK_BLOCK && e.player.isSneaking) {
                //右键放置
                val block = e.clickedBlock
                if (block != null) {
                    if (block.type != Material.BARRIER) {
                        if (block.chunk.persistentDataContainer.has(NamespacedKey.fromString("chunkworld_unlock")!!)) e.isCancelled =
                            false
                    } else {
                        return
                    }
                }
            } else {
                e.player.sendMessage("§c请在潜行状态右键你想改变的区块")
                return
            }
        }
        //如果是拓展物品，也要取消
        var isItem = false
        if (e.item?.type == Material.PAPER) {
            if (e.item!!.itemMeta.hasCustomModelData()) {
                //物品有模型值，看能不能和配置的模型值相等
                if (e.item!!.itemMeta.customModelData == 300008 || e.item!!.itemMeta.customModelData == 300009) {
                    isItem = true
                    //是指定物品，取消
                    e.isCancelled = true
                }
            }
        }

        //玩家不在世界世界就不管
        if (!e.player.world.name.contains("chunkworlds/")) return

        //吃东西也没事
        if (e.hasItem() && e.item!!.type.isEdible) {
            if (e.action == Action.RIGHT_CLICK_BLOCK || e.action == Action.RIGHT_CLICK_AIR) {
                return
            }
        }

        //在世界世界和屏障交互
        if (e.clickedBlock?.type == org.bukkit.Material.BARRIER) {
            //如果是世界主人，可以拓展世界
            e.isCancelled = true
            if (isItem) {
                if (e.player.world.name == "chunkworlds/world/${e.player.uniqueId}" || e.player.world.name == "chunkworlds/nether/${e.player.uniqueId}") {
                    if (e.item!!.hasLore("§c已绑定${e.player.name}")) {
                        ConfirmExpandGui(e.player, e.clickedBlock!!.chunk).build()
                    } else {
                        e.player.sendMessage("§c请勿使用他人物品")
                    }
                }
            }
            return
        }

        if (e.player.isOp) return

        //不是自己的世界或被信任的世界就取消
        if (!isInTrustedWorld(e.player)) {
            e.isCancelled = true
            return
        }

        //世界外面交互
        val chunk1 = e.clickedBlock?.chunk
        if (chunk1?.persistentDataContainer?.has(NamespacedKey.fromString("chunkworld_unlock")!!) != true) {
            e.isCancelled = true
            return
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
        if (e.rightClicked.type == EntityType.PLAYER && e.player.isSneaking) {
            PassportGui(e.player, e.rightClicked as Player).build()
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
    fun hangEntity(e: HangingBreakByEntityEvent) {
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
    fun vehicle(e: VehicleDamageEvent) {
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

    /**传送到世界世界的事件，统一先阻止、判断权限后放行，并根据情况给与玩家相关权限,到没有权限的世界变为冒险模式
     * @param e 玩家传送事件
     */
    @EventHandler
    fun worldTp(e: PlayerTeleportEvent) {
        //传送不涉及世界切换就返回
        val world = e.to.world
        if (world == e.from.world) return
        val player = e.player
        //禁止传送到要卸载的世界
        if (isUnloadWorld(world)) {
            e.isCancelled = true
            player.sendMessage("§c此世界主人不在线")
            return
        }
        //不是玩家世界就不管
        if (!world.name.contains("chunkworlds/")) return
        //如果是自己的世界，那也没啥好取消的
        if (world.name == "chunkworlds/world/${player.uniqueId}" || world.name == "chunkworlds/nether/${player.uniqueId}") return
        //查看是否能传送
        val state =
            world.persistentDataContainer.get(NamespacedKey.fromString("chunkworld_state")!!, PersistentDataType.BYTE)!!
        if (!e.player.isOp) {
            when (state) {
                //玩家世界开放
                0.toByte() -> {
                    //如果是黑名单，也无法进入
                    if (isBeBan(player, world)) {
                        //取消传送任务
                        player.sendMessage("§c此世界禁止你访问")
                        e.isCancelled = true
                        return
                    }
                }

                1.toByte() -> {
                    //部分开放，看看是否被信任
                    if (!isBeTrust(player, world)) {
                        player.sendMessage("§c此世界只允许共享世界的玩家访问")
                        e.isCancelled = true
                        return
                    }
                }
                //玩家世界仅对自己开放
                2.toByte() -> {
                    //取消传送任务
                    player.sendMessage("§c此世界禁止他人访问")
                    e.isCancelled = true
                    return
                }

                else -> {
                    //取消传送任务
                    player.sendMessage("§c此世界禁止访问")
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
    fun worldChange(e: PlayerChangedWorldEvent) {
        //world世界和世界世界为冒险模式，主人和信任者除外
        val player = e.player
        //玩家去主城，就改为冒险模式
        if (player.world.name == "world") {
            player.gameMode = GameMode.ADVENTURE
            return
        }
        //不是玩家世界就生存
        if (!player.world.name.contains("chunkworlds/")) {
            player.gameMode = GameMode.SURVIVAL
            return
        }
        //现在肯定是世界世界，玩家去世界世界，是被信任的就生存，不是就冒险
        if (!isInTrustedWorld(player)) {
            player.gameMode = GameMode.ADVENTURE
            return
        } else {
            player.gameMode = GameMode.SURVIVAL
            return
        }
    }

    /**
     * 世界穿过地狱门
     */
    @EventHandler
    fun portal(e: EntityPortalEnterEvent) {
        if (e.entity is Player) return
        e.isCancelled = true
    }

    @EventHandler
    fun playerPortal(e: PlayerPortalEvent) {
        e.isCancelled = true
        val p = e.player
        if (p.world.name == "world") {
            p.performCommand("chunkworld tp")
            return
        }
        if (!p.world.name.startsWith("chunkworlds/")) {
            p.showTitle(
                Title.title(
                    Component.text("§d请使用邀请函"), Component.text("§f在菜单使用邀请函进行传送"),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
                )
            )
        }
        if (p.world.name == "chunkworlds/world/${p.uniqueId}"){
            val nether = Bukkit.getWorld(p.world.name.replace("/world/", "/nether/"))
            if (nether != null) {
                p.teleportAsync(nether.spawnLocation)
            } else {
                if ((p.world.persistentDataContainer.get(
                        NamespacedKey.fromString("chunkworld_chunks")!!,
                        PersistentDataType.STRING
                )!!.split("|").size - 1) >= 50) {
                    //创建地狱
                    p.showTitle(
                        Title.title(
                            Component.text("§d地狱创建中"), Component.text("§f请稍后"),
                            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
                        )
                    )
                    Tp.createNether(p)
                } else {
                    p.showTitle(
                        Title.title(
                            Component.text("§d地狱未解锁"), Component.text("§f解锁地狱需解锁50个区块"),
                            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
                        )
                    )
                }

            }
        }else if (p.world.name == "chunkworlds/nether/${p.uniqueId}"){
            val world = Bukkit.getWorld(p.world.name.replace("/nether/", "/world/"))!!
            p.teleportAsync(world.spawnLocation)
        } else {
            if (p.world.environment == World.Environment.NORMAL){
                val world = Bukkit.getWorld(p.world.name.replace("/world/","/nether/"))
                if (world != null) p.teleportAsync(world.spawnLocation)
                else p.showTitle(
                    Title.title(
                        Component.text("§d地狱未解锁"), Component.text("§f此世界还没解锁地狱呢"),
                        Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
                    )
                )
            }else{
                val world = Bukkit.getWorld(p.world.name.replace("/nether/","/world/"))!!
                p.teleportAsync(world.spawnLocation)
            }
            //别人家

        }

    }

    /**
     * 玩家死亡重生,在自己世界死就在自己世界生
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun death(e: PlayerDeathEvent) {
        val p = e.player
        if (p.world.name == "chunkworlds/world/${p.uniqueId}" || p.world.name == "chunkworlds/nether/${p.uniqueId}") {
            val world = p.world
            val location = world.spawnLocation
            respawn[p] = location
        }
        submit(delay = 60) { respawn.remove(p) }
    }

    /**
     * 玩家重生事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun reborn(e: PlayerRespawnEvent) {
        val location = respawn[e.player]
        if (location != null) {
            e.respawnLocation = location
            respawn.remove(e.player)
        }


        //出生在自己世界
        //val dao = RedisData.getPlayerDao(e.player.uniqueId.toString())

    }
}