package com.nokopi.peaareforged.gameObjs.container;

import moze_intel.projecte.gameObjs.container.slots.ISlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotGhost;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import com.nokopi.peaareforged.gameObjs.block_entities.CollectorPEAABlockEntity;
import com.nokopi.peaareforged.gameObjs.registries.PEAAMenuTypes;

/**
 * Menu for Energy Collector MK4 / MK5.
 *
 * <p>Slot layout is SPEC 3.1.5, which is identical down to the pixel to ProjectE's
 * {@code CollectorMK3Container} (refs/projecte/.../container/CollectorMK3Container.java:23-35).
 * ProjectE's own container cannot be reused because it is typed against {@code CollectorMK1BlockEntity}.
 *
 * <p>Values shown in the GUI are pushed into standalone {@link DataSlot}s from
 * {@link #broadcastChanges()} on the server; the client reads the same slots back.
 */
public class CollectorPEAAContainer extends AbstractContainerMenu {

	/** Progress bars are synced as a 0..8000 integer, matching ProjectE's precision. */
	private static final int PROGRESS_SCALE = 8_000;

	private final CollectorPEAABlockEntity collector;

	private final SyncedLong emc = new SyncedLong();
	private final SyncedLong kleinEmc = new SyncedLong();
	private final DataSlot sunLevel = DataSlot.standalone();
	private final DataSlot kleinChargeProgress = DataSlot.standalone();
	private final DataSlot fuelProgress = DataSlot.standalone();

	/** Client-side factory: the block entity is looked up from the position written by {@code openMenu}. */
	public CollectorPEAAContainer(int windowId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
		this(windowId, playerInv, readCollector(playerInv, buf.readBlockPos()));
	}

	public CollectorPEAAContainer(int windowId, Inventory playerInv, CollectorPEAABlockEntity collector) {
		super(PEAAMenuTypes.COLLECTOR.get(), windowId);
		this.collector = collector;

		emc.addTo(this::addDataSlot);
		addDataSlot(sunLevel);
		addDataSlot(kleinChargeProgress);
		addDataSlot(fuelProgress);
		kleinEmc.addTo(this::addDataSlot);

		IItemHandler aux = collector.getAux();
		IItemHandler main = collector.getInput();

		// Klein Star / fuel input slot
		addSlot(new ValidatedSlot(aux, CollectorPEAABlockEntity.UPGRADING_SLOT, 158, 58, SlotPredicates.COLLECTOR_INV));
		// Fuel storage, 4x4
		int counter = 0;
		for (int i = 3; i >= 0; i--) {
			for (int j = 3; j >= 0; j--) {
				addSlot(new ValidatedSlot(main, counter++, 18 + i * 18, 8 + j * 18, SlotPredicates.COLLECTOR_INV));
			}
		}
		// Upgrade result (take-only)
		addSlot(new ValidatedSlot(aux, CollectorPEAABlockEntity.UPGRADE_SLOT, 158, 13, SlotPredicates.ALWAYS_FALSE));
		// Upgrade target (ghost)
		addSlot(new SlotGhost(aux, CollectorPEAABlockEntity.LOCK_SLOT, 187, 36, SlotPredicates.COLLECTOR_LOCK));
		addPlayerInventory(playerInv, 30, 84);
	}

	private static CollectorPEAABlockEntity readCollector(Inventory playerInv, BlockPos pos) {
		if (playerInv.player.level().getBlockEntity(pos) instanceof CollectorPEAABlockEntity collector) {
			return collector;
		}
		throw new IllegalStateException("No PEAA collector at " + pos);
	}

	private void addPlayerInventory(Inventory playerInv, int xStart, int yStart) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlot(new Slot(playerInv, j + i * 9 + 9, xStart + j * 18, yStart + i * 18));
			}
		}
		for (int i = 0; i < Inventory.getSelectionSize(); i++) {
			addSlot(new Slot(playerInv, i, xStart + i * 18, yStart + 58));
		}
	}

	@Override
	public void broadcastChanges() {
		if (!collector.getLevel().isClientSide) {
			emc.set(collector.getStoredEmc());
			kleinEmc.set(collector.getItemCharge());
			sunLevel.set(collector.getSunLevel());
			kleinChargeProgress.set((int) (collector.getItemChargeProportion() * PROGRESS_SCALE));
			fuelProgress.set((int) (collector.getFuelProgress() * PROGRESS_SCALE));
		}
		super.broadcastChanges();
	}

	@Override
	public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
		Slot slot = slotId >= 0 && slotId < slots.size() ? getSlot(slotId) : null;
		// Clicking a filled ghost slot clears it instead of performing a normal click.
		if (!(slot instanceof ISlotGhost ghost) || !ghost.tryClear()) {
			super.clicked(slotId, button, clickType, player);
		}
	}

	@NotNull
	@Override
	public ItemStack quickMoveStack(@NotNull Player player, int slotId) {
		Slot slot = getSlot(slotId);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int playerStart = collectorSlotCount();
		if (slotId < playerStart) {
			// Out of the collector, into the player.
			if (!moveItemStackTo(stack, playerStart, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else {
			// Into the collector, but only things the fuel storage actually accepts.
			if (!SlotPredicates.COLLECTOR_INV.test(stack) || !moveItemStackTo(stack, 1, 17, false)) {
				return ItemStack.EMPTY;
			}
		}
		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		slot.onTake(player, stack);
		return original;
	}

	/** Number of slots belonging to the collector itself (input + fuel storage + result + lock). */
	private int collectorSlotCount() {
		int count = 0;
		for (Slot slot : slots) {
			if (slot instanceof SlotItemHandler) {
				count++;
			}
		}
		return count;
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return Container.stillValidBlockEntity(collector, player);
	}

	public CollectorPEAABlockEntity getCollector() {
		return collector;
	}

	public long getEmc() {
		return emc.get();
	}

	public long getKleinEmc() {
		return kleinEmc.get();
	}

	public int getSunLevel() {
		return sunLevel.get();
	}

	public double getKleinChargeProgress() {
		return kleinChargeProgress.get() / (double) PROGRESS_SCALE;
	}

	public double getFuelProgress() {
		return fuelProgress.get() / (double) PROGRESS_SCALE;
	}
}
