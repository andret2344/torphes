package eu.andret.torphes.guild;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildsCountListener extends ListenerAdapter {
	@Override
	public void onGuildJoin(@NotNull final GuildJoinEvent event) {
		setPresence(event);
	}

	@Override
	public void onGuildLeave(@NotNull final GuildLeaveEvent event) {
		setPresence(event);
	}

	// Fires once after all guilds are loaded, unlike GuildReadyEvent which fires for every guild
	@Override
	public void onReady(@NotNull final ReadyEvent event) {
		setPresence(event);
	}

	private void setPresence(@NotNull final Event event) {
		final JDA jda = event.getJDA();
		final Activity activity = Activity.listening(String.format("%d servers!", jda.getGuildCache().size()));
		jda.getPresence().setPresence(activity, false);
	}
}
