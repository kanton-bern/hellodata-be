# PDF export templates

These files control the layout of the Superset → PDF export in the HelloDATA portal. The portal
**seeds this folder on startup** from the packaged defaults (only files that don't already exist,
so your edits survive restarts) and **renders from here**. You can edit them without rebuilding the
image — changes take effect on the **next export** (no restart needed).

## Files

| File | Used for |
|------|----------|
| `portrait_template.html`  | Custom grid export (the PDF-builder page), A4 **portrait** |
| `landscape_template.html` | Custom grid export, A4 **landscape** |
| `dashboard.html`          | Full-dashboard export (charts grouped by tab) |
| `fragments/pdf.html`      | **Shared** styles (all the CSS), page header, and the chart/markdown + grid fragments used by the three templates above |

Most visual changes (colors, fonts sizes, spacing, header) live in **`fragments/pdf.html`**.

## Rules — please read before editing

1. **Renderer is openhtmltopdf, which supports CSS 2.1 only.** No flexbox, no CSS grid. Use tables,
   floats, or absolute positioning (the grid layout already uses absolute positioning).
2. **Keep these substitution tokens exactly as-is** — the portal replaces them at render time, and
   removing them breaks the page footer/geometry:
   - `@@PAGE_SIZE@@` — the `@page { size: … }` value (e.g. `210mm 297mm`)
   - `@@PAGE_MARGIN@@` — the `@page { margin: … }` value
   - `@@FOOTER_TITLE@@` — the dashboard title in the footer
   - `@@FOOTER_DATE@@` — the generation timestamp in the footer
3. **Keep the Thymeleaf fragment names and `th:*` attributes.** The templates pull shared blocks by
   name: `~{fragments/pdf :: styles}`, `:: header`, `:: item(...)`, `:: gridPage(...)`. Renaming or
   removing a fragment, or dropping a `th:each` / `th:replace`, breaks rendering.
4. **Data comes from these Thymeleaf variables** (don't invent others):
   - Custom grid (`portrait_/landscape_template.html`): `layout.title()`, `layout.pages()` → each
     `page.heightCss()` + `page.tiles()`, and each tile's `item()`, `left()`, `top()`, `width()`,
     `height()`, `imageHeight()`.
   - Full dashboard (`dashboard.html`): `export.title()`, `export.sections()`.
   - Both: `logoDataUri`, `orgName`, `reportCaption`, `generatedAt`.
5. **Fonts and the logo are provided by the application**, not from this folder: the Roboto fonts are
   embedded by the renderer, and the header logo is injected as `${logoDataUri}`. Editing the CSS to
   use a different embedded font family won't work unless that font is also registered in the app.

## If something goes wrong

- A **missing** file automatically falls back to the packaged default (and is re-seeded on the next
  restart) — so to reset one file to the default, just delete it and restart.
- A file that **exists but is malformed** will make that export **fail** (there is no fallback for a
  broken file, only for a missing one). **Keep a backup before editing**, and if an export starts
  failing after an edit, delete the file to restore the default.
