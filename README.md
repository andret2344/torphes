# Torphes

Discord bot asking Java quiz questions via the `/question` slash command.

## Developing

1. Check out the repo.
2. Build it with the `./gradlew build` command.
3. Ask the author about the bot token
   ([PROD](https://discord.com/developers/applications/606928324970938389/bot),
   [DEV](https://discord.com/developers/applications/1308008107661725737/bot)).
4. Start the app with the token in the `TORPHES_TOKEN` environment variable.
   Optionally set `TORPHES_LOG_LEVEL` (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`, `OFF`; `INFO` by default).

The token is read only at runtime and never ends up in the JAR or the Docker image.

## Using

[ADD THIS BOT TO YOUR SERVER](https://discord.com/application-directory/606928324970938389)
