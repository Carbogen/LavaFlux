package ca.carbogen.korra.lavaflux;

import com.projectkorra.ProjectKorra.Ability.AbilityModule;
import com.projectkorra.ProjectKorra.Element;
import com.projectkorra.ProjectKorra.ProjectKorra;
import com.projectkorra.ProjectKorra.SubElement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

/**
 * Created by Carbogen on 2/8/2015.
 */
public class LFInfo
		extends AbilityModule
{
	private Plugin pk;
	private FileConfiguration config;

	public LFInfo()
	{
		super("LavaFlux");
	}

	public String getDescription()
	{
		return getName() + " " + getVersion() + " developed by " + getAuthor() + " \n"
				+ "This offensive ability enables a Lavabender to create a wave of lava, " +
				"swiftly progressing forward and hurting/burning anything in its way. To use, " +
				"simply swing your arm towards a target and the ability will activate. ";
	}

	public String getAuthor()
	{
		return "Carbogen";
	}

	public String getVersion()
	{
		return "v2.0.0";
	}

	public String getElement()
	{
		return Element.Earth.toString();
	}

	public boolean isShiftAbility()
	{
		return true;
	}

	public boolean isHarmlessAbility()
	{
		return false;
	}

	public SubElement getSubElement()
	{
		return SubElement.Lavabending;
	}

	public void onThisLoad()
	{
		loadConfig();
		config.addDefault("ExtraAbilities.Carbogen.LavaFlux.range", 16);
		config.addDefault("ExtraAbilities.Carbogen.LavaFlux.cooldown", 8000);
		config.addDefault("ExtraAbilities.Carbogen.LavaFlux.damage", 6);
		config.addDefault("ExtraAbilities.Carbogen.LavaFlux.cleanupDelay", 6000);
		config.addDefault("ExtraAbilities.Carbogen.LavaFlux.speed", "FAST");
		config.addDefault("ExtraAbilities.Carbogen.LavaFlux.waveEnabled", true);
		pk.saveConfig();
		ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new LFListener(), ProjectKorra.plugin);
		ProjectKorra.plugin.getServer().getPluginManager().addPermission(new Permission("bending.ability.LavaFlux"));
		ProjectKorra.plugin.getServer().getPluginManager().getPermission("bending.ability.LavaFlux").setDefault(
				PermissionDefault.TRUE);
		LFListener.manage();
		ProjectKorra.log.info(getName() + " by " + getAuthor() + " has been loaded.");
	}

	public void loadConfig()
	{
		pk = ProjectKorra.plugin;
		config = pk.getConfig();
	}

	public void stop()
	{
		LavaFlux.forceRemoveAll();
	}
}
