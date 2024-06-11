package com.dongzh1.chunkworld.database

import com.xbaimiao.easylib.database.OrmliteSQLite

class SQLiteDatabaseApi : AbstractDatabaseApi(OrmliteSQLite("database.db"))