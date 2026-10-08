package eu.andret.torphes.entity;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum Advancement {
	BASIC,
	MEDIUM,
	EXPERT;

	@Nullable
	public static Advancement fromName(@Nullable final String name) {
		return Arrays.stream(values())
				.filter(advancement -> advancement.name().equalsIgnoreCase(name))
				.findFirst()
				.orElse(null);
	}
}
