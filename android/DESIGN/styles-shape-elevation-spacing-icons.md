# M3 Styles: Shape · Elevation · Spacing · Icons — Reference Specsheet

> Original summary of factual specifications from
> https://m3.material.io/styles/shape, /styles/elevation, /styles/spacing, /styles/icons
> (verified live, Aug 2026). Facts only; prose paraphrased. Source licensed Apache 2.0 / CC BY 4.0.

## Shape

Source: `/styles/shape/overview-principles`

- **35 shapes** in the M3 shape library (Figma + Jetpack Compose), refreshed for M3 Expressive (May 2025) with shape morphing.
- Rectangular shapes are **fully rounded by default**; individual corners can be tuned for asymmetric shapes.
- Expressive update added corner-radius tokens:
  - `large increased` = **20dp**
  - `extra large increased` = **32dp**
  - `extra extra large` = **48dp**
- Fully-rounded corners now use a `full` token (previously "50% of component size").
- Principles: shapes echo typography's roundness attributes; morph to communicate state/progress/environment changes; contrast ("tension") between round and sharp shapes is encouraged but intentional; shapes are versatile, not semantic — avoid assigning fixed meaning to one shape; use abstract shapes sparingly in product UI; 2.5D depth effects via layered motion.

## Elevation

Source: `/styles/elevation/overview` (+ token set)

- Elevation = z-axis distance between surfaces in **dp**. Tokens carry the distance only; platforms choose their own shadow/tint rendering.
- M3 differences from M2: shadows applied selectively (not at every level); color (surface tone) carries elevation; levels model.
- All components define a default **resting elevation** — keep defaults; hover raises most buttons/FABs by exactly **1 level** (e.g., FAB level 3 → level 4).
- Canonical M3 level ladder used by tokens (`md.sys.elevation.level0…level5`): **0, 1, 3, 6, 8, 12 dp**.
- Purposes: ordering surfaces (content scrolling behind app bars), spatial relationships, focusing attention (dialogs).

## Spacing

Source: `/styles/spacing/overview` (+ token set)

- System measured on an **8dp scale**; baseline unit **`space100` = 8dp**, other units are multiples. Only recommended values are tokenized; extendable.
- Three categories:
  - **Padding** — space inside an element
  - **Gap** — space between elements
  - **Margin** — space outside an element (used sparingly; prefer padding/gaps on the parent)
- Directions: vertical/horizontal, leading/trailing (swap in RTL), top/bottom.
- Worked example from the page: search container uses 8dp vertical padding, 8dp horizontal gaps, 24dp margins (12dp when focused).
- Implementation status: Jetpack Compose available; Android Views & Web unavailable at crawl time.

## Icons

Source: `/styles/icons/overview`

- Icon set: **Material Symbols** variable font (fonts.google.com/icons), three styles: **Outlined, Rounded, Sharp**.
- Four adjustable axes: **weight, fill, optical size, grade**.
- Tooling: icons catalog, Material Symbols Figma plugin, keyline template (ZIP); copy-and-paste of customized symbols supported.

## Section map (pages crawled)

- `/styles/shape` (+ sub-pages)
- `/styles/elevation` (+ `overview`)
- `/styles/spacing` (+ `overview`)
- `/styles/icons` (+ `overview`)
