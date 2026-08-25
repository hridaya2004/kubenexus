# Search (M3)

> Source: https://m3.material.io/components/search/overview · https://m3.material.io/components/search/guidelines · https://m3.material.io/components/search/specs · https://m3.material.io/components/search/accessibility

## Variants/types

Two official variants: search bar and search view — collectively named search since the February 2025 M3 Expressive update (formerly "open search bar"). Two styles: contained (recommended; expressive look and feel with a persistent filled container separating the bar from suggestions/results) and divided (baseline; a divider separates the bar from results — no latest visual style, motion, or flexibility, and not recommended in Expressive). Two list layouts: docked and full-screen.

M2 → M3 differences: new color mappings with dynamic-color compatibility; lower elevation with no shadow by default; rounded tonal-surface containers instead of square elevated ones. M3 Expressive adds a new visual style, motion, and more flexibility for trailing icons.

The overview notes only Jetpack Compose as a supported platform; no availability & resources table was captured for this scrape.

## Anatomy

1. Search bar container
2. Leading icon
3. Supporting text
4. Avatar or trailing icon (optional)
5. Input text
6. Container for search suggestions or results

In the contained style the container keeps the same shape unfocused and focused; in the divided (baseline) style a divider separates the search bar and results. The suggestions/results container is empty by default — use the list component to add content.

## Key dimensions

Search bar:

| Attribute | Value |
|---|---|
| Container width | Min: 360dp, max: 720dp |
| Container height | 56dp |
| Label alignment | Start-aligned |
| Leading padding | Unfocused: 24dp, focused: 12dp |
| Trailing padding | Unfocused: 24dp, focused: 12dp |
| Leading icon and label padding (from tap target) | 4dp |
| Label and trailing icon padding (from tap target) | 4dp |
| Avatar size | 30dp |

Focused search:

| Element | Attribute | Value |
|---|---|---|
| Full-screen container | Width / Height | Full width / full height |
| Docked container | Width | Min: 360dp, max: 720dp |
| Docked container | Height | Min: 240dp, max: 2/3 of screen height |
| Search bar container | Height | 56dp |
| Search bar container | Leading/trailing padding | 16dp |
| Search bar container | Leading icon and label padding (from tap target) | 4dp |

## States

Search bar: Enabled · Hovered · Focused · Pressed (ripple). Search suggestions & results container: Enabled · Hovered · Focused · Pressed (ripple).

Individual elements maintain their own interaction states while search is focused.

## Behavior

- Selecting a search entry point opens focused search, which can show historical suggestions before typing, show suggestions/results as someone types, or wait until a query is executed.
- The search bar grows wider when focused (margins change from 24dp to 12dp); the back icon releases focus, dismisses suggestions/results, and restores the original state.
- Focused-search layouts: docked (list below the bar under a scrim — best for medium/expanded windows) and full-screen (default for compact breakpoints); swap between them as windows resize.
- Executing a search: type a query and press Enter, or select a suggestion/result without querying. Results appear in a list below the bar and scroll beneath it.
- Gaps separate suggestions/results into groups; consider leading icons, category labels (Recent, Contacts, Suggestions), avatars, and filter chips for variety and context.
- Placement: typically at the top of the screen, staying in its pane and scaling in width with the layout, close to the searchable content.
- Scroll: the bar can scroll away with content and reappear on upward scroll, or remain fixed at top.
- Entry points by need: search bar for a specific view, search app bar when search is the primary global function, search icon button when search is secondary.
- Container color: surface container high for clear contrast; avoid surface container high on a surface container background (keep roles more than one step apart). Headings also reference predictive back, though its body text was not captured.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Tokens split into two sets: the search bar set (unfocused bar only) and the search view set (all other tokens when interacting with search, across styles and layouts), organized under Default, Light with Enabled / Hovered / Focused / Pressed folders. Color roles captured: full-screen layout uses Surface container low, On surface variant, Surface container high, On surface; docked layout uses Surface container high, On surface variant, On surface (the scraped role lists don't map each role to its element).

## Accessibility summary

- Assistive technology users must be able to navigate to and focus a search bar, view hinted search text or persistent label, input text and complete a search, interact with suggestions/results, and clear input.
- Autosuggest: when suggestions/results appear, the screen reader must announce the change so people know list items are selectable.
- Initial focus lands on the first interactive element — usually a leading icon button or, if there's none, the text field.
- Labeling: the accessibility label should match the hinted search text; role is Text field on Android, Search field on iOS. Leading/trailing icon buttons follow icon-label guidance; suggestions/results are lists announced as lists per list accessibility guidelines.

## Captured spec tables

*Reproduced as captured from notes/articles/search--*.json; sparse rows reflect the original scrape.*

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| Search | Available | Available |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Style | Contained | -- | Available |
| Style | Divided | Available | Not recommended. Use contained. |
| Layout | Docked, full-screen | Available | Available |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab or Shift + Tab | Navigate between interactive elements |
| Space or Enter | Activate the search text field for input |
| Arrows | Navigate between search result items |

No availability & resources table was captured in the overview notes.
