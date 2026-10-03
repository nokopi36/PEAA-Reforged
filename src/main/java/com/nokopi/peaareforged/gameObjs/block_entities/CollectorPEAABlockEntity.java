package com.nokopi.peaareforged.gameObjs.block_entities;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.block_entity.BaseEmcBlockEntity;
import moze_intel.projecte.api.block_entity.IRelay;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.gameObjs.block_entities.WrappedItemHandler;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.nokopi.peaareforged.gameObjs.EnumCollectorTierPEAA;
import com.nokopi.peaareforged.gameObjs.blocks.AEGUBlock;
import com.nokopi.peaareforged.gameObjs.blocks.CollectorPEAABlock;
import com.nokopi.peaareforged.gameObjs.container.CollectorPEAAContainer;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlockEntityTypes;

/**
 * Energy Collector MK4 / MK5 (SPEC 3.1).
 *
 * <p>Deviation D-005: this is a re-implementation rather than a subclass of ProjectE's
 * {@code CollectorMK1BlockEntity}, which cannot be extended for three reasons:
 * <ol>
 *   <li>its generation rate and storage come only from {@code EnumCollectorTier}, which has no MK4/MK5
 *       and is not a NeoForge extensible enum;</li>
 *   <li>{@code emcGen} is {@code private final};</li>
 *   <li>the generation formula calls the <em>static</em> {@code getSunLevel(Level, BlockPos)}, so the
 *       override the 1.7.10 version relied on (com.nokopi.peaareforged.gameObjs.tiles.CollectorMK4Tile:32-39) is
 *       impossible here.</li>
 * </ol>
 * The behaviour below otherwise mirrors ProjectE's {@code CollectorMK1BlockEntity} so that fuel
 * upgrading, Klein Star charging, EMC hand-off and relay bonuses stay identical.
 */
public class CollectorPEAABlockEntity extends BaseEmcBlockEntity implements MenuProvider {

	public static final int UPGRADING_SLOT = 0;
	public static final int UPGRADE_SLOT = 1;
	public static final int LOCK_SLOT = 2;
	private static final int AUX_SLOTS = 3;

	/**
	 * ProjectE divides the sun level by this and multiplies by the per-second generation rate, so at
	 * the maximum sun level of 16 a collector produces {@code genRate / 20} EMC per tick -- i.e.
	 * exactly {@code genRate} EMC per second. See CollectorMK1BlockEntity:193.
	 */
	private static final float SUN_LEVEL_DIVISOR = 320.0F;
	private static final int MAX_SUN_LEVEL = 16;

	/**
	 * Same split as ProjectE: the top and bottom faces expose the aux slots (extract-only, and only the
	 * upgrade result), every other face exposes the fuel storage (insert-only, collector-valid items).
	 */
	public static final ICapabilityProvider<CollectorPEAABlockEntity, @Nullable Direction, IItemHandler> INVENTORY_PROVIDER = (collector, side) -> {
		if (side == null) {
			return collector.joined;
		} else if (side.getAxis().isVertical()) {
			return collector.automationAuxSlots;
		}
		return collector.automationInput;
	};

	private final EnumCollectorTierPEAA tier;
	private final long emcGen;

	private final ItemStackHandler input;
	private final ItemStackHandler auxSlots;
	private final CombinedInvWrapper toSort;
	private final IItemHandlerModifiable automationInput;
	private final IItemHandlerModifiable automationAuxSlots;
	private final IItemHandler joined;

	private double unprocessedEMC;
	private boolean hasChargeableItem;
	private boolean hasFuel;
	/** Start as needing a compaction pass so a freshly loaded inventory gets tidied once. */
	private boolean needsCompacting = true;
	private int lastComparatorSignal = -1;

	public CollectorPEAABlockEntity(BlockPos pos, BlockState state) {
		super(PEAABlockEntityTypes.COLLECTOR.get(), pos, state);
		// One block entity type serves both tiers; the tier comes from whichever block we belong to.
		this.tier = state.getBlock() instanceof CollectorPEAABlock block ? block.getTier() : EnumCollectorTierPEAA.MK4;
		this.emcGen = tier.getGenRate();
		setMaximumEMC(tier.getStorage());

		this.input = new NotifyingStackHandler(tier.getInvSize()) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				needsCompacting = true;
			}
		};
		this.auxSlots = new NotifyingStackHandler(AUX_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				if (slot == UPGRADING_SLOT) {
					needsCompacting = true;
				}
			}
		};
		this.toSort = new CombinedInvWrapper(new RangedWrapper(auxSlots, UPGRADING_SLOT, UPGRADING_SLOT + 1), input);
		this.automationInput = new WrappedItemHandler(input, WrappedItemHandler.WriteMode.IN) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return SlotPredicates.COLLECTOR_INV.test(stack) ? super.insertItem(slot, stack, simulate) : stack;
			}
		};
		this.automationAuxSlots = new WrappedItemHandler(auxSlots, WrappedItemHandler.WriteMode.OUT) {
			@NotNull
			@Override
			public ItemStack extractItem(int slot, int count, boolean simulate) {
				return slot == UPGRADE_SLOT ? super.extractItem(slot, count, simulate) : ItemStack.EMPTY;
			}
		};
		this.joined = new CombinedInvWrapper(automationInput, automationAuxSlots);
	}

	public EnumCollectorTierPEAA getTier() {
		return tier;
	}

	public IItemHandler getInput() {
		return input;
	}

	public IItemHandler getAux() {
		return auxSlots;
	}

	@Override
	protected boolean canAcceptEmc() {
		// Accept EMC from providers while there is something to spend it on, otherwise hand it onwards.
		return hasFuel || hasChargeableItem;
	}

	// --- sun level (SPEC 3.1.2) ------------------------------------------------------------------

	/**
	 * MK4/MK5 ignore light entirely: they run at full rate when any AEGU sits directly above, and stop
	 * otherwise (com.nokopi.peaareforged.gameObjs.tiles.CollectorMK4Tile:32-39).
	 *
	 * <p>Per decision 16 the AEGU's own running state is deliberately not checked, matching the original.
	 */
	public static int getSunLevel(@NotNull Level level, @NotNull BlockPos pos) {
		return level.getBlockState(pos.above()).getBlock() instanceof AEGUBlock ? MAX_SUN_LEVEL : 0;
	}

	public int getSunLevel() {
		return level == null ? 0 : getSunLevel(level, worldPosition);
	}

	// --- ticking --------------------------------------------------------------------------------

	public static void tickServer(Level level, BlockPos pos, BlockState state, CollectorPEAABlockEntity collector) {
		if (collector.needsCompacting) {
			ItemHelper.compactInventory(collector.toSort);
			collector.needsCompacting = false;
		}
		collector.checkFuelOrKlein();
		collector.updateEmc(level, pos);
		collector.rotateUpgraded();
		collector.updateComparators(level, pos);
	}

	private void checkFuelOrKlein() {
		ItemStack upgrading = getUpgrading();
		if (upgrading.isEmpty()) {
			hasFuel = false;
			hasChargeableItem = false;
			return;
		}
		IItemEmcHolder emcHolder = upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
		if (emcHolder != null) {
			if (emcHolder.getNeededEmc(upgrading) > 0) {
				hasChargeableItem = true;
				hasFuel = false;
			} else {
				hasChargeableItem = false;
			}
		} else {
			hasFuel = FuelMapper.isStackFuel(upgrading);
			hasChargeableItem = false;
		}
	}

	private void updateEmc(@NotNull Level level, @NotNull BlockPos pos) {
		if (!hasMaxedEmc()) {
			unprocessedEMC += emcGen * (getSunLevel(level, pos) / SUN_LEVEL_DIVISOR);
			if (unprocessedEMC >= 1) {
				// Force it in: generated EMC is ours regardless of whether we accept EMC from neighbours.
				unprocessedEMC -= forceInsertEmc((long) unprocessedEMC, IEmcStorage.EmcAction.EXECUTE);
			}
			setChanged();
		}
		if (getStoredEmc() <= 0) {
			return;
		}
		ItemStack upgrading = getUpgrading();
		if (hasChargeableItem) {
			IItemEmcHolder emcHolder = upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
			if (emcHolder != null) {
				long inserted = emcHolder.insertEmc(upgrading, Math.min(getStoredEmc(), emcGen), IEmcStorage.EmcAction.EXECUTE);
				forceExtractEmc(inserted, IEmcStorage.EmcAction.EXECUTE);
			}
			return;
		} else if (hasFuel) {
			ItemStack fuelUpgrade = FuelMapper.getFuelUpgrade(upgrading);
			if (!fuelUpgrade.isEmpty()) {
				ItemStack lock = getLock();
				ItemStack result = lock.isEmpty() ? fuelUpgrade : lock.copy();
				long upgradeCost = IEMCProxy.INSTANCE.getValue(result) - IEMCProxy.INSTANCE.getValue(upgrading);
				if (upgradeCost >= 0 && getStoredEmc() >= upgradeCost) {
					ItemStack upgrade = getUpgraded();
					if (upgrade.isEmpty()) {
						forceExtractEmc(upgradeCost, IEmcStorage.EmcAction.EXECUTE);
						auxSlots.setStackInSlot(UPGRADE_SLOT, result);
						upgrading.shrink(1);
					} else if (result.is(upgrade.getItem()) && upgrade.getCount() < upgrade.getMaxStackSize()) {
						forceExtractEmc(upgradeCost, IEmcStorage.EmcAction.EXECUTE);
						upgrade.grow(1);
						upgrading.shrink(1);
						auxSlots.setStackInSlot(UPGRADE_SLOT, upgrade);
					}
				}
				return;
			}
		}
		// Only pass EMC on when we are not upgrading fuel or charging an item.
		sendToAllAcceptors(level, pos, Math.min(getStoredEmc(), emcGen));
		sendRelayBonus(level, pos);
	}

	private void rotateUpgraded() {
		ItemStack upgraded = getUpgraded();
		if (upgraded.isEmpty()) {
			return;
		}
		ItemStack lock = getLock();
		if (lock.isEmpty() || upgraded.getItem() != lock.getItem() || upgraded.getCount() >= upgraded.getMaxStackSize()) {
			auxSlots.setStackInSlot(UPGRADE_SLOT, ItemHandlerHelper.insertItemStacked(input, upgraded.copy(), false));
		}
	}

	/**
	 * Splits the given EMC evenly between adjacent acceptors. Re-implementation of the same method on
	 * ProjectE's internal {@code EmcBlockEntity}, using only the public capability API.
	 */
	private void sendToAllAcceptors(@NotNull Level level, @NotNull BlockPos pos, long emc) {
		if (emc <= 0) {
			return;
		}
		List<IEmcStorage> targets = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			IEmcStorage theirStorage = level.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, pos.relative(dir), dir.getOpposite());
			// A collector is never a relay, so the relay-to-relay thrashing guard ProjectE has is not needed.
			if (theirStorage != null && theirStorage.insertEmc(1, IEmcStorage.EmcAction.SIMULATE) > 0) {
				targets.add(theirStorage);
			}
		}
		if (targets.isEmpty()) {
			return;
		}
		long emcPer = emc / targets.size();
		for (IEmcStorage target : targets) {
			long canProvide = extractEmc(emcPer, IEmcStorage.EmcAction.SIMULATE);
			long accepted = target.insertEmc(canProvide, IEmcStorage.EmcAction.EXECUTE);
			extractEmc(accepted, IEmcStorage.EmcAction.EXECUTE);
		}
	}

	private static void sendRelayBonus(@NotNull Level level, @NotNull BlockPos pos) {
		for (Direction dir : Direction.values()) {
			BlockPos relayPos = pos.relative(dir);
			BlockEntity blockEntity = level.getBlockEntity(relayPos);
			if (blockEntity instanceof IRelay relay) {
				relay.addBonus(level, relayPos);
			}
		}
	}

	// --- comparators ----------------------------------------------------------------------------

	private void updateComparators(@NotNull Level level, @NotNull BlockPos pos) {
		int signal = getComparatorSignal();
		if (signal != lastComparatorSignal) {
			lastComparatorSignal = signal;
			level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
		}
	}

	/** Mirrors ProjectE's {@code Collector#getAnalogOutputSignal}. */
	public int getComparatorSignal() {
		ItemStack charging = getUpgrading();
		if (charging.isEmpty()) {
			return MathUtils.scaleToRedstone(getStoredEmc(), getMaximumEmc());
		}
		IItemEmcHolder emcHolder = charging.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
		if (emcHolder != null) {
			return MathUtils.scaleToRedstone(emcHolder.getStoredEmc(charging), emcHolder.getMaximumEmc(charging));
		}
		long goal = getEmcToNextGoal();
		return goal == 0 ? 0 : MathUtils.scaleToRedstone(getStoredEmc(), goal);
	}

	// --- values the GUI shows (SPEC 3.1.6) ------------------------------------------------------

	/** EMC still needed to finish the current fuel upgrade. */
	public long getEmcToNextGoal() {
		ItemStack lock = getLock();
		ItemStack upgrading = getUpgrading();
		long targetEmc = lock.isEmpty() ? IEMCProxy.INSTANCE.getValue(FuelMapper.getFuelUpgrade(upgrading)) : IEMCProxy.INSTANCE.getValue(lock);
		return Math.max(targetEmc - IEMCProxy.INSTANCE.getValue(upgrading), 0);
	}

	/** EMC stored in the item being charged, or -1 when nothing chargeable is in the slot. */
	public long getItemCharge() {
		ItemStack upgrading = getUpgrading();
		IItemEmcHolder emcHolder = upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
		return emcHolder == null ? -1 : emcHolder.getStoredEmc(upgrading);
	}

	public double getItemChargeProportion() {
		long charge = getItemCharge();
		if (charge <= 0) {
			return -1;
		}
		ItemStack upgrading = getUpgrading();
		IItemEmcHolder emcHolder = upgrading.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
		if (emcHolder == null) {
			return -1;
		}
		long max = emcHolder.getMaximumEmc(upgrading);
		return charge >= max ? 1 : (double) charge / max;
	}

	public double getFuelProgress() {
		ItemStack upgrading = getUpgrading();
		if (!FuelMapper.isStackFuel(upgrading)) {
			return 0;
		}
		long reqEmc;
		ItemStack lock = getLock();
		if (lock.isEmpty()) {
			ItemStack fuelUpgrade = FuelMapper.getFuelUpgrade(upgrading);
			if (fuelUpgrade.isEmpty()) {
				return 0;
			}
			reqEmc = IEMCProxy.INSTANCE.getValue(fuelUpgrade) - IEMCProxy.INSTANCE.getValue(upgrading);
		} else {
			reqEmc = IEMCProxy.INSTANCE.getValue(lock) - IEMCProxy.INSTANCE.getValue(upgrading);
			if (reqEmc < 0) {
				return 0;
			}
		}
		if (reqEmc <= 0) {
			return 0;
		}
		return getStoredEmc() >= reqEmc ? 1 : (double) getStoredEmc() / reqEmc;
	}

	// --- slot access ----------------------------------------------------------------------------

	private ItemStack getUpgrading() {
		return auxSlots.getStackInSlot(UPGRADING_SLOT);
	}

	private ItemStack getUpgraded() {
		return auxSlots.getStackInSlot(UPGRADE_SLOT);
	}

	private ItemStack getLock() {
		return auxSlots.getStackInSlot(LOCK_SLOT);
	}

	public void clearLocked() {
		auxSlots.setStackInSlot(LOCK_SLOT, ItemStack.EMPTY);
	}

	// --- persistence & sync ---------------------------------------------------------------------

	@Override
	public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		unprocessedEMC = tag.getDouble("unprocessed_emc");
		input.deserializeNBT(registries, tag.getCompound("input"));
		auxSlots.deserializeNBT(registries, tag.getCompound("aux_slots"));
	}

	@Override
	protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putDouble("unprocessed_emc", unprocessedEMC);
		tag.put("input", input.serializeNBT(registries));
		tag.put("aux_slots", auxSlots.serializeNBT(registries));
	}

	@NotNull
	@Override
	public CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	// --- menu -----------------------------------------------------------------------------------

	@NotNull
	@Override
	public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
		return new CollectorPEAAContainer(windowId, playerInventory, this);
	}

	@NotNull
	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	/** {@link ItemStackHandler} that keeps the block entity marked dirty. */
	private class NotifyingStackHandler extends ItemStackHandler {

		NotifyingStackHandler(int size) {
			super(size);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			setChanged();
		}
	}
}
