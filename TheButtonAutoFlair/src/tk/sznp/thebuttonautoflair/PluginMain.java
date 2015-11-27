package tk.sznp.thebuttonautoflair;

import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.String;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PluginMain extends JavaPlugin { // Translated to Java: 2015.07.15.
	// A user, which flair isn't obtainable:
	// https://www.reddit.com/r/thebutton/comments/31c32v/i_pressed_the_button_without_really_thinking/
	public static PluginMain Instance;
	public static ConsoleCommandSender Console; // 2015.08.12.

	// Fired when plugin is first enabled
	@Override
	public void onEnable() {
		System.out.println("The Button Minecraft server plugin");
		getServer().getPluginManager().registerEvents(new PlayerListener(),
				this);
		Commands comm = new Commands();
		this.getCommand("u").setExecutor(comm);
		this.getCommand("u").setUsage(
				this.getCommand("u").getUsage().replace('&', '�'));
		this.getCommand("nrp").setExecutor(comm);
		this.getCommand("nrp").setUsage(
				this.getCommand("nrp").getUsage().replace('&', '�'));
		this.getCommand("ooc").setExecutor(comm);
		this.getCommand("ooc").setUsage(
				this.getCommand("ooc").getUsage().replace('&', '�'));
		Instance = this; // 2015.08.08.
		Console = this.getServer().getConsoleSender(); // 2015.08.12.
		LoadFiles(false); // 2015.08.09.
		Runnable r = new Runnable() {
			public void run() {
				ThreadMethod();
			}
		};
		Thread t = new Thread(r);
		t.start();
		r = new Runnable() {
			public void run() {
				AnnouncerThread.Run();
			}
		};
		t = new Thread(r);
		t.start();
	}

	public Boolean stop = false;

	// Fired when plugin is disabled
	@Override
	public void onDisable() {
		SaveFiles(); // 2015.08.09.
		stop = true;
	}

	public void ThreadMethod() // <-- 2015.07.16.
	{
		while (!stop) {
			try {
				String body = DownloadString("https://www.reddit.com/r/TheButtonMinecraft/comments/3d25do/autoflair_system_comment_your_minecraft_name_and/.json?limit=1000");
				JSONArray json = new JSONArray(body).getJSONObject(1)
						.getJSONObject("data").getJSONArray("children");
				for (Object obj : json) {
					JSONObject item = (JSONObject) obj;
					String author = item.getJSONObject("data").getString(
							"author");
					String ign = item.getJSONObject("data").getString("body");
					int start = ign.indexOf("IGN:") + "IGN:".length();
					if (start == -1 + "IGN:".length()) // +length: 2015.08.10.
						continue; // 2015.08.09.
					int end = ign.indexOf(' ', start);
					if (end == -1 || end == start)
						end = ign.indexOf('\n', start); // 2015.07.15.
					if (end == -1 || end == start)
						ign = ign.substring(start);
					else
						ign = ign.substring(start, end);
					ign = ign.trim();
					MaybeOfflinePlayer mp = MaybeOfflinePlayer.GetFromName(ign);
					if (mp == null)
						continue;
					if (HasIGFlair(mp.UUID))
						continue;
					try {
						Thread.sleep(10);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					String[] flairdata = DownloadString(
							"http://karmadecay.com/thebutton-data.php?users="
									+ author).replace("\"", "").split(":");
					String flair;
					if (flairdata.length > 1) // 2015.07.15.
						flair = flairdata[1];
					else
						flair = "";
					if (flair != "-1")
						flair = flair + "s";
					String flairclass;
					if (flairdata.length > 2)
						flairclass = flairdata[2];
					else
						flairclass = "unknown";
					SetFlair(mp.UUID, flair, flairclass, author);
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			} catch (Exception e) {
				// System.out.println("Error!\n" + e);
				LastException = e; // 2015.08.09.
			}
		}
	}

	public static Exception LastException; // 2015.08.09.

	public String DownloadString(String urlstr) throws MalformedURLException,
			IOException {
		URL url = new URL(urlstr);
		URLConnection con = url.openConnection();
		con.setRequestProperty("User-Agent", "TheButtonAutoFlair");
		InputStream in = con.getInputStream();
		String encoding = con.getContentEncoding();
		encoding = encoding == null ? "UTF-8" : encoding;
		String body = IOUtils.toString(in, encoding);
		in.close();
		return body;
	}

	public static Map<String, String> TownColors = new HashMap<String, String>(); // 2015.07.20.

	public Boolean HasIGFlair(UUID uuid) {
		MaybeOfflinePlayer p = MaybeOfflinePlayer.AddPlayerIfNeeded(uuid); // 2015.08.08.
		return p.CommentedOnReddit; // 2015.08.10.
	}

	public void SetFlair(UUID uuid, String text, String flairclass,
			String username) {
		MaybeOfflinePlayer p = MaybeOfflinePlayer.AddPlayerIfNeeded(uuid); // 2015.08.08.
		String finalflair;
		p.FlairDecided = true;
		p.FlairRecognised = true;
		p.CommentedOnReddit = true;
		p.UserName = username;
		switch (flairclass) {
		case "press-1":
			finalflair = "�c(" + text + ")�r";
			break;
		case "press-2":
			finalflair = "�6(" + text + ")�r";
			break;
		case "press-3":
			finalflair = "�e(" + text + ")�r";
			break;
		case "press-4":
			finalflair = "�a(" + text + ")�r";
			break;
		case "press-5":
			finalflair = "�9(" + text + ")�r";
			break;
		case "press-6":
			finalflair = "�5(" + text + ")�r";
			break;
		case "no-press":
			finalflair = "�7(--s)�r";
			break;
		case "cheater":
			finalflair = "�5(" + text + ")�r";
			break;
		case "cant-press": // 2015.08.08.
			finalflair = "�r(??s)�r";
			break;
		case "unknown":
			if (text.equals("-1")) // If true, only non-presser/can't press; if
									// false, any flair
				p.FlairDecided = false;
			else
				p.FlairRecognised = false;
			finalflair = "";
			break;
		default:
			return;
		}
		p.Flair = finalflair; // 2015.08.08.
		System.out.println("Added flair for " + p.PlayerName);
		AppendPlayerDisplayFlair(p, Bukkit.getPlayer(uuid));
	}

	public static String GetFlair(Player player) { // 2015.07.16.
		String flair = MaybeOfflinePlayer.AllPlayers.get(player.getUniqueId()).Flair; // 2015.08.08.
		return flair; // 2015.08.10.
	}

	public static void AppendPlayerDisplayFlair(MaybeOfflinePlayer player,
			Player p) // <-- 2015.08.09.
	{

		if (MaybeOfflinePlayer.AllPlayers.get(p.getUniqueId()).IgnoredFlair)
			return;
		if (MaybeOfflinePlayer.AllPlayers.get(p.getUniqueId()).AcceptedFlair) {
			if (!player.FlairDecided)
				p.sendMessage("�9Your flair type is unknown. Are you a non-presser or a can't press? (/u nonpresser or /u cantpress)�r"); // 2015.08.09.
		} else
			p.sendMessage("�9Are you Reddit user " + player.UserName
					+ "?�r �6Type /u accept or /u ignore�r");
	}

	public static String GetColorForTown(String townname) { // 2015.07.20.
		if (TownColors.containsKey(townname))
			return TownColors.get(townname);
		return "";
	}

	public static Collection<? extends Player> GetPlayers() {
		return Instance.getServer().getOnlinePlayers();
	}

	public static ArrayList<String> AnnounceMessages = new ArrayList<>();
	public static int AnnounceTime = 15 * 60 * 1000;

	public static void LoadFiles(boolean reload) // <-- 2015.08.09.
	{
		if (reload) { // 2015.08.09.
			System.out
					.println("The Button Minecraft plugin cleanup for reloading...");
			MaybeOfflinePlayer.AllPlayers.clear();
			TownColors.clear();
			AnnounceMessages.clear();
			Commands.Quiz.clear();
		}
		System.out.println("Loading files for The Button Minecraft plugin..."); // 2015.08.09.
		try {
			File file = new File("announcemessages.txt");
			if (file.exists())
				file.delete();
			file = new File("flairsaccepted.txt");
			if (file.exists())
				file.delete();
			file = new File("flairsignored.txt");
			if (file.exists())
				file.delete();
			file = new File("thebuttonmc.yml");
			if (file.exists()) {
				YamlConfiguration yc = new YamlConfiguration();
				yc.load(file);
				MaybeOfflinePlayer.Load(yc);
				PlayerListener.NotificationSound = yc
						.getString("notificationsound");
				PlayerListener.NotificationPitch = yc
						.getDouble("notificationpitch");
				AnnounceTime = yc.getInt("announcetime");
				AnnounceMessages.addAll(yc.getStringList("announcements"));
				Commands.Quiz.addAll(yc.getStringList("quiz"));
			}
			System.out.println("The Button Minecraft plugin loaded files!");
		} catch (IOException e) {
			System.out.println("Error!\n" + e);
			LastException = e; // 2015.08.09.
		} catch (InvalidConfigurationException e) {
			System.out.println("Error!\n" + e);
			LastException = e; // 2015.08.09.
		}
	}

	public static void SaveFiles() // <-- 2015.08.09.
	{
		System.out.println("Saving files for The Button Minecraft plugin..."); // 2015.08.09.
		try {
			File file = new File("thebuttonmc.yml");
			YamlConfiguration yc = new YamlConfiguration();
			MaybeOfflinePlayer.Save(yc);
			yc.set("notificationsound", PlayerListener.NotificationSound);
			yc.set("notificationpitch", PlayerListener.NotificationPitch);
			yc.set("announcetime", AnnounceTime);
			yc.set("announcements", AnnounceMessages);
			yc.set("quiz", Commands.Quiz);
			yc.save(file);
			System.out.println("The Button Minecraft plugin saved files!");
		} catch (IOException e) {
			System.out.println("Error!\n" + e);
			LastException = e; // 2015.08.09.
		}
	}
}
