package com.specarmed;

import com.google.inject.Provides;
import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.RuneLite;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Spec Armed",
	description = "Makes it obvious when your special attack is turned on",
	tags = {"spec", "special", "attack", "orb", "indicator", "highlight", "sound"}
)
public class SpecArmedPlugin extends Plugin
{
	static final File SOUND_DIR = new File(RuneLite.RUNELITE_DIR, "spec-armed");
	static final File CUSTOM_SOUND = new File(SOUND_DIR, "custom.wav");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SpecArmedConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SpecArmedWidgetOverlay widgetOverlay;

	@Inject
	private SpecArmedPlayerOverlay playerOverlay;

	@Getter
	private boolean specOn;

	// Varps get re-sent while logging in or hopping; don't play the sound until the first tick after that
	private boolean ready;

	@Inject
	private ConfigManager configManager;

	@Inject
	private AudioPlayer audioPlayer;

	// Opening an audio line can block for a noticeable moment, so sounds are played from
	// RuneLite's background executor instead of the client or settings-panel thread
	@Inject
	private ScheduledExecutorService executor;

	// Settings previews are delayed slightly so flipping through sounds only plays the last one
	private ScheduledFuture<?> pendingPreview;

	@Provides
	SpecArmedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpecArmedConfig.class);
	}

	@Override
	protected void startUp()
	{
		// Make the folder so it's easy to find when adding a custom sound
		SOUND_DIR.mkdirs();
		overlayManager.add(widgetOverlay);
		overlayManager.add(playerOverlay);
		clientThread.invoke(() ->
		{
			ready = client.getGameState() == GameState.LOGGED_IN;
			specOn = ready && client.getVarpValue(VarPlayerID.SA_ATTACK) == 1;
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(widgetOverlay);
		overlayManager.remove(playerOverlay);
		if (pendingPreview != null)
		{
			pendingPreview.cancel(false);
			pendingPreview = null;
		}
		specOn = false;
		ready = false;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() != VarPlayerID.SA_ATTACK)
		{
			return;
		}

		boolean on = event.getValue() == 1;
		if (on && !specOn && ready && config.playSound())
		{
			executor.execute(this::playSound);
		}
		specOn = on;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			ready = false;
			specOn = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		ready = true;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SpecArmedConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		switch (event.getKey())
		{
			case "preview":
				if (config.preview())
				{
					schedulePreview();
					// Untick so it works like a button
					configManager.setConfiguration(SpecArmedConfig.GROUP, "preview", false);
				}
				break;
			case "sound":
				schedulePreview();
				break;
		}
	}

	private void schedulePreview()
	{
		if (pendingPreview != null)
		{
			pendingPreview.cancel(false);
		}
		pendingPreview = executor.schedule(this::playSound, 250, TimeUnit.MILLISECONDS);
	}

	private void playSound()
	{
		// AudioPlayer takes gain in decibels; 0 dB is the file's own volume
		float gain = (float) (20 * Math.log10(Math.max(config.volume(), 1) / 100f));
		try
		{
			SpecArmedConfig.SoundChoice choice = config.sound();
			if (choice == SpecArmedConfig.SoundChoice.CUSTOM)
			{
				if (CUSTOM_SOUND.isFile())
				{
					audioPlayer.play(CUSTOM_SOUND, gain);
					return;
				}
				log.warn("Spec Armed: no custom sound at {}, playing the shotgun instead", CUSTOM_SOUND);
				choice = SpecArmedConfig.SoundChoice.SHOTGUN;
			}
			audioPlayer.play(SpecArmedPlugin.class, choice.getResource(), gain);
		}
		catch (Exception e)
		{
			log.warn("Spec Armed: unable to play sound", e);
		}
	}
}
