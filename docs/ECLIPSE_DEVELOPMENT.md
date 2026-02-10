# Eclipse-Entwicklung (PDE) fuer `ch.so.agi.dbeaver.ai`

Dieses Projekt ist ein Eclipse/PDE-Plugin-Projekt mit zusaetzlichem Gradle-Build fuer Tests und das Kopieren der Runtime-Libraries.

## Ziel

Moeglichst schnell entwickeln, starten, testen:

1. Code in Eclipse aendern.
2. DBeaver als `Eclipse Application` direkt aus Eclipse starten.
3. Plugin ohne manuelles Deploy im gestarteten Runtime-Workbench pruefen.

## Voraussetzungen

- Eclipse IDE for RCP and RAP Developers
- Java 21
- Lokale DBeaver-Installation (macOS Standard):
  - `/Applications/DBeaver.app/Contents/Eclipse`

## 1. Projekt in Eclipse importieren

Empfohlen:

1. `File -> Import -> Gradle -> Existing Gradle Project`
2. Projektordner waehlen: `/Users/stefan/sources/dbeaver-ai-plugin`

Hinweis:

- Das Laufzeitmodell bleibt PDE/OSGi.
- Gradle wird hier vor allem fuer `test` und `syncBundleLibs` genutzt.

## 2. Target Platform einrichten (wichtig)

1. `Settings/Preferences -> Plug-in Development -> Target Platform`
2. `Add... -> Installation`
3. Als Root die DBeaver-Eclipse-Installation waehlen:
   - `/Applications/DBeaver.app/Contents/Eclipse`
4. Die neue Target Platform aktiv setzen (`Set as Active`).

Alternative:

- Falls noetig nur das `plugins`-Verzeichnis waehlen:
  - `/Applications/DBeaver.app/Contents/Eclipse/plugins`

## 3. Third-Party Libraries synchronisieren

Wenn Abhaengigkeiten geaendert werden oder `lib/` fehlt:

```bash
./gradlew syncBundleLibs
```

Danach in Eclipse:

1. Rechtsklick auf Projekt
2. `PDE Tools -> Update Classpath`

Wichtig fuer OSGi Runtime:

- `META-INF/MANIFEST.MF` muss alle Runtime-JARs im `Bundle-ClassPath` enthalten.
- `build.properties` muss `lib/` in `bin.includes` enthalten.

Im aktuellen Projekt ist das vorbereitet; `syncBundleLibs` aktualisiert den `lib/`-Inhalt.

## 4. Launch Configuration fuer schnelles Feedback

1. `Run -> Run Configurations...`
2. Neue `Eclipse Application` anlegen, z.B. `DBeaver AI Runtime`
3. Tab `Main`:
   - `Run an application`: `org.jkiss.dbeaver.ui.app.standalone.standalone`
   - `Location` (Workspace/Data):
     - Sicher fuer Tests: eigener Temp-Workspace, z.B. `/tmp/dbeaver-ai-runtime-workspace`
     - Oder bestaender DBeaver-Workspace, z.B. `/Users/stefan/Library/DBeaverData/workspace6`
4. Tab `Plug-ins`:
   - `Launch with: all workspace and enabled target plug-ins`
5. Tab `Arguments` (empfohlen):
   - Program arguments: `-clean -consoleLog`

Dann `Run`.

## 5. Taeglicher Entwicklungs-Loop

1. Code aendern.
2. Falls Dependencies geaendert: `./gradlew syncBundleLibs` + `PDE Tools -> Update Classpath`.
3. Launch Config starten.
4. In Runtime-DBeaver pruefen:
   - Command `Open AI Chat`
   - Preferences `AI Chat`
   - Chat senden, `#datasource.schema.table` testen
   - `Stop` testen

## 6. Troubleshooting

## Unresolved bundles / ClassNotFound / NoClassDefFoundError

- Target Platform aktiv?
- `./gradlew syncBundleLibs` ausgefuehrt?
- `Bundle-ClassPath` in `META-INF/MANIFEST.MF` vollstaendig?
- `build.properties` enthaelt `lib/`?
- `PDE Tools -> Update Classpath` ausgefuehrt?

## Plugin startet, aber Commands/View fehlen

- `plugin.xml` IDs/Classes pruefen.
- Sicherstellen, dass die Klassen wirklich im Output/JAR liegen.

## Runtime-Workspace gesperrt

- Wenn der normale DBeaver bereits laeuft, denselben Workspace nicht parallel verwenden.
- Fuer Entwicklungslaeufe besser separaten Workspace nutzen.

## Dropins funktioniert nicht

In der aktuellen DBeaver-Konfiguration wird `dropins` nicht automatisch reconciled. Fuer die Entwicklung ist das egal, weil die PDE-Runtime direkt aus Eclipse startet.

## 7. Hinweis zu custom Download-Tasks (wie bei `ili2pg`)

Dein bisheriger Ansatz mit einem Gradle-Task wie `downloadAndExtractIli2pg` ist gueltig. Pattern:

1. Artefakt downloaden
2. Entpacken
3. benoetigte JARs nach `lib/` kopieren
4. `MANIFEST.MF` + `build.properties` konsistent halten

Das aktuelle Projekt nutzt fuer Standard-Abhaengigkeiten den einfacheren Weg ueber Maven + `syncBundleLibs`.
