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
    @DatabaseField(dataType = DataType.UUID, columnName = "uuid")
    lateinit var uuid: UUID

    //玩家名字
    @DatabaseField(dataType = DataType.STRING, columnName = "name")
    lateinit var name: String

    //世界创建时间,yyyy_mm_dd_hh_mm_ss
    @DatabaseField(dataType = DataType.STRING, columnName = "create_time")
    lateinit var createTime: String

    //世界出生点
    @DatabaseField(dataType = DataType.STRING, columnName = "spawn")
    lateinit var spawn: String

    //世界状态,012分别代表开放,部分开放,关闭
    @DatabaseField(dataType = DataType.SHORT, columnName = "world_status")
    var worldStatus: Short = 0

}