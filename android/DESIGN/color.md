# M3 Color System — Reference Specsheet

> Original summary of factual specifications from
> https://m3.material.io/styles/color/system/overview and https://m3.material.io/styles/color/roles
> (verified live, Aug 2026). Facts only; prose paraphrased. Source licensed Apache 2.0 / CC BY 4.0.

## System at a glance

- Built-in set of accessible color relationships; pairs guarantee **minimum 3:1 contrast**.
- **26+ color roles** mapped to Material components.
- Built-in dark theme; tone-based surface colors (not elevation-tied).
- Static baseline scheme (fixed defaults) + dynamic color (user-generated and content-based).
- Dynamic color provides: personalized UI, accessible contrast, user-controlled contrast, automatic dark theme.
- Semantic colors (e.g., error) stay static even in dynamic schemes, but still flip for light/dark.

## Recent system updates (dates from live site)

- **May 2025** — three tokenized contrast levels: standard, medium, high.
- **Aug 2024** — more colorful light-theme values for on-primary-container, on-secondary-container, on-tertiary-container, on-error-container (affects badges, buttons, FABs, chips, lists, menus, navigation bar/drawer/rail, switches, toolbars).
- **Feb 2023** — tone-based surface roles replaced the old surface +1..+5 elevation tints; default light surface moved **tone 99 → 98**; neutral palette chroma raised **4 → 6**; dark surface roles slightly darkened. Also added **fixed accent colors** that hold the same tone across light and dark themes.

## Role naming vocabulary

| Prefix/suffix | Meaning |
|---|---|
| `surface` | Backgrounds and large low-emphasis areas |
| `primary/secondary/tertiary` | Accent roles ordered by emphasis |
| `container` | Fill colors for elements (never for text/icons) |
| `on-…` | Foreground color paired with its parent fill |
| `…-variant` | Lower-emphasis alternative to its non-variant pair |

## Accent roles (4 each: base, on, container, on-container)

- **Primary** — highest emphasis: FABs, high-emphasis buttons, active states.
- **Secondary** — lesser prominence: filter chips, selected nav icon states, dismissive actions.
- **Tertiary** — small special emphasis: badges, input-field accents; balances primary/secondary.
- **Error** — urgency/error states; static by default under dynamic color.

## Surface roles

Core three:
- `surface` — default backgrounds
- `on-surface` — text/icons on any surface or surface container
- `on-surface-variant` — lower-emphasis text/icons on surfaces

Plus five containers by emphasis:
`surface-container-lowest`, `surface-container-low`, `surface-container`, `surface-container-high`, `surface-container-highest`.

Default component mappings seen on the page: menus/dialogs → high/highest tiers; nav areas → `surface-container`; body areas → `surface`; keep mappings stable across breakpoints.

## Inverse roles

- `inverse-surface`, `inverse-on-surface`, `inverse-primary`.
- Canonical use: snackbar (background / text / action).

## Outline roles

- `outline` — important boundaries needing 3:1 contrast (text-field borders).
- `outline-variant` — decorative dividers; usable on target borders when inner content carries ≥4.5:1 contrast.
- Anti-patterns: outline for dividers; outline-variant for hierarchy/boundary definition.

## Add-on roles (optional)

- **Fixed accents**: `primary-fixed`, `secondary-fixed`, `tertiary-fixed` (+ `*-fixed-dim`) keep constant tone across themes, unlike regular container roles.

## Pairing rules

Apply colors only in intended pairs/layering (e.g., primary + on-primary, secondary-container + on-secondary-container). Improper mapping breaks accessibility guarantees, especially with user-controlled contrast.

## Section map (pages crawled)

- `/styles/color/system` — system overview + updates
- `/styles/color/roles` — full role catalog
- `/styles/color/choosing-a-scheme`
- `/styles/color/static`
- `/styles/color/dynamic` (incl. `choosing-a-source`)
- `/styles/color/advanced`
- `/styles/color/resources`
