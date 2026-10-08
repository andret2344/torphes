package eu.andret.torphes.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public record Question(int id, @NotNull Advancement advancement, @NotNull String category, @NotNull String text,
					   @Nullable String code, @NotNull List<Answer> answers
) {
	// Every answer is a button and Discord allows at most 5 buttons in one row
	private static final int MAX_ANSWERS = 5;

	public boolean isValid() {
		return answers.size() <= MAX_ANSWERS
				&& answers.stream().filter(Answer::correct).count() == 1;
	}

	public boolean matches(@Nullable final Advancement advancement, @Nullable final String category) {
		return (advancement == null || this.advancement == advancement)
				&& (category == null || this.category.equalsIgnoreCase(category));
	}

	public int correctAnswerIndex() {
		return IntStream.range(0, answers.size())
				.filter(index -> answers.get(index).correct())
				.findFirst()
				.orElseThrow();
	}

	@NotNull
	public Question withShuffledAnswers() {
		final List<Answer> shuffled = new ArrayList<>(answers);
		Collections.shuffle(shuffled);
		return new Question(id, advancement, category, text, code, List.copyOf(shuffled));
	}
}
