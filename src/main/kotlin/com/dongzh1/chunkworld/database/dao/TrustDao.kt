package com.dongzh1.chunkworld.database.dao

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "trustDao")
class TrustDao {
    //有点用的id
    @DatabaseField(generatedId = true)
    var id: Int = 0

    //playerDao表里的玩家对应的id
    @DatabaseField(dataType = DataType.INTEGER, columnName = "playerID")
    var playerID: Int = 0

    //被信任的玩家的id
    @DatabaseField(dataType = DataType.INTEGER, columnName = "trustedPlayerID")
    var trustedPlayerID: Int = 0


}