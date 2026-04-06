# embabel-agent-intellij

An IntelliJ IDEA plugin that provides framework support for the [embabel-agent](https://github.com/embabel/embabel-agent) library.

## Features

- Suppresses false "unused method" warnings for methods annotated with `@Action`, `@Condition`, and `@Cost` — framework-invoked annotations that IntelliJ cannot detect as called at compile time.

## Building

```bash
./gradlew buildPlugin
```

The built plugin ZIP will be located in `build/distributions/`.

## Running Locally

To launch a sandboxed IDE instance with the plugin installed:

```bash
./gradlew runIde
```

## Publishing to the JetBrains Marketplace

Publishing requires the following GitHub Actions secrets to be configured in the repository:

| Secret | Description |
|---|---|
| `PUBLISH_TOKEN` | JetBrains Marketplace API token |
| `CERTIFICATE_CHAIN` | PEM certificate chain for plugin signing |
| `PRIVATE_KEY` | Private key for plugin signing |
| `PRIVATE_KEY_PASSWORD` | Password for the private key |

## License

Apache License 2.0 — see [LICENSE](LICENSE).
