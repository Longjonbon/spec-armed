package com.specarmed;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(SpecArmedConfig.GROUP)
public interface SpecArmedConfig extends Config
{
	String GROUP = "specarmed";

	enum OrbStyle
	{
		RING,
		FILL,
		BOTH
	}

	@Getter
	@RequiredArgsConstructor
	enum OrbArea
	{
		WHOLE("Orb and number"),
		ORB_ONLY("Orb only");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum SoundChoice
	{
		SHOTGUN("Shotgun", "shotgun.wav"),
		HAMMER_COCK("Hammer cock", "hammer_cock.wav"),
		CUSTOM("Custom file", null);

		private final String name;
		private final String resource;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigSection(
		name = "Orb & spec bar",
		description = "Highlight the spec orb and combat tab spec bar while spec is on",
		position = 0
	)
	String orbSection = "orb";

	@ConfigSection(
		name = "Player text",
		description = "Text above your character while spec is on",
		position = 1
	)
	String textSection = "text";

	@ConfigSection(
		name = "Sound",
		description = "Sound played when spec is turned on",
		position = 2
	)
	String soundSection = "sound";

	@ConfigItem(
		keyName = "highlightOrb",
		name = "Highlight orb",
		description = "Highlight the special attack orb while spec is on",
		section = orbSection,
		position = 0
	)
	default boolean highlightOrb()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightSpecBar",
		name = "Highlight spec bar",
		description = "Highlight the spec bar in the combat tab while spec is on",
		section = orbSection,
		position = 1
	)
	default boolean highlightSpecBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "orbArea",
		name = "Highlight area",
		description = "Highlight the whole orb including the number, or only the round orb",
		section = orbSection,
		position = 2
	)
	default OrbArea orbArea()
	{
		return OrbArea.WHOLE;
	}

	@ConfigItem(
		keyName = "orbStyle",
		name = "Style",
		description = "Ring around the orb, a fill over it, or both",
		section = orbSection,
		position = 3
	)
	default OrbStyle orbStyle()
	{
		return OrbStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "ringColor",
		name = "Ring color",
		description = "Color of the ring/border",
		section = orbSection,
		position = 4
	)
	default Color ringColor()
	{
		return new Color(255, 215, 0, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "fillColor",
		name = "Fill color",
		description = "Color of the fill (keep it see-through)",
		section = orbSection,
		position = 5
	)
	default Color fillColor()
	{
		return new Color(255, 215, 0, 90);
	}

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "ringThickness",
		name = "Ring thickness",
		description = "Thickness of the ring/border in pixels",
		section = orbSection,
		position = 6
	)
	default int ringThickness()
	{
		return 3;
	}

	@Range(min = -20, max = 20)
	@ConfigItem(
		keyName = "orbOffsetY",
		name = "Nudge up/down",
		description = "Move the orb highlight up (positive) or down (negative) in pixels",
		section = orbSection,
		position = 7
	)
	default int orbOffsetY()
	{
		return 0;
	}

	@Range(min = -20, max = 20)
	@ConfigItem(
		keyName = "orbOffsetX",
		name = "Nudge left/right",
		description = "Move the orb highlight right (positive) or left (negative) in pixels",
		section = orbSection,
		position = 8
	)
	default int orbOffsetX()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "showPlayerText",
		name = "Show text",
		description = "Show text above your character while spec is on",
		section = textSection,
		position = 0
	)
	default boolean showPlayerText()
	{
		return true;
	}

	@ConfigItem(
		keyName = "playerText",
		name = "Text",
		description = "What to show above your character",
		section = textSection,
		position = 1
	)
	default String playerText()
	{
		return "Locked n' loaded";
	}

	@ConfigItem(
		keyName = "textColor",
		name = "Text color",
		description = "Color of the text",
		section = textSection,
		position = 2
	)
	default Color textColor()
	{
		return new Color(255, 215, 0);
	}

	@Range(min = -100, max = 200)
	@ConfigItem(
		keyName = "textHeight",
		name = "Height offset",
		description = "Move the text up (higher) or down (lower)",
		section = textSection,
		position = 3
	)
	default int textHeight()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "playSound",
		name = "Play sound",
		description = "Play a sound when spec is turned on",
		section = soundSection,
		position = 0
	)
	default boolean playSound()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sound",
		name = "Sound",
		description = "Which sound to play. Custom file plays .runelite/spec-armed/custom.wav",
		section = soundSection,
		position = 1
	)
	default SoundChoice sound()
	{
		return SoundChoice.SHOTGUN;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "Sound volume (0-100). Use Preview sound to hear it",
		section = soundSection,
		position = 2
	)
	default int volume()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "preview",
		name = "Preview sound",
		description = "Tick to hear the current sound at the current volume",
		section = soundSection,
		position = 3
	)
	default boolean preview()
	{
		return false;
	}
}
