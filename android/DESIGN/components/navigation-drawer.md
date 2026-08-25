# Navigation drawer (M3)

> Source: https://m3.material.io/components/navigation-drawer/overview · https://m3.material.io/components/navigation-drawer/guidelines · https://m3.material.io/components/navigation-drawer/specs · https://m3.material.io/components/navigation-drawer/accessibility

## Variants/types

Two variants: standard and modal. Standard drawers provide access to drawer destinations next to app content in expanded, large, and extra-large breakpoints; modal drawers use a scrim to block interaction with the rest of the app and are primarily used in compact and medium breakpoints where space is limited or prioritized for content. Drawers can be open or closed by default.

Note: the navigation drawer is no longer recommended in the M3 Expressive update (May 2025) — use an expanded navigation rail instead, which adapts better across breakpoints. Differences from M2: new color mappings with dynamic-color compatibility, two separately named variants, rounded corners at the ending edge of the drawer, and updated color and shape for indicating the selected state.

Availability: no resource-status table was captured in the overview scrape.

## Anatomy

1. Sheet (side sheet holding all navigation drawer elements)
2. Active indicator
3. Icon
4. Label
5. Badge label
6. Divider (optional)
7. Section label (optional)
8. Scrim (modal only)

Navigation drawers are essentially a list contained within a side sheet; they can also include headers and subheads to organize longer lists. The specs page names the same set as Container, Headline, Label text, Active indicator, Badge label text, Scrim, Icon.

## Key dimensions

| Attribute | Value |
|---|---|
| Container height | 100% |
| Container width | 360dp |
| Container shape (standard) | 0,16,16,0dp corner radii |
| Icon size | 24dp |
| Active indicator height | 56dp |
| Active indicator shape | 28dp |
| Active indicator width | 336dp |
| Horizontal label alignment | Start-aligned |
| Left padding / Right padding | 28dp / 28dp |
| Active indicator padding | 12dp |
| Padding between elements | 0dp |

The modal drawer shares every measurement above except it has no container-shape row in the captured table.

## States

Enabled · Hovered · Focused · Pressed — token folders under Default, Light; state specs live in the tokens module.

Color roles used for light and dark schemes: Surface container low, On surface variant, On secondary container, Secondary container, Scrim. For divider color roles, go to divider specs.

## Behavior

- Provides access to destinations and app functionality such as switching accounts; permanently on-screen, or opened and closed by a navigation menu icon. One destination is always active.
- Recommended when apps have five or more top-level destinations, two or more levels of navigation hierarchy, need quick navigation between unrelated destinations, or replace the rail/bar on large screens. Avoid combining with other primary navigation components on the same screen.
- Standard drawers can be permanently visible (best for frequently switching destinations) or toggled from a menu icon (best for focusing on screen content).
- Modal drawers don't affect the screen's layout grid, are always opened by an action outside the drawer (such as a menu icon in a navigation rail), and are dismissed by selecting a drawer item, tapping the scrim, or swiping toward the anchoring edge.
- The sheet sits on the start edge of the screen — left for LTR languages, right for RTL languages.
- Destinations are actionable list items with required label text (truncate beyond the container width rather than wrapping or shrinking text) and optional icons placed before text — used for all destinations or none. Full-width dividers separate groups of destinations, not individual ones.
- Swap components with a transition as breakpoints change (rail ↔ drawer); on web below 320 CSS pixels, swap the drawer for a navigation bar to ensure accessibility.

## Token group

No `md.comp.*` token names appear in the captured specs notes. The navigation drawer has one (baseline) token set organized under Default, Light with Enabled / Hovered / Focused / Pressed (ripple) groupings.

## Accessibility summary

- Users should be able to move between navigation destinations, select a particular destination from a set, and get appropriate feedback based on input type.
- Touch: tapping shows the active indicator in place with a ripple through it while the icon switches outlined→filled and darkens. Cursor: hovering shows a hover indicator; clicking ripples and changes icon contrast — darker in light theme, lighter in dark theme.
- Initial focus lands directly on the first navigation item, the component's first interactive element.
- Closing: the modal drawer is dismissed by selecting the scrim covering the rest of the screen.
- Visual indicators: icons give the dominant state cue — filled for the selected destination versus outlined for non-selected; keeping the same style removes the active cue.
- Keyboard: Tab lands focus on the first navigation destination; Space or Enter selects the focused destination and focus moves to the newly opened section (if applicable); Arrow navigates between destinations within the drawer.
- Labeling: the accessibility label typically equals the destination name; correctly linked UI text is read followed by the role. Make labels more descriptive when visible text is ambiguous ("Recents" → "Recent images"). On Android Views (MDC-Android), a more descriptive accessibility label isn't available and the role isn't announced.

## Captured spec tables

*Reproduced as captured from notes/articles/navigation-drawer--*.json; sparse rows reflect the original scrape.*

Measurements — standard navigation drawer (`specs`):

| Attribute | Value |
|---|---|
| Container height | 100% |
| Container width | 360dp |
| Container shape | 0,16,16,0dp corner radii |
| Icon size | 24dp |
| Active indicator height | 56dp |
| Active indicator shape | 28dp |
| Active indicator width | 336dp |
| Horizontal label alignment | Start-aligned |
| Left padding | 28dp |
| Right padding | 28dp |
| Active indicator padding | 12dp |
| Padding between elements | 0dp |

Measurements — modal navigation drawer (`specs`):

| Attribute | Value |
|---|---|
| Container height | 100% |
| Container width | 360dp |
| Icon size | 24dp |
| Active indicator height | 56dp |
| Active indicator shape | 28dp |
| Active indicator width | 336dp |
| Horizontal label alignment | Start-aligned |
| Left padding | 28dp |
| Right padding | 28dp |
| Active indicator padding | 12dp |
| Padding between elements | 0dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Focus lands on the first navigation destination |
| Space or Enter | Selects the focused navigation destination, and focus moves to the newly opened section (if applicable) |
| Arrow | Navigate between destinations within the navigation drawer |

(The overview scrape contained no availability & resources table.)
