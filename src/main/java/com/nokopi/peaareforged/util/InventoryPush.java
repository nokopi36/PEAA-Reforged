package com.nokopi.peaareforged.util;

import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.blocks.MatterFurnace;
import moze_intel.projecte.gameObjs.blocks.Relay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Moving items out of a block into whatever is next to it, the way PEAA did it.
 *
 * <p>Two places need this: the Energy Condenser MK2 driven by an AEGU group (SPEC 3.3.4) and the
 * matter furnaces (SPEC 3.4.6). The original implemented it twice with the same rules, so the rules
 * live here once: never upwards, and never into an Anti-Matter Relay or another matter furnace.
 */
public final class InventoryPush {

	private InventoryPush() {
	}

	/**
	 * Whether the original would refuse to push into the block at this position.
	 *
	 * <p>Relays and matter furnaces are skipped (com.nokopi.peaareforged.gameObjs.tiles.CondenserMK2TilePEAA:222-226).
	 * Relays would swallow the items as fuel and furnaces would bounce their output back and forth.
	 */
	public static boolean isRefusedTarget(@NotNull Level level, @NotNull BlockPos pos) {
		Block block = level.getBlockState(pos).getBlock();
		return block instanceof Relay || block instanceof MatterFurnace;
	}

	/** The inventory exposed to {@code side} by the block at {@code pos}, or null if it has none. */
	@Nullable
	public static IItemHandler neighbourInventory(@NotNull Level level, @NotNull BlockPos pos, @NotNull Direction side) {
		return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
	}

	public static boolean isEmpty(@NotNull IItemHandler handler) {
		for (int slot = 0, slots = handler.getSlots(); slot < slots; slot++) {
			if (!handler.getStackInSlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Moves the contents of one slot, the first that will go anywhere.
	 *
	 * @return whether anything moved
	 */
	public static boolean moveOneStack(@NotNull IItemHandler source, @NotNull IItemHandler destination) {
		for (int slot = 0, slots = source.getSlots(); slot < slots; slot++) {
			if (moveSlot(source, slot, destination)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Empties every slot it can into the destination, which is what ProjectE's own downward push does
	 * (DMFurnaceBlockEntity:319-332).
	 *
	 * @return whether anything moved
	 */
	public static boolean moveEverything(@NotNull IItemHandler source, @NotNull IItemHandler destination) {
		boolean moved = false;
		for (int slot = 0, slots = source.getSlots(); slot < slots; slot++) {
			moved |= moveSlot(source, slot, destination);
		}
		return moved;
	}

	/**
	 * Simulates first so a partial fit still moves what it can, and so a slot that refuses extraction
	 * -- the condenser locks its input half that way -- is passed over without side effects.
	 */
	private static boolean moveSlot(@NotNull IItemHandler source, int slot, @NotNull IItemHandler destination) {
		ItemStack extractable = source.extractItem(slot, Integer.MAX_VALUE, true);
		if (extractable.isEmpty()) {
			return false;
		}
		ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, extractable, true);
		int movable = extractable.getCount() - remainder.getCount();
		if (movable <= 0) {
			return false;
		}
		ItemStack moved = source.extractItem(slot, movable, false);
		if (moved.isEmpty()) {
			return false;
		}
		ItemHandlerHelper.insertItemStacked(destination, moved, false);
		return true;
	}

	// --- matter furnaces (SPEC 3.4.6) -----------------------------------------------------------

	/**
	 * Pushes a matter furnace's output into its four horizontal neighbours.
	 *
	 * <p>The original pushed in every direction but up
	 * (com.nokopi.peaareforged.gameObjs.tiles.RMFurnaceTilePEAA:359-464). ProjectE already covers the downward one, and
	 * runs it just before this is called, so only the sides are left -- and downwards keeps the
	 * priority it had in the original's direction loop.
	 *
	 * <p>ProjectE skips its own push when a hopper sits below, to let the hopper pull at its own rate.
	 * That is deliberately not extended sideways: a hopper beside a furnace cannot pull from it, so
	 * skipping it would mean it never receives anything at all.
	 *
	 * <p>Returns early while the output is empty, which is the usual case, so an idle furnace does not
	 * pay for a single capability lookup.
	 */
	public static void pushFurnaceOutputSideways(@NotNull Level level, @NotNull BlockPos pos, @NotNull DMFurnaceBlockEntity furnace) {
		IItemHandler output = furnace.getOutput();
		if (isEmpty(output)) {
			return;
		}
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			BlockPos targetPos = pos.relative(dir);
			if (isRefusedTarget(level, targetPos)) {
				continue;
			}
			IItemHandler destination = neighbourInventory(level, targetPos, dir.getOpposite());
			if (destination == null) {
				continue;
			}
			moveEverything(output, destination);
			if (isEmpty(output)) {
				return;
			}
		}
	}
}
