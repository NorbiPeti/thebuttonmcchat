package buttondevteam.chat.commands;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import buttondevteam.chat.ChatPlayer;
import buttondevteam.lib.chat.TBMCCommandBase;
import buttondevteam.lib.player.TBMCPlayer;

public final class ChatonlyCommand extends TBMCCommandBase { //TODO: Add annotation

	@Override
	public String[] GetHelpText(String alias) {
		return new String[] { "§6---- Chat-only mode ----", //
				"This mode makes you invincible but unable to move, teleport or interact with the world in any way", //
				"It was designed for chat clients", //
				"Once enabled, the only way of disabling it is by relogging to the server" //
		};
	}

	@Override
	public boolean OnCommand(CommandSender sender, String alias, String[] args) {
		Player player = (Player) sender;
		ChatPlayer p = TBMCPlayer.getPlayer(player.getUniqueId(), ChatPlayer.class);
		p.ChatOnly = true;
		player.setGameMode(GameMode.SPECTATOR);
		player.sendMessage("§bChat-only mode enabled. You are now invincible.");
		return true;
	}

}
