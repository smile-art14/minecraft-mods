package com.example.client;

import com.example.ExampleMod;
import com.example.item.ExcaliburItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public final class ExcaliburChargeHud {
	private static final int BAR_WIDTH = 46;
	private static final int BAR_HEIGHT = 4;
	private static final int BORDER_COLOR = 0xFF17130B;
	private static final int EMPTY_COLOR = 0xFF302A1C;
	private static final int GOLD_COLOR = 0xFFE5AC21;
	private static final int GOLD_HIGHLIGHT_COLOR = 0xFFFFE58A;

	private ExcaliburChargeHud() {
	}

	public static void initialize() {
		HudRenderCallback.EVENT.register(ExcaliburChargeHud::render);
	}

	private static void render(GuiGraphics graphics, float tickDelta) {
		Player player = Minecraft.getInstance().player;
		if (player == null
				|| !player.isUsingItem()
				|| !player.getUseItem().is(ExampleMod.EXCALIBUR)) {
			return;
		}

		float progress = ExcaliburItem.getChargeProgress(player.getTicksUsingItem());
		int filledWidth = Math.round(BAR_WIDTH * progress);
		int x = (graphics.guiWidth() - BAR_WIDTH) / 2;
		int y = graphics.guiHeight() / 2 + 18;

		graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BORDER_COLOR);
		graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, EMPTY_COLOR);
		if (filledWidth > 0) {
			graphics.fill(x, y, x + filledWidth, y + BAR_HEIGHT, GOLD_COLOR);
			graphics.fill(x, y, x + filledWidth, y + 1, GOLD_HIGHLIGHT_COLOR);
		}
	}
}
