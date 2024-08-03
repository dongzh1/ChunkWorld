package com.dongzh1.chunkworld.database.dao

data class WorldInfo(
    var state: Byte = 0.toByte(),
    var normalChunks: Int = 0,
    var netherChunks: Int = 0,
    val serverName: String
)
