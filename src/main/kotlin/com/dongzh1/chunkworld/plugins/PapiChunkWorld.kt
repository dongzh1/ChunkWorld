package com.dongzh1.chunkworld.plugins

import com.xbaimiao.easylib.bridge.PlaceholderExpansion
import org.bukkit.OfflinePlayer

object PapiChunkWorld : PlaceholderExpansion() {
    /**
     * 定义一个只读属性 identifier，用于标识插件的名称
     */
    override val identifier: String
        get() = "chunkworld"

    /**
     * 定义一个只读属性 version，用于标识插件的版本号
     */
    override val version: String
        get() = "1.0.0"

    override fun onRequest(p: OfflinePlayer, params: String): String? {
        when (params) {
            "world" -> {
                return "chunkworlds/world/${p.uniqueId}"
            }

            "nether" -> {
                return "chunkworlds/nether/${p.uniqueId}"
            }

            else -> return null
        }
    }
}