# Eclipse-Entwicklung (PDE) für `ch.so.agi.dbeaver.ai`

Dieses Projekt ist ein Eclipse/PDE-Plugin-Projekt mit zusätzlichem Gradle-Build für Tests und das Kopieren der Runtime-Libraries.

## Ziel

Möglichst schnell entwickeln, starten, testen:

1. Code in Eclipse ändern.
2. DBeaver als `Eclipse Application` direkt aus Eclipse starten.
3. Plugin ohne manuelles Deploy im gestarteten Runtime-Workbench prüfen.

## Voraussetzungen

- Eclipse IDE for RCP and RAP Developers
- Java 21
- Lokale DBeaver-Installation (macOS Standard):
  - `/Applications/DBeaver.app/Contents/Eclipse`

## 1. Projekt in Eclipse importieren

Empfohlen:

1. `File -> Import -> Gradle -> Existing Gradle Project`
2. Projektordner wählen: `/Users/stefan/sources/dbeaver-ai-plugin`

Hinweis:

- Das Laufzeitmodell bleibt PDE/OSGi.
- Gradle wird hier vor allem für `test` und `syncBundleLibs` genutzt.

## 2. Target Platform einrichten (wichtig)

1. `Settings/Preferences -> Plug-in Development -> Target Platform`
2. `Add... -> Installation`
3. Als Root die DBeaver-Eclipse-Installation wählen:
   - `/Applications/DBeaver.app/Contents/Eclipse`
4. Die neue Target Platform aktiv setzen (`Set as Active`).

Alternative:

- Falls nötig nur das `plugins`-Verzeichnis wählen:
  - `/Applications/DBeaver.app/Contents/Eclipse/plugins`

## 3. Third-Party Libraries synchronisieren

Wenn Abhängigkeiten geändert werden oder `lib/` fehlt:

```bash
./gradlew syncBundleLibs
```

Danach in Eclipse:

1. Rechtsklick auf Projekt
2. `PDE Tools -> Update Classpath`

Wichtig für OSGi Runtime:

- `META-INF/MANIFEST.MF` muss alle Runtime-JARs im `Bundle-ClassPath` enthalten.
- `build.properties` muss `lib/` in `bin.includes` enthalten.

Im aktuellen Projekt ist das vorbereitet; `syncBundleLibs` aktualisiert den `lib/`-Inhalt.

## 4. Launch Configuration für schnelles Feedback

1. `Run -> Run Configurations...`
2. Neue `Eclipse Application` anlegen, z.B. `DBeaver AI Runtime`
3. Tab `Main`:
   - `Run an application`: `org.jkiss.dbeaver.ui.app.standalone.standalone`
   - `Location` (Workspace/Data):
     - Sicher für Tests: eigener Temp-Workspace, z.B. `/tmp/dbeaver-ai-runtime-workspace`
     - Oder bestaender DBeaver-Workspace, z.B. `/Users/stefan/Library/DBeaverData/workspace6`
4. Tab `Plug-ins`:
   - `Launch with: all workspace and enabled target plug-ins`
5. Tab `Arguments` (empfohlen):
   - Program arguments: `-clean -consoleLog`

Dann `Run`.

## 5. Täglicher Entwicklungs-Loop

1. Code ändern.
2. Falls Dependencies geändert: `./gradlew syncBundleLibs` + `PDE Tools -> Update Classpath`.
3. Launch Config starten.
4. In Runtime-DBeaver prüfen:
   - Command `Open AI Chat`
   - Preferences `AI Chat`
   - Chat senden, `#datasource.schema.table` testen
   - `Stop` testen

## 6. Troubleshooting

## Unresolved bundles / ClassNotFound / NoClassDefFoundError

- Target Platform aktiv?
- `./gradlew syncBundleLibs` ausgeführt?
- `Bundle-ClassPath` in `META-INF/MANIFEST.MF` vollständig?
- `build.properties` enthält `lib/`?
- `PDE Tools -> Update Classpath` ausgeführt?

## Plugin startet, aber Commands/View fehlen

- `plugin.xml` IDs/Classes prüfen.
- Sicherstellen, dass die Klassen wirklich im Output/JAR liegen.

## Runtime-Workspace gesperrt

- Wenn der normale DBeaver bereits läuft, denselben Workspace nicht parallel verwenden.
- Für Entwicklungsläufe besser separaten Workspace nutzen.

## Dropins funktioniert nicht

In der aktuellen DBeaver-Konfiguration wird `dropins` nicht automatisch reconciled. Für die Entwicklung ist das egal, weil die PDE-Runtime direkt aus Eclipse startet.

## 7. Hinweis zu custom Download-Tasks (wie bei `ili2pg`)

Dein bisheriger Ansatz mit einem Gradle-Task wie `downloadAndExtractIli2pg` ist gültig. Pattern:

1. Artefakt downloaden
2. Entpacken
3. benötigte JARs nach `lib/` kopieren
4. `MANIFEST.MF` + `build.properties` konsistent halten

Das aktuelle Projekt nutzt für Standard-Abhängigkeiten den einfacheren Weg über Maven + `syncBundleLibs`.
