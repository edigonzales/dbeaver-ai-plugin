# Architektur

## Überblick

Das Plugin `ch.so.agi.dbeaver.ai` ist in 6 Schichten gegliedert:

1. `ui`
2. `chat`
3. `mention`
4. `context`
5. `llm`
6. `config`

## Komponenten

## UI (`ch.so.agi.dbeaver.ai.ui`)

- `AiChatViewPart`
  - Chat-Transcript
  - Eingabe, Send/Stop/Clear Context
  - `#`-Autocomplete
  - `@sql`-Prompt-Erweiterung aus dem aktiven SQL-Editor
  - Prompt-Dateiaktionen (`Open...`, `Save`, `Save As...`)
  - Busy-Indikator
  - Statusausgabe/Warnungen
- `OpenAiChatViewHandler`
- `AskWithSelectionHandler`

## Chat-Orchestrierung (`ch.so.agi.dbeaver.ai.chat`)

- `ChatController`
  - Mention extrahieren
  - Kontext aufbauen
  - Prompt komponieren
  - Streaming-LLM aufrufen
  - sichtbare Warnung bei Token-Budget-Trunkierung emittieren
- `ChatSession`
  - lokale Historie (USER/ASSISTANT)
  - `clear()` fuer expliziten Kontext-Reset

## Mentions (`ch.so.agi.dbeaver.ai.mention`)

- `MentionTriggerDetector`
- `MentionParser`
- `MentionProposalProvider`
- `DBeaverMentionCatalog`

Mention-Format: `#datasource.schema.table`

## Kontext (`ch.so.agi.dbeaver.ai.context`)

- `DBeaverTableReferenceResolver`
  - Referenz -> DBSEntity/ExecutionContext
- `DBeaverTableDdlExtractor`
  - native DDL via `DBStructUtils.generateObjectDDL/getTableDDL` ohne DBeaver-Header
  - verwirft strukturell leere `CREATE TABLE (...)`-Skeletons
  - Fallback auf metadatenbasiertes `CREATE TABLE ...`
- `DBeaverSampleRowsCollector`
  - Sample-Abfrage + `setLimit`
- `ContextEnricher`
  - DDL und Sample-Query sammeln
  - Sample-Row-Sammlung bleibt vorhanden, ist produktseitig aber global deaktiviert
  - sensible Felder maskieren
  - Warnungen fuer degradierte DDL-Fallbacks
- `ContextAssembler`
  - Prompt-Block erstellen
  - Token-Budget anwenden

## LLM (`ch.so.agi.dbeaver.ai.llm`)

- `LlmClient` (Abstraktion)
- `LangChain4jOpenAiClient`
  - OpenAI-kompatibler Streaming-Client
- `ContextAwarePromptComposer`

## Konfiguration (`ch.so.agi.dbeaver.ai.config`)

- `AiPreferenceInitializer`
- `AiPreferencePageMain`
- `AiSettingsService`
- `AiSettings`

System-Prompt und Limits in Preferences, API-Token im Secret Storage.

## Datenfluss (Request)

1. User sendet Nachricht in `AiChatViewPart`.
2. `ChatController` extrahiert Mentions.
3. Resolver löst Tabellen auf.
4. DDL und Sample-Query werden gesammelt; Sample Rows werden derzeit nicht an das Modell gesendet.
5. Kontext wird token-basiert gekuerzt; Trunkierung wird im Prompt vermerkt und als Warnung in der UI angezeigt.
6. Prompt wird strukturiert kombiniert: Nutzeranfrage + Arbeitsauftrag + Tabellenkontext.
7. `LangChain4jOpenAiClient` streamt Antwort.
8. UI zeigt Chunks live an, speichert finalen Verlauf.

## Prompt Pipeline

1. Mention-Parsing identifiziert `#datasource.schema.table`-Referenzen aus der User-Nachricht.
2. Kontextaufbau sammelt pro referenzierter Tabelle DDL und Sample-Query.
   - die Sample-Row-Sammlung ist technisch weiter vorhanden, wird aber derzeit nicht in Requests aufgenommen.
   - zusätzlich wird der Datenbanktyp je Datasource ermittelt (Treibername, fallback Dialect).
3. Budgeting kuerzt den Tabellenkontext bei Bedarf ueber `maxContextTokens`.
   - die UI zeigt dafuer eine explizite Warnung, und der Prompt-Block enthaelt eine Truncation-Note.
4. `ContextAwarePromptComposer` baut den finalen User-Prompt in Abschnitten auf:
   - `## Nutzeranfrage`
   - `## Arbeitsauftrag`
   - `## Tabellenkontext (automatisch aus der Datenbank extrahiert)`
5. Die aktuelle User-Nachricht wird genau einmal gesendet:
   - als `userPrompt` im aktuellen Request
   - nicht zusaetzlich in `history` desselben Requests

## Logging-Pipeline

- Logging ist ueber Preferences steuerbar:
  - `LLM Logging = OFF | METADATA | FULL`
  - `LangChain HTTP Logging (Request/Response) = true/false`
- `METADATA` schreibt kompakte Start/Ende-Infos (Modell, Groessen, Dauer) ins DBeaver `Error Log`.
- `FULL` schreibt zusaetzlich vollstaendige Prompt-/Antwort-Payloads (SYSTEM/HISTORY/USER/CONTEXT/ASSISTANT) ins DBeaver `Error Log`.
- Lange Payloads werden in `part x/y`-Bloecke gesplittet.
- Potenziell sensible Muster (`authorization`, `apiKey`, `token`) werden vor dem Volltext-Logging maskiert.
- LangChain4j-HTTP-Logging (`logRequests`/`logResponses`) ist separat schaltbar und ergaenzt nur Framework-seitige Logs.

## Fehlerpfade

- Mention unauflösbar: Warnung im Chat, Request läuft weiter.
- DDL-/Kontextfehler pro Tabelle: Warnung, Restkontext bleibt nutzbar.
- native DDL unbrauchbar oder leer: Fallback auf `getTableDDL`, danach ggf. Metadaten-DDL.
- API-Token fehlt: Request wird vor dem Senden abgebrochen.
- LLM-Fehler/Timeout: UI zeigt Fehler, Send ist wieder aktiv.

## Erweiterungspunkte

- Weitere LLM-Provider via `LlmClient`
- Persistente Chat-History
- Selektives Tabellen-Sampling (nur benötigte Spalten)
- SQL-Editor-Integration für Mentions/Chat-Aktionen
