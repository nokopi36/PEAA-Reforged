package peaa.client.gui;

import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import peaa.gameObjs.container.CollectorPEAAContainer;

/**
 * Screen for Energy Collector MK4 / MK5 (SPEC 3.1.6).
 *
 * <p>The texture is ProjectE's, not a copy: the file the 1.7.10 mod shipped was byte-identical to
 * ProjectE's {@code collector3.png} (deviation D-002 in docs/DEVIATIONS.md).
 *
 * <p>All coordinates are the ones SPEC 3.1.6 records. They match ProjectE's current
 * {@code AbstractCollectorScreen.MK3} exactly, except for the two text positions: SPEC says x=91
 * while ProjectE now draws at x=94. SPEC wins here; the 3px difference is cosmetic either way.
 */
public class CollectorPEAAScreen extends AbstractContainerScreen<CollectorPEAAContainer> {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("projecte", "textures/gui/collector3.png");

	private static final int TEXT_COLOR = 0x404040;
	private static final int MAX_LIGHT_BAR = 12;
	private static final int MAX_EMC_BAR = 48;
	private static final int MAX_FUEL_BAR = 24;

	public CollectorPEAAScreen(CollectorPEAAContainer container, Inventory playerInv, Component title) {
		super(container, playerInv, title);
		this.imageWidth = 218;
		this.imageHeight = 165;
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
		// The title and inventory labels are deliberately not drawn; there is no room on this texture.
		graphics.drawString(font, Long.toString(menu.getEmc()), 91, 32, TEXT_COLOR, false);
		long kleinCharge = menu.getKleinEmc();
		if (kleinCharge > 0) {
			graphics.drawString(font, EMCHelper.formatEmc(kleinCharge), 91, 44, TEXT_COLOR, false);
		}
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		// Light level bar, filling upwards.
		int progress = menu.getSunLevel() * MAX_LIGHT_BAR / 16;
		graphics.blit(TEXTURE, leftPos + 160, topPos + 49 - progress, 220, 13 - progress, 12, progress);

		// EMC storage bar.
		long max = menu.getCollector().getMaximumEmc();
		int emcBar = max == 0 ? 0 : (int) (menu.getEmc() * MAX_EMC_BAR / max);
		graphics.blit(TEXTURE, leftPos + 98, topPos + 18, 0, 166, emcBar, 10);

		// Klein Star charge bar.
		progress = (int) (menu.getKleinChargeProgress() * MAX_EMC_BAR);
		graphics.blit(TEXTURE, leftPos + 98, topPos + 58, 0, 166, progress, 10);

		// Fuel upgrade progress, filling upwards.
		progress = (int) (menu.getFuelProgress() * MAX_FUEL_BAR);
		graphics.blit(TEXTURE, leftPos + 172, topPos + 55 - progress, 219, 38 - progress, 10, progress + 1);
	}
}
