package tk.sznp.thebuttonautoflair;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.earth2me.essentials.Mob;
import com.earth2me.essentials.Mob.MobException;

import au.com.mineauz.minigames.MinigamePlayer;
import au.com.mineauz.minigames.Minigames;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class Commands implements CommandExecutor {

	public static boolean PluginUpdated = false; // 2015.08.31.

	// This method is called, when somebody uses our command
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			switch (cmd.getName()) {
			case "u": {
				if (args.length < 1)
					return false;
				MaybeOfflinePlayer p = MaybeOfflinePlayer.AllPlayers.get(player
						.getUniqueId()); // 2015.08.08.
				if (!p.CommentedOnReddit
						&& !args[0].toLowerCase().equals("admin")
						&& !args[0].toLowerCase().equals("ignore")) {
					player.sendMessage("�cError: You need to write your username to the reddit thread at /r/TheButtonMinecraft�r");
					return true;
				}
				if (!p.FlairRecognised
						&& !args[0].toLowerCase().equals("admin")
						&& !args[0].toLowerCase().equals("ignore")) {
					player.sendMessage("Sorry, but your flair isn't recorded. Please ask a mod to set it for you.");
					return true;
				}
				if (!p.FlairDecided && !args[0].toLowerCase().equals("admin")
						&& !args[0].toLowerCase().equals("ignore")) {
					player.sendMessage("Please select between /u nonpresser and /u cantpress");
					return true;
				}
				switch (args[0].toLowerCase()) // toLowerCase: 2015.08.09.
				{
				case "accept": {
					if (p.IgnoredFlair)
						p.IgnoredFlair = false; // 2015.08.08.
					if (!p.AcceptedFlair) {
						String flair = p.Flair; // 2015.08.08.
						p.AcceptedFlair = true; // 2015.08.08.
						PluginMain.AppendPlayerDisplayFlair(p, player);
						player.sendMessage("�bYour flair has been set:�r "
								+ flair);
					} else
						player.sendMessage("�cYou already have this user's flair.�r");
					break;
				}
				case "ignore": {
					if (p.AcceptedFlair)
						p.AcceptedFlair = false; // 2015.08.08.
					if (!p.IgnoredFlair) {
						p.IgnoredFlair = true;
						player.sendMessage("�bYou have ignored this request. You can still use /u accept though.�r");
					} else
						player.sendMessage("�cYou already ignored this request.�r");
					break;
				}
				case "admin": // 2015.08.09.
					DoAdmin(player, args);
					break;
				case "nonpresser": // 2015.08.09.
					if (!p.AcceptedFlair) {
						player.sendMessage("�cYou need to accept the flair first.�r");
						break;
					}
					if (p.FlairDecided) {
						player.sendMessage("�cYou have already set the flair type.�r");
						break;
					}
					SetPlayerFlair(player, p, "�7(--s)�r");
					break;
				case "cantpress": // 2015.08.09.
					if (!p.AcceptedFlair) {
						player.sendMessage("�cYou need to accept the flair first.�r");
						break;
					}
					if (p.FlairDecided) {
						player.sendMessage("�cYou have already set the flair type or your flair type is known.�r");
						break;
					}
					SetPlayerFlair(player, p, "�r(??s)�r");
					break;
				case "opme": // 2015.08.10.
					player.sendMessage("It would be nice, isn't it?"); // Sometimes
																		// I'm
																		// bored
																		// too
					break;
				case "announce":
					DoAnnounce(player, args, null);
					break;
				case "name":
					if (args.length == 1) {
						player.sendMessage("�cUsage: /u name <playername>�r");
						break;
					}
					if (!MaybeOfflinePlayer.AllPlayers.containsKey(args[1])) {
						player.sendMessage("�cUnknown user: " + args[1]);
						break;
					}
					player.sendMessage("�bUsername of "
							+ args[1]
							+ ": "
							+ MaybeOfflinePlayer.AllPlayers.get(args[1]).UserName);
					break;
				case "enable":
					if (player.getName().equals("NorbiPeti")) {
						PlayerListener.Enable = true;
						player.sendMessage("Enabled.");
					} else
						player.sendMessage("Unknown command: " + cmd.getName());
					break;
				case "disable":
					if (player.getName().equals("NorbiPeti")) {
						PlayerListener.Enable = false;
						player.sendMessage("Disabled.");
					} else
						player.sendMessage("Unknown command: " + cmd.getName());
					break;
				case "quiz":
					DoQuiz(player, args, null);
					break;
				case "kittycannon":
					DoKittyCannon(player, args);
					break;
				default:
					return false;
				}
				return true;
			}
			case "nrp":
			case "ooc":
				if (args.length == 0) {
					return false;
				} else {
					MaybeOfflinePlayer.AddPlayerIfNeeded(player.getUniqueId()).RPMode = false;
					String message = "";
					for (String arg : args)
						message += arg + " ";
					player.chat(message.substring(0, message.length() - 1));
					MaybeOfflinePlayer.AddPlayerIfNeeded(player.getUniqueId()).RPMode = true;
				}
				return true;
			default:
				player.sendMessage("Unknown command: " + cmd.getName());
				break;
			}
		} else if (args.length > 0 && args[0].toLowerCase().equals("admin")) // 2015.08.09.
		{
			DoAdmin(null, args); // 2015.08.09.
			return true; // 2015.08.09.
		} else if (args.length > 0 && args[0].toLowerCase().equals("announce")) {
			if (sender instanceof BlockCommandSender)
				DoAnnounce(null, args, (BlockCommandSender) sender);
			else
				DoAnnounce(null, args, null);
			return true;
		} else if (args.length > 0 && args[0].toLowerCase().equals("quiz")) {
			if (sender instanceof BlockCommandSender)
				DoQuiz(null, args, (BlockCommandSender) sender);
			else
				DoQuiz(null, args, null);
			return true;
		}
		return false;
	}

	private static void DoReload(Player player) { // 2015.07.20.
		try {
			PluginMain.Console
					.sendMessage("�6-- Reloading The Button Minecraft plugin...�r");
			PluginMain.LoadFiles(true); // 2015.08.09.
			for (Player p : PluginMain.GetPlayers()) {
				MaybeOfflinePlayer mp = MaybeOfflinePlayer.AddPlayerIfNeeded(p
						.getUniqueId());
				// if(mp.Flair!=null)
				if (mp.CommentedOnReddit) {
					PluginMain.AppendPlayerDisplayFlair(mp, p); // 2015.08.09.
				}
				String msg = "�bNote: The auto-flair plugin has been reloaded. You might need to wait 10s to have your flair.�r"; // 2015.08.09.
				p.sendMessage(msg); // 2015.08.09.
			}
			PluginMain.Console.sendMessage("�6-- Reloading done!�r");
		} catch (Exception e) {
			System.out.println("Error!\n" + e);
			if (player != null)
				player.sendMessage("�cAn error occured. See console for details.�r");
			PluginMain.LastException = e; // 2015.08.09.
		}
	}

	private static Player ReloadPlayer; // 2015.08.09.

	private static String DoAdminUsage = "�cUsage: /u admin reload|playerinfo|getlasterror|save|setflair|updateplugin|togglerpshow|toggledebug|savepos|loadpos�r";

	private static void DoAdmin(Player player, String[] args) { // 2015.08.09.
		if (player == null || player.isOp()
				|| player.getName().equals("NorbiPeti")) {
			if (args.length == 1) {
				String message = DoAdminUsage;
				SendMessage(player, message);
				return;
			}
			// args[0] is "admin"
			switch (args[1].toLowerCase()) {
			case "reload":
				ReloadPlayer = player; // 2015.08.09.
				SendMessage(
						player,
						"�bMake sure to save the current settings before you modify and reload them! Type /u admin confirm when done.�r");
				break;
			case "playerinfo":
				DoPlayerInfo(player, args);
				break;
			case "getlasterror":
				DoGetLastError(player, args);
				break; // <-- 2015.08.10.
			case "confirm":
				if (ReloadPlayer == player)
					DoReload(player); // 2015.08.09.
				else
					SendMessage(player,
							"�cYou need to do /u admin reload first.�r");
				break;
			case "save":
				PluginMain.SaveFiles(); // 2015.08.09.
				SendMessage(player,
						"�bSaved files. Now you can edit them and reload if you want.�r");
				break;
			case "setflair":
				DoSetFlair(player, args);
				break;
			case "updateplugin": // 2015.08.10.
				DoUpdatePlugin(player);
				break;
			case "togglerpshow":
				PlayerListener.ShowRPTag = !PlayerListener.ShowRPTag;
				SendMessage(player, "RP tag showing "
						+ (PlayerListener.ShowRPTag ? "enabled" : "disabled"));
				break;
			case "toggledebug":
				PlayerListener.DebugMode = !PlayerListener.DebugMode;
				SendMessage(player, "DebugMode: " + PlayerListener.DebugMode);
				break;
			case "savepos":
				DoSaveLoadPos(player, args);
				break;
			case "loadpos":
				DoSaveLoadPos(player, args);
				break;
			case "updatedynmap":
				DoUpdateDynmap(player, args);
			default:
				String message = DoAdminUsage;
				SendMessage(player, message);
				return;
			}
		} else
			player.sendMessage("�cYou need to be OP to use this command.�r");
	}

	private static void DoPlayerInfo(Player player, String[] args) { // 2015.08.09.
		// args[0] is "admin" - args[1] is "playerinfo"
		if (args.length == 2) {
			String message = "�cUsage: /u admin playerinfo <player>�r";
			SendMessage(player, message);
			return;
		}
		MaybeOfflinePlayer p = MaybeOfflinePlayer.GetFromName(args[2]);
		if (p == null) {
			String message = "�cPlayer not found: " + args[2] + "�r";
			SendMessage(player, message);
			return;
		}
		SendMessage(player, "Player name: " + p.PlayerName);
		SendMessage(player, "User flair: " + p.Flair);
		SendMessage(player, "Username: " + p.UserName);
		SendMessage(player, "Flair accepted: " + p.AcceptedFlair);
		SendMessage(player, "Flair ignored: " + p.IgnoredFlair);
		SendMessage(player, "Flair decided: " + p.FlairDecided);
		SendMessage(player, "Flair recognised: " + p.FlairRecognised);
		SendMessage(player, "Commented on Reddit: " + p.CommentedOnReddit);
	}

	private static void SendMessage(Player player, String message) { // 2015.08.09.
		if (player == null)
			PluginMain.Console.sendMessage(message); // 2015.08.12.
		else
			player.sendMessage(message);
	}

	private static void DoGetLastError(Player player, String[] args) { // 2015.08.09.
		// args[0] is "admin" - args[1] is "getlasterror"
		if (PluginMain.LastException != null) {
			SendMessage(player, "Last error:");
			SendMessage(player, PluginMain.LastException.toString());
			PluginMain.LastException = null;
		} else
			SendMessage(player, "There were no exceptions.");
	}

	private static void SetPlayerFlair(Player player,
			MaybeOfflinePlayer targetplayer, String flair) { // 2015.08.09.
		flair = flair.replace('&', '�');
		targetplayer.Flair = flair;
		targetplayer.CommentedOnReddit = true; // Or at least has a flair in
												// some way
		targetplayer.FlairRecognised = true;
		SendMessage(player, "�bThe flair has been set. Player: "
				+ targetplayer.PlayerName + " Flair: " + flair + "�r");
	}

	private static void DoSetFlair(Player player, String[] args) {
		// args[0] is "admin" - args[1] is "setflair"
		if (args.length < 4) {
			SendMessage(player,
					"�cUsage: /u admin setflair <playername> <flair>");
			return;
		}
		if (args[3].charAt(0) != '&') {
			SendMessage(player,
					"�cYou need to start the flair with a color code: &6(19s)&r");
			return;
		}
		Player p = Bukkit.getPlayer(args[2]);
		if (p == null) {
			SendMessage(player, "�cPLayer not found.&r");
			return;
		}
		SetPlayerFlair(player,
				MaybeOfflinePlayer.AddPlayerIfNeeded(p.getUniqueId()), args[3]);
	}

	private static void DoUpdatePlugin(Player player) { // 2015.08.10.
		SendMessage(player, "Updating Auto-Flair plugin...");
		System.out.println("Forced updating of Auto-Flair plugin.");
		URL url;
		try {
			url = new URL(
					"https://github.com/NorbiPeti/thebuttonautoflairmc/raw/master/TheButtonAutoFlair.jar");
			FileUtils.copyURLToFile(url, new File(
					"plugins/TheButtonAutoFlair.jar"));
			PluginUpdated = true; // 2015.08.31.
			SendMessage(player, "Updating done!");
		} catch (MalformedURLException e) {
			System.out.println("Error!\n" + e);
			PluginMain.LastException = e; // 2015.08.09.
		} catch (IOException e) {
			System.out.println("Error!\n" + e);
			PluginMain.LastException = e; // 2015.08.09.
		}
	}

	private static void DoAnnounce(Player player, String[] args,
			BlockCommandSender commandblock) {
		if (player == null || player.isOp()
				|| player.getName().equals("NorbiPeti")) {
			if (args.length == 1) {
				String message = "�cUsage: /u announce add|remove|settime|list|edit�r";
				SendMessage(player, message);
				return;
			}
			switch (args[1].toLowerCase()) {
			case "add":
				if (args.length < 3) {
					SendMessage(player, "�cUsage: /u announce add <message>");
					return;
				}
				StringBuilder sb = new StringBuilder();
				for (int i = 2; i < args.length; i++) {
					sb.append(args[i]);
					if (i != args.length - 1)
						sb.append(" ");
				}
				String finalmessage = sb.toString().replace('&', '�');
				PluginMain.AnnounceMessages.add(finalmessage);
				SendMessage(player, "�bAnnouncement added.�r");
				break;
			case "remove":
				if (args.length < 3) {
					SendMessage(player, "�cUsage: /u announce remove <index>");
					return;
				}
				PluginMain.AnnounceMessages.remove(Integer.parseInt(args[2]));
				break;
			case "settime":
				if (args.length < 3) {
					SendMessage(player,
							"�cUsage: /u announce settime <minutes>");
					return;
				}
				PluginMain.AnnounceTime = Integer.parseInt(args[2]) * 60 * 1000;
				SendMessage(player, "Time set between announce messages");
				break;
			case "list":
				SendMessage(player, "�bList of announce messages:�r");
				SendMessage(player, "�bFormat: [index] message�r");
				int i = 0;
				for (String message : PluginMain.AnnounceMessages)
					SendMessage(player, "[" + i++ + "] " + message);
				SendMessage(player,
						"�bCurrent wait time between announcements: "
								+ PluginMain.AnnounceTime / 60 / 1000
								+ " minute(s)�r");
				break;
			case "edit":
				if (commandblock == null) {
					SendMessage(
							player,
							"�cError: This command can only be used from a command block. Use /u announce remove.");
					break;
				}
				if (args.length < 4) {
					commandblock
							.sendMessage("�cUsage: /u announce edit <index> <message>");
					return;
				}
				StringBuilder sb1 = new StringBuilder();
				for (int i1 = 3; i1 < args.length; i1++) {
					sb1.append(args[i1]);
					if (i1 != args.length - 1)
						sb1.append(" ");
				}
				String finalmessage1 = sb1.toString().replace('&', '�');
				int index = Integer.parseInt(args[2]);
				if (index > 100)
					break;
				while (PluginMain.AnnounceMessages.size() <= index)
					PluginMain.AnnounceMessages.add("");
				PluginMain.AnnounceMessages.set(Integer.parseInt(args[2]),
						finalmessage1);
				commandblock.sendMessage("Announcement edited.");
				break;
			default:
				String message = "�cUsage: /u announce add|remove|settime|list|edit�r";
				SendMessage(player, message);
				return;
			}
		}
	}

	@SuppressWarnings("unused")
	private static void DoSaveLoadPos(Player player, String[] args) { // 2015.08.09.
		// args[0] is "admin" - args[1] is "savepos|loadpos"
		if (args.length == 2) {
			String message = "�cUsage: /u admin savepos|loadpos <player>�r";
			SendMessage(player, message);
			return;
		}
		Player p = null;
		try {
			p = Bukkit.getPlayer(args[2]);
		} catch (Exception e) {
		}
		if (!MaybeOfflinePlayer.AllPlayers.containsKey(p.getUniqueId())) {
			String message = "�cPlayer not found: " + args[2] + "�r";
			SendMessage(player, message);
			return;
		}
		MaybeOfflinePlayer mp = MaybeOfflinePlayer.AllPlayers.get(p
				.getUniqueId());
		if (p == null) {
			String message = "�cPlayer is not online: " + args[2] + "�r";
			SendMessage(player, message);
			return;
		}
		if (args[1].equalsIgnoreCase("savepos")) {
			mp.SavedLocation = p.getLocation();
		} else if (args[1].equalsIgnoreCase("loadpos")) {
			if (mp.SavedLocation != null)
				p.teleport(mp.SavedLocation);
		} else {
			String message = "�cUsage: /u admin savepos|loadpos <player>�r";
			SendMessage(player, message);
			return;
		}
		// SendMessage(player, "Player " + p.getName() +
		// " position saved/loaded.");s
	}

	private static void DoUpdateDynmap(Player player, String[] args) {
		// args[0] is "admin" - args[1] is "updatedynmap"
		if (args.length == 2) {
			String message = "�cUsage: /u admin updatedynmap <password>�r";
			SendMessage(player, message);
			return;
		}
	}

	public static ArrayList<String> Quiz = new ArrayList<>();

	private static void DoQuiz(Player player, String[] args,
			BlockCommandSender commandblock) {
		if (player == null || player.isOp()
				|| player.getName().equals("NorbiPeti")) {
			if (args.length == 1) {
				String message = "�cUsage: /u quiz add|remove|list|edit�r";
				SendMessage(player, message);
				return;
			}
			switch (args[1].toLowerCase()) {
			case "add":
				if (args.length < 3) {
					SendMessage(player, "�cUsage: /u quiz add <message>");
					return;
				}
				StringBuilder sb = new StringBuilder();
				for (int i = 2; i < args.length; i++) {
					sb.append(args[i]);
					if (i != args.length - 1)
						sb.append(" ");
				}
				String finalmessage = sb.toString().replace('&', '�');
				Quiz.add(finalmessage);
				SendMessage(player, "�Quiz question added.�r");
				break;
			case "remove":
				if (args.length < 3) {
					SendMessage(player, "�cUsage: /u quiz remove <index>");
					return;
				}
				Quiz.remove(Integer.parseInt(args[2]));
				break;
			case "list":
				SendMessage(player, "�bList of quiz questions:�r");
				SendMessage(player, "�bFormat: [index] question�r");
				int i = 0;
				for (String question : Quiz)
					SendMessage(player, "[" + i++ + "] " + question);
				break;
			case "edit":
				if (commandblock == null) {
					SendMessage(
							player,
							"�cError: This command can only be used from a command block. Use /u quiz remove.");
					break;
				}
				if (args.length < 4) {
					commandblock
							.sendMessage("�cUsage: /u quiz edit <index> <message>");
					return;
				}
				StringBuilder sb1 = new StringBuilder();
				for (int i1 = 3; i1 < args.length; i1++) {
					sb1.append(args[i1]);
					if (i1 != args.length - 1)
						sb1.append(" ");
				}
				String finalmessage1 = sb1.toString().replace('&', '�');
				int index = Integer.parseInt(args[2]);
				if (index > 100)
					break;
				while (Quiz.size() <= index)
					Quiz.add("");
				Quiz.set(Integer.parseInt(args[2]), finalmessage1);
				commandblock.sendMessage("Question edited.");
				break;
			default:
				String message = "�cUsage: /u quiz add|remove|list|edit�r";
				SendMessage(player, message);
				return;
			}
		}
	}

	private static Random random = new Random();

	public static String KittyCannonMinigame = "KittyCannon";

	private static void DoKittyCannon(Player player, String[] args) {
		if (player == null) {
			SendMessage(player,
					"�cThis command can only be used by a player.�r");
			return;
		}
		MinigamePlayer mp = Minigames.plugin.pdata.getMinigamePlayer(player);
		if (!player.isOp()
				&& (!mp.isInMinigame() || mp.getMinigame().getName(false)
						.equalsIgnoreCase(KittyCannonMinigame))) {
			SendMessage(player,
					"�You can only use KittyCannon in it's minigame!");
			return;
		}
		try {
			final Mob cat = Mob.OCELOT;
			final Ocelot ocelot = (Ocelot) cat.spawn(player.getWorld(),
					player.getServer(), player.getEyeLocation());
			if (ocelot == null) {
				return;
			}
			final ArrayList<String> lore = new ArrayList<>();
			lore.add(player.getName());
			final int i = random.nextInt(Ocelot.Type.values().length);
			ocelot.setCatType(Ocelot.Type.values()[i]);
			ocelot.setTamed(true);
			ocelot.setBaby();
			ocelot.addPotionEffect(new PotionEffect(
					PotionEffectType.DAMAGE_RESISTANCE, 5, 5));
			ocelot.setVelocity(player.getEyeLocation().getDirection()
					.multiply(2));
			Bukkit.getScheduler().scheduleSyncDelayedTask(PluginMain.Instance,
					new Runnable() {
						@SuppressWarnings("deprecation")
						@Override
						public void run() {
							final Location loc = ocelot.getLocation();
							ocelot.remove();
							loc.getWorld().createExplosion(loc, 0F);
							final ItemStack head = new ItemStack(
									Material.SKULL_ITEM, 1, (short) 3, (byte) 3);
							SkullMeta im = (SkullMeta) head.getItemMeta();
							im.setDisplayName("�rOcelot Head");
							im.setOwner("MHF_Ocelot");
							im.setLore(lore);
							head.setItemMeta(im);
							loc.getWorld().dropItem(loc, head);
						}
					}, 20);
		} catch (MobException e) {
		}
	}
}
