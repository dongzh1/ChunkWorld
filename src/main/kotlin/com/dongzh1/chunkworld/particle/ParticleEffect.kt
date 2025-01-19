import com.xbaimiao.easylib.task.EasyLibTask
import com.xbaimiao.easylib.util.submit
import org.bukkit.Location
import org.bukkit.Particle
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

object ParticleEffect {

    // 存储所有粒子效果任务，使用UUID作为标识符
    private val taskList = mutableMapOf<UUID, EasyLibTask>()

    // 启动粒子循环效果并返回效果ID,center是方块坐标，要偏移
    fun startCircleEffect(center: Location, radius: Double, count: Int, particle: Particle): UUID {
        val id = UUID.randomUUID()
        val list = mutableListOf<Location>()
        val world = center.world
        val x = center.x + 0.5
        val y = center.y
        val z = center.z + 0.5
        // 计算每 30° 的边上点
        for (i in 0..11) {  // 总共计算 30° 到 330° 的点（每30°）
            val angle = Math.PI / 6 * i
            val xOffset = radius * cos(angle)
            val zOffset = radius * sin(angle)
            list.add(Location(world, x + xOffset, y, z + zOffset))
        }
        val task = submit(delay = 60, period = 2) {
            if (center.world != null && center.chunk.isLoaded) {
                // 播放粒子
                list.forEach { center.world?.spawnParticle(particle, it, count, 0.0, 0.0, 0.0, 0.0) }
            } else {
                cancel()
                taskList.remove(id)
            }
        }
        taskList[id] = task

        return id
    }

    // 停止某个特定的粒子效果
    fun stopEffect(effectId: UUID) {
        taskList[effectId]?.cancel()
        taskList.remove(effectId)
    }

    // 停止所有粒子效果
    fun stopAllEffects() {
        taskList.values.forEach { it.cancel() }
        taskList.clear()
    }
}
