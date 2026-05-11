package com.rs_games.resonance.config;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    public String lastModel = "gemma-2-2b-it-Q4_K_M.gguf";
    public int maxContextTokens = 2048;
    public int threads = 3;
    public double temperature = 0.7;
    public double repeatPenalty = 1.2;
    public String lastPersonality = "HELPER";
    public List<Personality> personalities = new ArrayList<>();
    public List<ModelEntry> availableModels = new ArrayList<>();

    public static Settings createDefault() {
        Settings s = new Settings();
        s.availableModels.add(new ModelEntry(
                "Gemma 2B (Q4_K_M)",
                "gemma-2-2b-it-Q4_K_M.gguf",
                "Standard, fastest. ~1.5 GB RAM."
        ));

        s.availableModels.add(new ModelEntry(
                "Llama 3.2 3B (Q4_K_M)",
                "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
                "Balanced. ~2.0 GB RAM."
        ));

        s.availableModels.add(new ModelEntry(
                "Phi-3.5-mini 3.8B (Q4_K_M)",
                "Phi-3.5-mini-instruct-Q4_K_M.gguf",
                "Most powerful. ~2.3 GB RAM."
        ));
        Personality helper = new Personality();
        helper.id = "HELPER";
        helper.name = "Помощник";
        helper.color = "§b";
        helper.prompt = "Ты — ИИ-помощник RS Games Studio. Отвечай кратко и по делу. Языки общения - английский, русский.";
        helper.aliases = new ArrayList<>();
        helper.aliases.add("Помощник");
        helper.aliases.add("помощник");
        helper.aliases.add("Помоги");
        helper.aliases.add("помоги");
        helper.aliases.add("Help");
        helper.aliases.add("help");
        helper.aliases.add("Helper");
        helper.aliases.add("helper");
        helper.aliases.add("Вики");
        helper.aliases.add("вики");
        helper.aliases.add("Wiki");
        helper.aliases.add("wiki");
        helper.aliases.add("Хелпер");
        helper.aliases.add("хелпер");
        helper.aliases.add("Ассистент");
        helper.aliases.add("ассистент");
        helper.aliases.add("Бот");
        helper.aliases.add("бот");
        s.personalities.add(helper);
        Personality v = new Personality();
        v.id = "";
        v.name = "";
        v.color = "§";
        v.prompt = "";
        v.aliases = new ArrayList<>(List.of("", ""));
        s.personalities.add(v);
        Personality n = new Personality();
        n.id = "";
        n.name = "";
        n.color = "§";
        n.prompt = "";
        n.aliases = new ArrayList<>(List.of("", ""));
        s.personalities.add(n);
        Personality uzi = new Personality();
        uzi.id = "";
        uzi.name = "";
        uzi.color = "§";
        uzi.prompt = "";
        uzi.aliases = new ArrayList<>(List.of("", ""));
        s.personalities.add(uzi);
        return s;
    }
    public static class Personality {
        public String id;
        public String name;
        public String color = "§b";
        public String prompt;
        public List<String> aliases = new ArrayList<>();
    }
    public static class ModelEntry {
        public String name;
        public String file;
        public String info;
        public ModelEntry() {}
        public ModelEntry(String name, String file, String info) {
            this.name = name;
            this.file = file;
            this.info = info;
        }
    }
}