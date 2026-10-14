package com.specarmed;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class SpecArmedWidgetOverlay extends Overlay
{
	private final Client client;
	private final SpecArmedPlugin plugin;
	private final SpecArmedConfig config;

	@Inject
	SpecArmedWidgetOverlay(Client client, SpecArmedPlugin plugin, SpecArmedConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isSpecOn())
		{
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (config.highlightOrb())
		{
			int dx = config.orbOffsetX();
			int dy = -config.orbOffsetY();
			if (config.orbArea() == SpecArmedConfig.OrbArea.WHOLE)
			{
				// The frame sprite behind both the number and the orb
				Rectangle whole = visibleBounds(InterfaceID.Orbs.SPECENERGY_BACKING);
				if (whole != null)
				{
					draw(graphics, new RoundRectangle2D.Double(whole.x + dx, whole.y + dy, whole.width, whole.height, 12, 12));
				}
			}
			else
			{
				// The round orb sprite itself (the click area sits a couple of pixels lower)
				Rectangle orb = visibleBounds(InterfaceID.Orbs.SPECENERGY_INDICATOR);
				if (orb != null)
				{
					draw(graphics, new Ellipse2D.Double(orb.x + dx, orb.y + dy, orb.width, orb.height));
				}
			}
		}

		if (config.highlightSpecBar())
		{
			Rectangle bar = visibleBounds(InterfaceID.CombatInterface.SP_ATTACKBAR);
			if (bar != null)
			{
				draw(graphics, bar);
			}
		}

		return null;
	}

	private void draw(Graphics2D graphics, Shape shape)
	{
		SpecArmedConfig.OrbStyle style = config.orbStyle();
		if (style != SpecArmedConfig.OrbStyle.RING)
		{
			graphics.setColor(config.fillColor());
			graphics.fill(shape);
		}
		if (style != SpecArmedConfig.OrbStyle.FILL)
		{
			graphics.setColor(config.ringColor());
			graphics.setStroke(new BasicStroke(config.ringThickness()));
			graphics.draw(shape);
		}
	}

	private Rectangle visibleBounds(int componentId)
	{
		Widget widget = client.getWidget(componentId);
		if (widget == null || widget.isHidden())
		{
			return null;
		}
		// Off-screen pieces report x/y of -1
		Rectangle bounds = widget.getBounds();
		return bounds.width > 0 && bounds.height > 0 && bounds.x >= 0 && bounds.y >= 0 ? bounds : null;
	}
}
