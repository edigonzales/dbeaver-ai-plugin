# DBeaver AI Plugin (`ch.so.agi.dbeaver.ai`)

DBeaver-Plugin für AI-gestützten Chat mit Tabellenkontext.

## Features

- Eigene Chat-View: `ch.so.agi.dbeaver.ai.views.chat`
- Send/Stop/Clear Context direkt in der Chat-View
- `#`-Autocomplete im Chat-Eingabefeld für Referenzen im Format `datasource.schema.table`
- `@sql` für die aktive oder markierte SQL-Query aus dem SQL-Editor
- Mention-Parsing vor dem Senden
- Kontextanreicherung je referenzierter Tabelle:
  - Datenbanktyp (primär Treibername, fallback SQL-Dialect)
  - DDL (primär via DBeaver-DDL-Generator ohne Header, sonst robuster Fallback)
  - Sample-SQL (`SELECT * ... LIMIT n`)
- Sichtbare Warnung bei Token-Budget-Kürzung des Tabellenkontexts
- LLM-Streaming über LangChain4j (`langchain4j` + `langchain4j-open-ai`)
- Konfigurierbarer System-Prompt
- Prompt-Dateien öffnen/speichern (`Open...`, `Save`, `Save As...`)
- Lokales Log aller gesendeten User-Prompts in `~/.dbeaver-ai-messages.log`
- Settings in regulären DBeaver-Preferences
- API-Token in DBeaver Secret Storage (nicht im Klartext in Preferences)

## Installation in DBeaver

1. In DBeaver `Help -> Install New Software...` öffnen.
2. Als Update Site `https://dbeaver.sogeo.services/updates` eintragen.
3. Warten, bis die Site geladen ist, und das AI-Feature auswählen.
4. Den Installationsdialog durchklicken. Je nach DBeaver-/Netzwerkzustand muss dabei häufig mehrmals bestätigt oder erneut auf `Next` bzw. `Finish` geklickt werden, bis alle Artefakte geladen sind.
5. Falls DBeaver vor untrusted Content oder unsignierten Inhalten warnt, der Installation explizit vertrauen.
6. DBeaver nach der Installation neu starten.

## Projektstruktur

- Java-Quellen: `./src/ch/so/agi/dbeaver/ai`
- Tests: `./test/ch/so/agi/dbeaver/ai`
- OSGi/Plugin-Metadaten:
  - `./plugin.xml`
  - `./META-INF/MANIFEST.MF`
  - `./build.properties`

## Build und Test

### Voraussetzungen

- Java 21
- Lokale DBeaver-Installation (Standardpfad auf macOS):
  - `/Applications/DBeaver.app/Contents/Eclipse/plugins`

Optional anderer Plugin-Pfad:

- Gradle Property: `-PdbeaverPluginsDir=/path/to/plugins`
- oder Env-Var: `DBEAVER_PLUGINS_DIR=/path/to/plugins`

### Kommandos

```bash
./gradlew syncBundleLibs
./gradlew printBundleClassPath
./gradlew clean test
```

## Entwicklung in Eclipse (PDE)

Detaillierte Schritt-für-Schritt-Anleitung für Target Platform, Launch Configuration, Runtime-Loop und Troubleshooting:

- [Eclipse Development Guide](docs/ECLIPSE_DEVELOPMENT.md)

Kurz:

1. In Eclipse (RCP/RAP) importieren.
2. DBeaver-Installation als aktive PDE Target Platform setzen.
3. Bei Lib-Änderungen `./gradlew syncBundleLibs` ausführen und danach `PDE Tools -> Update Classpath`.
4. Als `Eclipse Application` mit `org.jkiss.dbeaver.ui.app.standalone.standalone` starten.

## Release / Update Site

Die Veröffentlichung auf der gemeinsamen Update-Site (`ili2pg` + `AI`) ist hier beschrieben:

- [Releasing / Update Site](docs/RELEASING_UPDATE_SITE.md)

## DBeaver Preferences

Preference Page ID: `ch.so.agi.dbeaver.ai.preferences.main`

Alle Settings (UI, Key, Default, Verhalten):

| UI Feld | Key / Secret ID | Default | Bedeutung / Verhalten |
|---|---|---|---|
| Base URL | `ch.so.agi.dbeaver.ai.baseUrl` | `https://api.infomaniak.com/2/ai/103965/openai/v1/` | Basis-URL für den standardmäßig verwendeten Infomaniak OpenAI-kompatiblen Endpoint (LangChain4j OpenAI Client). Leer/Blank fällt auf Default zurück. |
| Model | `ch.so.agi.dbeaver.ai.model` | `qwen3` | Modellname für Chat-Requests. Leer/Blank fällt auf Default zurück. |
| System Prompt | `ch.so.agi.dbeaver.ai.systemPrompt` | Deutsch, SQL als SQL-Codeblock + Erklärung | System-Nachricht, die jedem Request vorangestellt wird. Fokus: anspruchsvolle SQL-Abfragen; Antworten auf Deutsch; SQL zwingend als SQL-Codeblock; pro SQL immer Erklärung. Vollständig durch Benutzer austauschbar. |
| API Token | `ch.so.agi.dbeaver.ai.openai.apiToken` (Secret Store) | kein Wert | Wird **nicht** in Preferences gespeichert, sondern im DBeaver Secret Store. Leeres Token-Feld beim Speichern behält den vorhandenen Secret-Wert. Checkbox "Gespeicherten API Token löschen" entfernt den Secret-Wert. |
| DDL im Kontext mitsenden | `ch.so.agi.dbeaver.ai.includeDdl` | `true` | Wenn aktiv, wird für referenzierte Tabellen DDL in den Prompt aufgenommen. |
| Sample Rows im Kontext mitsenden | `ch.so.agi.dbeaver.ai.includeSampleRows` | `false` | Sichtbar, aber in der UI deaktiviert. Das Plugin sendet derzeit keine Sample Rows an das Modell. Gespeicherte Legacy-`true`-Werte werden ignoriert. |
| Sample Row Limit | `ch.so.agi.dbeaver.ai.sampleRowLimit` | `5` | Aus Kompatibilitätsgründen sichtbar, derzeit aber in der UI deaktiviert und ohne Wirkung auf den gesendeten Sample-Row-Kontext. Werte `<1` werden intern auf `1` angehoben. |
| Max Referenced Tables | `ch.so.agi.dbeaver.ai.maxReferencedTables` | `8` | Max. Anzahl Tabellen, die aus Mentions (`#...`) pro Anfrage aufgelöst und angereichert werden. Werte `<1` werden intern auf `1` angehoben. |
| Max Columns per Sample | `ch.so.agi.dbeaver.ai.maxColumnsPerSample` | `30` | Aus Kompatibilitätsgründen sichtbar, derzeit aber in der UI deaktiviert und ohne Wirkung, solange Sample Rows global abgeschaltet sind. Werte `<1` werden intern auf `1` angehoben. |
| Chat History Size | `ch.so.agi.dbeaver.ai.historySize` | `12` | Anzahl letzter Messages, die zusätzlich zur aktuellen User-Message in den LLM-Request aufgenommen werden. Werte `<0` werden auf `0` gesetzt. |
| Max Context Tokens | `ch.so.agi.dbeaver.ai.maxContextTokens` | `4000` | Token-Budget für den Tabellenkontext. Bei Überschreitung wird Kontext gekürzt und im Chat sichtbar gewarnt. Werte `<100` werden auf `100` angehoben. |
| Autocomplete Candidate Scan Limit | `ch.so.agi.dbeaver.ai.mentionCandidateLimit` | `100000` | Max. Anzahl Tabellen, die beim Metadaten-Scan in den internen `#`-Autocomplete-Katalog geladen werden. Höhere Werte erhöhen Vollständigkeit, können den Initial-Scan aber verlangsamen. Werte `<1` werden auf `1` angehoben. |
| Autocomplete Proposal Limit | `ch.so.agi.dbeaver.ai.mentionProposalLimit` | `40` | Max. Anzahl sichtbarer `#`-Autocomplete-Vorschläge je Schritt. Die Statuszeile zeigt `angezeigt/gefunden` (z.B. `40/312 Treffer`). Werte `<1` werden auf `1` angehoben. |
| Temperature (0.0 - 2.0) | `ch.so.agi.dbeaver.ai.temperature` | `0.0` | Sampling-Temperature für das Modell. Werte werden auf Bereich `[0.0, 2.0]` begrenzt. |
| LLM Logging | `ch.so.agi.dbeaver.ai.llmLogMode` | `METADATA` | Logging-Modus für LLM-Kommunikation im DBeaver Error Log: `OFF` (kein Request/Response-Info-Logging), `METADATA` (nur kompakte Metadaten), `FULL` (vollständige Prompt-/Antwort-Texte). |
| LangChain HTTP Logging (Request/Response) | `ch.so.agi.dbeaver.ai.langchainHttpLogging` | `false` | Schaltet das interne LangChain4j HTTP-Request/Response-Logging ein/aus (`logRequests`/`logResponses`). Sichtbarkeit zusätzlicher Framework-Logs hängt vom Runtime-Logger ab. |

Hinweis:

- Request-Timeout ist derzeit fest auf `90s` implementiert und aktuell **kein** Preference-Setting.
- Vollständige Payload-Logs (`LLM Logging = FULL`) erscheinen im DBeaver `Error Log`.

## Nutzung

1. `AI Chat` öffnen:
   - `Window -> Show View -> Other... -> AI Chat`, oder
   - `Find Actions` (`Cmd+3`) und `Open AI Chat`, oder
   - Shortcut `Cmd+Alt+A`.
2. Optional markierten Text direkt an AI übergeben:
   - `Window -> Ask AI About Selection`, oder
   - `Edit -> Ask AI About Selection`.
3. In Preferences Base URL, Model, System Prompt und API-Token setzen.
4. Chat-Nachricht schreiben, optional mit `#datasource.schema.table` und/oder `@sql`.
5. Antwort wird streaming-basiert im Chat angezeigt; bei gekürztem Tabellenkontext erscheint eine sichtbare Warnung.
6. `Clear Context` leert den sichtbaren Transcript und die Chat-History, ohne den aktuellen Prompt im Eingabefeld zu löschen.

## Hinweise

- `#`-Autocomplete ist in der Chat-View implementiert (nicht im SQL-Editor).
- `#`-Autocomplete arbeitet stufenweise:
  - `#<datasource>` zeigt Datasources
  - `#<datasource>.` zeigt Schemas dieser Datasource
  - `#<datasource>.<schema>.` zeigt Tabellen dieses Schemas
- Mentions mit abschliessendem Punkt werden robust erkannt (z. B. `#db.schema.table.`).
- Verlauf ist aktuell view-lokal (nicht über DBeaver-Neustart persistent).
- Bei unauflösbaren Mentions wird eine Warnung ausgegeben, der Chat läuft weiter.
- Sample Rows werden derzeit nicht an das Modell gesendet; die zugehörigen Settings sind nur noch als deaktivierte Kompatibilitätsfelder sichtbar.
- Gesendete User-Prompts werden lokal in `~/.dbeaver-ai-messages.log` protokolliert.
- Falls Menüeinträge nach Plugin-Update fehlen: DBeaver mit `-clean` starten und Perspektive zurücksetzen.

Details: [Architektur](docs/ARCHITECTURE.md)
