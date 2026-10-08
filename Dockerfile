FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="torphes"
LABEL org.opencontainers.image.authors="Andret2344"
LABEL org.opencontainers.image.description="Discord bot 'torphes' asking Java quiz questions, written in Java 21, packaged as a runnable JAR and intended to run inside a container. Requires the TORPHES_TOKEN environment variable."
LABEL org.opencontainers.image.url="https://github.com/Andret2344/torphes"
LABEL org.opencontainers.image.source="https://github.com/Andret2344/torphes"
LABEL org.opencontainers.image.licenses="CC-BY-SA-4.0"

RUN groupadd --system --gid 10001 torphes \
    && useradd --system --uid 10001 --gid torphes --no-create-home --shell /usr/sbin/nologin torphes

COPY build/libs/torphes.jar torphes.jar

# The bot needs no privileges: it only makes outbound connections and logs to stdout
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/torphes.jar"]
