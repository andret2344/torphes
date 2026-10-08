package eu.andret.torphes.command;

import com.google.gson.reflect.TypeToken;
import eu.andret.torphes.entity.Advancement;
import eu.andret.torphes.entity.Answer;
import eu.andret.torphes.entity.Question;
import eu.andret.torphes.util.Requestor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class QuestionCommand extends ListenerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionCommand.class);
	private static final SecureRandom RANDOM = new SecureRandom();

	private static final String NAME = "question";
	private static final String ADVANCEMENT_OPTION = "advancement";
	private static final String CATEGORY_OPTION = "category";
	private static final List<String> CATEGORIES = List.of("General", "Design patterns", "Java language", "Java software", "Spring");

	private static final String QUESTIONS_URL = "https://public.andret.eu/questions.json";
	private static final TypeToken<List<Question>> QUESTIONS_TYPE = new TypeToken<>() {
	};

	// Answer button ids carry the answer index, report modal ids carry the question id
	private static final String ANSWER_PREFIX = "answer:";
	private static final String REPORT_BUTTON = "report";
	private static final String REPORT_MODAL_PREFIX = "report:";
	private static final String REPORT_DESCRIPTION = "description";
	private static final String OWNER_ID = "185829743134769153";

	private static final long EXPIRY_HOURS = 2;

	// Message id -> question shown in that message, with answers in the order of its buttons
	private final Map<String, Question> activeQuestions = new ConcurrentHashMap<>();

	private final Requestor requestor;

	public QuestionCommand(@NotNull final Requestor requestor) {
		this.requestor = requestor;
	}

	@NotNull
	public static SlashCommandData commandData() {
		return Commands.slash(NAME, "Get a question from the database")
				.addOption(OptionType.STRING, ADVANCEMENT_OPTION, "What should be the question difficulty?", false, true)
				.addOption(OptionType.STRING, CATEGORY_OPTION, "What should the question be about?", false, true)
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VIEW_CHANNEL));
	}

	@Override
	public void onSlashCommandInteraction(@NotNull final SlashCommandInteractionEvent event) {
		if (!event.getName().equals(NAME)) {
			return;
		}
		final Advancement advancement = Advancement.fromName(event.getOption(ADVANCEMENT_OPTION, null, OptionMapping::getAsString));
		final String category = event.getOption(CATEGORY_OPTION, null, OptionMapping::getAsString);
		LOGGER.info("Executed command: /question (advancement: {}, category: {})", advancement, category);
		event.deferReply().queue();
		requestor.executeRequest(QUESTIONS_URL, QUESTIONS_TYPE)
				.thenAccept(questions -> sendRandomQuestion(event.getHook(), questions, advancement, category))
				.exceptionally(throwable -> {
					LOGGER.error("Could not send a question", throwable);
					event.getHook().editOriginalEmbeds(errorEmbed("Something went wrong, try again later.")).queue();
					return null;
				});
	}

	private void sendRandomQuestion(@NotNull final InteractionHook hook, @NotNull final List<Question> questions,
									@Nullable final Advancement advancement, @Nullable final String category) {
		final List<Question> matching = questions.stream()
				.filter(Question::isValid)
				.filter(question -> question.matches(advancement, category))
				.toList();
		LOGGER.debug("Matching questions: {} of {}", matching.size(), questions.size());
		if (matching.isEmpty()) {
			hook.editOriginalEmbeds(errorEmbed("No question matches the given difficulty and category.")).queue();
			return;
		}
		final Question question = matching.get(RANDOM.nextInt(matching.size())).withShuffledAnswers();
		hook.editOriginalEmbeds(questionEmbed(question))
				.setComponents(ActionRow.of(unansweredButtons(question)))
				.queue(message -> {
					activeQuestions.put(message.getId(), question);
					CompletableFuture.delayedExecutor(EXPIRY_HOURS, TimeUnit.HOURS)
							.execute(() -> activeQuestions.remove(message.getId()));
				});
	}

	@Override
	public void onButtonInteraction(@NotNull final ButtonInteractionEvent event) {
		final String componentId = event.getComponentId();
		final Question question = activeQuestions.get(event.getMessageId());
		if (question == null) {
			// Expired or sent before a restart; the interaction token of the original reply is long gone,
			// so the buttons are removed only now, through the click
			event.editComponents().queue();
			event.getHook().sendMessageEmbeds(errorEmbed("The question has expired.")).setEphemeral(true).queue();
			return;
		}
		if (componentId.equals(REPORT_BUTTON)) {
			event.replyModal(reportModal(question)).queue();
			return;
		}
		if (!componentId.startsWith(ANSWER_PREFIX)) {
			return;
		}
		final int index = Integer.parseInt(componentId.substring(ANSWER_PREFIX.length()));
		final Answer answer = question.answers().get(index);
		LOGGER.debug("Question {}: clicked answer {}", question.id(), answer);
		// Before the first answer all buttons are primary; afterward a click only shows that answer's explanation
		final boolean firstAnswer = event.getButton().getStyle() == ButtonStyle.PRIMARY;
		if (firstAnswer) {
			event.editComponents(
							ActionRow.of(answeredButtons(question, index)),
							ActionRow.of(Button.danger(REPORT_BUTTON, "Report")))
					.queue();
		} else {
			event.deferEdit().queue();
		}
		event.getHook().sendMessage(String.format(" > %s. %s", letter(index), answer.explanation())).queue();
	}

	@Override
	public void onModalInteraction(@NotNull final ModalInteractionEvent event) {
		if (!event.getModalId().startsWith(REPORT_MODAL_PREFIX)) {
			return;
		}
		final String questionId = event.getModalId().substring(REPORT_MODAL_PREFIX.length());
		final List<MessageEmbed> description = Optional.ofNullable(event.getValue(REPORT_DESCRIPTION))
				.map(ModalMapping::getAsString)
				.filter(Predicate.not(String::isBlank))
				.map(string -> new EmbedBuilder().setDescription(string).build())
				.stream()
				.toList();
		LOGGER.info("Reported question: {}", questionId);

		event.getJDA()
				.openPrivateChannelById(OWNER_ID)
				.flatMap(channel -> channel
						.sendMessage(String.format("Zgłoszone pytanie o id: `%s`", questionId))
						.addEmbeds(description))
				.queue(null, error -> LOGGER.error("Could not deliver the report of question {}", questionId, error));

		event.reply("Zgłoszenie zostało wysłane").setEphemeral(true).queue();
	}

	@Override
	public void onCommandAutoCompleteInteraction(@NotNull final CommandAutoCompleteInteractionEvent event) {
		if (!event.getName().equals(NAME)) {
			return;
		}
		final String typed = event.getFocusedOption().getValue().toUpperCase(Locale.ROOT);
		final Stream<String> candidates = switch (event.getFocusedOption().getName()) {
			case ADVANCEMENT_OPTION -> Stream.of(Advancement.values()).map(Enum::name);
			case CATEGORY_OPTION -> CATEGORIES.stream();
			default -> Stream.empty();
		};
		final List<Command.Choice> choices = candidates
				.filter(candidate -> candidate.toUpperCase(Locale.ROOT).startsWith(typed))
				.map(candidate -> new Command.Choice(candidate, candidate))
				.toList();
		event.replyChoices(choices).queue();
	}

	@NotNull
	private static MessageEmbed questionEmbed(@NotNull final Question question) {
		final StringBuilder stringBuilder = new StringBuilder();
		if (question.code() != null) {
			stringBuilder.append("```java\n")
					.append(question.code())
					.append("\n```\n");
		}
		for (int i = 0; i < question.answers().size(); i++) {
			stringBuilder.append("* ")
					.append(letter(i))
					.append(". ")
					.append(question.answers().get(i).text())
					.append("\n");
		}
		return new EmbedBuilder()
				.setTitle(String.format("%d. %s", question.id(), question.text()))
				.setDescription(stringBuilder.toString())
				.build();
	}

	@NotNull
	private static MessageEmbed errorEmbed(@NotNull final String text) {
		return new EmbedBuilder()
				.setColor(Color.RED)
				.setDescription(text)
				.build();
	}

	@NotNull
	private static Modal reportModal(@NotNull final Question question) {
		final TextInput descriptionInput = TextInput.create(REPORT_DESCRIPTION, TextInputStyle.PARAGRAPH)
				.setRequired(false)
				.setPlaceholder("What's wrong with the question?")
				.setMaxLength(1000)
				.build();
		return Modal.create(REPORT_MODAL_PREFIX + question.id(), "Question report")
				.addComponents(Label.of("Description", descriptionInput))
				.build();
	}

	@NotNull
	private static List<Button> unansweredButtons(@NotNull final Question question) {
		return IntStream.range(0, question.answers().size())
				.mapToObj(index -> Button.primary(ANSWER_PREFIX + index, letter(index)))
				.toList();
	}

	@NotNull
	private static List<Button> answeredButtons(@NotNull final Question question, final int selectedIndex) {
		final int correctIndex = question.correctAnswerIndex();
		return IntStream.range(0, question.answers().size())
				.mapToObj(index -> {
					final String id = ANSWER_PREFIX + index;
					if (index == correctIndex) {
						return Button.success(id, letter(index));
					}
					if (index == selectedIndex) {
						return Button.danger(id, letter(index));
					}
					return Button.secondary(id, letter(index));
				})
				.toList();
	}

	@NotNull
	private static String letter(final int index) {
		return String.valueOf((char) ('A' + index));
	}
}
