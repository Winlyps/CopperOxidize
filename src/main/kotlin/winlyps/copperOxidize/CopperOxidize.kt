package winlyps.copperOxidize

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class CopperOxidize : JavaPlugin(), Listener {

    private val copperBlocks = mutableMapOf<Block, Material>()

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        startOxidationTask()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        if (isCopper(block)) {
            copperBlocks[block] = block.type
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (isCopper(block)) {
            copperBlocks.remove(block)
        }
    }

    private fun startOxidationTask() {
        object : BukkitRunnable() {
            override fun run() {
                val iterator = copperBlocks.entries.iterator()
                while (iterator.hasNext()) {
                    val (block, currentStage) = iterator.next()
                    val nextStage = getNextOxidationStage(currentStage)
                    if (nextStage != null) {
                        block.type = nextStage
                        copperBlocks[block] = nextStage
                    } else {
                        iterator.remove()
                    }
                }
            }
        }.runTaskTimer(this, 200L, 200L) // 200 ticks = 10 seconds
    }

    private fun isCopper(block: Block): Boolean {
        return block.type in copperStages
    }

    private fun getNextOxidationStage(current: Material): Material? {
        return copperStages.indexOf(current).takeIf { it >= 0 && it < copperStages.size - 1 }?.let { copperStages[it + 1] }
    }

    companion object {
        val copperStages = listOf(
                Material.COPPER_BLOCK,
                Material.EXPOSED_COPPER,
                Material.WEATHERED_COPPER,
                Material.OXIDIZED_COPPER
        )
    }
}