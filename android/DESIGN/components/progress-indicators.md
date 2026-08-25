# Progress indicators (M3)

> Source: https://m3.material.io/components/progress-indicators/overview · https://m3.material.io/components/progress-indicators/guidelines · https://m3.material.io/components/progress-indicators/specs · https://m3.material.io/components/progress-indicators/accessibility

## Variants/types

Two variants: linear and circular. Both come in determinate (default; known progress and wait time) and indeterminate (unknown) behaviors, and in flat (default) or wavy shapes — M3 Expressive (Aug 2024) added configurable track height and the wavy shape for increased expressiveness.

Previous updates: Dec 2023 non-text contrast work added an end stop indicator, raised track-vs-active-indicator contrast, new motion behavior, and rounded corners. Differences from M2 (added to Material 3 July 2022): new color mappings with dynamic-color compatibility replacing the boxier neutral style.

Availability: Design Kit (Figma); implementations for Flutter, Jetpack Compose (+Expressive), Android Views (MDC-Android) (+Expressive), Web — Available; Web: Expressive Unavailable.

## Anatomy

1. Active indicator
2. Track
3. Stop indicator

The stop indicator is a 4dp circle marking the end of a linear determinate indicator to meet Material's accessibility standards; it isn't used for indeterminate or circular indicators.

## Key dimensions

The captured specs notes carry no attribute/value measurement table; measurements are presented as annotated images plus prose notes:

| Attribute | Value |
|---|---|
| Track thickness | Fixed 4dp default; configurable in M3 Expressive |
| Stop indicator | 4dp circle |
| Linear inset | 4dp from the edge of the screen; end padding 4dp minimum (modifiable) |
| Linear minimum element width | Not used in elements smaller than 40dp |
| Circular size range | 24dp–240dp depending on placement and breakpoint |
| Wavy shape | Amplitude measures center of resting position to center of peak; wavelength measures distance between adjacent peaks; height is the overall container height |

Thicker variants are provided as sample measurements for makers adjusting the defaults.

## States

Not an interactive control — the captured specs module organizes tokens under Default, Light with Color / Shape folders and a [Deprecated] Enabled folder; no enabled/hover/focus/press state set was captured.

Color roles used for light and dark schemes: Primary, Secondary container.

## Behavior

- Communicate the status of ongoing processes such as loading an app, submitting a form, or saving updates; use one indicator per group of items rather than one per activity.
- Match the indicator to expected wait time: instant (under 200ms) shows no indicator; short waits (200ms–5s) use a loading indicator; long waits (over 5s) use a progress indicator — and consider letting people navigate away from very long processes.
- Linear indicators sit on the edge of a loading container (the animating edge when shape changes); circular indicators are centered directly on the container or page that's loading.
- Determinate indicators must accurately represent progress and fill from 0% to 100%; indeterminate indicators grow and shrink along a fixed track and should switch to determinate as more process information becomes available.
- The active indicator appears as soon as progress begins, rendering as a dot at low percentages where space is limited; linear indicators animate leading→trailing while circular ones animate clockwise from the top by default.
- Wavy active indicators make long processes feel less static but increase overall component height and may not be visible at very small sizes.
- In buttons, a circular indicator signals an in-progress action: use the flat shape in very small buttons, match the active indicator color to the icon/label text for 3:1 contrast, remove the track, and avoid applying indicators to every button in a list.
- RTL languages mirror linear indicators horizontally; circular indicators don't need mirroring. Reserve very large circular indicators for large and extra-large windows, scaling the waveform with size.
- A process should always use the same variant throughout the product.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Tokens live in a shared "Progress Indicator - Common" set under Default, Light with Color and Shape folders; the separate baseline circular and linear token sets are no longer recommended.

## Accessibility summary

- People should be able to navigate to the progress indicator and understand what progress it communicates using assistive technology.
- Contrast: the active indicator provides visual contrast of at least 3:1 against most background colors, as does the combination of progress indicator and stop indicator.
- When integrated into another component such as a button, keep at least 3:1 contrast against that component — use the same color as its label text or icon and remove the track.
- For linear indicators, the stop indicator is required if the track's contrast falls below 3:1 with its container or the surface behind it; only remove it when contrast reaches at least 3:1 so the end of the track stays easy to identify.
- Labeling: since the indicator is a visual cue, give it an accessibility label describing the kind and amount of progress made; use the progress bar role, naming the process ("loading") and affected content — e.g., "Loading news article" or "Refreshing page".

## Captured spec tables

*Reproduced as captured from notes/articles/progress-indicators--*.json; sparse rows reflect the original scrape.* (The specs scrape contained no measurement tables.)

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
|  | language Web | Available |  |  |
|  | language Web: Expressive | Unavailable |  |  |

Variants (`specs`):

| Variant | M3 | M3 Expressive |
|---|---|---|
| Linear progress indicator | Available | Available |
| Circular progress indicator | Available | Available |

Configurations (`specs`):

| Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Behavior | Determinate (default), Indeterminate | Available | Available |
| Track thickness | Fixed (4dp) | Available | Available |
| Track thickness | Configurable | -- | Available |
| Shape | Flat (default) | Available | Available |
| Shape | Wavy | -- | Available |

Expected wait time (`guidelines`):

| Expected wait time | Recommendation |
|---|---|
| Instant (under 200ms) | No indicator |
| Short (between 200ms and 5s) | Loading indicator |
| Long (Over 5s) | Progress indicator |

(The accessibility scrape contained no keyboard-navigation table.)
