package com.nokopi.peaareforged.gameObjs.registries;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.nokopi.peaareforged.PEAACore;
import com.nokopi.peaareforged.gameObjs.container.CollectorPEAAContainer;

/**
 * Menu (container) types added by PEAA. Replaces the 1.7.10 {@code IGuiHandler} and its numeric GUI
 * ids (peaa.utils.ConstantsPEAA COLLECTOR4_GUI / COLLECTOR5_GUI).
 *
 * <p>Modelled on ProjectE's {@code PEContainerTypes}, which uses the same
 * {@link IContainerFactory} + block position pattern for block entity menus.
 */
public class PEAAMenuTypes {

	public static final DeferredRegister<MenuType<?>> MENU_TYPES =
			DeferredRegister.create(Registries.MENU, PEAACore.MODID);

	public static final DeferredHolder<MenuType<?>, MenuType<CollectorPEAAContainer>> COLLECTOR =
			MENU_TYPES.register("collector", () -> new MenuType<>(
					(IContainerFactory<CollectorPEAAContainer>) CollectorPEAAContainer::new, FeatureFlags.VANILLA_SET));

	private PEAAMenuTypes() {
	}
}
