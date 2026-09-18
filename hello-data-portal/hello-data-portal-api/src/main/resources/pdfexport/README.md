# PDF export templates & branding

These files control the layout **and** branding of the Superset → PDF export in the HelloDATA portal.
The portal **seeds this folder on startup** from the packaged defaults (only files that don't already
exist, so your edits survive restarts) and **renders from here**. Edits take effect on the **next
export** — no restart, no rebuild.

```
.
├── portrait_template.html     PAGE template — custom grid export, A4 portrait
├── landscape_template.html    PAGE template — custom grid export, A4 landscape
├── dashboard.html             PAGE template — full-dashboard export
├── fragments/
│   └── pdf.html               SHARED building blocks: the CSS + header + chart/grid rendering
├── branding/
│   └── logo.png               Header logo (swap this file to rebrand)
├── fonts/
│   └── Roboto-*.ttf           Embedded fonts (Light/Regular/Medium/Bold)
└── README.md                  This file
```

---

## ⚠️ The one thing to understand first: PAGE templates vs FRAGMENTS

`fragments/pdf.html` is **not a page**. It is a library of named blocks (`th:fragment="…"`) that the
page templates pull in. **Only the named fragments are used** — any markup you add *outside* a
fragment (e.g. a `<div>` right under `<body>`) is **silently ignored**. It is the single most common
mistake, so:

- **To change styling** (colors, fonts, spacing, header look) → edit inside the `styles` or `header`
  fragment in **`fragments/pdf.html`**.
- **To add/'change page content** (a disclaimer line, extra heading, a page) → edit a **PAGE
  template** (`portrait_template.html` / `landscape_template.html` / `dashboard.html`), which is what
  actually gets rendered.

### Example — add a line of text to the exported page
Edit `portrait_template.html` (and/or `landscape_template.html`) — **not** `fragments/pdf.html`:
```html
<body>
  <div class="cover-page">
    <th:block th:replace="~{fragments/pdf :: header}"></th:block>
    <h1 class="title" th:text="${layout.title()}">Layout Title</h1>
    <div>My extra text</div>            <!-- shows once, on the cover page -->
  </div>
  <div class="pdf-page" th:each="page,ps : ${layout.pages()}" ...>
    <div>My footer note</div>          <!-- shows on every page -->
    <th:block th:replace="~{fragments/pdf :: gridPage(${page})}"></th:block>
  </div>
</body>
```

---

## Rules — read before editing

1. **The renderer is openhtmltopdf, CSS 2.1 only.** No flexbox, no CSS grid. Use tables, floats, or
   absolute positioning (the chart grid already uses absolute positioning).
2. **Keep these tokens exactly as-is** — the portal replaces them at render time; removing them breaks
   the page geometry/footer:
   `@@PAGE_SIZE@@`, `@@PAGE_MARGIN@@`, `@@FOOTER_TITLE@@`, `@@FOOTER_DATE@@`
3. **Keep the fragment names and `th:*` attributes.** The pages pull blocks by name
   (`~{fragments/pdf :: styles / header / item(...) / gridPage(...)}`). Renaming/removing a fragment,
   or dropping a `th:each` / `th:replace` / `th:text`, breaks rendering.
4. **Only these Thymeleaf variables exist** (don't invent others):
   - Custom grid (`portrait_/landscape_template.html`): `layout.title()`, `layout.pages()` → per page
     `page.heightCss()` + `page.tiles()`, each tile `item()`, `left()`, `top()`, `width()`, `height()`,
     `imageHeight()`.
   - Full dashboard (`dashboard.html`): `export.title()`, `export.sections()`.
   - Both: `logoDataUri`, `orgName`, `reportCaption`, `generatedAt`.

---

## Branding

- **Logo:** replace `branding/logo.png` with your own PNG (keep the filename). The header sizes it to
  110px wide; a similar aspect ratio looks best.
- **Fonts:** replace the files in `fonts/` with your own, **keeping the same filenames**
  (`Roboto-Light.ttf`, `Roboto-Regular.ttf`, `Roboto-Medium.ttf`, `Roboto-Bold.ttf`). They stay
  registered under the CSS family name **`Roboto`**, so the existing CSS keeps working — you're just
  swapping the glyphs. (To use a differently *named* family you'd also change every `font-family` in
  the `styles` fragment.)

---

## If something goes wrong

- A **missing** file falls back to the packaged default automatically, and is re-seeded on the next
  restart — so **to reset a file to the default, delete it** (and restart to get the copy back).
- A file that **exists but is malformed** makes that export **fail** — there is **no fallback for a
  broken file, only for a missing one**. **Keep a backup before editing**, and if exports start
  failing after a change, delete the file to restore the default.
