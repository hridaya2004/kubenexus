# Navigation bar (M3)

> Source: https://m3.material.io/components/navigation-bar/overview · https://m3.material.io/components/navigation-bar/guidelines · https://m3.material.io/components/navigation-bar/specs · https://m3.material.io/components/navigation-bar/accessibility

## Variants/types

Two variants: the baseline navigation bar and the flexible navigation bar introduced in the M3 Expressive update (May 2025). The baseline is no longer recommended and should be replaced by the flexible bar, which is shorter and supports horizontal navigation items in medium windows. Bottom navigation was renamed navigation bar in M3; differences from M2 include new color mappings with dynamic-color compatibility, a taller container, no drop shadow, and a pill-shaped active indicator in a contrasting color. The active label changed from on-surface-variant to secondary.

Availability: Design Kit (Figma); implementations for Flutter, Jetpack Compose, Android Views (MDC-Android) plus their Expressive editions — all Available. Web and Web: Expressive are Unavailable.

## Anatomy

1. Container
2. Icon
3. Label text
4. Active indicator
5. Small badge (optional)
6. Large badge (optional)
7. Large badge label

The guidelines group items 2–4 as navigation items that hold all elements for each destination, laid out vertically (text below icon and indicator) or horizontally (icon and text beside each other inside the indicator).

## Key dimensions

The captured specs notes carry no numeric measurement table; measurements are presented as annotated images. Qualitative geometry from the notes:

| Attribute | Value |
|---|---|
| Container width | Spans 100% of the window |
| Breakpoints | Compact and medium only; use a navigation rail for expanded and extra-large |
| Destinations | 3–5 of equal importance |
| Vertical navigation item width | Dynamically changes to equally fit the container |
| Horizontal navigation item width | Fixed; extra space is added to the ends of the bar |

## States

Enabled · Hovered (8% state layer) · Focused (10% state layer) · Pressed (10% state layer).

Interaction feedback renders a translucent content-color overlay over the control; per-state opacities are defined in design.md (hover 8% / focus 10% / press 10% / drag 16%).

Color roles used for light and dark schemes: Surface container, On-secondary container, Secondary, Secondary container, On-surface variant. For badge color roles, go to badge specs.

## Behavior

- Provides access to three to five destinations at the bottom of the window for convenient access; each destination is an icon plus label text, and one destination is always active.
- Destinations don't change — they are consistent across app screens with fixed positions; never scroll them or modify their positions.
- Selecting an unselected item navigates using a top-level transition pattern that either preserves state (scroll position, current tab, in-line search) or resets it to the default view; choose based on product needs (frequent section switching favors preserving state).
- Use vertical items in compact windows and horizontal items in medium windows; horizontal items stay centered with outer margins and the same padding at each breakpoint.
- A FAB sits right-aligned above the bar; dialogs, bottom sheets, drawers, the on-screen keyboard, or flow elements may temporarily cover it, but it must never be permanently obstructed.
- For more than five destinations the elements may collide and translated text won't fit — use tabs within a page or hide navigation behind a menu icon in a modal expanded navigation rail instead. Don't remove labels, and don't use the bar for fewer than three destinations (use tabs).

## Token group

No `md.comp.*` token names appear in the captured specs notes. The specs module offers separate token sets for the navigation bar and the nav items under Default, Light with Enabled / Hovered / Focused / Pressed (ripple) groupings; the baseline bar carries its own token set with roles Surface, On secondary container, On surface, Secondary container, On surface variant.

## Accessibility summary

- Assistive technology users must be able to move between navigation destinations, select one from a set, and get appropriate feedback based on input type.
- Touch: tapping shows the active indicator in place with a ripple passing through it while the icon switches from outlined to filled and changes color. Cursor: hovering shows a reduced active indicator; clicking ripples through the indicator and darkens the icon.
- Text scaling: the bar grows vertically while retaining default padding and scaled text may wrap; keep the full label visible on-screen up to 2x sizing — beyond that text can truncate.
- Initial focus lands directly on the first navigation item, which is selected with Space/Enter.
- Visual indicators: filled icon with bold label for selected destinations versus outlined icon with medium label otherwise; if no filled style exists, use a thicker or heavier icon version.
- Keyboard: Tab moves between navigation items; Space / Enter selects the focused navigation item.
- Labeling: the accessibility label typically equals the destination name; when visible UI text is ambiguous, be more descriptive (visible "Library" → accessibility label clarifying "Music library"). On Android Views (MDC-Android) a more descriptive label isn't available and the role isn't announced.

## Captured spec tables

*Reproduced as captured from notes/articles/navigation-bar--*.json; sparse rows reflect the original scrape.*

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
|  | android Android Views (MDC-Android): Expressive | Available |  |  |
|  | language Web | Unavailable |  |  |
|  | language Web: Expressive | Unavailable |  |  |

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| Flexible navigation bar | -- | Available |
| Navigation bar | Available | Not recommended.Use flexible navigation bar. |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Navigation item layout | Vertical (default) | Available | Available |
|  | Horizontal | -- | Available |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Move between navigation items |
| Space / Enter | Selects the focused navigation item |
