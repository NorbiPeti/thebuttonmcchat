package buttondevteam.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

import com.earth2me.essentials.Essentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import buttondevteam.chat.commands.UnlolCommand;
import buttondevteam.chat.commands.ucmds.admin.DebugCommand;
import buttondevteam.chat.formatting.*;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TellrawSerializableEnum;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;
import buttondevteam.chat.listener.PlayerListener;
import buttondevteam.lib.chat.*;

public class ChatProcessing {
	private static final Pattern NULL_MENTION_PATTERN = Pattern.compile("null");
	private static final Pattern CYAN_PATTERN = Pattern.compile("cyan");
	private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\");
	private static final Pattern CONSOLE_PING_PATTERN = Pattern.compile("(?i)" + Pattern.quote("@console"));
	private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
	private static final Pattern URL_PATTERN = Pattern.compile("(http[\\w:/?=$\\-_.+!*'(),]+)");
	private static final Pattern ENTIRE_MESSAGE_PATTERN = Pattern.compile(".+");
	private static final Pattern UNDERLINED_PATTERN = Pattern.compile("\\_");
	private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*");
	private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*");
	private static final String[] RainbowPresserColors = new String[] { "red", "gold", "yellow", "green", "blue",
			"dark_purple" }; // TODO
	private static boolean pingedconsole = false;

	private static ArrayList<ChatFormatter> commonFormatters = new ArrayList<>();
	private static Gson gson;

	public static final ChatFormatter ESCAPE_FORMATTER = new ChatFormatterBuilder().setRegex(ESCAPE_PATTERN).build();

	private ChatProcessing() {
	}

	static {
		commonFormatters.add(new ChatFormatterBuilder().setRegex(BOLD_PATTERN).setBold(true)
				.setRemoveCharCount((short) 2).setRange(true).build());
		commonFormatters.add(new ChatFormatterBuilder().setRegex(ITALIC_PATTERN).setItalic(true)
				.setRemoveCharCount((short) 1).setRange(true).build());
		commonFormatters.add(new ChatFormatterBuilder().setRegex(UNDERLINED_PATTERN).setUnderlined(true)
				.setRemoveCharCount((short) 1).setRange(true).build());
		commonFormatters.add(ESCAPE_FORMATTER);
		commonFormatters.add(new ChatFormatterBuilder().setRegex(URL_PATTERN).setUnderlined(true).setOpenlink("$1")
				.setRange(true).build());
		commonFormatters.add(new ChatFormatterBuilder().setRegex(NULL_MENTION_PATTERN).setColor(Color.DarkRed).build()); // Properly added a bug as a feature
		commonFormatters.add(new ChatFormatterBuilder().setRegex(CONSOLE_PING_PATTERN).setColor(Color.Aqua)
				.setOnmatch((String match) -> {
					if (!pingedconsole) {
						System.out.print("\007");
						pingedconsole = true; // Will set it to false in ProcessChat
					}
					return match;
				}).setPriority(Priority.High).build());

		commonFormatters.add(new ChatFormatterBuilder().setRegex(HASHTAG_PATTERN).setColor(Color.Blue)
				.setOpenlink("https://twitter.com/hashtag/$1").setPriority(Priority.High).build());
		commonFormatters.add(new ChatFormatterBuilder().setRegex(CYAN_PATTERN).setColor(Color.Aqua).build()); // #55
		gson = new GsonBuilder()
				.registerTypeHierarchyAdapter(TellrawSerializableEnum.class, new TellrawSerializer.TwEnum())
				.registerTypeHierarchyAdapter(Collection.class, new TellrawSerializer.TwCollection())
				.registerTypeAdapter(Boolean.class, new TellrawSerializer.TwBool())
				.registerTypeAdapter(boolean.class, new TellrawSerializer.TwBool()).disableHtmlEscaping().create();
	}

	// Returns e.setCancelled for custom event
	public static boolean ProcessChat(Channel channel, CommandSender sender, String message) {
		long processstart = System.nanoTime();
		if (PluginMain.essentials == null)
			PluginMain.essentials = (Essentials) (Bukkit.getPluginManager().getPlugin("Essentials"));
		Player player = (sender instanceof Player ? (Player) sender : null);

		if (player != null && PluginMain.essentials.getUser(player).isMuted())
			return true;

		doFunStuff(sender, message);

		ChatPlayer mp = null;
		if (player != null)
			mp = TBMCPlayerBase.getPlayer(player.getUniqueId(), ChatPlayer.class);

		Color colormode = channel.color;
		if (mp != null && mp.OtherColorMode != null)
			colormode = mp.OtherColorMode;
		if (mp != null && mp.RainbowPresserColorMode)
			colormode = Color.RPC;
		if (message.startsWith(">"))
			colormode = Color.Green;
		// If greentext, ignore channel or player colors

		ArrayList<ChatFormatter> formatters = addFormatters(colormode);
		pingedconsole = false; // Will set it to true onmatch (static constructor)
		final String channelidentifier = getChannelID(channel, sender, mp);

		TellrawPart json = createTellraw(sender, message, player, mp, channelidentifier);
		long combinetime = System.nanoTime();
		ChatFormatter.Combine(formatters, message, json);
		combinetime = System.nanoTime() - combinetime;
		String jsonstr = toJson(json);
		if (jsonstr.length() >= 32767) {
			sender.sendMessage(
					"§cError: Message too long. Try shortening it, or remove hashtags and other formatting.");
			return true;
		}
		DebugCommand.SendDebugMessage(jsonstr);
		if (channel.equals(Channel.TownChat) || channel.equals(Channel.NationChat)) {
			if (player == null) {
				sender.sendMessage("§cYou are not a player!");
				return true;
			}
			for (Player p : Bukkit.getOnlinePlayers()) {
				try {
					Resident resident = PluginMain.Instance.TU.getResidentMap().get(p.getName().toLowerCase());
					if (resident != null && !resident.getName().equals(player.getName())
							&& resident.getModes().contains("spy"))
						Bukkit.getPlayer(resident.getName()).sendMessage(String.format("[SPY-%s] - %s: %s",
								channel.DisplayName, player.getDisplayName(), message));
				} catch (Exception e) {
				}
			}
		}
		try {
			if (channel.equals(Channel.TownChat)) {
				Town town = null;
				try {
					final Resident resident = PluginMain.Instance.TU.getResidentMap()
							.get(player.getName().toLowerCase());
					if (resident != null && resident.hasTown())
						town = resident.getTown();
				} catch (NotRegisteredException e) {
				}
				if (town == null) {
					player.sendMessage("§cYou aren't in a town or an error occured.");
					return true;
				}
				int index = PluginMain.Instance.Towns.indexOf(town);
				if (index < 0) {
					PluginMain.Instance.Towns.add(town);
					index = PluginMain.Instance.Towns.size() - 1;
				}
				Objective obj = PluginMain.SB.getObjective("town");
				for (Player p : Bukkit.getOnlinePlayers()) {
					try {
						if (town.getResidents().stream().anyMatch(r -> r.getName().equalsIgnoreCase(p.getName())))
							obj.getScore(p.getName()).setScore(index);
						else
							obj.getScore(p.getName()).setScore(-1);
					} catch (Exception e) {
						obj.getScore(p.getName()).setScore(-1);
					}
				}
				PluginMain.Instance.getServer().dispatchCommand(PluginMain.Console,
						String.format("tellraw @a[score_town=%d,score_town_min=%d] %s", index, index, jsonstr));
			} else if (channel.equals(Channel.NationChat)) {
				Town town = null;
				try {
					final Resident resident = PluginMain.Instance.TU.getResidentMap()
							.get(player.getName().toLowerCase());
					if (resident != null && resident.hasTown())
						town = resident.getTown();
				} catch (NotRegisteredException e) {
				}
				if (town == null) {
					player.sendMessage("§cYou aren't in a town or an error occured.");
					return true;
				}
				Nation nation = null;
				try {
					nation = town.getNation();
				} catch (NotRegisteredException e) {
				}
				if (nation == null) {
					player.sendMessage("§cYour town isn't in a nation or an error occured.");
					return true;
				}
				int index = PluginMain.Instance.Nations.indexOf(nation);
				if (index < 0) {
					PluginMain.Instance.Nations.add(nation);
					index = PluginMain.Instance.Nations.size() - 1;
				}
				Objective obj = PluginMain.SB.getObjective("nation");
				for (Player p : Bukkit.getOnlinePlayers()) {
					try {
						if (nation.getResidents().stream().anyMatch(r -> r.getName().equalsIgnoreCase(p.getName())))
							obj.getScore(p.getName()).setScore(index);
						else
							obj.getScore(p.getName()).setScore(-1);
					} catch (Exception e) {
					}
				}
				PluginMain.Instance.getServer().dispatchCommand(PluginMain.Console,
						String.format("tellraw @a[score_nation=%d,score_nation_min=%d] %s", index, index, jsonstr));
			} else if (channel.equals(Channel.AdminChat)) {
				if (player != null && !player.isOp()) {
					player.sendMessage("§cYou need to be an OP to use this channel.");
					return true;
				}
				Objective obj = PluginMain.SB.getObjective("admin");
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.isOp())
						obj.getScore(p.getName()).setScore(1);
					else
						obj.getScore(p.getName()).setScore(0);
				}
				PluginMain.Instance.getServer().dispatchCommand(PluginMain.Console,
						String.format("tellraw @a[score_admin=%d,score_admin_min=%d] %s", 1, 1, jsonstr));
			} else if (channel.equals(Channel.ModChat)) {
				if (player != null && !PluginMain.permission.playerInGroup(player, "mod")) {
					player.sendMessage("§cYou need to be a mod to use this channel.");
					return true;
				}
				Objective obj = PluginMain.SB.getObjective("mod");
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (PluginMain.permission.playerInGroup(p, "mod"))
						obj.getScore(p.getName()).setScore(1);
					else
						obj.getScore(p.getName()).setScore(0);
				}
				PluginMain.Instance.getServer().dispatchCommand(PluginMain.Console,
						String.format("tellraw @a[score_mod=%d,score_mod_min=%d] %s", 1, 1, jsonstr));
			} else
				PluginMain.Instance.getServer().dispatchCommand(PluginMain.Console,
						String.format("tellraw @a %s", jsonstr));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while sending a chat message!", e);
			player.sendMessage("§cAn error occured while sending the message.");
			return true;
		}
		PluginMain.Instance.getServer().getConsoleSender().sendMessage(String.format("%s <%s> %s", channelidentifier,
				(player != null ? player.getDisplayName() : sender.getName()), message));
		DebugCommand.SendDebugMessage(
				"-- Full ChatProcessing time: " + (System.nanoTime() - processstart) / 1000000f + " ms");
		DebugCommand.SendDebugMessage("-- ChatFormatter.Combine time: " + combinetime / 1000000f + " ms");
		return false;
	}

	static String toJson(TellrawPart json) {
		String jsonstr = gson.toJson(json);
		return jsonstr;
	}

	static TellrawPart createTellraw(CommandSender sender, String message, Player player, ChatPlayer mp,
			final String channelidentifier) {
		TellrawPart json = new TellrawPart("");
		if (mp != null && mp.ChatOnly) {
			json.addExtra(new TellrawPart("[C]").setHoverEvent(
					TellrawEvent.create(TellrawEvent.HoverAC, TellrawEvent.HoverAction.SHOW_TEXT, "Chat only")));
		}
		json.addExtra(
				new TellrawPart(channelidentifier)
						.setHoverEvent(
								TellrawEvent.create(TellrawEvent.HoverAC, TellrawEvent.HoverAction.SHOW_TEXT,
										new TellrawPart((sender instanceof IDiscordSender ? "From Discord\n" : "")
												+ "Copy message").setColor(Color.Blue)))
						.setClickEvent(TellrawEvent.create(TellrawEvent.ClickAC,
								TellrawEvent.ClickAction.SUGGEST_COMMAND, message)));
		json.addExtra(new TellrawPart(" <"));
		json.addExtra(
				new TellrawPart(
						(player != null ? player.getDisplayName() : sender.getName()))
								.setHoverEvent(
										TellrawEvent
												.create(TellrawEvent.HoverAC, TellrawEvent.HoverAction.SHOW_TEXT,
														new TellrawPart("")
																.addExtra(new TellrawPart(String.format("Flair: %s",
																		(mp != null ? mp.GetFormattedFlair() : "-"))))
																.addExtra(new TellrawPart(
																		String.format("\nPlayername: %s\n",
																				(player != null ? player.getName()
																						: sender.getName())))
																								.setColor(Color.Aqua))
																.addExtra(new TellrawPart(String.format("World: %s\n",
																		(player != null ? player.getWorld().getName()
																				: "-"))))
																.addExtra(new TellrawPart(String.format(
																		"Respect: %s%s%s",
																		(mp != null ? (mp.FCount().getOrDefault(0)
																				/ (double) mp.FDeaths().getOrDefault(0))
																				: "Infinite"),
																		(mp != null && mp.UserName().get() != null
																				&& !mp.UserName().get().isEmpty()
																						? "\nUserName: "
																								+ mp.UserName().get()
																						: ""),
																		(mp != null && mp.PlayerName().get()
																				.equals("\nAlpha_Bacca44")
																						? "\nDeaths: "
																								+ PlayerListener.AlphaDeaths
																						: ""))))
																.addExtra(new TellrawPart("\nFor more, do /u info "
																		+ sender.getName())))));
		json.addExtra(new TellrawPart("> "));
		return json;
	}

	static String getChannelID(Channel channel, CommandSender sender, ChatPlayer mp) {
		final String channelidentifier = ("[" + (sender instanceof IDiscordSender ? "d|" : "") + channel.DisplayName)
				+ "]" + (mp != null && !mp.RPMode ? "[OOC]" : "");
		return channelidentifier;
	}

	static ArrayList<ChatFormatter> addFormatters(Color colormode) {
		@SuppressWarnings("unchecked")
		ArrayList<ChatFormatter> formatters = (ArrayList<ChatFormatter>) commonFormatters.clone();

		formatters.add(new ChatFormatterBuilder().setRegex(ENTIRE_MESSAGE_PATTERN).setColor(colormode)
				.setPriority(Priority.Low).build());

		if (Bukkit.getOnlinePlayers().size() > 0) {
			StringBuilder namesb = new StringBuilder("(?i)(");
			for (Player p : Bukkit.getOnlinePlayers())
				namesb.append(p.getName()).append("|");
			namesb.deleteCharAt(namesb.length() - 1);
			namesb.append(")");
			StringBuilder nicksb = new StringBuilder("(?i)(");
			boolean addNickFormatter = false;
			final int size = Bukkit.getOnlinePlayers().size();
			int index = 0;
			for (Player p : Bukkit.getOnlinePlayers()) {
				final String nick = PlayerListener.nicknames.inverse().get(p.getUniqueId());
				if (nick != null) {
					nicksb.append(nick);
					if (index < size - 1) {
						nicksb.append("|");
						addNickFormatter = true;
					}
				}
				index++;
			}
			nicksb.append(")");

			formatters.add(new ChatFormatterBuilder().setRegex(Pattern.compile(namesb.toString())).setColor(Color.Aqua)
					.setOnmatch((String match) -> {
						Player p = Bukkit.getPlayer(match);
						if (p == null) {
							PluginMain.Instance.getLogger()
									.warning("Error: Can't find player " + match + " but was reported as online.");
							return "§c" + match + "§r";
						}
						ChatPlayer mpp = TBMCPlayer.getPlayer(p.getUniqueId(), ChatPlayer.class);
						if (PlayerListener.NotificationSound == null)
							p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f); // TODO: Airhorn
						else
							p.playSound(p.getLocation(), PlayerListener.NotificationSound, 1.0f,
									(float) PlayerListener.NotificationPitch);
						String color = String.format("§%x", (mpp.GetFlairColor() == 0x00 ? 0xb : mpp.GetFlairColor()));
						return color + p.getName() + "§r";
					}).setPriority(Priority.High).build());

			if (addNickFormatter)
				formatters.add(new ChatFormatterBuilder().setRegex(Pattern.compile(nicksb.toString()))
						.setColor(Color.Aqua).setOnmatch((String match) -> {
							if (PlayerListener.nicknames.containsKey(match)) {
								Player p = Bukkit.getPlayer(PlayerListener.nicknames.get(match));
								if (p == null) {
									PluginMain.Instance.getLogger().warning("Error: Can't find player nicknamed "
											+ match + " but was reported as online.");
									return "§c" + match + "§r";
								}
								if (PlayerListener.NotificationSound == null)
									p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
								else
									p.playSound(p.getLocation(), PlayerListener.NotificationSound, 1.0f,
											(float) PlayerListener.NotificationPitch);
								return PluginMain.essentials.getUser(p).getNickname();
							}
							Bukkit.getServer().getLogger().warning("Player nicknamed " + match
									+ " not found in nickname map but was reported as online.");
							return "§c" + match + "§r";
						}).setPriority(Priority.High).build());
		}
		return formatters;
	}

	static void doFunStuff(CommandSender sender, String message) {
		if (PlayerListener.ActiveF && !PlayerListener.Fs.contains(sender) && message.equalsIgnoreCase("F"))
			PlayerListener.Fs.add(sender);

		String msg = message.toLowerCase();
		if (msg.contains("lol")) {
			UnlolCommand.Lastlolornot = true;
			UnlolCommand.Lastlol = sender;
		} else {
			for (int i = 0; i < PlayerListener.LaughStrings.length; i++) {
				if (msg.contains(PlayerListener.LaughStrings[i])) {
					UnlolCommand.Lastlol = sender;
					UnlolCommand.Lastlolornot = false;
					break;
				}
			}
		}
	}
}
