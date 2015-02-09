package ca.carbogen.korra.lavaflux;

import com.projectkorra.ProjectKorra.ProjectKorra;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;

/**
 * Created by Carbogen on 2/8/2015.
 */
public class LFListener
		implements Listener
{
	public static void manage()
	{
		Runnable br = new Runnable()
		{
			public void run()
			{
				// 3x Speed
				for (int i = 0; i < 3; i++)
				{
					LavaFlux.progressAll();
				}
			}
		};

		ProjectKorra.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(ProjectKorra.plugin, br, 0, 1);
	}

	@EventHandler
	public void onPlayerSwing(PlayerAnimationEvent e)
	{
		new LavaFlux(e.getPlayer());
	}
}
