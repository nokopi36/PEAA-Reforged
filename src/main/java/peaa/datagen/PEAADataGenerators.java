package peaa.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import peaa.PEAACore;

/** Entry point for {@code ./gradlew runData}. */
@EventBusSubscriber(modid = PEAACore.MODID)
public class PEAADataGenerators {

	@SubscribeEvent
	static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();
		ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		generator.addProvider(event.includeClient(), new PEAABlockStateProvider(output, existingFileHelper));
		generator.addProvider(event.includeClient(), new PEAAItemModelProvider(output, existingFileHelper));
		generator.addProvider(event.includeClient(), new PEAALangProvider(output, "en_us"));
		generator.addProvider(event.includeClient(), new PEAALangProvider(output, "ja_jp"));
		generator.addProvider(event.includeServer(), new PEAARecipeProvider(output, lookupProvider));
		generator.addProvider(event.includeServer(), new PEAALootTableProvider(output, lookupProvider));
	}

	private PEAADataGenerators() {
	}
}
