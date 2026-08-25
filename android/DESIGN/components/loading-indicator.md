# Loading indicator (M3)

> Source: https://m3.material.io/components/loading-indicator/overview · https://m3.material.io/components/loading-indicator/guidelines · https://m3.material.io/components/loading-indicator/specs · https://m3.material.io/components/loading-indicator/accessibility

## Variants/types

One variant, added with the M3 Expressive update (May 2025), recommended as a replacement for most uses of the indeterminate circular progress indicator — designed for progress that loads in under five seconds. Always reflects an ongoing process, never merely decorative.

Configurations: Default (uncontained) or Contained (a circular container providing extra contrast from body content — used when placed over other content and with pull-to-refresh). Indicators use shape and motion to capture attention and can scale in size.

Availability: Design Kit (Figma), Jetpack Compose: Expressive, Android Views (MDC-Android): Expressive — Available; Web: Expressive — Unavailable.

## Anatomy

1. Active indicator
2. Container (optional)

The active indicator is a looping shape-morph sequence composed of seven unique Material 3 shapes. When the container is visible the active indicator changes color from primary to on-primary-container; the container-to-indicator ratio stays constant when resizing.

## Key dimensions

| Attribute | Value |
|---|---|
| Default size | 48dp |
| Shape container | 38dp |
| Flexible size range | 24dp–240dp |

Beyond these values no further per-attribute measurement table was captured in the specs notes.

## States

Not an interactive control — the captured specs module organizes tokens under Default, Light with Color / Size / Shape folders only; no enabled/hover/focus/press state set was captured.

## Behavior

- Choose by expected wait time: Instant (under 200ms) → no indicator, display content immediately; Short (200ms–5s) → loading indicator; Long (over 5s) → progress indicator.
- Never transition a loading indicator into a determinate progress indicator — transition between indeterminate and determinate progress indicators instead.
- Placement: centered on the page/container while loading; when loading more items into existing content, centered in the empty space where new content will appear without overlapping it; can sit inside components like buttons to show an ongoing action (validating a form, checking for updates).
- Responsive layout: default 48dp scales between 24–240dp with placement and breakpoint; reserve very large sizes for large/extra-large windows like desktop, scaling so it stays proportional to surrounding empty space without exceeding 240dp.
- Pull-to-refresh (Jetpack Compose only): used at the start of lists, grids, and card collections with dynamic, frequently updated content; must pass a threshold before the app refreshes (reversing past the threshold cancels); remains visible until refresh completes and new content is visible or someone navigates away — don't scroll it off-screen, which would imply the refresh belongs to one component rather than the whole screen.

## Token group

No `md.comp.*` token names appear in the captured specs notes; loading indicators have a single token set organized under Default, Light with Color / Size / Shape folders. Color roles: Default uses Primary; Contained uses On primary container and Primary container.

## Accessibility summary

- Assistive technology users must be able to navigate to the indicator, understand what progress it communicates, and initiate a content refresh without relying on a gesture.
- Contrast: the active indicator provides ≥ 3:1 visual contrast against most container/surface colors — required for the indicator itself, not its container; inside another component (like a button) keep ≥ 3:1 against that component too.
- Pull-to-refresh can't be accessible by swiping alone: provide an alternate single-pointer refresh path, such as a refresh button in a menu or alongside the content (e.g., in an app bar).
- Labeling: being only a visual cue, it needs an accessibility label using the progress bar role, describing the purpose such as "loading news article" or "refreshing page".

## Captured spec tables

*Reproduced as captured from notes/articles/loading-indicator--*.json; sparse rows reflect the original scrape.*

Availability & resources (`overview`):

| Type | Resource | Status | Design | Implementation |
|---|---|---|---|---|
|  |  |  |  |  |
|  | Design Kit (Figma) | Available |  |  |
|  |  |  |  |  |
|  | android Jetpack Compose: Expressive | Available |  |  |
|  | android Android Views (MDC-Android): Expressive | Available |  |  |
|  | language Web: Expressive | Unavailable |  |  |

Variants & configurations (`specs`):

| Variant / Category | Configuration | M3 | M3 Expressive |
|---|---|---|---|
| Loading indicator | -- | -- | Available |
| Containment | Default | -- | Available |
| Containment | Contained | -- | Available |

Expected wait time (`guidelines`):

| Expected wait time | Recommendation |
|---|---|
| Instant (under 200ms) | No indicator |
| Short (between 200ms and 5s) | Loading indicator |
| Long (Over 5s) | Progress indicator |

No keyboard navigation table was captured (non-interactive control).
