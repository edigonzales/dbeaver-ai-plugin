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
   - Erwartung: Beide Bereiche lassen sich frei in der Hoehe anpassen.
   - View schliessen und erneut oeffnen.
   - Erwartung: Die zuletzt gewaehlte Aufteilung bleibt erhalten.

9. Prompt-Dateien
   - In der View `Open...` verwenden und eine Textdatei laden.
   - Erwartung: Der Dateiinhalt erscheint im Prompt-Feld, die View zeigt den Dateinamen an.
   - Prompt aendern und `Save` ausfuehren.
   - Erwartung: Die Datei wird aktualisiert, Dirty-Markierung verschwindet.
   - Bei ungebundenem Prompt `Save` ausfuehren.
   - Erwartung: `Save As...` wird geoeffnet und speichert den Prompt als neue Datei.
   - Prompt A speichern, senden und danach den Editorzustand pruefen.
   - Erwartung: Das Prompt-Feld ist leer, ohne Dirty-Markierung und wieder ein untitled/ungebundener Draft.
   - Neuen Prompt B schreiben und `Save` ausfuehren.
   - Erwartung: `Save As...` wird erneut geoeffnet; die frueher geoeffnete Draft-Datei wird nicht automatisch weiterverwendet.

10. Nachrichten-Log
   - Mehrere Prompts senden.
   - Erwartung: `~/.dbeaver-ai-messages.log` wird angelegt und enthaelt jede gesendete User-Nachricht als eigenen `USER MESSAGE`-Block.
   - Dieselbe Nachricht zweimal senden.
   - Erwartung: Beide Nachrichten erscheinen separat im Log, nichts wird ersetzt oder dedupliziert.
   - Einen Prompt mit `@sql` senden.
   - Erwartung: Im Log steht der rohe User-Prompt mit `@sql`, nicht der expandierte SQL-Kontext.

11. Dirty-Schutz
   - Einen geladenen oder neuen Prompt aendern, ohne zu speichern.
   - `Open...` oder `Ask AI About Selection` ausloesen.
   - Erwartung: Dialog mit `Save`, `Verwerfen`, `Abbrechen`.
   - Nach erfolgreichem `Send` pruefen.
   - Erwartung: Das Prompt-Feld ist leer und ohne Dirty-Markierung zurueckgesetzt; die fruehere Dateibindung ist geloest.

12. Logging-Modi
   - Preferences setzen: `LLM Logging = OFF`, Anfrage senden.
   - Erwartung: keine Start/Complete-Infozeilen fuer Request/Response im `Error Log`.
   - Preferences setzen: `LLM Logging = METADATA`, Anfrage senden.
   - Erwartung: kompakte Start/Complete-Metadaten im `Error Log`.
   - Preferences setzen: `LLM Logging = FULL`, Anfrage senden.
   - Erwartung: vollständiger Payload fuer Request/Response im `Error Log` (ggf. in mehreren `part x/y`-Bloecken), sensible Muster maskiert.

13. LangChain HTTP Logging
   - Checkbox `LangChain HTTP Logging (Request/Response)` aktivieren.
   - Erwartung: zusaetzliche LangChain4j-HTTP-Logs je nach Runtime-Logger-Konfiguration; Plugin-eigene Logs bleiben unveraendert.

14. Cache-/Perspektivenfall
   - Wenn View/Command fehlt: DBeaver mit `-clean` starten und Perspektive resetten.
   - Erwartung: `AI Chat` ist danach ueber `Show View` und `Find Actions` wieder auffindbar.

15. `@sql`-Injection
   - Im SQL-Editor eine Query markieren und im AI Chat einen Prompt mit `@sql` senden.
   - Erwartung: Die Anfrage laeuft mit der markierten SQL als zusaetzlichem Prompt-Abschnitt; im sichtbaren Chat bleibt der rohe User-Text mit `@sql`.
   - Ohne markierte Query, aber mit Cursor in einer aktiven Query, erneut `@sql` senden.
   - Erwartung: Die Query unter dem Cursor wird verwendet.
   - `@sql` ohne aktiven SQL-Editor oder ohne extrahierbare Query senden.
   - Erwartung: Der Request laeuft weiter und im Chat erscheint nur eine Warnung.

## Bekannte Grenzen der aktuellen Tests

- Keine UI-Bot-Tests (SWT/JFace) enthalten.
- DBeaver-spezifische Resolver-/Collector-Pfade sind primär manuell zu verifizieren, da sie echte DB-Metadaten und ExecutionContext voraussetzen.
