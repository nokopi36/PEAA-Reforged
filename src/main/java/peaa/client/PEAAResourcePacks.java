package peaa.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import peaa.PEAACore;

/**
 * Applies the original PEAA's Energy Condenser MK2 texture over ProjectE's (SPEC 5.3).
 *
 * <p>The original rewrote the texture namespace inside ProjectE's renderer with ASM
 * ({@code MK2TextureTransformer}). On 1.21.1 the same result comes from shipping a replacement at
 * {@code assets/projecte/textures/block/condenser_mk2.png}. ProjectE's block model carries only a
 * particle texture and is drawn by a dedicated renderer, so that 64x64 file is a model atlas in
 * exactly the same layout as the original's -- it drops straight in.
 *
 * <p>It ships as a built-in pack rather than loose assets because two mods claiming one resource path
 * is resolved by mod load order, which is not something to rely on. Registering the pack at
 * {@link Pack.Position#TOP} and always active makes it win deterministically, and it still shows up in
 * the resource pack screen so it can be inspected.
 */
@EventBusSubscriber(modid = PEAACore.MODID, value = Dist.CLIENT)
public class PEAAResourcePacks {

	private static final String PACK_PATH = "peaa_condenser_texture";

	@SubscribeEvent
	static void addPackFinders(AddPackFindersEvent event) {
		event.addPackFinders(
				ResourceLocation.fromNamespaceAndPath(PEAACore.MODID, PACK_PATH),
				PackType.CLIENT_RESOURCES,
				Component.translatable("peaa_reforged.resourcepack.condenser_texture"),
				PackSource.BUILT_IN,
				true,
				Pack.Position.TOP);
	}

	private PEAAResourcePacks() {
	}
}
