package com.dongzh1.chunkworld.database.dao

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
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
    
    val tName get() = "chunkworlds/${teleport.split(",")[0]}/$uuid"
    val tX get() = teleport.split(",")[1].toDouble()
    val tY get() = teleport.split(",")[2].toDouble()
    val tZ get() = teleport.split(",")[3].toDouble()
    val tYaw get() = teleport.split(",")[4].toFloat()
    val tPitch get() = teleport.split(",")[5].toFloat()
    val wName get() = "chunkworlds/world/$uuid"
    val nName get() = "chunkworlds/nether/$uuid"
    val wX get() = spawn.split(",")[0].toDouble()
    val wY get() = spawn.split(",")[1].toDouble()
    val wZ get() = spawn.split(",")[2].toDouble()
    val wYaw get() = spawn.split(",")[3].toFloat()
    val wPitch get() = spawn.split(",")[4].toFloat()
    val nX get() = netherSpawn.split(",")[0].toDouble()
    val nY get() = netherSpawn.split(",")[1].toDouble()
    val nZ get() = netherSpawn.split(",")[2].toDouble()
    val nYaw get() = netherSpawn.split(",")[3].toFloat()
    val nPitch get() = netherSpawn.split(",")[4].toFloat()

}