<img align="left" src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

# embabel-agent-intellij

[![Build](https://github.com/embabel/embabel-agent-intellij/actions/workflows/build.yml/badge.svg)](https://github.com/embabel/embabel-agent-intellij/actions/workflows/build.yml)
[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/com.embabel.agent.intellij-plugin)](https://plugins.jetbrains.com/plugin/com.embabel.agent.intellij-plugin)
[![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/com.embabel.agent.intellij-plugin)](https://plugins.jetbrains.com/plugin/com.embabel.agent.intellij-plugin)
[![License](https://img.shields.io/github/license/embabel/embabel-agent-intellij)](LICENSE)

<br clear="left"/>

IntelliJ IDEA plugin providing framework support for the [embabel-agent](https://github.com/embabel/embabel-agent) library.

## Features

- Suppresses false "unused method" warnings for methods annotated with `@Action`, `@Condition`, and `@Cost` — framework-invoked annotations that IntelliJ cannot detect as called at compile time.

## Installation

Install directly from the JetBrains Marketplace via **Settings → Plugins → Marketplace** and search for **Embabel Agent Support**.

Or install manually: download the ZIP from the [releases page](https://github.com/embabel/embabel-agent-intellij/releases) and install via **Settings → Plugins → ⚙ → Install Plugin from Disk**.

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
