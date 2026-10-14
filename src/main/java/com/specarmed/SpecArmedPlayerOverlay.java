package com.specarmed;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class SpecArmedPlayerOverlay extends Overlay
{
	private final Client client;
	private final SpecArmedPlugin plugin;
	private final SpecArmedConfig config;

	@Inject
	SpecArmedPlayerOverlay(Client client, SpecArmedPlugin plugin, SpecArmedConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isSpecOn() || !config.showPlayerText())
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		String text = config.playerText();
		if (player == null || text == null || text.isEmpty())
		{
			return null;
		}

		graphics.setFont(FontManager.getRunescapeBoldFont());
		Point location = player.getCanvasTextLocation(graphics, text, player.getLogicalHeight() + config.textHeight());
		if (location != null)
		{
			OverlayUtil.renderTextLocation(graphics, location, text, config.textColor());
		}
		return null;
	}
}
