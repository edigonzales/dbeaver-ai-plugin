# Testing

## Ausführung

Letzter verifizierter Lauf am **2026-03-11**:

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
  - DDL-Kontext, Maskierung sensibler Felder und degradierte DDL-Warnungen
- `ContextAssemblerTest`
  - Token-Budget/Trunkierung
  - Prompt-Block-Reihenfolge
- `SensitiveDataMaskerTest`
  - Regex-basierte Maskierung
- `DBeaverTableDdlExtractorTest`
  - native DDL ohne Header
  - Verwerfen leerer `CREATE TABLE (...)`-Skeletons
  - Fallback-DDL ohne native Table-Instanz

### Chat/LLM-Orchestrierung

- `ChatControllerTest`
  - Ende-zu-Ende Orchestrierung mit Stub-LLM
  - Persistierung USER/ASSISTANT in Session
  - deterministisches Cancel-Verhalten
  - sichtbare Token-Budget-Warnung
- `ContextAwarePromptComposerTest`
  - korrekte Prompt-Komposition
- `ChatSessionTest`
  - `clear()` leert Snapshot und Recent-History

### Config

- `AiSettingsTest`
  - Defaulting/Clamping/Normalisierung
- `AiSettingsServiceTest`
  - Defaults aus Preferences
  - Ignorieren von Legacy-`includeSampleRows=true`
  - Persistierung des hart deaktivierten Sample-Row-Flags
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

2. Menü/Toolbar-Sichtbarkeit
   - `Window`-Menü prüfen: Eintrag `Open AI Chat` vorhanden.
   - `Window`-Menü prüfen: Eintrag `Ask AI About Selection` vorhanden.
   - `Edit`-Menü prüfen: Eintrag `Ask AI About Selection` vorhanden.
   - Main-Toolbar prüfen: AI-Command-Icon vorhanden.
   - Erwartung: `Open AI Chat` öffnet die View; `Ask AI About Selection` öffnet ebenfalls die View und übernimmt markierten Text.

3. Settings prüfen
   - Preference Page: `ch.so.agi.dbeaver.ai.preferences.main`
   - Erwartung: Base URL / Model / Prompt / allgemeine Limits editierbar.
   - Erwartung: `Sample Rows im Kontext mitsenden` sichtbar, aber deaktiviert und auf `false`.
   - Erwartung: `Sample Row Limit` und `Max Columns per Sample` sichtbar, aber deaktiviert.
   - Erwartung: Hinweis sichtbar, dass Sample Rows derzeit nicht an das Modell gesendet werden.

4. Secret-Handling
   - API-Token setzen, DBeaver neu starten.
   - Erwartung: Token bleibt im Secret Store erhalten, nicht im Klartext in Preferences.

5. Mention-Flow
   - Eingabe: `Erkläre #<datasource>.<schema>.<table>`
   - Erwartung: Autocomplete-Vorschläge erscheinen.

6. Kontextanreicherung
   - Request mit gültiger Mention senden.
   - Erwartung: DDL und Sample Query werden intern in den Prompt übernommen; der `Sample Rows`-Abschnitt bleibt leer.
   - Bei degradierter DDL oder Resolver-/Collector-Fehlern erscheinen Warnungen im Chat.
   - Wenn `maxContextTokens` künstlich klein gesetzt ist, erscheint zusätzlich eine sichtbare Trunkierungswarnung.

7. Streaming + Stop
   - Längere Anfrage senden, dann `Stop`.
   - Erwartung: Stream stoppt, UI wird wieder freigegeben.
   - Nach `Send` ohne sofortige Token-Antwort auf den Chat schauen.
   - Erwartung: Ein sichtbarer Busy-Strip erscheint, `Send` zeigt `Working...` und im Transcript steht direkt `AI> ...`.
   - Sobald die ersten Tokens eintreffen oder der Run beendet/gestoppt wird, erneut prüfen.
   - Erwartung: Der Platzhalter wird durch die echte Antwort ersetzt bzw. bei Fehler/Stop entfernt; der Busy-Strip verschwindet.
   - Nach einigen Chat-Turns `Clear Context` klicken.
   - Erwartung: Der sichtbare Transcript wird auf die initiale Hinweiszeile zurückgesetzt, das Prompt-Feld bleibt unverändert und Folgeanfragen enthalten keine alte Chat-History mehr.
   - Während eines laufenden Streams `Clear Context` klicken.
   - Erwartung: Der Stream stoppt, der Transcript bleibt geleert und es erscheinen keine verspäteten Chunks der abgebrochenen Antwort mehr.

8. Prompt-Editor / Splitter
   - Chat-View vertikal aufziehen und den Trenner zwischen Transcript und Prompt verschieben.
   - Erwartung: Beide Bereiche lassen sich frei in der Höhe anpassen.
   - View schliessen und erneut öffnen.
   - Erwartung: Die zuletzt gewählte Aufteilung bleibt erhalten.

9. Prompt-Dateien
   - In der View `Open...` verwenden und eine Textdatei laden.
   - Erwartung: Der Dateiinhalt erscheint im Prompt-Feld, die View zeigt den Dateinamen an.
   - Prompt ändern und `Save` ausführen.
   - Erwartung: Die Datei wird aktualisiert, Dirty-Markierung verschwindet.
   - Bei ungebundenem Prompt `Save` ausführen.
   - Erwartung: `Save As...` wird geöffnet und speichert den Prompt als neue Datei.
   - Prompt A speichern, senden und danach den Editorzustand prüfen.
   - Erwartung: Das Prompt-Feld ist leer, ohne Dirty-Markierung und wieder ein untitled/ungebundener Draft.
   - Neuen Prompt B schreiben und `Save` ausführen.
   - Erwartung: `Save As...` wird erneut geöffnet; die früher geöffnete Draft-Datei wird nicht automatisch weiterverwendet.

10. Nachrichten-Log
   - Mehrere Prompts senden.
   - Erwartung: `~/.dbeaver-ai-messages.log` wird angelegt und enthält jede gesendete User-Nachricht als eigenen `USER MESSAGE`-Block.
   - Dieselbe Nachricht zweimal senden.
   - Erwartung: Beide Nachrichten erscheinen separat im Log, nichts wird ersetzt oder dedupliziert.
   - Einen Prompt mit `@sql` senden.
   - Erwartung: Im Log steht der rohe User-Prompt mit `@sql`, nicht der expandierte SQL-Kontext.

11. Dirty-Schutz
   - Einen geladenen oder neuen Prompt ändern, ohne zu speichern.
   - `Open...` oder `Ask AI About Selection` auslösen.
   - Erwartung: Dialog mit `Save`, `Verwerfen`, `Abbrechen`.
   - Nach erfolgreichem `Send` prüfen.
   - Erwartung: Das Prompt-Feld ist leer und ohne Dirty-Markierung zurückgesetzt; die frühere Dateibindung ist gelöst.

12. Logging-Modi
   - Preferences setzen: `LLM Logging = OFF`, Anfrage senden.
   - Erwartung: keine Start/Complete-Infozeilen für Request/Response im `Error Log`.
   - Preferences setzen: `LLM Logging = METADATA`, Anfrage senden.
   - Erwartung: kompakte Start/Complete-Metadaten im `Error Log`.
   - Preferences setzen: `LLM Logging = FULL`, Anfrage senden.
   - Erwartung: vollständiger Payload für Request/Response im `Error Log` (ggf. in mehreren `part x/y`-Blöcken), sensible Muster maskiert.

13. LangChain HTTP Logging
   - Checkbox `LangChain HTTP Logging (Request/Response)` aktivieren.
   - Erwartung: zusätzliche LangChain4j-HTTP-Logs je nach Runtime-Logger-Konfiguration; Plugin-eigene Logs bleiben unverändert.

14. Cache-/Perspektivenfall
   - Wenn View/Command fehlt: DBeaver mit `-clean` starten und Perspektive resetten.
   - Erwartung: `AI Chat` ist danach über `Show View` und `Find Actions` wieder auffindbar.

15. `@sql`-Injection
   - Im SQL-Editor eine Query markieren und im AI Chat einen Prompt mit `@sql` senden.
   - Erwartung: Die Anfrage läuft mit der markierten SQL als zusätzlichem Prompt-Abschnitt; im sichtbaren Chat bleibt der rohe User-Text mit `@sql`.
   - Ohne markierte Query, aber mit Cursor in einer aktiven Query, erneut `@sql` senden.
   - Erwartung: Die Query unter dem Cursor wird verwendet.
   - `@sql` ohne aktiven SQL-Editor oder ohne extrahierbare Query senden.
   - Erwartung: Der Request läuft weiter und im Chat erscheint nur eine Warnung.

## Bekannte Grenzen der aktuellen Tests

- Keine UI-Bot-Tests (SWT/JFace) enthalten.
- DBeaver-spezifische Resolver-/Collector-Pfade sind primär manuell zu verifizieren, da sie echte DB-Metadaten und ExecutionContext voraussetzen.
