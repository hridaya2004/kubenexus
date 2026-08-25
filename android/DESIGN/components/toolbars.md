# Toolbars (M3)

> Source: https://m3.material.io/components/toolbars/overview · https://m3.material.io/components/toolbars/guidelines · https://m3.material.io/components/toolbars/specs · https://m3.material.io/components/toolbars/accessibility

## Variants/types

Two M3 Expressive variants: docked toolbar (spans full window width; global actions that stay the same across pages) and floating toolbar (floats above body content; contextual actions relevant to the page). The baseline bottom app bar is no longer recommended but still supported — the docked toolbar replaces it, functioning similarly with a shorter height and more flexibility.

Configurations: color Standard (default; low emphasis, keeps focus on content) or Vibrant (high emphasis, draws attention to controls, can signal temporary behavior change like edit mode); floating layout Horizontal (default) or Vertical (medium+ breakpoints); pairing With FAB. Bottom app bar remains available but not recommended. Implementation differs per platform: on Jetpack Compose the floating toolbar is a separate component from the docked toolbar/bottom app bar, and floating-with-FAB is fully supported there while other platforms add each component separately.

M2 → M3: new color mappings with dynamic color, no shadow, taller container, and the FAB now contained within the app bar container (M2 bottom app bar had 8dp elevation and didn't contain the FAB).

## Anatomy

1. Container
2. Placed elements

Think of the toolbar as a container with slots populated by buttons, icon buttons, images, text fields, or custom components.

## Key dimensions

By default all toolbars are 64dp high, center-aligned, with equal padding between items and minimum outside padding of 16dp. Docked toolbars always span 100% of screen width; keep at least 16dp leading/trailing padding inside, with 32dp between items as the default arrangement; every element needs a minimum 48×48dp target area. Floating toolbars need ≥ 16dp margin from window edges horizontally and ≥ 24dp margin vertically. No numeric per-attribute measurement table was captured — the specs scrape conveys sizes via images and these defaults.

## States

The toolbar itself has no interactions by default — all interactions belong to the elements placed inside. Token folders captured under Default, Light: Enabled · Disabled · Hovered · Focused · Pressed.

Touch: tapping an icon button shows a ripple. Cursor: hover provides a visual cue of interactivity; clicking (active or inactive) shows a ripple.

## Behavior

- Use for actions related to the current page; scale to show more actions in larger windows. When actions don't fit, add a menu.
- Toolbars & navigation bars share the bottom placement — never show both at once. Navigation bar on primary pages, toolbars on subsequent pages with actions. Floating toolbars can act as tabs between related subsequent pages; avoid redundant/confusing navigation combinations.
- Docked container spans full window width; avoid rounded corners on it (implies the container expands/changes on interaction). In compact breakpoints space elements evenly; in medium+ adjust padding between controls (the scraped text truncates mid-explanation here).
- Floating containers must be fully visible on screen — use an overflow menu if more actions are needed. Horizontal floating toolbars sit at least 16dp from edges; vertical ones at least 24dp, positioned opposite the navigation rail (use the rail's centered configuration when both show) and without wide icon buttons to stay compact.
- Elevation: floating toolbars have elevation by default; removable if the underlying content is visually distinct.
- Emphasis: emphasize one action at a time (filled/tonal/standard icon button styles, customized color roles, wide/narrow icon buttons, or FAB pairing); avoid square icon buttons in floating toolbars (they conflict with the fully-rounded container shape) though they're fine in docked toolbars.
- RTL: in right-to-left languages layouts mirror (per overview guidance on mirrored alignment).
- A Scrolling section appears in the scraped headings, but its body text was truncated in the capture.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Tokens organize under Default, Light with Enabled / Disabled / Hovered / Focused / Pressed folders. Color roles captured: Standard scheme uses Surface container; Vibrant scheme uses Primary container. Icon-button types per scheme — Standard: filled button (Primary, On primary), toggle tonal button (Secondary container, On secondary container), standard button (Primary). Vibrant: filled button (Primary, On primary), toggle tonal button (Surface container, On surface), standard button (On primary container). Baseline bottom app bar role: Surface container, in one token set.

## Accessibility summary

- Assistive technology users must be able to navigate and activate any action in the toolbar, select a menu destination, activate a back button, and keep access to controls while content scrolls or collapses.
- Initial focus lands on the first interactive element; Tab moves through all other actions.
- Keyboard: Tab or Arrows navigate between interactive elements; Space or Enter activates the focused element.
- Labeling: on web the toolbar container should have the toolbar role; on mobile a generic container is fine. All actions inside follow their own components' accessibility guidelines.

## Captured spec tables

*Reproduced as captured from notes/articles/toolbars--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | android Jetpack Compose - Bottom app bar | Available |  |  |
|  | android Jetpack Compose: Expressive - Docked toolbar | Available |  |  |
|  | android Jetpack Compose: Expressive - Floating toolbar | Available |  |  |
|  | android Android Views (MDC-Android) - Bottom app bar | Available |  |  |
|  | android Android Views (MDC-Android): Expressive - Docked toolbar | Available |  |  |
|  | android Android Views (MDC-Android): Expressive - Floating toolbar | Available |  |  |
|  | Flutter - Bottom app bar | Available |  |  |
|  | language Web | Unavailable |  |  |

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| Docked toolbar | -- | Available |
| Floating toolbar | -- | Available |
| Bottom app bar | Available | Not recommended. Use docked toolbar. |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Color | Standard (default) | Available as bottom app bar | Available |
| Color | Vibrant | -- | Available |
| Floating toolbar layout | Horizontal (default) | -- | Available |
| Floating toolbar layout | Vertical | -- | Available |
| Other elements | With FAB | Available as bottom app bar | Available* |

(*Implementation differs per platform: fully supported on Jetpack Compose; elsewhere components are added separately.)

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab or Arrows | Navigate between interactive elements |
| Space or Enter | Activate the focused element |
