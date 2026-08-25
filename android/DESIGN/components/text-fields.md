# Text fields (M3)

> Source: https://m3.material.io/components/text-fields/overview · https://m3.material.io/components/text-fields/guidelines · https://m3.material.io/components/text-fields/specs · https://m3.material.io/components/text-fields/accessibility

## Variants/types

Two variants: filled text fields and outlined text fields. Both use a container for a visual interaction cue and provide identical functionality — variant choice can depend on style alone (what fits the app's visual style, the UI's goals, and distinctness from buttons/surrounding content). Outlined fields have less visual emphasis, which simplifies layouts where many fields sit together (like forms). If both variants appear in one UI, separate them by section/region — never intermix within the same form or region.

M2 → M3 differences: new color mappings with dynamic-color compatibility.

## Anatomy

Filled text field:

1. Container
2. Leading icon (optional)
3. Label text in empty field / label text in populated field
4. Trailing icon (optional)
5. Focused active indicator / enabled active indicator
6. Caret
7. Input text
8. Supporting text (optional)

Outlined text field:

1. Enabled container outline / focused container outline
2. Leading icon (optional)
3. Label text in empty field / label text in populated field
4. Trailing icon (optional)
5. Caret
6. Input text
7. Supporting text (optional)

Containers have fill and stroke (full outline, or just the bottom edge) whose color/thickness can signal activation; outlined containers have rounded corners while filled containers have rounded top corners and square bottom corners.

## Key dimensions

Filled text field:

| Attribute | Value |
|---|---|
| Default container height | 56dp |
| Label alignment (unpopulated) | Vertically centered |
| Top/bottom padding | 8dp |
| Left/right padding without icons | 16dp |
| Left/right padding with icons | 12dp |
| Icon alignment | Vertically centered |
| Padding between icons and text | 16dp |
| Supporting text and character counter top padding | 4dp |
| Padding between supporting text and character counter | 16dp |
| Target size | 56dp |

Outlined text field:

| Attribute | Value |
|---|---|
| Container height | 56dp |
| Left/right padding without icons | 16dp |
| Left/right padding with icons | 12dp |
| Padding between icons and text | 16dp |
| Icon alignment | Vertically centered |
| Supporting text and character counter top padding | 4dp |
| Padding between supporting text and character counter | 16dp |
| Label alignment | Vertically centered |
| Left/right padding populated label text | 4dp |
| Target size | 56dp |

## States

Enabled · Focused · Hovered · Disabled, each crossed with empty / populated. Error states: Enabled · Focused · Hovered, each crossed with empty / populated (error messages display below the field as supporting text until fixed).

The guidelines' Adaptive design/Density sections appear in the scraped headings but their body text was truncated in the capture.

## Behavior

- Every text field should have a label: always visible, aligned with input text, placed mid-field or resting near the top; when selected it moves from the middle to the top of the field. Never truncate or wrap labels.
- A field may omit its own label if a separate adjacent label indicates purpose; adjacent labels align to the leading edge of the container.
- Required fields: show an asterisk (*) next to the label and explain it via supporting text or one note at the start of the form; if required text has a particular color, match the asterisk color to it.
- Input display: single-line fields scroll text left at the right edge (unsuitable for long responses); multi-line fields expand as text overflows, shifting content downward; text areas are fixed-height, wrap overflow onto new lines, scroll vertically when full, and are preferred over multi-line fields on the web (keep height within mobile screens).
- Prefix text (e.g., currency symbol) and suffix text (e.g., unit or email domain) are supported.
- Supporting text conveys extra info (ideally one line), persistently visible or on focus only; pair with a character/word counter showing used vs limit when limits exist.
- For validating fields, replace supporting text with error text (prevents layout bumps): describe how to avoid the single possible error, or the most likely of several. Long errors may wrap — keep padding between fields sufficient.
- Strongly recommended: an error icon in error state, highlighting errors for people with visual impairments.

## Token group

No `md.comp.*` token names appear in the captured specs notes; tokens organize per variant under Default, Light state folders. Color roles captured: filled text field uses Surface container highest, On surface variant, Primary, On surface; outlined text field uses Outline, On surface variant, Primary, On surface (the scraped role lists don't map each role to its element).

## Accessibility summary

- Users must be able to navigate to and activate a text field with assistive technology, input information, receive and understand supporting/error messages, and navigate to and select interactive icons.
- Contrast: outlined-field containers should reach ≥ 3:1 contrast between container outline and background to improve perception of the fields.
- Keyboard: Tab focuses the (non-disabled) text field.
- Labeling: accessibility label equals the text field's label; correctly linked UI text is read before the role. Interactive trailing icons clarify function ("Show password" ↔ "Hide password"); non-actionable icons like the error icon are labeled "Error". Prefixes/suffixes need unique id attributes (e.g., currency name for a symbol). Errors get an "alert" role applied to the role and message; if both supporting and error text exist, the label reads supporting text first, then error text. Character counters are labeled "character count" clarifying remaining characters; supporting text doubles as its own label. Required fields include the asterisk in the accessibility label.

## Captured spec tables

*Reproduced as captured from notes/articles/text-fields--*.json; sparse rows reflect the original scrape.*

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

Measurements (`specs`): see the two Key dimensions tables above (filled and outlined attribute/value pairs reproduced from the specs notes).

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Focus lands on (non-disabled) text field |
