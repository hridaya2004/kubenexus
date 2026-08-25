# FAB menu (M3)

> Source: https://m3.material.io/components/fab-menu/overview · https://m3.material.io/components/fab-menu/guidelines · https://m3.material.io/components/fab-menu/specs · https://m3.material.io/components/fab-menu/accessibility

## Variants/types

One variant, added with the M3 Expressive update (May 2025): the FAB menu opens from a FAB to show 2–6 related actions floating on screen. It adds options to the FAB and should replace the speed dial and any usage of stacked small FABs (M2's speed dial used small round FABs; M3 uses dynamic color and larger item size). One menu size pairs with any FAB size; not used with extended FABs or any other component.

Configurations: three color sets — primary, secondary, tertiary — chosen to match the FAB color style (primary set with primary or primary container FAB, and so on). Contrasting close button/item colors support dynamic color.

## Anatomy

1. Close button
2. Menu item (up to six)

FAB menu items must always keep label text; icons shouldn't be removed since they differentiate items (remove only if necessary).

## Key dimensions

| Attribute | Value |
|---|---|
| Close button | Always 56dp |
| FAB margins | 16dp (increasing to 24dp in large/extra-large windows) |
| Minimum target size | 48dp elements with sufficient spacing |
| Web gap between FAB and menu | 4dp recommended |

Item measurements are shared with the medium button specs; no per-attribute numeric table was captured beyond these values.

## States

Close button: Enabled · Hovered · Focused · Pressed. Menu item: Enabled · Hovered · Focused · Pressed. On web the menu inherits states and specs from the baseline menu component (Enabled · Hovered · Selected shown for web spacing/interaction).

## Behavior

- Opens from a FAB and should always appear in the same place as the FAB that opened it, aligned to the trailing edge of the window (left-aligned with mirrored element layout in RTL languages).
- Contains 2–6 items closely related under a single action (like Share); don't group unrelated actions, and never use it with just one item.
- Don't pair with a floating toolbar or navigation rail — this prevents cognitive overload and clutter (a FAB itself can sit next to toolbars).
- Appearing: the FAB transforms into the close button; items appear via enter/exit transition originating from one of the FAB's trailing corners, preferably the top-aligned one. Larger FABs place the menu slightly higher with larger margins underneath.
- Adaptive: works from any sized FAB matched to window size class (larger FABs for larger windows); stays anchored to the same corner regardless of window size; on web it renders as a menu component consistent with other desktop apps.
- Scrolling: when window height is limited (e.g., phones in landscape), items scroll behind the close button.
- Expanding: any item can expand into any shape using a container transform transition — an app-structure surface or a full-screen surface.
- Web accessibility positioning: don't completely obscure the focus indicator of an actionable element; partial covering is fine if the indicator stays visible.

## Token group

No `md.comp.*` token names appear in the captured specs notes. The specs module defines a common token set plus six color sets — three per element (close button and list item) across primary/secondary/tertiary. Color roles captured per set: Primary / On primary / Primary container / On primary container, likewise Secondary* and Tertiary* roles.

## Accessibility summary

- Assistive technology users must be able to navigate and interact with the FAB menu with correct focus order through it.
- Elements meet the minimum 48dp target size with sufficient spacing by default.
- When scrolling is possible, items must scroll behind the close button so it stays easy to access and unobstructed (especially short screens like landscape).
- Initial focus lands on the close button (which took the FAB's place), then moves top-to-bottom through menu items.
- Keyboard: Tab navigates to the next interactive element; Space or Enter activates the focused button or item.
- Labeling (Android): close button labeled "Toggle menu", role Button, state Expanded or Collapsed; menu items match their UI text (e.g., "Reply all") with role Button.
- Labeling (Web): the FAB menu combines FAB + menu components — follow both components' guidelines; the FAB's label should describe the menu it opens.

## Captured spec tables

*Reproduced as captured from notes/articles/fab-menu--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | android Jetpack Compose: Expressive | Available |  |  |
|  | android Android Views (MDC-Android): Expressive | Unavailable |  |  |
|  | language Web: Expressive | Unavailable |  |  |

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| FAB menu | -- | Available |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Color | Primary set, secondary set, tertiary set | -- | Available |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Navigate to the next interactive element |
| Space or Enter | Activate the focused button or item |
