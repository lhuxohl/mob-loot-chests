package de.tivin.mobLootChest

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class MobLootChest : JavaPlugin(), Listener {
    private val activeTasks: MutableMap<Location, BukkitRunnable> = mutableMapOf()
    private val replaceBlockTasks: MutableMap<Location, Material> = mutableMapOf()

    override fun onEnable() {
        // Register the event listener
        server.pluginManager.registerEvents(this, this)
        logger.info { "MobLootChest plugin enabled!" }
    }

    override fun onDisable() {
        activeTasks.values.onEach { runnable ->
            runnable.cancel()
        }
        activeTasks.clear()
        logger.info { "MobLootChest plugin disabled!" }
    }

    @EventHandler
    fun onMobDeath(event: EntityDeathEvent) {
        event.entity.location.let { deathLocation ->
            val drops = event.drops.map { it.clone() }
            if (drops.isEmpty()) return

            // Remove default drop handling
            event.drops.clear()

            val isSolidGround = !deathLocation.block.type.isAir

            // Chest should go ABOVE the ground block if the ground block is solid
            val chestBlock = deathLocation.block.takeIf { !isSolidGround }
                ?: deathLocation.block.getRelative(BlockFace.UP)

            // Only place if space is empty
            if (!chestBlock.type.isAir) return

            // Block under the chest (to be restored later)
            val blockUnder = chestBlock.getRelative(BlockFace.DOWN)
            replaceBlockTasks[chestBlock.location] = blockUnder.type

            // Place the chest and add items
            chestBlock.type = Material.CHEST

            val chest = chestBlock.state as Chest
            val chestInventory = chest.inventory

            drops.forEach { chestInventory.addItem(it) }

            // Schedule removal task
            val task = object : BukkitRunnable() {
                override fun run() {
                    if (chestBlock.type == Material.CHEST) {
                        chestBlock.type = Material.AIR
                    }

                    // Restore the block under the chest
                    blockUnder.type = replaceBlockTasks[chestBlock.location]
                        ?: Material.AIR

                    replaceBlockTasks.remove(chestBlock.location)
                    activeTasks.remove(chestBlock.location)
                }
            }

            task.runTaskLater(this, 600L)
            activeTasks[chestBlock.location] = task
        }
    }


    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block: Block = event.block
        val location: Location = block.location

        // If the broken block is a chest placed by this plugin, cancel the scheduled task
        if (activeTasks.containsKey(location)) {
            val task = activeTasks[location]
            task?.cancel()
            activeTasks.remove(location)
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory.holder is Chest) {
            val chest: Chest = event.inventory.holder as Chest
            val block: Block = chest.block
            val location: Location = block.location

            // If the chest is empty after closing, remove it immediately
            if (event.inventory.isEmpty) {
                val task = activeTasks[location]
                task?.cancel()

                // Restore the block under the chest
                val blockUnder = block.getRelative(BlockFace.DOWN)
                val originalMaterial = replaceBlockTasks[location]
                blockUnder.type = originalMaterial ?: Material.AIR

                block.type = Material.AIR

                replaceBlockTasks.remove(location)
                activeTasks[location]?.cancel()
                activeTasks.remove(location)
            }
        }
    }
}
