package com.dongzh1.chunkworld.database.dao

import com.dongzh1.chunkworld.ChunkWorld
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import org.bukkit.Bukkit
import java.util.*


@DatabaseTable(tableName = "playerDao")
class PlayerDao {
    //有点用的id
    @DatabaseField(generatedId = true)
    var id: Int = 0

    //玩家UUID
    @DatabaseField(canBeNull = false, dataType = DataType.UUID, columnName = "uuid")
    lateinit var uuid: UUID

    //玩家名字
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = "name")
    lateinit var name: String

    //世界创建时间,yyyy_mm_dd_hh_mm_ss
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = "createTime")
    lateinit var createTime: String

    //玩家上次上线时间
    @DatabaseField(canBeNull = false, dataType = DataType.LONG, columnName = "lastTime")
    var lastTime: Long = 0

    //世界传送点
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = "teleport")
    lateinit var teleport: String

    //世界出生点
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = "spawn")
    lateinit var spawn: String

    //世界出生点
    @DatabaseField(canBeNull = false, dataType = DataType.STRING, columnName = "netherSpawn")
    lateinit var netherSpawn: String

    //世界状态,012分别代表开放,部分开放,关闭
    @DatabaseField(canBeNull = false, dataType = DataType.BYTE, columnName = "worldStatus")
    var worldStatus: Byte = 0
    
    val teleportWorldName get() = ChunkWorld.inst.config.getString("World")!! + "/" + uuid.toString()+"/"+teleport.split(",")[0]
    val teleportX get() = teleport.split(",")[1].toDouble()
    val teleportY get() = teleport.split(",")[2].toDouble()
    val teleportZ get() = teleport.split(",")[3].toDouble()
    val teleportYaw get() = teleport.split(",")[4].toFloat()
    val teleportPitch get() = teleport.split(",")[5].toFloat()
    val normalWorldName get() = ChunkWorld.inst.config.getString("World")!! + "/" + uuid.toString()+"/world"
    val netherWorldName get() = ChunkWorld.inst.config.getString("World")!! + "/" + uuid.toString()+"/nether"
    val normalX get() = spawn.split(",")[0].toDouble()
    val normalY get() = spawn.split(",")[1].toDouble()
    val normalZ get() = spawn.split(",")[2].toDouble()
    val normalYaw get() = spawn.split(",")[3].toFloat()
    val normalPitch get() = spawn.split(",")[4].toFloat()
    val netherX get() = netherSpawn.split(",")[0].toDouble()
    val netherY get() = netherSpawn.split(",")[1].toDouble()
    val netherZ get() = netherSpawn.split(",")[2].toDouble()
    val netherYaw get() = netherSpawn.split(",")[3].toFloat()
    val netherPitch get() = netherSpawn.split(",")[4].toFloat()

}