# Switch (M3)

> Source: https://m3.material.io/components/switch/overview · https://m3.material.io/components/switch/guidelines · https://m3.material.io/components/switch/specs · https://m3.material.io/components/switch/accessibility

## Variants/types

Single binary control (on/off, true/false). M3 changes vs M2: more accessible visual presentation; new color mappings meeting non-text-contrast requirements plus dynamic-color compatibility; an optional icon inside the handle; a taller and wider track (M2's circular handle extended beyond the track edge).

## Anatomy

1. Track
2. Handle (formerly "thumb")
3. Icon (optional)

Switches should always be paired with an inline label describing what the switch controls when selected.

## Key dimensions

| Attribute | Value |
|---|---|
| Track height | 32dp |
| Track width | 52dp |
| Track outline width | 2dp |
| Track shape | md.sys.shape.corner.full |
| Handle height (unselected) | 16dp |
| Handle height — with icon / selected | 24dp |
| Handle height (pressed) | 28dp |
| Handle width (unselected) | 16dp |
| Handle width — with icon / selected | 24dp |
| Handle width (pressed) | 28dp |
| Handle shape | md.sys.shape.corner.full |
| State-layer size | 40dp |
| State-layer shape | md.sys.shape.corner.full |
| Target size | 48dp |
| Icon size (selected / unselected) | 16dp |

## States

Enabled · Disabled · Hovered · Focused · Pressed. State specs are in the token module.

Interaction feedback: when tapped or dragged, and when clicked with a cursor, the handle size grows; on hover (both on and off states) the hover area grows to cue interactivity.

## Behavior

- A switch is successfully toggled when the handle slides to the other side of the track; effects start immediately, without needing to save.
- Use switches for standalone or more verbose options in a list, like settings — commonly arranged in stacked layouts on settings screens.
- Switches control binary options, not opposing ones; use a connected button group for opposing options (only one of a set selectable, like list vs map view).
- Alternate selection controls: checkboxes select multiple related options; radio buttons a single option from a list; switches toggle one item on/off or immediately activate/deactivate something.
- Don'ts captured as guidance: a switch can't replace a button (people expect calls to action to be buttons); avoid switches for multiple options requiring a save (use checkboxes).
- The optional handle icon should clearly and unambiguously communicate selection (e.g., checkmark/X), never ambiguous icons like moon or edit.

## Token group

No `md.comp.*` token names appear in the captured specs notes — only system-level shape tokens (`md.sys.shape.corner.full` for track, handle, and state layer). Color resolves through roles per state folder; the specs module organizes under Default, Light with Enabled / Disabled / Hovered / Focused / Pressed (ripple) groupings. Roles used for light and dark themes: Surface container highest, Outline, Primary, On primary, On primary container. Adjacent text labels use On surface (unchanged during interaction); supporting text may use On surface variant.

## Accessibility summary

- Assistive technology users must be able to navigate to a switch via keyboard or switch input, toggle it, and receive input-appropriate feedback.
- Avoid applying density by default: it pushes targets below the 48×48 CSS-pixel best practice; any controls used to enable denser layouts must themselves remain ≥ 48×48 CSS px so the setting stays revertible.
- Initial focus lands directly on the handle, the primary interactive element.
- Labeling: the accessibility label uses the adjacent label text if implemented correctly; screen readers read UI text followed by the role. When visible text is ambiguous, make labels more descriptive (e.g., "Photo album" → "Photo album access") — preferably by improving the adjacent label itself.

## Captured spec tables

*Reproduced as captured from notes/articles/switch--*.json; sparse rows reflect the original scrape.*

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

| Element | Attribute | Value |
|---|---|---|
| Track | Height | 32dp |
| Track | Width | 52dp |
| Track | Outline width | 2dp |
| Track | Shape | md.sys.shape.corner.full |
| Handle | Height (unselected) | 16dp |
| Handle | Height - with icon | 24dp |
| Handle | Height (selected) | 24dp |
| Handle | Height (pressed) | 28dp |
| Handle | Width (unselected) | 16dp |
| Handle | Width - with icon | 24dp |
| Handle | Width (selected) | 24dp |
| Handle | Width (pressed) | 28dp |
| Handle | Shape | md.sys.shape.corner.full |
| State layer | Size | 40dp |
| State layer | Shape | md.sys.shape.corner.full |
| Target | Size | 48dp |
| Icon | Size (selected) | 16dp |
| Icon | Size (unselected) | 16dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Focus lands on the switch handle |
| Space or Enter | Toggles the handle on and off |
