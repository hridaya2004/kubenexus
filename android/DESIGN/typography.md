# M3 Typography — Reference Specsheet

> Original summary of factual specifications from https://m3.material.io/styles/typography/overview
> and https://m3.material.io/styles/typography/type-scale-tokens (verified live, Aug 2026).
> Values are design facts; explanatory prose is paraphrased. Source content is licensed by Google under Apache 2.0 / CC BY 4.0.

## Structure of the M3 type scale

- One type scale, two style sets: **15 baseline + 15 emphasized = 30 styles** (emphasized set added in the M3 Expressive update).
- Both sets span the same roles from Display Large → Label Small.
- Emphasized styles carry higher weight (plus minor adjustments) and are intended for emphasis: selections, actions, headlines, editorial moments.
- Scale ratio: **Major Second (1.125)** with key base size **14** (anchored to body text). Source: type-scale-tokens page, "Customizing your type scale".
- Default typeface: **Roboto** for both brand and plain roles. Brand face covers large styles (Display/Headline); plain face covers small styles (Body/Label).

## Baseline type style tokens (verified from live token explorer)

| Style | Font | Weight | Size | Tracking | Line height | Token |
|---|---|---|---|---|---|---|
| Display Large   | Google Sans      | 400 | 57sp | 0   | 64sp | `md.sys.typescale.display-large` |
| Display Medium  | Google Sans      | 400 | 45sp | 0   | 52sp | `md.sys.typescale.display-medium` |
| Display Small   | Google Sans      | 400 | 36sp | 0   | 44sp | `md.sys.typescale.display-small` |
| Headline Large  | Google Sans      | 400 | 32sp | 0   | 40sp | `md.sys.typescale.headline-large` |
| Headline Medium | Google Sans      | 400 | 28sp | 0   | 36sp | `md.sys.typescale.headline-medium` |
| Headline Small  | Google Sans      | 400 | 24sp | 0   | 32sp | `md.sys.typescale.headline-small` |
| Title Large     | Google Sans      | 400 | 22sp | 0   | 28sp | `md.sys.typescale.title-large` |
| Title Medium    | Google Sans Text | 500 | 16sp | 0   | 24sp | `md.sys.typescale.title-medium` |
| Title Small     | Google Sans Text | 500 | 14sp | 0   | 20sp | `md.sys.typescale.title-small` |
| Body Large      | Google Sans Text | 400 | 16sp | 0   | 24sp | `md.sys.typescale.body-large` |
| Body Medium     | Google Sans Text | 400 | 14sp | 0   | 20sp | `md.sys.typescale.body-medium` |
| Body Small      | Google Sans Text | 400 | 12sp | 0.1 | 16sp | `md.sys.typescale.body-small` |
| Label Large     | Google Sans Text | 500 | 14sp | 0   | 20sp | `md.sys.typescale.label-large` |
| Label Medium    | Google Sans Text | 500 | 12sp | 0.1 | 16sp | `md.sys.typescale.label-medium` |
| Label Small     | Google Sans Text | 500 | 11sp | 0.1 | 16sp | `md.sys.typescale.label-small` |

Notes from the live explorer:
- Label Large and Label Medium also expose a **prominent weight** value (700) in the expressive token set.
- Each style has a single aggregate token plus individual axis tokens (font, line height, size, tracking, weight) for customization.
- Variable-font axes are exposed per style: `wght`, `GRAD`, `wdth`, `ROND`, `opsz`, `CRSV`, `slnt`, `FILL`, plus `HEXP`.

## Emphasized styles

- Token pattern: `md.sys.typescale.emphasized.display-large` etc. (swap the baseline token of the same name).
- Recommended placements: badges, buttons (primary actions), extended FAB, selected list/menu items.
- Components do **not** use emphasized styles by default; they must be opted into.
- Use on text that is already weight-differentiated to signal hierarchy or state (e.g., unread messages, selected items).

## Language height support

Line heights adapt automatically to four script-height categories:

| Category | Height delta | Scripts (examples) |
|---|---|---|
| Small (base) | — | Cyrillic, Greek, Hebrew, Latin (except Vietnamese) |
| Medium | ~7% taller | Arabic, Bangla, Chinese, Devanagari-based scripts, Japanese, Korean, Thai, Vietnamese, most others |
| Large | ~30% taller | Burmese, Telugu |
| Extra large | ~100% taller | Nastaliq |

Guidance: default to medium height; fixed-height components may not adapt automatically; ignoring language height risks overlapping text and i18n breakage.

## Units & conversion

- Android font sizes in **sp**, letter spacing in **em**; web uses **rem** (`SP_SIZE / 16 = rem`).
- Examples: 10sp = 0.625rem · 12sp = 0.75rem · 24sp = 1.5rem · 60sp = 3.75rem.
- Letter spacing example: 0.2px tracking at 16sp ≈ 0.0125 em.

## Customization guidance (summary)

- Change brand/plain typeface tokens first; then adjust line height / tracking. Avoid changing type sizes (components depend on them).
- Keep emphasized styles visually consistent (e.g., uniformly wider than baseline).
- Heavier faces may need wider tracking; long ascenders/descenders may need different leading.
- Customizing tokens may opt you out of receiving upstream typography token updates.
- Products rarely need all 15 styles — reduce the set deliberately while keeping strong contrast between retained sizes.

## Section map (pages crawled)

- `/styles/typography/overview` — system overview
- `/styles/typography/type-scale-tokens` — token explorer (values above)
- `/styles/typography/applying-type`
- `/styles/typography/editorial-treatments`
- `/styles/typography/fonts`
