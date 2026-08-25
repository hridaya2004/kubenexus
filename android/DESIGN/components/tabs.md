# Tabs (M3)

> Source: https://m3.material.io/components/tabs/overview · https://m3.material.io/components/tabs/guidelines · https://m3.material.io/components/tabs/specs · https://m3.material.io/components/tabs/accessibility

## Variants/types

Two variants: primary and secondary tabs, placed next to each other as peers. Primary tabs sit at the top of the content pane under an app bar and display main content destinations; secondary tabs are used within a content area to further separate related content and establish hierarchy (a simpler indicator style with identical function). Tab containers can be fixed or scrollable — tabs can horizontally scroll, so a UI can have as many tabs as needed.

Differences from M2: new color mappings with dynamic-color compatibility, and icons/labels now vertically centered within the container.

Availability: Design Kit (Figma); implementations for Flutter, Jetpack Compose, Android Views (MDC-Android), Web — all Available.

## Anatomy

1. Container
2. Icon (optional)
3. Badge (optional)
4. Label
5. Divider
6. Active indicator

Secondary tabs drop the icon element: Container, Badge (optional), Label, Divider, Active indicator.

## Key dimensions

| Attribute | Value |
|---|---|
| Container height (label text only) | 48dp |
| Container height (icon and label text) | 64dp |
| Icon size | 24dp |
| Divider height | 1dp |
| Primary active indicator height | 3dp |
| Secondary active indicator height | 2dp |
| Active indicator shape | 3, 3, 0, 0 |
| Active indicator minimum length | 24dp |
| Padding between inline icon and text | 8dp |
| Padding between inline text and badge | 4dp |
| Overlap of badge on stacked icon | 6dp |

Primary tab active indicators are inset 2dp on each side with a fully rounded corner radius; the divider is included in the container height, placed inside it.

## States

By default tabs inherit enabled states with one active state; both the inactive and active destination carry Hover, Focused, and Pressed states — token folders under Default, Light list Enabled / Hovered / Focused / Pressed (ripple).

Primary tab color roles used for light and dark schemes: Surface, Primary, On surface variant, Outline variant. Secondary tab color roles: Surface, On surface, On surface variant, Outline variant, Primary.

## Behavior

- Organize groups of related content at the same level of hierarchy — never sequential content that must be read in order.
- The container extends the full window width divided into equal sections (fixed tabs), or scrolls; a bottom-edge divider separates it from content that may scroll underneath.
- Fixed tabs display all tabs simultaneously and suit quick switching between related content; navigate by tapping a tab or swiping left/right within the content area — use caution when placing other swipeable content there, and prefer different gesture directions when combining gestures.
- Scrollable tabs are used when a set cannot fit on screen, allow longer labels and more tabs, and offset the first visible tab 52dp from the left on web and mobile so more content is visibly available; each tab's width follows its label length with consistent padding.
- Avoid more than four tabs at once — at five or more the container becomes cramped.
- Labels live in a single row (a second line with truncation if needed); don't truncate unless required. Icons should be globally recognizable when used alone, and applied consistently across all tabs in a set.
- Badges show notifications per tab, limited to four characters including "+", updating or disappearing once the user views the relevant content.
- The active tab is differentiated by an underline (active indicator) plus color change to its text and icon.
- When screen content scrolls vertically, tabs either pin to the top or scroll off and return on upward scroll; don't scroll tabs behind an app bar — attached components appear and move as a single unit.

## Token group

No `md.comp.*` token names appear in the captured specs notes. The specs module offers token sets per variant ("Tabs - Primary navigation", with a parallel secondary set) organized under Default, Light with Enabled / Hovered / Focused / Pressed (ripple) groupings.

## Accessibility summary

- Users should be able to undertake actions or invoke navigation with assistive tech, select an action or destination from an off-screen tab, and maintain access of primary actions while content is scrolled.
- Touch: tapping shows a touch ripple and the selected indicator shifts into position once the touch engages. Cursor: hovering cues interactivity; clicking ripples and shifts the indicator into position.
- Scrollable: navigate by swiping left/right or arrow/tab; select via tap or Space/Enter. Don't loop a tab set scrolling infinitely — it traps users navigating linearly with a screen reader. Horizontal scrolling tabs meet accessibility requirements because they grow in width for labels without affecting layout.
- Keyboard/Switch: tabbing shows a focus indicator; Space/Enter takes the user to the new destination; arrow/tab moves through menu items and Tab exits the active state. Use Arrow/Tab to navigate items — never Space/Enter, which is reserved for completing actions.
- Density: don't apply density by default — it lowers targets below the 48×48 CSS-pixel best practice; offer density as an opt-in and keep all controls to revert it at ≥ 48×48 CSS px each.
- Initial focus: on arrow/tab into a tab menu, the active indicator appears on the first interactive element, then Tab reaches further elements until all items are complete.
- Labeling: when visible UI text is ambiguous or absent, make accessibility labels descriptive — an icon visually representing "Video camera" gets a label clarifying its function such as "Video format media content".

## Captured spec tables

*Reproduced as captured from notes/articles/tabs--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | Flutter | Available |  |  |
|  | android Jetpack Compose | Available |  |  |
|  | android Android Views (MDC-Android) | Available |  |  |
|  | language Web | Available |  |  |

Measurements (`specs`):

| Attribute | Value |
|---|---|
| Container height (label text only) | 48dp |
| Container height (icon and label text) | 64dp |
| Icon size | 24dp |
| Divider height | 1dp |
| Primary active indicator height | 3dp |
| Secondary active indicator height | 2dp |
| Active indicator shape | 3, 3, 0, 0 |
| Active indicator minimum length | 24dp |
| Padding between inline icon and text | 8dp |
| Padding between inline text and badge | 4dp |
| Overlap of badge on stacked icon | 6dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Arrow | Focus lands on the next available navigation destination |
| Space / Enter | Activates the focused navigation destination |
| Arrow | Allows navigation through menu items |
