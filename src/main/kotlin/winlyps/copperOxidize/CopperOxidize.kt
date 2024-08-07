package winlyps.copperOxidize

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class CopperOxidize : JavaPlugin(), Listener {

    private val copperBlocks = ConcurrentHashMap<Block, Material>()
    private val blockUpdateQueue = ConcurrentLinkedQueue<Pair<Block, Material>>()
    private val executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val updateCounter = AtomicInteger(0)

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        startOxidationTask()
    }

    override fun onDisable() {
        executorService.shutdown()
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
                        blockUpdateQueue.add(Pair(block, nextStage))
                        copperBlocks[block] = nextStage
                    } else {
                        iterator.remove()
                    }
                }
                processBlockUpdates()
            }
        }.runTaskTimer(this, 30 * 60 * 20L, 30 * 60 * 20L) // 30 minutes = 30 * 60 * 20 ticks
    }

    private fun processBlockUpdates() {
        val batchSize = 1000
        while (!blockUpdateQueue.isEmpty()) {
            val batch = mutableListOf<Pair<Block, Material>>()
            for (i in 0 until batchSize) {
                val update = blockUpdateQueue.poll()
                if (update != null) {
                    batch.add(update)
                } else {
                    break
                }
            }
            executorService.submit {
                try {
                    val updates = batch.map { (block, nextStage) ->
                        block to nextStage
                    }
                    server.scheduler.runTask(this, Runnable {
                        try {
                            updates.forEach { (block, nextStage) ->
                                block.type = nextStage
                            }
                        } catch (e: Exception) {
                            logger.severe("Error updating block types: ${e.message}")
                        }
                    })
                    updateCounter.addAndGet(-batch.size)
                } catch (e: Exception) {
                    logger.severe("Error processing block updates: ${e.message}")
                }
            }
            updateCounter.addAndGet(batch.size)
        }
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