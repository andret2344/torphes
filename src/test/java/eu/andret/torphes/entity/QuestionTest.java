package eu.andret.torphes.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionTest {
	private static final Answer CORRECT = new Answer("correct", true, "Yes");
	private static final Answer WRONG_1 = new Answer("wrong 1", false, "No");
	private static final Answer WRONG_2 = new Answer("wrong 2", false, "No");
	private static final Answer WRONG_3 = new Answer("wrong 3", false, "No");

	private static Question question(final Advancement advancement, final String category, final List<Answer> answers) {
		return new Question(1, advancement, category, "Text", null, answers);
	}

	private static Question question(final Advancement advancement, final String category) {
		return question(advancement, category, List.of(CORRECT, WRONG_1, WRONG_2, WRONG_3));
	}

	@Test
	void testMatchesWithoutFilters() {
		assertTrue(question(Advancement.BASIC, "Spring").matches(null, null));
	}

	@Test
	void testMatchesByAdvancementOnly() {
		final Question question = question(Advancement.BASIC, "Spring");

		assertTrue(question.matches(Advancement.BASIC, null));
		assertFalse(question.matches(Advancement.EXPERT, null));
	}

	@Test
	void testMatchesByCategoryOnlyIgnoringCase() {
		final Question question = question(Advancement.BASIC, "Java language");

		assertTrue(question.matches(null, "java LANGUAGE"));
		assertFalse(question.matches(null, "Spring"));
	}

	@Test
	void testMatchesRequiresBothFilters() {
		final Question question = question(Advancement.BASIC, "Spring");

		assertTrue(question.matches(Advancement.BASIC, "Spring"));
		assertFalse(question.matches(Advancement.EXPERT, "Spring"));
		assertFalse(question.matches(Advancement.BASIC, "General"));
	}

	@Test
	void testIsValidWithExactlyOneCorrectAnswer() {
		assertTrue(question(Advancement.BASIC, "Spring").isValid());
	}

	@Test
	void testIsInvalidWithoutCorrectAnswer() {
		assertFalse(question(Advancement.BASIC, "Spring", List.of(WRONG_1, WRONG_2, WRONG_3)).isValid());
	}

	@Test
	void testIsInvalidWithTwoCorrectAnswers() {
		assertFalse(question(Advancement.BASIC, "Spring", List.of(CORRECT, CORRECT, WRONG_1)).isValid());
	}

	@Test
	void testIsInvalidWithMoreAnswersThanButtons() {
		final List<Answer> answers = List.of(CORRECT, WRONG_1, WRONG_2, WRONG_3, WRONG_1, WRONG_2);

		assertFalse(question(Advancement.BASIC, "Spring", answers).isValid());
	}

	@Test
	void testShuffledAnswersKeepCorrectAnswerIndex() {
		final Question shuffled = question(Advancement.BASIC, "Spring").withShuffledAnswers();

		assertEquals(4, shuffled.answers().size());
		assertEquals(CORRECT, shuffled.answers().get(shuffled.correctAnswerIndex()));
	}
}
