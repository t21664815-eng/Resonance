# Resonance AI

Local AI in Minecraft. Single JAR. No setup.

---

Runs LLM models locally via llama.cpp. AI responds in chat. No internet. No API keys.

## Install

1. Download JAR from [Releases](https://github.com) or [Modrinth](https://modrinth.com).
2. Place in your mods folder. Launch with Forge 47.2.20+.
3. Click the AI button in the main menu and download your preferred model.
4. Type "Helper, hello" in the game chat.

## Requirements

- Minecraft Version: 1.20.1
- Mod Loader: Forge 47.2.20+
- Java Version: Java 17+
- RAM: 4 GB minimum (Free System RAM)

## Safety & Security

- Local Isolation: Binds strictly to 127.0.0.1. No internet connection during inference.
- Open Source Backend: Binaries from [llama.cpp](https://github.com) (MIT License).
- VirusTotal: 0/100 detections (Clean).
- Official Dataset: Models downloaded via official [Hugging Face](https://huggingface.co) repository.
> **Note for developers:** The `src/main/resources/llama/` folder is excluded from the repository due to binary size limits. The official JAR release includes the binaries via Gradle build configuration.

## License

Distributed under the MIT License. See LICENSE file for details.
