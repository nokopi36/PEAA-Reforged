package peaa.gameObjs.blocks;

import java.util.List;
import moze_intel.projecte.gameObjs.blocks.CondenserMK2;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import peaa.gameObjs.EnumAEGUTier;
import peaa.gameObjs.block_entities.AEGUBlockEntity;
import peaa.gameObjs.registries.PEAABlockEntityTypes;

/**
 * Alchemical Energy Generating Unit (SPEC 3.2).
 *
 * <p>An AEGU does nothing on its own. It produces EMC only once at least 25 of them surround an
 * Energy Condenser MK2, and it also acts as the "power source" that an Energy Collector MK4/MK5
 * looks for directly above itself (SPEC 3.1.2). The group logic lives in {@link AEGUBlockEntity}.
 *
 * <p>Deviation D-004: the original registered a separate block per running state
 * ({@code AEGU MK1} / {@code AEGU MK1_on}, peaa.gameObjs.ObjHandlerPEAA:39-44). Here the running
 * state is the {@link #GENERATING} blockstate property instead. The original already dropped the
 * stopped variant in every case (peaa.gameObjs.blocks.AEGU:126-137), so this is one item either way,
 * and keeping the same block means the block entity survives the switch.
 */
public class AEGUBlock extends Block implements EntityBlock {

	/** Whether this AEGU is currently running. Corresponds to the original's {@code _on} block variants. */
	public static final BooleanProperty GENERATING = BooleanProperty.create("generating");

	private final EnumAEGUTier tier;

	public AEGUBlock(EnumAEGUTier tier, Properties props) {
		super(props);
		this.tier = tier;
		registerDefaultState(stateDefinition.any().setValue(GENERATING, false));
	}

	@Override
	protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(GENERATING);
	}

	public EnumAEGUTier getTier() {
		return tier;
	}

	public static boolean isGenerating(BlockState state) {
		return state.getBlock() instanceof AEGUBlock && state.getValue(GENERATING);
	}

	// --- block entity ---------------------------------------------------------------------------

	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return PEAABlockEntityTypes.AEGU.get().create(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state,
			@NotNull BlockEntityType<T> type) {
		if (level.isClientSide || type != PEAABlockEntityTypes.AEGU.get()) {
			return null;
		}
		return (lvl, pos, st, be) -> {
			if (be instanceof AEGUBlockEntity aegu) {
				AEGUBlockEntity.tickServer(lvl, pos, st, aegu);
			}
		};
	}

	// --- group upkeep ---------------------------------------------------------------------------

	@Override
	@Deprecated
	public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState,
			boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.isClientSide && !oldState.is(this)) {
			pokeGroup(level, pos);
		}
	}

	@Override
	@Deprecated
	public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState,
			boolean isMoving) {
		boolean removed = !newState.is(this);
		super.onRemove(state, level, pos, newState, isMoving);
		if (!level.isClientSide && removed) {
			pokeGroup(level, pos);
		}
	}

	@Override
	@Deprecated
	public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block,
			@NotNull BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof AEGUBlockEntity aegu) {
			aegu.markNeedsRescan();
		}
	}

	/**
	 * Tells every AEGU sharing this position's condenser to re-evaluate.
	 *
	 * <p>Vanilla only notifies the six direct neighbours of a change, which would leave the twenty
	 * AEGUs sitting diagonally unaware that the group just became valid or invalid.
	 */
	private static void pokeGroup(Level level, BlockPos pos) {
		for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
			if (!(level.getBlockState(candidate).getBlock() instanceof CondenserMK2)) {
				continue;
			}
			for (BlockPos member : BlockPos.betweenClosed(candidate.offset(-1, -1, -1), candidate.offset(1, 1, 1))) {
				if (level.getBlockEntity(member) instanceof AEGUBlockEntity aegu) {
					aegu.markNeedsRescan();
				}
			}
			return;
		}
	}

	// --- interaction ----------------------------------------------------------------------------

	/**
	 * SPEC 3.2.5: right-clicking a running AEGU opens the GUI of the condenser it feeds
	 * (peaa.gameObjs.blocks.AEGU:217-236).
	 */
	@NotNull
	@Override
	@Deprecated
	protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
			@NotNull Player player, @NotNull BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof AEGUBlockEntity aegu) {
			BlockPos condenser = aegu.getActiveCondenser();
			if (condenser != null && level.getBlockEntity(condenser) instanceof MenuProvider provider) {
				player.openMenu(provider, condenser);
				return InteractionResult.CONSUME;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip,
			@NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, tooltip, flags);
		// SPEC 3.8 -- the original hardcoded English here (peaa.events.ToolTipEvent:44-57); this is translated instead.
		tooltip.add(Component.translatable("peaa_reforged.tooltip.emc_gen_rate", EMCHelper.formatEmc(tier.getGenRate()))
				.withStyle(ChatFormatting.DARK_PURPLE));
	}
}
