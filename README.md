# DBeaver AI Plugin (`ch.so.agi.dbeaver.ai`)

DBeaver-Plugin für AI-gestützten Chat mit Tabellenkontext.

## Features

- Eigene Chat-View: `ch.so.agi.dbeaver.ai.views.chat`
- `#`-Autocomplete im Chat-Eingabefeld für Referenzen im Format `datasource.schema.table`
- Mention-Parsing vor dem Senden
- Kontextanreicherung je referenzierter Tabelle:
  - DDL (primär via DBeaver-DDL-Generator, sonst Fallback)
  - Sample-SQL (`SELECT * ... LIMIT n`)
  - Sample-Rows (limitiert, mit Maskierung sensibler Spalten)
- LLM-Streaming über LangChain4j (`langchain4j` + `langchain4j-open-ai`)
- Konfigurierbarer System-Prompt
- Settings in regulären DBeaver-Preferences
- API-Token in DBeaver Secret Storage (nicht im Klartext in Preferences)

## Projektstruktur

- Java-Quellen: `./dbeaver-ai-plugin/src/ch/so/agi/dbeaver/ai`
- Tests: `./dbeaver-ai-plugin/test/ch/so/agi/dbeaver/ai`
- OSGi/Plugin-Metadaten:
  - `./dbeaver-ai-plugin/plugin.xml`
  - `./dbeaver-ai-plugin/META-INF/MANIFEST.MF`
  - `./dbeaver-ai-plugin/build.properties`

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

Detaillierte Schritt-fuer-Schritt-Anleitung fuer Target Platform, Launch Configuration, Runtime-Loop und Troubleshooting:

- [Eclipse Development Guide](docs/ECLIPSE_DEVELOPMENT.md)

Kurz:

1. In Eclipse (RCP/RAP) importieren.
2. DBeaver-Installation als aktive PDE Target Platform setzen.
3. Bei Lib-Aenderungen `./gradlew syncBundleLibs` ausfuehren und danach `PDE Tools -> Update Classpath`.
4. Als `Eclipse Application` mit `org.jkiss.dbeaver.ui.app.standalone.standalone` starten.

## DBeaver Preferences

Preference Page ID: `ch.so.agi.dbeaver.ai.preferences.main`

Alle Settings (UI, Key, Default, Verhalten):

| UI Feld | Key / Secret ID | Default | Bedeutung / Verhalten |
|---|---|---|---|
| Base URL | `ch.so.agi.dbeaver.ai.baseUrl` | `https://api.openai.com/v1` | Basis-URL fuer den OpenAI-kompatiblen Endpoint (LangChain4j OpenAI Client). Leer/Blank faellt auf Default zurueck. |
| Model | `ch.so.agi.dbeaver.ai.model` | `gpt-4o-mini` | Modellname fuer Chat-Requests. Leer/Blank faellt auf Default zurueck. |
| System Prompt | `ch.so.agi.dbeaver.ai.systemPrompt` | Deutsch, SQL als SQL-Codeblock + Erklaerung | System-Nachricht, die jedem Request vorangestellt wird. Fokus: anspruchsvolle SQL-Abfragen; Antworten auf Deutsch; SQL zwingend als SQL-Codeblock; pro SQL immer Erklaerung. Vollstaendig durch Benutzer austauschbar. |
| OpenAI API Token | `ch.so.agi.dbeaver.ai.openai.apiToken` (Secret Store) | kein Wert | Wird **nicht** in Preferences gespeichert, sondern im DBeaver Secret Store. Leeres Token-Feld beim Speichern behaelt den vorhandenen Secret-Wert. Checkbox "Gespeicherten API Token loeschen" entfernt den Secret-Wert. |
| DDL im Kontext mitsenden | `ch.so.agi.dbeaver.ai.includeDdl` | `true` | Wenn aktiv, wird fuer referenzierte Tabellen DDL in den Prompt aufgenommen. |
| Sample Rows im Kontext mitsenden | `ch.so.agi.dbeaver.ai.includeSampleRows` | `true` | Wenn aktiv, werden Beispielzeilen (`SELECT * ... LIMIT n`) in den Prompt aufgenommen. |
| Sample Row Limit | `ch.so.agi.dbeaver.ai.sampleRowLimit` | `5` | Max. Zeilen pro referenzierter Tabelle fuer Sample Rows. Werte `<1` werden intern auf `1` angehoben. |
| Max Referenced Tables | `ch.so.agi.dbeaver.ai.maxReferencedTables` | `8` | Max. Anzahl Tabellen, die aus Mentions (`#...`) pro Anfrage aufgeloest und angereichert werden. Werte `<1` werden intern auf `1` angehoben. |
| Max Columns per Sample | `ch.so.agi.dbeaver.ai.maxColumnsPerSample` | `30` | Max. Spaltenanzahl pro Sample-Row-Ausgabe im Prompt. Werte `<1` werden intern auf `1` angehoben. |
| Chat History Size | `ch.so.agi.dbeaver.ai.historySize` | `12` | Anzahl letzter Messages, die zusaetzlich zur aktuellen User-Message in den LLM-Request aufgenommen werden. Werte `<0` werden auf `0` gesetzt. |
| Max Context Tokens | `ch.so.agi.dbeaver.ai.maxContextTokens` | `4000` | Token-Budget fuer den Tabellenkontext (DDL + Samples). Bei Ueberschreitung wird Kontext gekuerzt. Werte `<100` werden auf `100` angehoben. |
| Autocomplete Proposal Limit | `ch.so.agi.dbeaver.ai.mentionProposalLimit` | `40` | Max. Anzahl sichtbarer `#`-Autocomplete-Vorschlaege je Schritt. Die Statuszeile zeigt `angezeigt/gefunden` (z.B. `40/312 Treffer`). Werte `<1` werden auf `1` angehoben. |
| Temperature (0.0 - 2.0) | `ch.so.agi.dbeaver.ai.temperature` | `0.0` | Sampling-Temperature fuer das Modell. Werte werden auf Bereich `[0.0, 2.0]` begrenzt. |

Hinweis:

- Request-Timeout ist derzeit fest auf `90s` implementiert und aktuell **kein** Preference-Setting.

## Nutzung

1. `AI Chat` oeffnen:
   - `Window -> Show View -> Other... -> AI Chat`, oder
   - `Find Actions` (`Cmd+3`) und `Open AI Chat`, oder
   - Shortcut `Cmd+Alt+A`.
2. Optional markierten Text direkt an AI uebergeben:
   - `Window -> Ask AI About Selection`, oder
   - `Edit -> Ask AI About Selection`.
3. In Preferences Base URL, Model, System Prompt und API-Token setzen.
4. Chat-Nachricht schreiben, optional mit `#datasource.schema.table`.
5. Antwort wird streaming-basiert im Chat angezeigt.

## Hinweise

- `#`-Autocomplete ist in der Chat-View implementiert (nicht im SQL-Editor).
- `#`-Autocomplete arbeitet stufenweise:
  - `#<datasource>` zeigt Datasources
  - `#<datasource>.` zeigt Schemas dieser Datasource
  - `#<datasource>.<schema>.` zeigt Tabellen dieses Schemas
- Verlauf ist aktuell view-lokal (nicht über DBeaver-Neustart persistent).
- Bei unauflösbaren Mentions wird eine Warnung ausgegeben, der Chat läuft weiter.
- Falls Menueeintraege nach Plugin-Update fehlen: DBeaver mit `-clean` starten und Perspektive zuruecksetzen.

Details: [Architektur](docs/ARCHITECTURE.md)
