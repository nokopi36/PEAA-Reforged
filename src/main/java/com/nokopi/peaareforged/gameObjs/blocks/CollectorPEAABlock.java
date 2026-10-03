package com.nokopi.peaareforged.gameObjs.blocks;

import java.util.List;
import moze_intel.projecte.gameObjs.blocks.BlockDirection;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.nokopi.peaareforged.gameObjs.EnumCollectorTierPEAA;
import com.nokopi.peaareforged.gameObjs.block_entities.CollectorPEAABlockEntity;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlockEntityTypes;

/**
 * Energy Collector MK4 / MK5 (SPEC 3.1).
 *
 * <p>Extends ProjectE's {@link BlockDirection} so the facing property, the philosopher's stone
 * rotation and the drop-inventory-on-break behaviour match ProjectE's own collectors, which is what
 * the original got by extending {@code Collector} (com.nokopi.peaareforged.gameObjs.blocks.CollectorPEAA:16).
 *
 * <p>{@link EntityBlock} is implemented directly rather than via ProjectE's {@code PEEntityBlock},
 * because that interface is typed against ProjectE's internal registry object.
 */
public class CollectorPEAABlock extends BlockDirection implements EntityBlock {

	private final EnumCollectorTierPEAA tier;

	public CollectorPEAABlock(EnumCollectorTierPEAA tier, Properties props) {
		super(props);
		this.tier = tier;
	}

	public EnumCollectorTierPEAA getTier() {
		return tier;
	}

	@NotNull
	@Override
	@Deprecated
	protected InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player,
			@NotNull BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof CollectorPEAABlockEntity collector) {
			player.openMenu(collector, pos);
		}
		return InteractionResult.CONSUME;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return PEAABlockEntityTypes.COLLECTOR.get().create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
			@NotNull BlockEntityType<T> type) {
		if (level.isClientSide || type != PEAABlockEntityTypes.COLLECTOR.get()) {
			return null;
		}
		return (lvl, pos, st, be) -> {
			if (be instanceof CollectorPEAABlockEntity collector) {
				CollectorPEAABlockEntity.tickServer(lvl, pos, st, collector);
			}
		};
	}

	@Override
	@Deprecated
	public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
		return true;
	}

	@Override
	@Deprecated
	public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof CollectorPEAABlockEntity collector) {
			return collector.getComparatorSignal();
		}
		return super.getAnalogOutputSignal(state, level, pos);
	}

	@Override
	@Deprecated
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock() && level.getBlockEntity(pos) instanceof CollectorPEAABlockEntity collector) {
			// Clear the ghost lock slot first so super doesn't drop the item that only ever displayed a target.
			collector.clearLocked();
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip,
			@NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, tooltip, flags);
		// SPEC 3.8 -- the original hardcoded English here (com.nokopi.peaareforged.events.ToolTipEvent:29-39); this is translated instead.
		tooltip.add(Component.translatable("peaa_reforged.tooltip.emc_gen_rate", EMCHelper.formatEmc(tier.getGenRate()))
				.withStyle(ChatFormatting.DARK_PURPLE));
		tooltip.add(Component.translatable("peaa_reforged.tooltip.emc_max_storage", EMCHelper.formatEmc(tier.getStorage()))
				.withStyle(ChatFormatting.DARK_PURPLE));
	}
}
