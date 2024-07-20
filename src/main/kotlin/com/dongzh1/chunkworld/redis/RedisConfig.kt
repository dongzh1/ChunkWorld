package com.dongzh1.chunkworld.redis

import org.bukkit.configuration.Configuration
import redis.clients.jedis.JedisPoolConfig

class RedisConfig(val host: String?, val port: Int) : JedisPoolConfig() {

    var password: String? = null
        private set

    constructor(configuration: Configuration) : this(
        configuration.getString("redis.host"),
        configuration.getInt("redis.port")
    ) {
        val password = configuration.getString("redis.password")
        if (!password.isNullOrEmpty()) {
            this.password = password
        }
    }
}