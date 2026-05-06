# UX Writing Guidelines

These guidelines ensure clarity, consistency, and user support across the HelloDATA portal interface.

## Language & Tone

- **Language**: German (Swiss), key `de_CH`
- **Tone**: Professional, clear, and polite — use "Sie" (formal address)
- **Voice**: Active voice preferred; passive only when the actor is irrelevant
- **Brevity**: Keep UI text short. Prefer single sentences over paragraphs

## Translation Key Naming

All keys start with `@` and use English title case:

```
@Delete user question
@Comment added successfully
@Search dashboards
```

## Patterns by UI Element

### Action Buttons

Use a single verb in infinitive form:

| Key | German |
|-----|--------|
| `@Create` | Erstellen |
| `@Edit` | Bearbeiten |
| `@Delete` | Löschen |
| `@Save` | Speichern |
| `@Cancel` | Abbrechen |
| `@Search` | Suchen |
| `@Confirm` | Bestätigen |

Button labels must **not** include the object name (e.g., use "Erstellen", not "Ankündigung erstellen").

### Confirmation Dialogs

Always use the pattern:

> **Möchten Sie [object] [action verb]?**

For destructive actions add "wirklich":

> **Möchten Sie [object] wirklich [action verb]?**

Examples:

| Key | German |
|-----|--------|
| `@Delete user question` | Möchten Sie den Benutzer {{user}} wirklich löschen? |
| `@Enable user question` | Möchten Sie den Benutzer {{user}} aktivieren? |
| `@Publish comment question` | Möchten Sie diesen Kommentar veröffentlichen? |

If additional context is needed, place it before the question:

> Dieser Kommentar ist veröffentlicht. Die Bearbeitung erstellt eine neue Entwurfsversion. Möchten Sie fortfahren?

### Success Messages

Use the pattern:

> **[Object] erfolgreich [past participle]**

Examples:

| Key | German |
|-----|--------|
| `@Announcement added successfully` | Ankündigung erfolgreich hinzugefügt |
| `@User deleted successfully` | Benutzer {{email}} erfolgreich gelöscht |
| `@Dashboard group updated successfully` | Dashboard-Gruppe erfolgreich aktualisiert |

### Empty States

Use the pattern:

> **Keine [plural noun] [gefunden/verfügbar]**

Examples:

| Key | German |
|-----|--------|
| `@No dashboards found` | Keine Dashboards gefunden |
| `@No users available` | Keine Benutzer verfügbar |

### Warnings & Explanations

Use declarative sentences in present tense. End with a period:

> Sie sind dabei, diesen Kommentar zu löschen. Bitte geben Sie einen Grund für die Löschung an.

### Hints & Tooltips

Use imperative or descriptive sentences. Keep them concise (one sentence preferred):

| Key | German |
|-----|--------|
| `@Filter hint` | Eingeben und Enter drücken zum Filtern |
| `@Search hint` | Geben Sie ein Stichwort ein und drücken Sie Enter, um einen Filterchip hinzuzufügen. |

### Search & Filter Labels

Use verb infinitive form for action labels. Search placeholders must **not** include the object name (e.g., use "Suchen", not "Dashboards suchen"):

| Key | German |
|-----|--------|
| `@Search` | Suchen |
| `@Filter by role` | Nach Datendomänenrolle filtern |
| `@Clear all filters` | Alle Filter löschen |

## Rules Summary

1. **One pattern per element type** — never mix "Soll...?" and "Möchten Sie...?" in the same UI
2. **Formal address** — always use "Sie", never "du"
3. **No trailing periods** on button labels, column headers, or menu items
4. **Trailing periods** on full sentences (warnings, explanations, hints)
5. **No exclamation marks** — they feel aggressive in German formal UI
6. **Consistent articles** — always include the article before nouns in sentences ("den Benutzer", not "Benutzer")
7. **Variables** use double braces: `{{user}}`, `{{count}}`, `{{role}}`

## Adding New Keys

When adding a translation key:

1. Choose the correct pattern from the table above
2. Name the key in English title case with `@` prefix
3. Add the German value following the established pattern
4. Verify consistency by searching for similar keys in `de_CH.json`
