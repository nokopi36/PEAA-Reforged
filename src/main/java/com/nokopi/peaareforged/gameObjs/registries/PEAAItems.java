package com.nokopi.peaareforged.gameObjs.registries;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.items.RingOfTheSpace;

/**
 * All items added by PEAA, including the {@code BlockItem}s for {@link PEAABlocks}.
 *
 * Modelled on ProjectE's {@code PEItems}.
 */
public class PEAAItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PEAACore.MODID);

	public static final DeferredItem<BlockItem> COLLECTOR_MK4 = ITEMS.registerSimpleBlockItem(PEAABlocks.COLLECTOR_MK4);
	public static final DeferredItem<BlockItem> COLLECTOR_MK5 = ITEMS.registerSimpleBlockItem(PEAABlocks.COLLECTOR_MK5);
	public static final DeferredItem<BlockItem> AEGU_MK1 = ITEMS.registerSimpleBlockItem(PEAABlocks.AEGU_MK1);
	public static final DeferredItem<BlockItem> AEGU_MK2 = ITEMS.registerSimpleBlockItem(PEAABlocks.AEGU_MK2);
	public static final DeferredItem<BlockItem> AEGU_MK3 = ITEMS.registerSimpleBlockItem(PEAABlocks.AEGU_MK3);

	public static final DeferredItem<RingOfTheSpace> RING_OF_THE_SPACE =
			ITEMS.registerItem("ring_of_the_space", RingOfTheSpace::new, new Item.Properties().fireResistant());

	private PEAAItems() {
	}
}
