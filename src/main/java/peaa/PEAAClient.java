package peaa;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import peaa.client.gui.CollectorPEAAScreen;
import peaa.gameObjs.items.RingOfTheSpace;
import peaa.gameObjs.registries.PEAAItems;
import peaa.gameObjs.registries.PEAAMenuTypes;

/**
 * Client-only entry point. This class is not loaded on dedicated servers, so client-only code
 * (screens, key mappings, particles) is safe to reference from here.
 *
 * <p>Replaces the 1.7.10 {@code peaa.proxies.ClientProxy} / {@code CommonProxy} pair.
 */
@Mod(value = PEAACore.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = PEAACore.MODID, value = Dist.CLIENT)
public class PEAAClient {

	public PEAAClient(ModContainer container) {
	}

	@SubscribeEvent
	static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(PEAAMenuTypes.COLLECTOR.get(), CollectorPEAAScreen::new);
	}

	@SubscribeEvent
	static void clientSetup(FMLClientSetupEvent event) {
		// Swaps the ring's texture while flight is on, the same way ProjectE drives the SWRG's models.
		event.enqueueWork(() -> ItemProperties.register(PEAAItems.RING_OF_THE_SPACE.get(), PEAACore.rl("flying"),
				(stack, level, entity, seed) -> RingOfTheSpace.isFlying(stack) ? 1 : 0));
	}
}
