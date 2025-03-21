package trplugins.menu.api.receptacle

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submit
import taboolib.module.nms.nmsProxy

/**
 * @author Arasple
 * @date 2020/11/29 10:38
 */
open class Receptacle(var type: VanillaLayout, title: String = type.toBukkitType().defaultTitle) {

    private var viewer: Player? = null

    private var onOpen: ((player: Player, receptacle: Receptacle) -> Unit) = { _, _ -> }

    private var onClose: ((player: Player, receptacle: Receptacle) -> Unit) = { _, _ -> }

    private var onClick: ((player: Player, event: ReceptacleInteractEvent) -> Unit) = { _, _ -> }

    private val contents by lazy { arrayOfNulls<ItemStack?>(type.totalSize) }

    private var hidePlayerInventory = false

    private var stateId = 1
        get() {
            return field++
        }

    var title = title
        set(value) {
            field = value
            submit(delay = 3, async = true) {
                initializationPackets()
            }
        }

    fun hidePlayerInventory(hidePlayerInventory: Boolean) {
        this.hidePlayerInventory = hidePlayerInventory
    }

    fun getItem(slot: Int): ItemStack? {
        setupPlayerInventorySlots()
        return contents.getOrNull(slot)
    }

    fun hasItem(slot: Int): Boolean {
        return getItem(slot) != null
    }

    fun setItem(itemStack: ItemStack? = null, slots: Collection<Int>, display: Boolean = true) {
        setItem(itemStack, *slots.toIntArray(), display = display)
    }

    fun setItem(itemStack: ItemStack? = null, vararg slots: Int, display: Boolean = true) {
        slots.forEach { contents[it] = itemStack }
        if (display && viewer != null) {
            slots.forEach { nmsProxy<NMS>().sendWindowsSetSlot(viewer!!, slot = it, itemStack = itemStack, stateId = stateId) }
        }
    }

    fun clear(display: Boolean = true) {
        contents.indices.forEach { contents[it] = null }
        if (display) {
            refresh()
        }
    }

    fun refresh(slot: Int = -1) {
        if (viewer != null) {
            setupPlayerInventorySlots()
            if (slot >= 0) {
                nmsProxy<NMS>().sendWindowsSetSlot(viewer!!, slot = slot, itemStack = contents[slot], stateId = stateId)
            } else {
                nmsProxy<NMS>().sendWindowsItems(viewer!!, items = contents)
            }
        }
    }

    fun onOpen(handler: (player: Player, receptacle: Receptacle) -> Unit) {
        this.onOpen = handler
    }

    fun onClose(handler: (player: Player, receptacle: Receptacle) -> Unit) {
        this.onClose = handler
    }

    fun onClick(clickEvent: (player: Player, event: ReceptacleInteractEvent) -> Unit) {
        this.onClick = clickEvent
    }

    fun open(player: Player) {
        viewer = player
        initializationPackets()
        player.setViewingReceptacle(this)
        onOpen(player, this)
    }

    fun close(sendPacket: Boolean = true) {
        if (viewer != null) {
            if (sendPacket) {
                nmsProxy<NMS>().sendWindowsClose(viewer!!)
            }
            onClose(viewer!!, this)
            viewer!!.setViewingReceptacle(null)
        }
    }

    fun callEventClick(event: ReceptacleInteractEvent) {
        if (viewer != null) {
            onClick(viewer!!, event)
        }
    }

    internal fun initializationPackets() {
        if (viewer != null) {
            nmsProxy<NMS>().sendWindowsOpen(viewer!!, title = title, type = type)
            refresh()
        }
    }

    internal fun setupPlayerInventorySlots() {
        if (hidePlayerInventory || viewer == null) {
            return
        }
        viewer!!.inventory.contents.forEachIndexed { index, itemStack ->
            if (itemStack != null) {
                val slot = when (index) {
                    in 0..8 -> type.hotBarSlots[index]
                    in 9..35 -> type.mainInvSlots[index - 9]
                    else -> -1
                }
                if (slot > 0) {
                    contents[slot] = itemStack
                }
            }
        }
    }
}
