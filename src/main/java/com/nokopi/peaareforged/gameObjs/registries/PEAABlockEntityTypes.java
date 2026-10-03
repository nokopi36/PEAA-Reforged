package com.nokopi.peaareforged.gameObjs.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.block_entities.AEGUBlockEntity;
import com.nokopi.peaareforged.gameObjs.block_entities.CollectorPEAABlockEntity;

/**
 * Block entity types added by PEAA. Modelled on ProjectE's {@code PEBlockEntityTypes}.
 */
public class PEAABlockEntityTypes {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
			DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PEAACore.MODID);

	/**
	 * One type serves both collector tiers; the block entity reads its tier from the block it belongs to.
	 */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CollectorPEAABlockEntity>> COLLECTOR =
			BLOCK_ENTITY_TYPES.register("collector", () -> BlockEntityType.Builder.of(CollectorPEAABlockEntity::new,
					PEAABlocks.COLLECTOR_MK4.get(), PEAABlocks.COLLECTOR_MK5.get()).build(null));

	/** One type for all three AEGU tiers; the block entity reads its tier from the block. */
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AEGUBlockEntity>> AEGU =
			BLOCK_ENTITY_TYPES.register("aegu", () -> BlockEntityType.Builder.of(AEGUBlockEntity::new,
					PEAABlocks.AEGU_MK1.get(), PEAABlocks.AEGU_MK2.get(), PEAABlocks.AEGU_MK3.get()).build(null));

	private PEAABlockEntityTypes() {
	}
}
