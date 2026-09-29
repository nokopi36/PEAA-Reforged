package peaa.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import peaa.PEAACore;
import peaa.gameObjs.registries.PEAABlocks;
import peaa.gameObjs.registries.PEAAItems;

/**
 * Display names and tooltips. English strings come from SPEC 2.1 (the original en_US.lang) and
 * SPEC 3.8 (the original's hardcoded tooltips); Japanese from the original ja_JP.lang.
 */
public class PEAALangProvider extends LanguageProvider {

	private final boolean japanese;

	public PEAALangProvider(PackOutput output, String locale) {
		super(output, PEAACore.MODID, locale);
		this.japanese = locale.equals("ja_jp");
	}

	@Override
	protected void addTranslations() {
		if (japanese) {
			add("itemGroup." + PEAACore.MODID, "PEAA");
			addBlock(PEAABlocks.COLLECTOR_MK4, "EMCコレクター MK4");
			addBlock(PEAABlocks.COLLECTOR_MK5, "EMCコレクター MK5");
			addBlock(PEAABlocks.AEGU_MK1, "錬金術的エネルギー生成装置");
			addBlock(PEAABlocks.AEGU_MK2, "発展型AEGU");
			addBlock(PEAABlocks.AEGU_MK3, "究極型AEGU");
			addItem(PEAAItems.RING_OF_THE_SPACE, "司空の指輪");
			add("peaa_reforged.tooltip.emc_gen_rate", "生成量: %s EMC/秒");
			add("peaa_reforged.tooltip.emc_max_storage", "最大貯蔵量: %s EMC");
			add("peaa_reforged.tooltip.ring_speed", "速度段階: %s / %s");
			add("peaa_reforged.tooltip.ring_flight_cost", "飛行中の消費: %s EMC/tick");
			add("peaa_reforged.tooltip.ring_charge_prompt", "%s で速度段階を変更（手に持った状態で）");
			// ProjectE の mapping config に出るマッパーの名前・説明・ボタン
			add("peaa_reforged.resourcepack.condenser_texture", "PEAA: EMCコンデンサー MK2 のテクスチャ");
			add("peaa_reforged.config.emc_mapper", "PEAA EMC マッパー");
			add("peaa_reforged.config.emc_mapper.button", "PEAA EMC マッパーを編集");
			add("peaa_reforged.config.emc_mapper.tooltip", "究極型 AEGU に EMC 値を与えます。レシピが満タンのクラインの星 Omega を要求するため ProjectE 側では導出できません。原典に合わせて既定では無効です。値は星の充填分を含まないため実際の製作コストより低くなります。正確な値にしたい場合は /projecte setemc で上書きしてください。");
		} else {
			add("itemGroup." + PEAACore.MODID, "PEAA");
			addBlock(PEAABlocks.COLLECTOR_MK4, "Energy Collector MK4");
			addBlock(PEAABlocks.COLLECTOR_MK5, "Energy Collector MK5");
			addBlock(PEAABlocks.AEGU_MK1, "Alchemical Energy Generating Unit");
			addBlock(PEAABlocks.AEGU_MK2, "Advanced AEGU");
			addBlock(PEAABlocks.AEGU_MK3, "Ultimate AEGU");
			addItem(PEAAItems.RING_OF_THE_SPACE, "Ring of the Space");
			add("peaa_reforged.tooltip.emc_gen_rate", "Generation Rate: %s EMC/s");
			add("peaa_reforged.tooltip.emc_max_storage", "Max Storage: %s EMC");
			add("peaa_reforged.tooltip.ring_speed", "Speed Setting: %s / %s");
			add("peaa_reforged.tooltip.ring_flight_cost", "Flight Cost: %s EMC/tick");
			add("peaa_reforged.tooltip.ring_charge_prompt", "Hold and press %s to change speed");
			// Name, description and button for the mapper as it appears in ProjectE's mapping config
			add("peaa_reforged.resourcepack.condenser_texture", "PEAA: Energy Condenser MK2 Texture");
			add("peaa_reforged.config.emc_mapper", "PEAA EMC Mapper");
			add("peaa_reforged.config.emc_mapper.button", "Edit PEAA EMC Mapper");
			add("peaa_reforged.config.emc_mapper.tooltip", "Gives the Ultimate AEGU an EMC value, which ProjectE cannot derive because its recipe requires a fully charged Klein Star Omega. Off by default, matching the original mod. The value ignores the star's stored EMC, so it is lower than the true crafting cost; use /projecte setemc to override it.");
		}
	}
}
