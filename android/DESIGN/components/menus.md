# Menus (M3)

> Source: https://m3.material.io/components/menus/overview · https://m3.material.io/components/menus/guidelines · https://m3.material.io/components/menus/specs · https://m3.material.io/components/menus/accessibility

## Variants/types

Two variants: vertical menus (introduced with the M3 Expressive update, November 2025 — recommended for new designs) and the baseline menu (still available, without the latest shapes, color styles, selection states, and motion). Vertical menus bring new shapes, color styles (standard and vibrant), selection states, refined submenu motion, and gaps for more flexible layout on Android. Context menus provide actions for a specific element like an image or highlighted text and usually open with a secondary click.

Differences from M2: new color mappings compatible with dynamic color; dropdown menu and exposed dropdown menu are now both referred to as menu since they differ only in which element opens the surface (M2's former menu colors didn't contrast with the background).

Availability: Design Kit (Figma); Flutter, Jetpack Compose (+ Expressive), Android Views (MDC-Android), Web — Available; Android Views: Expressive and Web: Expressive — Unavailable.

## Anatomy

Vertical menus:

1. Menu item
2. Leading icon (optional)
3. Menu item text
4. Trailing icon (optional)
5. Badge (optional)
6. Trailing text (optional)
7. Container
8. Supporting text (optional)
9. Label text (optional)
10. Gap (optional)
11. Divider (optional)

Baseline menu: list item, list item leading icon, list item trailing icon, container, list item trailing text, divider.

Menu items can include label text, leading icons, trailing icons, and keyboard commands; when a menu item is only usable under specific conditions it should appear disabled rather than be removed. Vertical menus have round corners versus the baseline variant's square corners.

## Key dimensions

Baseline menu measurements:

| Attribute | Value |
|---|---|
| Container width | 112dp min, 280dp max |
| Corner radius | 4dp |
| Vertical label text alignment | Center-aligned |
| Horizontal label text alignment | Start-aligned |
| Left/right padding | 12dp |
| Left/right padding with icon | 12dp |
| List item height | 48dp |
| Padding between elements within a list item | 12dp |
| Divider top/bottom padding | 8dp |
| Divider height | 1dp |
| Divider width | Dynamic |
| Leading/trailing icon size | 24dp |

No numeric measurement table was captured for vertical menus.

## States

Vertical menus: Enabled · Disabled · Hovered · Focused · Pressed · Active (main menu reveals submenu). Shape morphing creates an expressive active state; as focus moves between submenus, corner shape changes to highlight the active menu.

Baseline menus: default items and selected items, each with Enabled · Disabled · Hovered · Focused · Pressed; selected-item highlight uses tertiary container roles in vertical menus; baseline uses an on-surface state layer at opacity 0.08.

## Behavior

- Use a menu to show a temporary set of actions; use a toolbar to show actions at all times. A menu takes up less space than a set of radio buttons or chips.
- Opening: a menu appears when a person selects an element (icon, button, text field) or performs a triggering action like right-click or press-and-hold. Typical situations: overflow menus, text field dropdowns, select menus, context menus. Menus temporarily appear in front of all other permanent UI elements.
- Placement: positioned relative to the window edge — typically below, next to, or in front of the generating element; if cut off, automatically reposition left, right, or above. A menu opened at the top of the screen expands downward.
- Grouping: dividers create subtle separation (scrollable menus, text-field dropdowns where grouped treatment isn't appropriate, and on web); gaps are more expressive, making relationships clear — avoid resizing gaps, limit one or two per menu, don't use gaps in scrollable menus, and note gaps aren't currently available on web.
- Slots: custom slots support flexible layouts with simple content such as images, progress indicators, and color swatches; they can appear anywhere in a menu but require caution (see Accessibility).
- Submenus open next to the parent item without overlapping it; best on large screens (submenus are not currently available on Jetpack Compose).
- Adaptive: consider bottom sheets on compact breakpoints (more room for items and longer labels); on medium/expanded windows menus appear in context, and larger screens can hold more items plus submenus.
- Motion: enter/exit transition links the menu to its trigger; the trigger becomes pressed while expanded and a ripple appears when selecting an item on touch. In dense products such as desktop, menus can open instantly to reduce motion.
- Filtering: a text field can filter options as someone types (autocomplete), with items easing into new positions.
- Scrolling: menus scroll when all items can't display at once and show a persistent scrollbar; don't use gaps when a menu scrolls.
- Selecting: while a menu is open, the triggering button/icon keeps its visuals plus a pressed state — even when opened via keyboard shortcut.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Menus have two color mappings: standard (surface-based, lower emphasis) and vibrant (tertiary-based, higher prominence — use sparingly). Standard roles include on surface variant, on surface, on surface state layer, surface container low, tertiary container (selected), on tertiary container (selected); vibrant roles include on tertiary container, tertiary container, tertiary (selected), on tertiary (selected). Baseline roles add surface container, surface container highest, and outline variant. Tokens organize under Default, Light with Enabled / Hovered / Focused / Pressed (ripple) folders.

## Accessibility summary

- Assistive technology users must be able to navigate to, open, and close a menu, then navigate between and select menu items.
- Selection cues: by default items change shape and color when selected with 3:1 contrast between selected and unselected items; include another visual cue such as a checkmark — combine color, shape, and icons rather than any single cue.
- Slot cautions: keep the menu accessible, follow menu interaction rules, keep identical item padding, and keep targets ≥48×48dp. Don't add buttons, switches, or other direct actions into a menu item — nested elements should perform only one action; multiple actions break keyboard navigation and screen reader functionality.
- Focus: initial focus lands on the first menu item so keyboard users begin navigating immediately. Expected exits: selecting an option, tapping Escape or outside the menu, or using the system back button; post-close focus placement depends on the app.
- Interactability: disabled menu items can receive focus but aren't selectable; dividers and gaps can't receive focus.
- Labeling: the accessibility label equals the menu item text; role depends on platform (Web: Menu item; Android Views and Jetpack Compose: generic actionable element). For items with text and an icon, mark the icon's label decorative to avoid redundant verbalization.
- Keyboard (Android and web): Tab selects a menu item; Space or Enter opens a menu; Space or Enter selects a menu item; Escape closes a menu.

## Captured spec tables

*Reproduced as captured from notes/articles/menus--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | Flutter | Available |  |  |
|  | android Jetpack Compose | Available |  |  |
|  | android Jetpack Compose: Expressive | Available |  |  |
|  | android Android Views (MDC-Android) | Available |  |  |
|  | android Android Views (MDC-Android): Expressive | Unavailable |  |  |
|  | language Web | Available |  |  |
|  | language Web: Expressive | Unavailable |  |  |

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| Vertical menus | -- | Available |
| Menu (baseline) | Available | Available |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Color | Standard | Available | Available |
| Color | Vibrant | -- | Available |
| Layout | Standard | Available | Available |
| Layout | Grouped | -- | Available |

Measurements — baseline menu (`specs`):

| Attribute | Value |
|---|---|
| Container width | 112dp min, 280dp max |
| Corner radius | 4dp |
| Vertical label text alignment | Center-aligned |
| Horizontal label text alignment | Start-aligned |
| Left/right padding | 12dp |
| Left/right padding with-icon | 12dp |
| List item height | 48dp |
| Padding between elements within a list item | 12dp |
| Divider top/bottom padding | 8dp |
| Divider height | 1dp |
| Divider width | Dynamic |
| Leading/trailing icon size | 24dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Focus lands on menu |
| Space or Enter | For closed menus: Opens menu or submenu; for open menus: Selects a menu item |
| Up and Down arrows | For closed menus: Opens menu; for open menus: Moves focus to the next item |
| Left and Right arrows | Opens or closes a submenu |
| Letters | Focus moves to the next menu item starting with letter |
| Escape | Closes menu |

Labeling elements (`accessibility`):

| Element | A11y label | Role (Web) | Role (Android Views) | Role (Jetpack Compose) |
|---|---|---|---|---|
| Menu item text | Preview | Menu item | Generic actionable element | Generic actionable element |
