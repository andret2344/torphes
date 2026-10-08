package eu.andret.torphes;

import eu.andret.torphes.command.QuestionCommand;
import eu.andret.torphes.guild.GuildsCountListener;
import eu.andret.torphes.util.Requestor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public final class Torphes {
	private static final String TOKEN_VARIABLE = "TORPHES_TOKEN";
	private static final String LOG_LEVEL_VARIABLE = "TORPHES_LOG_LEVEL";
	private static final Logger LOGGER = LoggerFactory.getLogger(Torphes.class);

	public static void main(final String[] args) {
		Configurator.setRootLevel(Level.toLevel(System.getenv(LOG_LEVEL_VARIABLE), Level.INFO));

		final String token = System.getenv(TOKEN_VARIABLE);
		if (token == null || token.isBlank()) {
			LOGGER.error("Missing the {} environment variable", TOKEN_VARIABLE);
			System.exit(1);
		}

		final JDA jda = JDABuilder.createLight(token, Collections.emptyList())
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.addEventListeners(new GuildsCountListener())
				.addEventListeners(new QuestionCommand(new Requestor()))
				.build();

		// Replaces all global commands, so the removed ones disappear from Discord as well
		jda.updateCommands()
				.addCommands(QuestionCommand.commandData())
				.queue();
	}
}
