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

    @DatabaseField(canBeNull = false, dataType = DataType.LONG, columnName = "onlineTime")
    var onlineTime: Long = 0

    //玩家上次上线时间
    @DatabaseField(canBeNull = false, dataType = DataType.LONG, columnName = "lastTime")
    var lastTime: Long = 0

}