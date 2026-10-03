package com.nokopi.peaareforged.gameObjs.block_entities;

import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.gameObjs.blocks.CondenserMK2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.nokopi.peaareforged.gameObjs.EnumAEGUTier;
import com.nokopi.peaareforged.gameObjs.blocks.AEGUBlock;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlockEntityTypes;
import com.nokopi.peaareforged.util.InventoryPush;

/**
 * Drives an AEGU group (SPEC 3.2.3 to 3.2.7 and 3.3.2).
 *
 * <p>The original kept the group state on a replaced Energy Condenser MK2 block entity
 * (com.nokopi.peaareforged.gameObjs.tiles.CondenserMK2TilePEAA). ProjectE's block entity cannot be replaced on 1.21.1,
 * so the state lives here instead: every AEGU works out on its own whether it belongs to a valid
 * group and contributes its own share of EMC. The sum across the group equals the original's
 * {@code generateEmc} total.
 *
 * <p>ProjectE's condenser only accepts EMC once a target item has been set
 * ({@code CondenserBlockEntity#canAcceptEmc}), whereas the original bypassed that check by writing
 * to the condenser's buffer directly. To keep EMC from being lost in the meantime, undelivered EMC
 * is held in {@link #buffer} and pushed as soon as the condenser will take it (deviation D-008).
 */
public class AEGUBlockEntity extends BlockEntity {

	/** SPEC 3.3.2 / decision 15: at least 25 of the 26 positions around the condenser must be AEGUs. */
	public static final int REQUIRED_AEGU_COUNT = 25;

	/**
	 * Safety-net rescan interval. Placing or breaking a block only notifies its six direct
	 * neighbours, so an AEGU sitting diagonally from the change would otherwise never hear about it.
	 * {@link AEGUBlock} also pokes the whole shell on placement and removal, so this only covers
	 * cases those miss.
	 */
	private static final int RESCAN_INTERVAL = 20;

	private static final int TICKS_PER_SECOND = 20;

	private final EnumAEGUTier tier;

	@Nullable
	private BlockPos linkedCondenser;
	private boolean groupActive;
	/** Only one AEGU per group moves the condenser's output, to avoid 25 redundant passes per tick. */
	private boolean primary;

	private long buffer;
	private double unprocessed;

	private boolean needsRescan = true;
	private int rescanCooldown;

	public AEGUBlockEntity(BlockPos pos, BlockState state) {
		super(PEAABlockEntityTypes.AEGU.get(), pos, state);
		this.tier = state.getBlock() instanceof AEGUBlock block ? block.getTier() : EnumAEGUTier.MK1;
	}

	public EnumAEGUTier getTier() {
		return tier;
	}

	/** The condenser this AEGU currently feeds, or null when it is not part of a running group. */
	@Nullable
	public BlockPos getActiveCondenser() {
		return groupActive ? linkedCondenser : null;
	}

	public boolean isGroupActive() {
		return groupActive;
	}

	/** EMC produced but not yet handed to the condenser. */
	public long getBuffer() {
		return buffer;
	}

	/** Forces the group to be re-evaluated on the next tick. */
	public void markNeedsRescan() {
		needsRescan = true;
	}

	public static void tickServer(Level level, BlockPos pos, BlockState state, AEGUBlockEntity aegu) {
		if (aegu.needsRescan || --aegu.rescanCooldown <= 0) {
			aegu.rescan(level, pos);
		}
		aegu.syncGeneratingState(level, pos, state);
		if (!aegu.groupActive || aegu.linkedCondenser == null) {
			return;
		}
		aegu.produce();
		aegu.deliver(level);
		if (aegu.primary) {
			aegu.pushCondenserOutput(level, aegu.linkedCondenser);
		}
	}

	// --- group detection ------------------------------------------------------------------------

	/**
	 * Mirrors the original's two checks: an AEGU registers with a condenser only when exactly one sits
	 * in its own 3x3x3 (com.nokopi.peaareforged.gameObjs.blocks.AEGU:142-169 refused to register when it found several),
	 * and the group runs only once enough AEGUs surround that condenser
	 * (com.nokopi.peaareforged.gameObjs.tiles.CondenserMK2TilePEAA:389-403).
	 */
	private void rescan(Level level, BlockPos pos) {
		needsRescan = false;
		rescanCooldown = RESCAN_INTERVAL;
		linkedCondenser = null;
		groupActive = false;
		primary = false;

		BlockPos found = null;
		int condensers = 0;
		for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
			if (candidate.equals(pos)) {
				continue;
			}
			if (level.getBlockState(candidate).getBlock() instanceof CondenserMK2) {
				condensers++;
				found = candidate.immutable();
			}
		}
		if (condensers != 1) {
			return;
		}
		linkedCondenser = found;

		int aeguCount = 0;
		long lowest = Long.MAX_VALUE;
		for (BlockPos candidate : BlockPos.betweenClosed(found.offset(-1, -1, -1), found.offset(1, 1, 1))) {
			if (candidate.equals(found)) {
				continue;
			}
			if (level.getBlockState(candidate).getBlock() instanceof AEGUBlock) {
				aeguCount++;
				lowest = Math.min(lowest, candidate.asLong());
			}
		}
		groupActive = aeguCount >= REQUIRED_AEGU_COUNT;
		primary = groupActive && pos.asLong() == lowest;
	}

	private void syncGeneratingState(Level level, BlockPos pos, BlockState state) {
		if (state.getValue(AEGUBlock.GENERATING) != groupActive) {
			// Same block, only a property changes, so this block entity survives the update.
			level.setBlock(pos, state.setValue(AEGUBlock.GENERATING, groupActive), Block.UPDATE_ALL);
		}
	}

	// --- EMC ------------------------------------------------------------------------------------

	private void produce() {
		unprocessed += tier.getGenRate() / (double) TICKS_PER_SECOND;
		long whole = (long) unprocessed;
		if (whole > 0) {
			unprocessed -= whole;
			buffer += whole;
			setChanged();
		}
	}

	private void deliver(@NotNull Level level) {
		if (buffer <= 0 || linkedCondenser == null) {
			return;
		}
		IEmcStorage storage = level.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, linkedCondenser, null);
		if (storage == null) {
			return;
		}
		long inserted = storage.insertEmc(buffer, IEmcStorage.EmcAction.EXECUTE);
		if (inserted > 0) {
			buffer -= inserted;
			setChanged();
		}
	}

	// --- moving the condenser's output out (SPEC 3.3.4) -----------------------------------------

	/**
	 * Empties the condenser's output slots into neighbouring inventories.
	 *
	 * <p>Like the original (com.nokopi.peaareforged.gameObjs.tiles.CondenserMK2TilePEAA:199-304) this skips the upward
	 * direction and refuses to feed Anti-Matter Relays and matter furnaces. At most one slot is moved
	 * per tick, which is far more than the condenser can produce, and keeps the per-tick cost flat.
	 */
	private void pushCondenserOutput(@NotNull Level level, @NotNull BlockPos condenserPos) {
		// The condenser only allows extraction from its output half, so its input slots are passed over.
		IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, condenserPos, null);
		if (source == null) {
			return;
		}
		for (Direction dir : Direction.values()) {
			if (dir == Direction.UP) {
				continue;
			}
			BlockPos targetPos = condenserPos.relative(dir);
			if (InventoryPush.isRefusedTarget(level, targetPos)) {
				continue;
			}
			IItemHandler destination = InventoryPush.neighbourInventory(level, targetPos, dir.getOpposite());
			if (destination != null && InventoryPush.moveOneStack(source, destination)) {
				return;
			}
		}
	}

	// --- persistence ----------------------------------------------------------------------------

	@Override
	public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		buffer = tag.getLong("buffer");
		unprocessed = tag.getDouble("unprocessed_emc");
		// The link and group state are derived, so rebuild them instead of storing them.
		needsRescan = true;
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong("buffer", buffer);
		tag.putDouble("unprocessed_emc", unprocessed);
	}
}
