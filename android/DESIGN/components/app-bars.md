# App bars (M3)

> Source: https://m3.material.io/components/app-bars/overview · /guidelines · /specs · /accessibility

Bar at the top of the window carrying page labels and navigation controls; scope is the current page with 1–2 essential actions (page-level action overflow belongs in a toolbar). Renamed from "top app bar" to "app bar" in the May 2025 M3 Expressive update.

## Variants/types

Four variants (all Expressive-era):

- **Search app bar** — emphasized search entry point for home pages where search is a key product function; opens the search view component when selected
- **Small** — dense layouts or scrolled pages
- **Medium flexible** — larger headline; collapses into small on scroll
- **Large flexible** — emphasizes the page headline

Baseline medium and large app bars are no longer recommended (replaced by medium/large flexible); center-aligned small merged into small as a centered-text configuration. Flexible additions vs baseline: reduced height, larger title text, subtitle, leading- or center-aligned text, text wrapping, roomier imagery/filled-button slots. Small gains subtitle + centered text.

M2 → M3: new color mappings (dynamic color), scroll separation via color fill instead of drop shadow, larger default text, smaller default height.

## Anatomy

- **Container** — full window width, default height, straight corners; holds all elements
- **Leading button** — navigation: menu icon (opens modal expanded navigation rail) or back arrow; first interactive element
- **Headline** — page/section/product name; leading- or center-aligned; wraps to 2 lines max in flexible variants, never truncated; can be replaced by an image/logo (small) or placed above text (others)
- **Subtitle** — optional context line, aligned to headline; flexible bars hug content so they grow taller when a subtitle shows
- **Trailing icon buttons** — up to 2, trailing edge, most-used nearest the leading edge; at most one may be filled/tonal (default or wide size)

Headline typography per variant — Search: Body large · Small: Title large · Medium flexible: Headline medium · Large flexible: Display small. Subtitle — Small: Label medium · Medium flexible: Label large · Large flexible: Title medium.

Search app bar anatomy: container, leading icon button, hinted search text, trailing icon/avatar; trailing icons may sit inside or outside the search field; label always contains the word "Search"; leading slot may carry a product logo (not as a rail expander).

## Key dimensions

No numeric dp captured on the specs page (measurements are rendered as diagrams). Captured facts: container spans 100% of window width at default height; ≤2 trailing icons mobile, up to 4 on large screens; flexible heights are shorter than the deprecated baselines.

## States

Enabled; flat vs on-scroll color change (container becomes surface container). Interactive children use ripple feedback on touch/click.

State-layer model shared across components: hover 8%, focus 10%, pressed 10%, drag 16% content-color overlay — see design.md.

## Behavior

- One primary action (two max) that alters or exits the page (Send/Save/Edit); avoid overflow menus here — use a toolbar
- On scroll, color fill separates bar from body content (no shadow)
- Can animate off screen paired with another control bar (e.g., chip row)
- Search app bar opens the search view when selected; adapts dynamically to available width
- Container resizes with the window; trailing actions may collapse into an overflow menu when space shrinks

Color roles (shared): Surface · On surface · On surface variant · Surface container (on scroll). Search adds Surface container and Surface container highest. Alternate search colors (e.g., surface bright) require ≥3:1 text/container contrast.

## Token group

Specs expose a common app-bar token set plus per-size sets (search, small, medium flexible, large flexible), each grouped by Color / Spacing / Shape / Size and by state. Search app bar reuses the default search component tokens including the "Search - View" set; family prefix `md.comp.top-app-bar.*` (baseline sizes) and `md.comp.search.*` (search entry points), following the `md.comp.<component>.<element>.<attribute>` grammar in tokens.md.

Captured value: search view container surface tint layer color = `#6750A4` (Default, Light).

## Accessibility summary

Users must identify the current page, act/navigate from the bar, and keep actions reachable while content scrolls. Touch/cursor produce ripples; hover cues interactivity; focus indicator required on keyboard/switch input. Initial focus lands on the leading button. Keys: Tab moves to next interactive element; Space/Enter activates it. Prefer default search-bar roles (surface container / on surface variant); custom mappings need ≥3:1 contrast. Accessibility label of a title equals its visible text plus clarifying context when needed; icon buttons labeled by their action (e.g., "View on map").

## Captured spec tables

Availability:

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| android Jetpack Compose | | Available |
| android Jetpack Compose: Expressive | | Available |
| android Android Views (MDC-Android) | | Available |
| android Android Views (MDC-Android): Expressive | | Available |
| language Web | | Unavailable |
| language Web: Expressive | | Unavailable |

Variants:

| Variant | M3 | M3 Expressive |
|---|---|---|
| Search app bar | -- | Available |
| Small | Available | Available |
| Center-aligned | Available | Merged into small. Use centered-text configuration. |
| Medium (baseline) | Available | Not recommended. Use medium flexible |
| Medium flexible | -- | Available |
| Large (baseline) | Available | Not recommended. Use large flexible |
| Large flexible | -- | Available |

Configurations:

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Text alignment | Leading edge (default) | Available | Available |
| Text alignment | Centered | -- | Available |

Accessibility keyboard map:

| Keys | Tab | Space or Enter |
|---|---|---|
| Actions | Move focus to the next interactive element | Activate the focused element |
