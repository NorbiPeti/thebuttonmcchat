package buttondevteam.chat;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import buttondevteam.lib.chat.*;
import buttondevteam.lib.player.EnumPlayerData;
import buttondevteam.lib.player.PlayerClass;
import buttondevteam.lib.player.PlayerData;
import buttondevteam.lib.player.TBMCPlayerBase;

@PlayerClass(pluginname = "Button1Chat")
public class ChatPlayer extends TBMCPlayerBase {
	public PlayerData<String> UserName() {
		return data(null);
	}

	public List<String> UserNames() {
		return data(new ArrayList<String>()).get();
	}

	public PlayerData<Integer> FlairTime() {
		return data(FlairTimeNone);
	}

	public EnumPlayerData<FlairStates> FlairState() {
		return dataEnum(FlairStates.class, FlairStates.NoComment);
	}

	public PlayerData<Integer> FCount() {
		return data(0);
	}

	public PlayerData<Integer> FDeaths() {
		return data(0);
	}

	public PlayerData<Boolean> FlairCheater() {
		return data(false);
	}

	public PlayerData<ArrayList<Integer>> NameColorLocations() { // No byte[], no TIntArrayList
		return data(null);
	}

	public Location SavedLocation;
	public boolean Working;
	// public int Tables = 10;
	public Channel CurrentChannel = Channel.GlobalChat;
	public boolean SendingLink = false;
	public boolean RainbowPresserColorMode = false;
	public Color OtherColorMode = null;
	public boolean ChatOnly = false;
	public int LoginWarningCount = 0;

	public static final int FlairTimeNonPresser = -1;
	public static final int FlairTimeCantPress = -2;
	public static final int FlairTimeNone = -3;

	/**
	 * Gets the player's flair, optionally formatting for Minecraft.
	 * 
	 * @param noformats
	 *            The MC formatting codes will be only applied if false
	 * @return The flair
	 */
	public String GetFormattedFlair(boolean noformats) {
		int time = FlairTime().get();
		if (time == FlairTimeCantPress)
			return String.format(noformats ? "(can't press)" : "§r(--s)§r");
		if (time == FlairTimeNonPresser)
			return String.format(noformats ? "(non-presser)" : "§7(--s)§r");
		if (time == FlairTimeNone)
			return "";
		return noformats ? String.format("(%ds)", time) : String.format("§%x(%ds)§r", GetFlairColor(), time);
	}

	/**
	 * Gets the player's flair, formatted for Minecraft.
	 * 
	 * @return The flair
	 */
	public String GetFormattedFlair() {
		return GetFormattedFlair(false);
	}

	public void SetFlair(int time) {
		FlairTime().set(time);
		FlairUpdate();
	}

	public void SetFlair(int time, boolean cheater) {
		FlairTime().set(time);
		FlairCheater().set(cheater);
		FlairUpdate();
	}

	public void FlairUpdate() {

		// Flairs from Command Block The Button - Teams
		// PluginMain.Instance.getServer().getScoreboardManager().getMainScoreboard().getTeams().add()
		Player p = Bukkit.getPlayer(uuid);
		if (p != null)
			p.setPlayerListName(String.format("%s%s", p.getName(), GetFormattedFlair()));
	}

	public short GetFlairColor() {
		if (FlairCheater().get())
			return 0x5;
		final int flairTime = FlairTime().get();
		if (flairTime == FlairTimeNonPresser)
			return 0x7;
		else if (flairTime == FlairTimeCantPress)
			return 0xf;
		else if (flairTime <= 60 && flairTime >= 52)
			return 0x5;
		else if (flairTime <= 51 && flairTime >= 42)
			return 0x9;
		else if (flairTime <= 41 && flairTime >= 32)
			return 0xa;
		else if (flairTime <= 31 && flairTime >= 22)
			return 0xe;
		else if (flairTime <= 21 && flairTime >= 11)
			return 0x6;
		else if (flairTime <= 11 && flairTime >= 0)
			return 0xc;
		return 0xf;
	}
}
