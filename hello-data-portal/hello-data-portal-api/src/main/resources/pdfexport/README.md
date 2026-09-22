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

## Recipes - copy-paste examples

Each recipe says **which file** to touch. Remember the golden rule above: **styling → `fragments/pdf.html`**,
**page content → the PAGE template**. After saving, just run a new export - no restart.

> Before every edit, **keep a backup of the file** (a malformed file makes that export fail; see the
> troubleshooting section). To undo, delete your copy to fall back to the packaged default.

### 1. Change the logo

1. **Swap the image** - replace `branding/logo.png` with your own PNG, **keeping the filename**. A
   transparent background looks best. The logo is embedded fresh on every export, so the new file shows
   on the next PDF.
2. **Resize it (optional)** - the size is CSS, in the `styles` fragment of **`fragments/pdf.html`**:
   ```css
   .header .logo-cell     { width: 120px; }   /* the column the logo sits in */
   .header .logo-cell img { width: 110px; }   /* the logo itself - raise for a bigger logo */
   ```
   If you make the logo much wider, raise the `.logo-cell` width to match.

### 2. Change the first (cover) page

The cover is the `<div class="cover-page">` block at the top of the **PAGE templates**. Edit **both**
`portrait_template.html` **and** `landscape_template.html` so portrait and landscape stay consistent.

Add a subtitle and a "generated on" date under the title:
```html
<div class="cover-page">
  <th:block th:replace="~{fragments/pdf :: header}"></th:block>
  <h1 class="title" th:text="${layout.title()}">Layout Title</h1>

  <!-- NEW cover content -->
  <div class="cover-subtitle" th:text="${reportCaption}">HelloDATA Dashboard Export</div>
  <div class="cover-date">Erstellt am <span th:text="${generatedAt}">2026-01-01</span></div>
</div>
```
Then style the new classes in the `styles` fragment of **`fragments/pdf.html`**:
```css
.cover-subtitle { font-size: 13pt; color: #444444; margin: 4px 0 0 0; }
.cover-date     { font-size: 9pt;  color: #999999; margin: 2px 0 0 0; }
```
Notes:
- The cover already forces the charts onto page 2 (`.cover-page { page-break-after: always; }`).
- Values you may use on the cover: `${layout.title()}`, `${orgName}`, `${reportCaption}`,
  `${generatedAt}`, `${logoDataUri}` (see the variables list above). Don't invent other names.

### 3. Change the fonts

**a) Keep the design, swap the glyphs (no code change)** - drop four TTF files into `fonts/`, named
**exactly**:
```
Roboto-Light.ttf    Roboto-Regular.ttf    Roboto-Medium.ttf    Roboto-Bold.ttf
```
They stay registered under the family **`Roboto`** at weights 300 / 400 / 500 / 700, so all the existing
CSS keeps working - you're only replacing the glyphs.

**b) Make the text bigger/smaller** - one line in the `styles` fragment of **`fragments/pdf.html`**:
```css
body { font-size: 10pt; }   /* base size; headings scale from h1.title / h2.tab-title / h3.chart-name */
```

**c) Use a *differently named* family (e.g. real "Open Sans")** - the family name is registered in the
portal **code** (`PdfRenderer.registerRoboto`, which maps the four files to family `Roboto`). From this
folder you can only swap the glyphs behind `Roboto`; a new family name needs a code change + rebuild -
ask the dev team.

### 4. Change the brand colours

The palette lives in the `styles` fragment of **`fragments/pdf.html`**. Find-and-replace these:
| Where | Default | Meaning |
|-------|---------|---------|
| `body`, titles, headings | `#0a0a0a` | near-black text |
| borders / hairlines | `#dedede` | thin separators |
| footer text | `#999999` | footer + meta |
| `.summary` / markdown box | `#faf1e3` | highlight background |

Example - give the report title a red brand accent:
```css
h1.title { color: #b5121b; }   /* was #0a0a0a */
```

### 5. Change the footer wording

The footer sits in `@page { … }` in the `styles` fragment. Left and right are portal-filled tokens
(**keep** `@@FOOTER_TITLE@@` / `@@FOOTER_DATE@@` as the `content`); the **middle page counter is plain
CSS you can reword**:
```css
@bottom-center {
  content: "Seite " counter(page) " / " counter(pages);   /* e.g. "Page " counter(page) " of " counter(pages) */
  font-family: "Roboto", sans-serif; font-size: 8pt; color: #999999;
}
```

### 6. Header caption text (org name / report caption) - config, not this folder

`${orgName}` ("Kanton Bern") and `${reportCaption}` ("HelloDATA Dashboard Export") come from the
portal's **application config**, not this folder:
```yaml
hello-data:
  pdf-export:
    org-name: "Kanton Bern"
    report-caption: "HelloDATA Dashboard Export"
```
You can *use* those variables in a template, but to *change the text* edit the config (ask dev/ops).
Page **size** and **margins** are portal-controlled too (the `@@PAGE_SIZE@@` / `@@PAGE_MARGIN@@` tokens) -
change orientation in the builder/portal, not here.

---

## If something goes wrong

- A **missing** file falls back to the packaged default automatically, and is re-seeded on the next
  restart — so **to reset a file to the default, delete it** (and restart to get the copy back).
- A file that **exists but is malformed** makes that export **fail** — there is **no fallback for a
  broken file, only for a missing one**. **Keep a backup before editing**, and if exports start
  failing after a change, delete the file to restore the default.
