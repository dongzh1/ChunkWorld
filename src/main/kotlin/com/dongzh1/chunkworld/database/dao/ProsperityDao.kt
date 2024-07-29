package com.dongzh1.chunkworld.database.dao

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "ProsperityDao")
class ProsperityDao {
    //有点用的id
    @DatabaseField(generatedId = true)
    var id: Int = 0

    //playerDao表里的玩家对应的id
    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER, columnName = "playerID")
    var playerID: Int = 0

    //食物繁荣度
    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER, columnName = "food")
    var food: Int = 0

    //装饰繁荣度
    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER, columnName = "decoration")
    var decoration: Int = 0

}