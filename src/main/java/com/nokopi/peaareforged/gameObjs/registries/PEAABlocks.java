package com.nokopi.peaareforged.gameObjs.registries;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.EnumAEGUTier;
import com.nokopi.peaareforged.gameObjs.EnumCollectorTierPEAA;
import com.nokopi.peaareforged.gameObjs.blocks.AEGUBlock;
import com.nokopi.peaareforged.gameObjs.blocks.CollectorPEAABlock;

/**
 * All blocks added by PEAA. Modelled on ProjectE's {@code PEBlocks}.
 */
public class PEAABlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PEAACore.MODID);

	public static final DeferredBlock<CollectorPEAABlock> COLLECTOR_MK4 = registerCollector(EnumCollectorTierPEAA.MK4);
	public static final DeferredBlock<CollectorPEAABlock> COLLECTOR_MK5 = registerCollector(EnumCollectorTierPEAA.MK5);

	public static final DeferredBlock<AEGUBlock> AEGU_MK1 = registerAEGU(EnumAEGUTier.MK1);
	public static final DeferredBlock<AEGUBlock> AEGU_MK2 = registerAEGU(EnumAEGUTier.MK2);
	public static final DeferredBlock<AEGUBlock> AEGU_MK3 = registerAEGU(EnumAEGUTier.MK3);

	/**
	 * The original built on ProjectE's MK3 collector (com.nokopi.peaareforged.gameObjs.blocks.CollectorPEAA:26 passes
	 * tier 3 to {@code super}), so these use MK3's block properties with its light level of 15.
	 */
	private static DeferredBlock<CollectorPEAABlock> registerCollector(EnumCollectorTierPEAA tier) {
		return BLOCKS.register(tier.getSerializedName(), () -> new CollectorPEAABlock(tier,
				BlockBehaviour.Properties.of()
						.mapColor(MapColor.SAND)
						.instrument(NoteBlockInstrument.PLING)
						.sound(SoundType.GLASS)
						.requiresCorrectToolForDrops()
						.strength(0.3F, 0.9F)
						.lightLevel(state -> 15)));
	}

	/** SPEC 3.2.1: Material.grass, hardness 0.3, full light, glass step sound. */
	private static DeferredBlock<AEGUBlock> registerAEGU(EnumAEGUTier tier) {
		return BLOCKS.register(tier.getSerializedName(), () -> new AEGUBlock(tier,
				BlockBehaviour.Properties.of()
						.mapColor(MapColor.PLANT)
						.sound(SoundType.GLASS)
						.strength(0.3F)
						.lightLevel(state -> 15)));
	}

	static BlockItem plainItem(net.minecraft.world.level.block.Block block) {
		return new BlockItem(block, new Item.Properties());
	}

	private PEAABlocks() {
	}
}
