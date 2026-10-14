package com.specarmed;

import com.google.inject.Provides;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
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

	private final Map<SpecArmedConfig.SoundChoice, byte[]> builtInSounds = new EnumMap<>(SpecArmedConfig.SoundChoice.class);

	// Only one clip at a time, so previews don't stack
	private Clip currentClip;

	@Inject
	private ConfigManager configManager;

	// Opening an audio line can block for a noticeable moment on Windows, so all sound work
	// runs here instead of on the client or settings-panel thread
	private ScheduledExecutorService soundExecutor;

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
		overlayManager.add(widgetOverlay);
		overlayManager.add(playerOverlay);
		soundExecutor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, "spec-armed-sound");
			t.setDaemon(true);
			return t;
		});
		// Load the sound and open the audio system once now, so the first spec isn't the slow one
		soundExecutor.execute(this::warmUp);
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
		soundExecutor.shutdownNow();
		soundExecutor = null;
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
			soundExecutor.execute(this::playSound);
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
		if (soundExecutor == null)
		{
			return;
		}
		if (pendingPreview != null)
		{
			pendingPreview.cancel(false);
		}
		pendingPreview = soundExecutor.schedule(this::playSound, 250, TimeUnit.MILLISECONDS);
	}

	private void warmUp()
	{
		loadSound();
		try
		{
			AudioSystem.getClip().close();
		}
		catch (Exception e)
		{
			log.debug("Spec Armed: audio warm-up failed", e);
		}
	}

	// Only called on soundExecutor
	private void playSound()
	{
		byte[] data = loadSound();
		if (data == null)
		{
			return;
		}

		if (currentClip != null)
		{
			currentClip.stop();
			currentClip.close();
			currentClip = null;
		}

		try
		{
			AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
			Clip clip = AudioSystem.getClip();
			currentClip = clip;
			clip.open(stream);
			clip.addLineListener(e ->
			{
				if (e.getType() == LineEvent.Type.STOP)
				{
					clip.close();
				}
			});

			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			{
				FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float volume = Math.max(config.volume(), 1) / 100f;
				float db = (float) (20 * Math.log10(volume));
				gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
			}

			clip.start();
		}
		catch (Exception e)
		{
			log.warn("Spec Armed: unable to play sound", e);
		}
	}

	private byte[] loadSound()
	{
		SpecArmedConfig.SoundChoice choice = config.sound();
		byte[] cached = builtInSounds.get(choice);
		if (cached != null)
		{
			return cached;
		}

		try (InputStream in = SpecArmedPlugin.class.getResourceAsStream(choice.getResource()))
		{
			if (in == null)
			{
				log.warn("Spec Armed: built-in sound {} missing", choice.getResource());
				return null;
			}
			byte[] data = in.readAllBytes();
			builtInSounds.put(choice, data);
			return data;
		}
		catch (IOException e)
		{
			log.warn("Spec Armed: unable to read built-in sound", e);
			return null;
		}
	}
}
