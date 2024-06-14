package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.database.dao.PlayerDao
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector
import com.sk89q.worldedit.world.chunk.Chunk
import com.xbaimiao.easylib.ui.Basic
import com.xbaimiao.easylib.ui.PaperBasic
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class ExpandGui(private val p: Player,private val chunkLevel:Int) {
    fun build() {
        val basic = PaperBasic(p, Component.text("生成区块"))
        //设置菜单大小为行
        basic.rows(4)
        //basic.set(11,)
        //basic.set(13)
        //basic.set(15)
        basic.set(30,Item.build(Material.LIME_CONCRETE,1,"§a确认生成",null,-1))
        basic.set(32,Item.build(Material.RED_CONCRETE,1,"§c再次抽取",null,-1))
        basic.openAsync()

    }


}