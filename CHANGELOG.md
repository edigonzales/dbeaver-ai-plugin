# Changelog

## Unreleased

### Added

- Initial OSGi plugin scaffold for `ch.so.agi.dbeaver.ai`
- Chat view (`ch.so.agi.dbeaver.ai.views.chat`) with Send/Stop and streaming output
- Commands:
  - `ch.so.agi.dbeaver.ai.commands.openChat`
  - `ch.so.agi.dbeaver.ai.commands.askWithSelection`
- Mention system for `#datasource.schema.table`
  - trigger detection
  - parser with quoted identifier support
  - autocomplete proposal provider
- DBeaver context pipeline
  - table reference resolver
  - DDL extractor with fallback
  - sample row collector with query limits
  - context assembly and token-budget truncation
  - sensitive data masking
- LangChain4j integration (`1.11.0`) with OpenAI-compatible streaming client
- Preference page and default initializer
- API token persistence through DBeaver secret storage
- Gradle build with `syncBundleLibs` and `printBundleClassPath`
- Unit test suite for parser/context/chat/prompt/config modules
- Documentation:
  - README
  - architecture doc
  - testing doc
  - Eclipse PDE development/setup guide
