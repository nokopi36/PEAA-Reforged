package com.nokopi.peaareforged;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;
import com.nokopi.peaareforged.config.PEAAConfig;
import com.nokopi.peaareforged.gameObjs.block_entities.CollectorPEAABlockEntity;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlockEntityTypes;
import com.nokopi.peaareforged.gameObjs.registries.PEAABlocks;
import com.nokopi.peaareforged.gameObjs.registries.PEAADataComponents;
import com.nokopi.peaareforged.gameObjs.registries.PEAACreativeTabs;
import com.nokopi.peaareforged.gameObjs.registries.PEAAItems;
import com.nokopi.peaareforged.gameObjs.registries.PEAAMenuTypes;
import com.nokopi.peaareforged.gameObjs.registries.PEAARecipeConditions;

/**
 * PEAA (ProjectE Advanced Alchemy) -- NeoForge 1.21.1 re-implementation.
 *
 * <p>The 1.7.10 original used a {@code @Mod} class plus a {@code SidedProxy}; on 1.21.1 the
 * client-only half lives in {@link PEAAClient} instead.
 */
@Mod(PEAACore.MODID)
public class PEAACore {

	public static final String MODID = "peaa_reforged";
	public static final Logger LOGGER = LogUtils.getLogger();

	public PEAACore(IEventBus modEventBus, ModContainer modContainer) {
		PEAABlocks.BLOCKS.register(modEventBus);
		PEAAItems.ITEMS.register(modEventBus);
		PEAABlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
		PEAAMenuTypes.MENU_TYPES.register(modEventBus);
		PEAACreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
		PEAADataComponents.DATA_COMPONENTS.register(modEventBus);
		PEAARecipeConditions.CONDITION_CODECS.register(modEventBus);
		modEventBus.addListener(PEAACore::registerCapabilities);
		modContainer.registerConfig(ModConfig.Type.COMMON, PEAAConfig.SPEC);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PEAABlockEntityTypes.COLLECTOR.get(),
				CollectorPEAABlockEntity.INVENTORY_PROVIDER);
		// Makes ProjectE's charge keybind (default V) cycle the Ring of the Space's speed setting.
		event.registerItem(moze_intel.projecte.api.capabilities.PECapabilities.CHARGE_ITEM_CAPABILITY,
				(stack, context) -> (moze_intel.projecte.api.capabilities.item.IItemCharge) stack.getItem(),
				PEAAItems.RING_OF_THE_SPACE.get());
		// Lets ProjectE relays, condensers and other collectors exchange EMC with ours.
		event.registerBlockEntity(moze_intel.projecte.api.capabilities.PECapabilities.EMC_STORAGE_CAPABILITY,
				PEAABlockEntityTypes.COLLECTOR.get(), moze_intel.projecte.api.block_entity.BaseEmcBlockEntity.EMC_STORAGE_PROVIDER);
	}

	/**
	 * Builds a {@link ResourceLocation} in this mod's namespace.
	 * Mirrors ProjectE's {@code PECore#rl(String)} helper.
	 */
	public static ResourceLocation rl(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
