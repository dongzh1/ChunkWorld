package com.dongzh1.chunkworld.database.dao

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "shipDao")
class ShipDao {
    //有点用的id
    @DatabaseField(generatedId = true)
    var id: Int = 0

    //playerDao表里的玩家对应的id
    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER, columnName = "player1Id")
    var player1Id: Int = 0

    @DatabaseField(canBeNull = false, dataType = DataType.INTEGER, columnName = "player2Id")
    var player2Id: Int = 0

    //关系是互相信任还是拉黑
    @DatabaseField(canBeNull = false, dataType = DataType.BOOLEAN, columnName = "type")
    var type = false


}