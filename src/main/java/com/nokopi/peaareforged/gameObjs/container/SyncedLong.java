package com.nokopi.peaareforged.gameObjs.container;

import java.util.function.Consumer;
import net.minecraft.world.inventory.DataSlot;

/**
 * Syncs a {@code long} through vanilla {@link DataSlot}s.
 *
 * <p>A {@link DataSlot} stores an int but only the low 16 bits survive the trip to the client
 * (refs/neoforge-docs/versioned_docs/version-1.21.1/gui/menus.md:115), so a long is carried as four
 * 16-bit chunks. Values like the Klein Star charge (up to 51,200,000) need this.
 */
public class SyncedLong {

	private static final int PARTS = 4;
	private static final int MASK = 0xFFFF;

	private final DataSlot[] slots = new DataSlot[PARTS];

	public SyncedLong() {
		for (int i = 0; i < PARTS; i++) {
			slots[i] = DataSlot.standalone();
		}
	}

	/** Registers the backing slots with the menu. Call once, from the menu constructor. */
	public void addTo(Consumer<DataSlot> adder) {
		for (DataSlot slot : slots) {
			adder.accept(slot);
		}
	}

	public void set(long value) {
		for (int i = 0; i < PARTS; i++) {
			slots[i].set((int) ((value >>> (16 * i)) & MASK));
		}
	}

	public long get() {
		long value = 0;
		for (int i = 0; i < PARTS; i++) {
			value |= (long) (slots[i].get() & MASK) << (16 * i);
		}
		return value;
	}
}
