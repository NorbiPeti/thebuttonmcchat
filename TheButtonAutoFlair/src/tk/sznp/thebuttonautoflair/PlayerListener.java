package tk.sznp.thebuttonautoflair;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.inventivegames.TellRawAutoMessage.Reflection;

public class PlayerListener implements Listener
{ //2015.07.16.
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player p=event.getPlayer();
		PluginMain.Players.add(p);
		//event.getPlayer().setDisplayName(p.getDisplayName()+PluginMain.GetFlair(p));
		if(PluginMain.PlayerUserNames.containsKey(p.getName())) //<-- 2015.07.20.
			PluginMain.AppendPlayerDisplayFlair(p, PluginMain.PlayerUserNames.get(p.getName()), PluginMain.GetFlair(p));
		else
		{ //2015.07.20.
			String json="[\"\",{\"text\":\"�6Hi! If you'd like your flair displayed ingame, write your Minecraft name to \"},{\"text\":\"[this thread.]\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://www.reddit.com/r/TheButtonMinecraft/comments/3d25do/\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":{\"text\":\"\",\"extra\":[{\"text\":\"Click here to go to the Reddit thread�r\"}]}}}]";
			sendRawMessage(p, json);
		}
		//System.out.println("Added player "+p.getName());
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event)
	{
		//for(Player player : PluginMain.Players)
		for(int i=0; i<PluginMain.Players.size();)
		{
			Player player=PluginMain.Players.get(i);
			if(player.getName().equals(event.getPlayer().getName()))
			{
				PluginMain.Players.remove(player);
				//System.out.println("Removed player "+event.getPlayer().getName());
			}
			else
				i++; //If the player is removed, the next item will be on the same index
		}
	}
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event)
	{
		
	}

	private static Class<?>	nmsChatSerializer		= Reflection.getNMSClass("IChatBaseComponent$ChatSerializer");
	private static Class<?>	nmsPacketPlayOutChat	= Reflection.getNMSClass("PacketPlayOutChat");
	public static void sendRawMessage(Player player, String message)
	{
		try {
			System.out.println("1");
			Object handle = Reflection.getHandle(player);
			System.out.println("2");
			Object connection = Reflection.getField(handle.getClass(), "playerConnection").get(handle);
			System.out.println("3");
			Object serialized = Reflection.getMethod(nmsChatSerializer, "a", String.class).invoke(null, message);
			System.out.println("4");
			Object packet = nmsPacketPlayOutChat.getConstructor(Reflection.getNMSClass("IChatBaseComponent")).newInstance(serialized);
			System.out.println("5");
			Reflection.getMethod(connection.getClass(), "sendPacket").invoke(connection, packet);
			System.out.println("6");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
