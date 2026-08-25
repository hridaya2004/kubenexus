# M3 Design Foundations — Reference Specsheet

> Original summary of factual specifications from
> https://m3.material.io/foundations/* — interaction states, layout/breakpoints, and site structure
> (verified live, Aug 2026). Facts only; prose paraphrased. Source licensed Apache 2.0 / CC BY 4.0.

## Interaction states

Source: `/foundations/interaction/states/overview`

Six defined states:

1. **Enabled** — interactive element at rest
2. **Disabled** — inoperable element
3. **Hover** — pointer over the element
4. **Focused** — highlighted via keyboard/voice input
5. **Pressed** — active tap
6. **Dragged** — press-and-move

Rules: states combine (e.g., selected+hover); apply consistently across components; use two visual indicators where possible for accessibility.

### State layers

Source: `/foundations/interaction/states/state-layers`

A state layer is a semi-transparent overlay using the content color ("on" color) at fixed opacity:

| State | Opacity |
|---|---|
| Hover | **8%** |
| Focus | **10%** |
| Press | **10%** |
| Drag | **16%** |

Geometry: state layer **40dp**, interactive target **48dp**. Only one layer at a time; layer color defaults to the content color of the enabled style.
Related page: `/foundations/interaction/states/applying-states`.

## Adaptive layout & breakpoints

Source: `/foundations/layout/breakpoints/overview`

Five breakpoints (a breakpoint = former "window size class"):

| Breakpoint | Width (dp) | Typical devices |
|---|---|---|
| Compact | < 600 | portrait phones |
| Medium | 600–839 | portrait tablets/foldables |
| Expanded | 840–1199 | landscape phones/tablets, desktops |
| Large | 1200–1599 | desktops |
| Extra-large | ≥ 1600 | desktop, ultra-wide |

- Android also exposes compact/medium/expanded for **height**.
- Pane guidance: compact/medium → 1 pane; expanded/large → 2 recommended; extra-large → up to 3.
- Adaptation checklist across sizes: reveal · divide · resize · reposition · swap.
- Keep line length ~**40–60 characters** when resizing text containers.
- Canonical swaps: navigation bar ↔ navigation rail ↔ expanded rail; bottom sheet ↔ menu; full-screen dialog ↔ basic dialog.
- Related pages: `/foundations/layout/layout-overview`, `adaptive-design`, `grids-spacing`, `scaffold`, `canonical-examples`, `bidirectionality-rtl`.

## Site information architecture (current)

Three top sections per `/get-started`:

1. **Foundations** — accessibility/building-for-all, content design, interaction, layout, tokens, customization, XR, watches, glossary
2. **Styles** — color, typography, elevation, icons, motion, shape, spacing
3. **Components** — 37 families (see components.md)

Licensing stated on-site: content available under **Apache 2.0 or CC BY 4.0**.

## M3 Expressive (May 2025 update) highlights

From `/get-started` and section pages:

- 15 new/updated components
- Emphasized typography set (+15 styles)
- Motion physics system (springs)
- Shape library expanded to 35 shapes with morphing
- New component families seen on the site: button groups, split button, toolbars, loading indicator, FAB menu, carousel

## Foundations section map (pages crawled)

- `/foundations` hub
- `/foundations/design-tokens/overview` → see tokens.md
- `/foundations/interaction/states/{overview,state-layers,applying-states}`
- `/foundations/layout/{breakpoints,layout-overview,grids-spacing,scaffold,bidirectionality-rtl,canonical-examples}`
- `/foundations/content-design/{overview,alt-text,global-writing,notifications,style-guide}`
- `/foundations/{designing,customization,usability,writing,glossary,watches}`
- `/foundations/xr/{design,components}`
