package com.dongzh1.chunkworld.database.dao

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "banDao")
class BanshipDao {
    @DatabaseField(generatedId = true)
    var id: Int = 0

    //playerDao表里的玩家对应的id
    @DatabaseField(canBeNull = false,dataType = DataType.INTEGER, columnName = "playerId")
    var playerId: Int = 0

    //被拉黑的玩家的id
    @DatabaseField(canBeNull = false,dataType = DataType.INTEGER, columnName = "bannerId")
    var bannerId: Int = 0


}