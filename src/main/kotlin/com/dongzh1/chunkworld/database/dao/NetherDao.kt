package com.dongzh1.chunkworld.database.dao

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import org.bukkit.Location
import java.util.*

@DatabaseTable(tableName = "netherDao")
class NetherDao {
    //有点用的id
    @DatabaseField(dataType = DataType.INTEGER, id = true)
    var id: Int = 0

    //世界出生点
    @DatabaseField(dataType = DataType.STRING, columnName = "spawn")
    lateinit var spawn: String

    //占领区块数量，防便调用查看
    @DatabaseField(dataType = DataType.INTEGER, columnName = "chunkCount")
    var chunkCount: Int = 0

    fun x() = spawn.split(",")[0].toDouble()
    fun y() = spawn.split(",")[1].toDouble()
    fun z() = spawn.split(",")[2].toDouble()
    fun yaw() = spawn.split(",")[3].toFloat()
    fun pitch() = spawn.split(",")[4].toFloat()

}