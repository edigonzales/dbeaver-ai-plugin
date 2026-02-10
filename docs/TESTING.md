# Testing

## Ausführung

Letzter verifizierter Lauf am **2026-02-10**:

```bash
./gradlew clean test
```

Ergebnis: **BUILD SUCCESSFUL**

## Automatisierte Testabdeckung

### Mention/Parser

- `MentionParserTest`
  - `#db.schema.table` korrekt
  - `#db.schema.table.` (trailing dot) korrekt
  - quoted Identifier inkl. escaped quotes
  - Dedup identischer Mentions
  - Tokenisierung trotz Satzzeichen
- `MentionTriggerDetectorTest`
  - Trigger-/Prefix-Erkennung
- `MentionProposalProviderTest`
  - Insert-Suffix für Autocomplete

### Kontext

- `ContextEnricherTest`
  - Warnungen bei unauflösbaren Referenzen
  - DDL + Sample-Kontext
  - Maskierung sensibler Felder
- `ContextAssemblerTest`
  - Token-Budget/Trunkierung
  - Prompt-Block-Reihenfolge
- `SensitiveDataMaskerTest`
  - Regex-basierte Maskierung
- `DBeaverTableDdlExtractorTest`
  - Fallback-DDL ohne native Table-Instanz

### Chat/LLM-Orchestrierung

- `ChatControllerTest`
  - Ende-zu-Ende Orchestrierung mit Stub-LLM
  - Persistierung USER/ASSISTANT in Session
  - deterministisches Cancel-Verhalten
- `ContextAwarePromptComposerTest`
  - korrekte Prompt-Komposition

### Config

- `AiSettingsTest`
  - Defaulting/Clamping/Normalisierung
- `LlmPayloadLoggerTest`
  - Volltext-Formatierung (SYSTEM/HISTORY/USER/CONTEXT/ASSISTANT)
  - Maskierung sensibler Werte (`authorization`, `apiKey`, `token`)
  - Chunking in `part x/y`

## Manuelle Smoke-Tests (Eclipse Application)

1. View öffnen
   - `Window -> Show View -> Other... -> AI Chat`
   - Alternativ: `Find Actions` (`Cmd+3`) und `Open AI Chat`
   - Alternativ: Shortcut `Cmd+Alt+A`
   - Erwartung: `AI Chat` View sichtbar.

2. Menue/Toolbar-Sichtbarkeit
   - `Window`-Menue pruefen: Eintrag `Open AI Chat` vorhanden.
   - `Window`-Menue pruefen: Eintrag `Ask AI About Selection` vorhanden.
   - `Edit`-Menue pruefen: Eintrag `Ask AI About Selection` vorhanden.
   - Main-Toolbar pruefen: AI-Command-Icon vorhanden.
   - Erwartung: `Open AI Chat` oeffnet die View; `Ask AI About Selection` oeffnet ebenfalls die View und uebernimmt markierten Text.

3. Settings prüfen
   - Preference Page: `ch.so.agi.dbeaver.ai.preferences.main`
   - Erwartung: Base URL / Model / Prompt / Limits editierbar.

4. Secret-Handling
   - API-Token setzen, DBeaver neu starten.
   - Erwartung: Token bleibt im Secret Store erhalten, nicht im Klartext in Preferences.

5. Mention-Flow
   - Eingabe: `Erkläre #<datasource>.<schema>.<table>`
   - Erwartung: Autocomplete-Vorschläge erscheinen.

6. Kontextanreicherung
   - Request mit gültiger Mention senden.
   - Erwartung: DDL + Sample-Rows werden intern in den Prompt übernommen; bei Fehlern erscheinen Warnungen.

7. Streaming + Stop
   - Längere Anfrage senden, dann `Stop`.
   - Erwartung: Stream stoppt, UI wird wieder freigegeben.

8. Logging-Modi
   - Preferences setzen: `LLM Logging = OFF`, Anfrage senden.
   - Erwartung: keine Start/Complete-Infozeilen fuer Request/Response im `Error Log`.
   - Preferences setzen: `LLM Logging = METADATA`, Anfrage senden.
   - Erwartung: kompakte Start/Complete-Metadaten im `Error Log`.
   - Preferences setzen: `LLM Logging = FULL`, Anfrage senden.
   - Erwartung: vollständiger Payload fuer Request/Response im `Error Log` (ggf. in mehreren `part x/y`-Bloecken), sensible Muster maskiert.

9. LangChain HTTP Logging
   - Checkbox `LangChain HTTP Logging (Request/Response)` aktivieren.
   - Erwartung: zusaetzliche LangChain4j-HTTP-Logs je nach Runtime-Logger-Konfiguration; Plugin-eigene Logs bleiben unveraendert.

10. Cache-/Perspektivenfall
   - Wenn View/Command fehlt: DBeaver mit `-clean` starten und Perspektive resetten.
   - Erwartung: `AI Chat` ist danach ueber `Show View` und `Find Actions` wieder auffindbar.

## Bekannte Grenzen der aktuellen Tests

- Keine UI-Bot-Tests (SWT/JFace) enthalten.
- DBeaver-spezifische Resolver-/Collector-Pfade sind primär manuell zu verifizieren, da sie echte DB-Metadaten und ExecutionContext voraussetzen.
