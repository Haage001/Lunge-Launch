package haage.lungelaunch.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "lunge-launch")
public class LungeLaunchConfig implements ConfigData {
	@ConfigEntry.Gui.Tooltip
	@ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
	public int activationDelayMs = 0;

	@ConfigEntry.Gui.Tooltip
	public boolean crouchToPreventFlight = true;

	@ConfigEntry.Gui.Tooltip
	public boolean crouchToCancelFlight = false;
}
