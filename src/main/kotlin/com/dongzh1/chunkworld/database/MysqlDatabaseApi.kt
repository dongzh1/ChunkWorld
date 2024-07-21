package com.dongzh1.chunkworld.database

import com.dongzh1.chunkworld.ChunkWorld
import com.xbaimiao.easylib.database.OrmliteMysql

class MysqlDatabaseApi : AbstractDatabaseApi(
    OrmliteMysql(
        ChunkWorld.inst.config.getConfigurationSection("Mysql")!!,
        ChunkWorld.inst.config.getBoolean("Mysql.HikariCP")
    )

)