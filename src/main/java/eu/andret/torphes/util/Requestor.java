package eu.andret.torphes.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class Requestor {
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final HttpClient client = HttpClient.newHttpClient();
	private final Gson gson = new Gson();

	@NotNull
	public <T> CompletableFuture<T> executeRequest(@NotNull final String url, @NotNull final TypeToken<T> typeToken) {
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(TIMEOUT)
				.build();
		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						throw new IllegalStateException(String.format("Request to %s failed with status %d", url, response.statusCode()));
					}
					return gson.fromJson(response.body(), typeToken);
				});
	}
}
