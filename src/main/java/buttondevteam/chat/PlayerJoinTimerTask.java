package buttondevteam.chat;

import java.util.TimerTask;

public abstract class PlayerJoinTimerTask extends TimerTask {

	@Override
	public abstract void run();

	public ChatPlayer mp;

}
