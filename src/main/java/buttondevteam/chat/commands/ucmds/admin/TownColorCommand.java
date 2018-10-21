package buttondevteam.chat.commands.ucmds.admin;

import buttondevteam.chat.PluginMain;
import buttondevteam.chat.listener.TownyListener;
import buttondevteam.lib.chat.Color;
import com.palmergames.bukkit.towny.object.Town;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.dynmap.towny.DynmapTownyPlugin;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class TownColorCommand extends AdminCommandBase {
    @Override
    public String GetHelpText(String alias)[] { // TODO: Command path aliases
        return new String[]{ //
                "§6---- Town Color ----", //
                "This command allows setting a color for a town.", //
                "The town will be shown with this color on Dynmap and all players in the town will appear in chat with these colors.", //
                "The colors will split the name evenly.", //
                "Usage: /" + GetCommandPath() + " <town> <colorname1> [colorname2...]", //
                "Example: /" + GetCommandPath() + " Alderon blue gray" //
        };
    }

    @Override
    public boolean OnCommand(CommandSender sender, String alias, String[] args) {
        return SetTownColor(sender, alias, args);
    }

    public static boolean SetTownColor(CommandSender sender, String alias, String[] args) {
        if (args.length < 2)
            return false;
        if (!PluginMain.TU.getTownsMap().containsKey(args[0].toLowerCase())) {
            sender.sendMessage("§cThe town '" + args[0] + "' cannot be found.");
            return true;
        }
	    Color[] clrs = new Color[args.length - 1];
	    Town targetTown = PluginMain.TU.getTownsMap().get(args[0].toLowerCase());
	    for (int i = 1; i < args.length; i++) {
		    val c = getColorOrSendError(args[i], sender);
		    if (!c.isPresent())
                return true;
		    clrs[i - 1] = c.get();
        }
        PluginMain.TownColors.put(args[0].toLowerCase(), clrs);
	    TownyListener.updateTownMembers(targetTown);

        val dtp = (DynmapTownyPlugin) Bukkit.getPluginManager().getPlugin("Dynmap-Towny");
        if (dtp == null) {
            sender.sendMessage("§cDynmap-Towny couldn't be found §6but otherwise §btown color set.");
            PluginMain.Instance.getLogger().warning("Dynmap-Towny not found for setting town color!");
            return true;
        }
	    PluginMain.setTownColor(dtp, targetTown.getName(), clrs);
        sender.sendMessage("§bColor(s) set.");
        return true;
    }

	private static Optional<Color> getColorOrSendError(String name, CommandSender sender) {
		val c = Arrays.stream(Color.values()).skip(1).filter(cc -> cc.getName().equalsIgnoreCase(name)).findAny();
		if (!c.isPresent()) { //^^ Skip black
			sender.sendMessage("§cThe color '" + name + "' cannot be found."); //ˇˇ Skip black
			sender.sendMessage("§cAvailable colors: " + Arrays.stream(Color.values()).skip(1).map(col -> String.format("§%x%s§r", col.ordinal(), col.getName())).collect(Collectors.joining(", ")));
			sender.sendMessage("§cMake sure to type them exactly as shown above.");
		}
		return c;
	}

	public static String getTownNameCased(String name) {
        return PluginMain.TU.getTownsMap().get(name.toLowerCase()).getName();
    }
}
