# Resonance

Local AI in Minecraft. Single JAR. No setup.

---

Runs LLM models locally via llama.cpp. AI responds in chat. No internet. No API keys.

## Install

1. Download JAR from [Releases](https://github.com/t21664815-eng/Resonance/releases) or [Modrinth](https://modrinth.com/mod/resonance).
2. Place in your mods folder. Launch with Forge 47.2.20+.
3. Click the AI button in the main menu and download your preferred model.
4. Type "Helper, hello" in the game chat.

## Requirements

- Minecraft Version: 1.20.1
- Mod Loader: Forge 47.2.20+
- Java Version: Java 17+
- RAM: 4 GB minimum

## Safety

- Binds strictly to 127.0.0.1. No internet during inference.
- Binaries from [llama.cpp](https://github.com/ggerganov/llama.cpp) (MIT License).
- VirusTotal: 0 detections.
- Models via official [Hugging Face](https://huggingface.co/RS-Games-Studio).

> **Note:** The `src/main/resources/llama/` folder is excluded from the repository due to binary size limits. The official JAR release includes the binaries via Gradle build configuration.

## License

MIT. See [LICENSE](LICENSE).
