package com.dongzh1.chunkworld.basic

import com.dongzh1.chunkworld.ChunkWorld
import com.xbaimiao.easylib.ui.PaperBasic
import com.xbaimiao.easylib.util.hasItem
import com.xbaimiao.easylib.util.submit
import com.xbaimiao.easylib.util.takeItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class ConfirmExpandGui(private val p: Player,private val chunkLevel :Int) {
    fun build() {
        val basic = PaperBasic(p, Component.text("生成区块"))
        //设置菜单大小为行
        basic.rows(4)
        basic.set(13,Item.build(Material.valueOf(ChunkWorld.inst.config.getString("item.material")!!)
            ,chunkLevel,"§3生成区块详情"
            , listOf("§f这是 §f$chunkLevel 级区块","§f消耗 §b$chunkLevel §f个区块碎片来生成区块","§f请勿在选择区块时离线","§4选择 §a确认 §4后碎片将无法退回")
            ,-1))
        basic.set(30,Item.build(Material.LIME_CONCRETE,1,"§a确认", listOf("§4确认后扣除碎片"),-1))
        basic.set(32,Item.build(Material.RED_CONCRETE,1,"§c取消",null,-1))
        //取消全局点击事件
        basic.onClick { event ->
            event.isCancelled = true
        }
        basic.onClick(30){
            if (deduct()){
                //扣除成功
                ExpandGui(p,chunkLevel).build()
            }else{
                //没有足够的物品
                p.sendMessage("§c你没有足够的物品")
                p.closeInventory()
            }
        }
        basic.onClick(32){
            p.closeInventory()
        }
        basic.openAsync()
    }

    /**
     * 从玩家背包判断是否有足够的指定物品，如果有就扣除并返回true,没有就返回false
     */
    private fun deduct():Boolean{
        val material = Material.valueOf(ChunkWorld.inst.config.getString("item.material")!!)
        return if (ChunkWorld.inst.config.getInt("item.customModelData") == -1) {
            //判断有没有那么多
            if (p.inventory.hasItem(amount = chunkLevel, matcher = { type == material }))
            //有就扣除并返回true
                p.inventory.takeItem(amount = chunkLevel, matcher = { type == material })
            else
                false
        }else{
            if (p.inventory.hasItem(amount = chunkLevel, matcher = { type == material && itemMeta?.customModelData == ChunkWorld.inst.config.getInt("item.customModelData") }))
                p.inventory.takeItem(amount = chunkLevel, matcher = { type == material && itemMeta?.customModelData == ChunkWorld.inst.config.getInt("item.customModelData") })
            else
                false
        }
    }
}