# All buttons (M3)

> Source: https://m3.material.io/components/all-buttons/overview

Hub page cataloging the 10 button-family types and ranking them by visual emphasis to guide component choice.

## Variants/types

Ten types: Button · Toggle button · Icon button · Toggle icon button · Split button · Standard button group · Connected button group · Floating action button (FAB) · Extended FAB · FAB menu

Emphasis order as ranked on the page (most → least prominent):

1. Extended FAB, FAB, FAB menu — largest controls; reserved for a screen's primary action
2. Button (filled) — primary palette; final or unblocking actions in a flow
3. Split button — primary palette plus menu affordance for key actions with options
4. Standard button group — color/motion/shape draw attention to several key actions
5. Button (tonal) — secondary palette; less prominent final or unblocking actions
6. Button (elevated) — secondary palette + shadow; separation from patterned backgrounds
7. Outlined button / connected button group — attention without primary status; related option switching
8. Button (text) — no outline or fill; non-essential actions
9. Icon button — most compact; optional supplementary actions

## Anatomy

Not defined on the hub page — see each family doc (`buttons.md`, `icon-buttons.md`, `button-groups.md`, `split-button.md`, `segmented-buttons.md`, `floating-action-button.md`, `extended-fab.md`, `fab-menu.md`).

## Key dimensions

None captured here; per-family values live in each family doc.

## States

Defined per family. Shared state-layer model: hover 8%, focus 10%, pressed 10%, drag 16% content-color overlay — see design.md.

## Attributes/behavior

- Selection is driven by emphasis level and action criticality, not aesthetics.
- Only one FAB-class control per screen; lower-emphasis actions move to text/icon buttons or groups.
- Example actions per tier are captured in the table below.

## Token group

Not applicable on the hub page; token families are documented per component (`md.comp.button.*`, `md.comp.icon-button.*`, `md.comp.button-group.*`, `md.comp.split-button.*`, `md.comp.segmented-button.*`, `md.comp.fab.*`, `md.comp.extended-fab.*`, `md.comp.fab-menu.*`).

## Accessibility summary

Covered by each family's accessibility article; the hub adds no separate a11y rules.

## Captured spec tables

Emphasis tiers defined on the page: high = primary/most important/most common action on a screen; medium = important actions that don't distract from the main task; low = optional/supplementary actions with least prominence.

| Component | Rationale | Example actions |
|---|---|---|
| Extended FAB, FAB, and FAB menu | Largest, most visually prominent; meant for a page's primary action (extended FAB suits large screens) | Create · Compose · New thread · New file |
| Button (filled) | Primary palette; most prominent after the FAB; final or unblocking flow actions | Save · Confirm · Done |
| Split button | Primary palette plus menu icon for key actions with multiple options | Send · Add · Create |
| Button group (standard) | Color, motion, and shape capture attention; shows multiple key actions | Back · Pause · Next |
| Button (tonal) | Secondary palette; less prominent than filled; final or unblocking actions | Save · Confirm · Done |
| Button (elevated) | Secondary palette + shadow; use only when separation from a patterned background is needed | Reply · View all · Add to cart · Take out of trash |
| Connected button group | Shows multiple related options; switches visible page content | Walk · Bike · Drive |
| Button (outlined) | Needs attention but not the primary action ("See all", "Add to cart") | Reply · View all · Add to cart · Take out of trash |
| Button (text) | No outline or fill; actions outside the essential user journey | Learn more · View all · Change account · Turn on |
| Icon button | Most compact and subtle; optional supplementary actions | Add to Favorites · Print |

Note: the captured rationale cells were truncated mid-sentence on two rows during crawl; rationales above are paraphrased summaries of the captured facts.
