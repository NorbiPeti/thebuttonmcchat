package tk.sznp.thebuttonautoflair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    // This method is called, when somebody uses our command
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	    if (sender instanceof Player) {
	        Player player = (Player) sender;
	        if(args.length<1)
	        	return false;
        	MaybeOfflinePlayer p=MaybeOfflinePlayer.AllPlayers.get(player.getName()); //2015.08.08.
	        //if(!PluginMain.PlayerFlairs.containsKey(player.getName()))
	        if(p.Flair==null)
	        {
	        	player.sendMessage("Error: You need to write your username to the reddit thread at /r/TheButtonMinecraft");
	        	return true;
	        }
	        switch(args[0].toLowerCase()) //toLowerCase: 2015.08.09.
	        {
	        case "accept":
	        {
	        	if(p.IgnoredFlair)
	        		p.IgnoredFlair=false; //2015.08.08.
	        	if(!p.AcceptedFlair)
	        	{
	        		String flair=p.Flair; //2015.08.08.
		        	PluginMain.AppendPlayerDisplayFlairFinal(player, flair); //2015.07.20.
		        	p.AcceptedFlair=true; //2015.08.08.
	        		player.sendMessage("�6Your flair has been set:�r "+flair);
	        	}
	        	else
	        		player.sendMessage("�cYou already have this user's flair.�r");
	        	break;
	        }
	        case "ignore":
	        {
	        	if(p.AcceptedFlair)
	        		p.AcceptedFlair=false; //2015.08.08.
        		if(!p.IgnoredFlair)
        		{
    	    		p.IgnoredFlair=true;
		        	String flair=p.Flair; //2015.08.08.
		        	PluginMain.RemovePlayerDisplayFlairFinal(player, flair); //2015.07.20.
		    		player.sendMessage("�6You have ignored this request. You can still use /u accept though.�r");
        		}
        		else
        			player.sendMessage("�cYou already ignored this request.�r");
	        	break;
	        }
	        /*case "reload": //2015.07.20.
	        	DoReload(player);
	        	break;*/
	        case "admin": //2015.08.09.
	        	DoAdmin(player, args);
	        	break;
        	default:
        		return false;
	        }
	        return true;
		}
	    /*if(args[0].toLowerCase()=="reload")
	    	DoReload(null); //2015.07.20.*/
	    else if(args.length>0 && args[0].toLowerCase().equals("admin")) //2015.08.09.
	    {
	    	DoAdmin(null, args); //2015.08.09.
	    	return true; //2015.08.09.
	    }
	    return false;
	}
	private static void DoReload(Player player)
	{ //2015.07.20.
    	//if(player==null || player.isOp() || player.getName()=="NorbiPeti")
    	//{
    		try
    		{
        		File file=new File("autoflairconfig.txt");
        		if(file.exists())
        		{
        			PluginMain.TownColors.clear();
    				BufferedReader br=new BufferedReader(new FileReader(file));
    				String line;
    				while((line=br.readLine())!=null)
    				{
    					String[] s=line.split(" ");
    					PluginMain.TownColors.put(s[0], s[1]);
    				}
    				br.close();
    				for(Player p : PluginMain.GetPlayers())
    				{
    					MaybeOfflinePlayer mp = MaybeOfflinePlayer.AllPlayers.get(p.getName());
    					if(mp.Flair!=null)
    					{
    						String flair=mp.Flair;
    						PluginMain.RemovePlayerDisplayFlairFinal(p, flair);
    						PluginMain.AppendPlayerDisplayFlairFinal(p, flair);
    					}	
    				}
    				String msg="�6Reloaded config file.�r";
    				SendMessage(player, msg); //2015.08.09.
        		}
    		}
    		catch(Exception e)
    		{
    			System.out.println("Error!\n"+e);
    			if(player!=null)
    				player.sendMessage("�cAn error occured. See console for details.�r");
    			PluginMain.LastException=e; //2015.08.09.
    		}
    	//}
    	//else
			//player.sendMessage("�cYou need to be OP to use this command.�r");
	}
	private static void DoAdmin(Player player, String[] args)
	{ //2015.08.09.
    	if(player==null || player.isOp() || player.getName()=="NorbiPeti")
    	{
    		//System.out.println("Args length: " + args.length);
    		if(args.length==1)
    		{
    			String message="�cUsage: /u admin reload|playerinfo�r";
    			SendMessage(player, message);
    			return;
    		}
    		//args[0] is "admin"
    		switch(args[1].toLowerCase())
    		{
    		case "reload":
    			DoReload(player);
    			break;
    		case "playerinfo":
    			DoPlayerInfo(player, args);
    			break;
    		case "getlasterror":
    			DoGetLastError(player, args);
			default:
				String message="�cUsage: /u admin reload|playerinfo�r";
				SendMessage(player, message);
				return;
    		}
    	}
    	else
			player.sendMessage("�cYou need to be OP to use this command.�r");
	}
	private static void DoPlayerInfo(Player player, String[] args)
	{ //2015.08.09.
		//args[0] is "admin" - args[1] is "playerinfo"
		if(args.length==2)
		{
			String message="�cUsage: /u admin playerinfo <player>�r";
			SendMessage(player, message);
			return;
		}
		if(!MaybeOfflinePlayer.AllPlayers.containsKey(args[2]))
		{
			String message="�cPlayer not found: "+args[2]+"�r";
			SendMessage(player, message);
			return;
		}
		MaybeOfflinePlayer p = MaybeOfflinePlayer.AllPlayers.get(args[2]);
		SendMessage(player, "Player name: "+p.PlayerName);
		SendMessage(player, "User flair: "+p.Flair);
		SendMessage(player, "Username: "+p.UserName);
		SendMessage(player, "Flair accepted: "+p.AcceptedFlair);
		SendMessage(player, "Flair ignored: "+p.IgnoredFlair);
	}
	private static void SendMessage(Player player, String message)
	{ //2015.08.09.
		if(player==null)
			System.out.println(message);
		else
			player.sendMessage(message);
	}
	private static void DoGetLastError(Player player, String[] args)
	{ //2015.08.09.
		//args[0] is "admin" - args[1] is "getlasterror"
		if(PluginMain.LastException!=null)
		{
			SendMessage(player, "Last error:");
			SendMessage(player, PluginMain.LastException.toString());
			PluginMain.LastException=null;
		}
		else
			SendMessage(player, "There were no exceptions.");
	}
}
