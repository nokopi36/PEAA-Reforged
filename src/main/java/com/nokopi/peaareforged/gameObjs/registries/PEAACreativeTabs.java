package com.nokopi.peaareforged.gameObjs.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nokopi.peaareforged.PEAACore;

/**
 * PEAA's own creative tab.
 *
 * <p>Deviation D-001: the 1.7.10 mod put everything into ProjectE's tab ({@code ObjHandler.cTab}).
 */
public class PEAACreativeTabs {

	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PEAACore.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
			CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup." + PEAACore.MODID))
					.icon(() -> PEAAItems.COLLECTOR_MK5.toStack())
					.displayItems((parameters, output) -> {
						output.accept(PEAAItems.COLLECTOR_MK4);
						output.accept(PEAAItems.COLLECTOR_MK5);
						output.accept(PEAAItems.AEGU_MK1);
						output.accept(PEAAItems.AEGU_MK2);
						output.accept(PEAAItems.AEGU_MK3);
						output.accept(PEAAItems.RING_OF_THE_SPACE);
					})
					.build());

	private PEAACreativeTabs() {
	}
}
