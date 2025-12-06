package de.tivin.mobLootChest

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
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
        val entity: LivingEntity = event.entity
        val location: Location = entity.location

        // Capture the drops first, then clear default drops
        val drops: List<ItemStack> = event.drops.map { it.clone() }
        event.drops.clear()

        // Place a chest at the mob's death location
        val block: Block = location.block

        // Save block type under chest to restore later
        val blockUnderChest: Block = block.getRelative(BlockFace.DOWN)
        replaceBlockTasks[blockUnderChest.location] = blockUnderChest.type

        if (drops.isNotEmpty()) {
            block.type = Material.CHEST
        }

        if (block.state is Chest) {
            val chest: Chest = block.state as Chest
            val chestInventory: Inventory = chest.inventory

            // Add the mob's loot to the chest
            drops.onEach { drop ->
                chestInventory.addItem(drop)
            }
        }

        // Despawn the chest after 30 seconds
        val task = object : BukkitRunnable() {
            override fun run() {
                if (block.type == Material.CHEST) {
                    block.type = Material.AIR
                }

                // Restore the block under the chest
                val blockUnder = block.getRelative(BlockFace.DOWN)
                val originalMaterial = replaceBlockTasks[blockUnder.location]

                blockUnder.type = originalMaterial ?: Material.AIR

                replaceBlockTasks.remove(blockUnder.location)
                activeTasks.remove(location)
            }
        }

        task.runTaskLater(this, 600L) // 30 * 20 ticks = 600L = 30 seconds
        activeTasks[location] = task;
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
                val originalMaterial = replaceBlockTasks[blockUnder.location]
                blockUnder.type = originalMaterial ?: Material.AIR

                block.type = Material.AIR

                replaceBlockTasks.remove(blockUnder.location)
                activeTasks.remove(location)
            }
        }
    }
}
