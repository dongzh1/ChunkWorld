package com.dongzh1.chunkworld

import com.xbaimiao.easylib.bridge.PlaceholderExpansion
import org.bukkit.entity.Player

object Papi: PlaceholderExpansion() {
    private val sizeMap = mapOf(
        1 to "", 2 to "", 3 to "", 4 to "", 5 to "", 6 to "", 7 to "", 15 to "", 31 to "", 63 to "", 127 to "", 256 to "", 512 to "", 1024 to "",
        -3 to "", -4 to "", -5 to "", -6 to "", -7 to "", -8 to "", -9 to "", -10 to "", -18 to "", -34 to "", -66 to "", -130 to "", -259 to "", -515 to "", -1027 to ""
    )

    override val identifier: String
        get() = "img"
    override val version: String
        get() = "1.0.0"

    override fun onPlaceholderRequest(p: Player, params: String): String? {
        if (params.startsWith("offset_")) {
            val size = try {
                params.split("_")[1].toInt()
            } catch (e: Exception) {
                return null
            }
            return getSizeString(size)
        }
        return null
    }

    private fun getSizeString(size: Int): String {
        val result = StringBuilder()
        var remainingSize = size
        val sortedKeys = if (size > 0) sizeMap.keys.sortedDescending() else sizeMap.keys.sorted()

        for (key in sortedKeys) {
            while (remainingSize >= key) {
                result.append(sizeMap[key])
                remainingSize -= key
            }
        }
        return result.toString()
    }
}