package ch.so.agi.dbeaver.ai.llm;

public final class ContextAwarePromptComposer {

    public String composeUserPrompt(String userInput, String contextBlock) {
        String normalizedUserInput = userInput == null ? "" : userInput.trim();
        if (contextBlock == null || contextBlock.isBlank()) {
            return normalizedUserInput;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Nutzeranfrage\n");
        sb.append(normalizedUserInput);
        sb.append("\n\n");
        sb.append("## Arbeitsauftrag\n");
        sb.append("- Nutze den untenstehenden Tabellenkontext als primaere Quelle.\n");
        sb.append("- Beruecksichtige DDL (Spalten, Typen, Schluessel) und Beispielzeilen.\n");
        sb.append("- Erfinde keine Tabellen/Spalten, die dort nicht vorkommen.\n");
        sb.append("- Wenn Beziehungen oder Filterbedingungen nicht eindeutig sind, nenne deine Annahmen explizit.\n");
        sb.append("\n");
        sb.append("## Tabellenkontext (automatisch aus der Datenbank extrahiert)\n");
        sb.append(contextBlock);
        return sb.toString();
    }
}
