# Releasing / Update Site

Diese Anleitung beschreibt die manuelle Erzeugung der gemeinsamen p2-Update-Site fuer:

- `ch.so.agi.dbeaver.ili2pg.feature`
- `ch.so.agi.dbeaver.ai.feature`

Die fertigen Artefakte (`content.jar`, `artifacts.jar`, `features/`, `plugins/`) werden anschliessend manuell nach `https://dbeaver.sogeo.services/updates/` hochgeladen.

## Einmaliges Setup

1. AI-Feature-Projekt vorhanden:
   - Pfad: `/Users/stefan/sources/dbeaver-ai-feature`
   - Feature-ID: `ch.so.agi.dbeaver.ai.feature`
2. Kategoriendefinition erweitert:
   - Datei: `/Users/stefan/sources/dbeaver-ilitools-feature/category.xml`
   - Kategorien:
     - `interlis` fuer `ili2pg`
     - `ai` fuer `AI`

## Release-Schritte

1. Versionen setzen:
   - AI Plugin `META-INF/MANIFEST.MF`: `Bundle-Version: x.y.z.qualifier`
   - AI Feature `feature.xml`: `version="x.y.z.qualifier"`
   - `ili2pg` nur anheben, wenn dieses Plugin ebenfalls neu ausgeliefert wird
2. Leeres Staging-Verzeichnis verwenden:
   - `/Users/stefan/sources/dbeaver-release-staging/update-input`
3. In Eclipse beide Features in dasselbe Staging-Verzeichnis exportieren:
   - `Deployable Features` fuer `ch.so.agi.dbeaver.ili2pg.feature`
   - `Deployable Features` fuer `ch.so.agi.dbeaver.ai.feature`
4. Staging-Inhalt pruefen:
   - `features/ch.so.agi.dbeaver.ili2pg.feature_<version>.jar`
   - `plugins/ch.so.agi.dbeaver.ili2pg_<version>.jar`
   - `features/ch.so.agi.dbeaver.ai.feature_<version>.jar`
   - `plugins/ch.so.agi.dbeaver.ai_<version>.jar`
5. p2-Repository erzeugen:

```bash
/Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository "file:/tmp/dbeaver-updatesite/" \
  -artifactRepository "file:/tmp/dbeaver-updatesite/" \
  -source "/Users/stefan/sources/dbeaver-release-staging/update-input" \
  -compress \
  -publishArtifacts
```

6. Kategorien anwenden:

```bash
/Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
  -metadataRepository "file:/tmp/dbeaver-updatesite/" \
  -categoryDefinition "file:/Users/stefan/sources/dbeaver-ilitools-feature/category.xml" \
  -categoryQualifier "interlis"
```

7. Ergebnis aus `/tmp/dbeaver-updatesite/` manuell nach `https://dbeaver.sogeo.services/updates/` hochladen.

## Optional: Ablauf ueber Hilfsskript

Statt der beiden Publisher-Kommandos kann das Skript im Plugin-Repo verwendet werden:

```bash
/Users/stefan/sources/dbeaver-ai-plugin/scripts/build-updatesite.sh
```

Defaults:

- Staging: `/Users/stefan/sources/dbeaver-release-staging/update-input`
- Output: `/tmp/dbeaver-updatesite`
- Kategorie-Datei: `/Users/stefan/sources/dbeaver-ilitools-feature/category.xml`

Alternative Pfade:

```bash
/Users/stefan/sources/dbeaver-ai-plugin/scripts/build-updatesite.sh \
  /Users/stefan/sources/dbeaver-release-staging/update-input \
  /tmp/dbeaver-updatesite \
  /Users/stefan/sources/dbeaver-ilitools-feature/category.xml
```

## Abnahmechecks

1. `content.jar` enthaelt:
   - `ch.so.agi.dbeaver.ili2pg`
   - `ch.so.agi.dbeaver.ai`
   - beide `*.feature.feature.group`
2. `artifacts.jar` enthaelt 4 Kernartefakte:
   - 2 Plugins
   - 2 Features
3. In DBeaver erscheinen auf der Site:
   - Kategorie `INTERLIS Tools`
   - Kategorie `AI Tools`
4. Installation testen:
   - nur `ili2pg`
   - nur `AI`
   - beide zusammen
