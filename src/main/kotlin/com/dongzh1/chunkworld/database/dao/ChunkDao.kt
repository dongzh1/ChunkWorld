package com.dongzh1.chunkworld.database.dao

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "chunkDao")
class ChunkDao {
    //有点用的id
    @DatabaseField(generatedId = true)
    var id: Int = 0

    //playerDao表里的玩家对应的id
    @DatabaseField(dataType = DataType.INTEGER, columnName = "playerID")
    var playerID: Int = 0

    //CHUNK的X坐标
    @DatabaseField(dataType = DataType.INTEGER, columnName = "x")
    var x: Int = 0

    //CHUNK的Z坐标
    @DatabaseField(dataType = DataType.INTEGER, columnName = "z")
    var z: Int = 0
    //0代表主世界，1代表地狱，2代表末地
    @DatabaseField(dataType = DataType.BYTE, columnName = "worldType")
    var worldType:Byte = 0

}