# Chips (M3)

> Source: https://m3.material.io/components/chips/overview · https://m3.material.io/components/chips/guidelines · https://m3.material.io/components/chips/specs · https://m3.material.io/components/chips/accessibility

## Variants/types

Four variants: assist, filter, input, and suggestion. Chip elevation defaults to 0 but can be elevated if chips need more visual separation (e.g., on images or dynamic backgrounds).

Differences from M2: new color mappings with dynamic-color compatibility; shape is a rounded rectangle; action chips were separated into assist chips and suggestion chips, and choice chips are now a subset of filter chips (M2 variants were input, choice, filter, and action). Aug 2024 update: stroke color changed from outline to outline variant — softened to improve visual hierarchy between chips and buttons.

Availability: Design Kit (Figma); implementations for Flutter, Jetpack Compose, Android Views (MDC-Android), Web — all Available.

## Anatomy

1. Container
2. Label text
3. Leading icon or image (optional)
4. Trailing icon (required for input chips, optional for filter chips)

All chips are slightly rounded with an 8dp corner. Leading circular images are sized larger than leading icons to provide more space for detail; icons are designed to be legible at small sizes.

## Key dimensions

Shared across assist / filter / suggestion / input chips:

| Attribute | Value |
|---|---|
| Container height | 32dp |
| Container shape | 8dp corner radius |
| Icon size | 18dp |
| Vertical label text alignment | Center-aligned |
| Horizontal label text alignment | Start-aligned |
| Left/right padding (without icon) | 16dp |
| Left/right padding with icon | 8dp |
| Padding between elements | 8dp |

Input chip additions:

| Attribute | Value |
|---|---|
| Avatar shape | 12dp corner radius |
| Avatar size | 24dp |
| Left padding for avatar | 4dp |
| Right padding for avatar | 8dp |
| Target size for close icon | Min 48dp |

Secondary actions (such as a trailing Remove) must have a 48×48dp interaction target that doesn't interfere with the chip's primary action — achieve this with a minimum chip width of 88dp, or 42dp applied to the label text.

## States

Enabled · Disabled · Hovered · Focused · Pressed · Dragged, each crossed with selected / unselected for every variant.

Color roles used per variant (light and dark themes): assist — surface container low (optional), on surface, outline, primary; filter — on surface variant, on secondary container, secondary container, outline variant, surface container low (optional); input — on surface variant, surface container low (optional), outline variant, primary, secondary container, on secondary container; suggestion — outline, surface container low (optional), on surface variant.

## Behavior

- Chips help people enter information, make selections, filter content, or trigger actions; they appear as a group of interactive elements and aren't buttons.
- Use chips to present contextual, supplemental options that enhance the current journey; buttons should progress people through the product and handle significant or final steps. Chips represent forking paths for a current task; buttons represent linear steps.
- Multiple chips appear together in a set; don't display a single chip by itself. Chip sets can be scrolled horizontally. (No more than 3 buttons belong in a single arrangement, by contrast.)
- Assist chips: smart or automated actions that can span multiple apps; write like buttons starting with a verb (adjust text dynamically, e.g., Save → Saved); displayed after primary content such as below a card or at the bottom of a screen; can transform into modals, full-screen views, or inline results, and can show progress and confirmation.
- Filter chips: tags or descriptive words to filter content; tapping activates the chip and appends a checkmark to the label's starting edge; multiple chips can be selected or unselected; write with nouns describing the category, avoiding negative phrases like "Exclude images".
- Input chips: discrete pieces of information entered by a person (e.g., Gmail contact in the To field); the trailing remove icon is required.
- Suggestion chips: dynamically generated suggestions that help narrow a person's intent (e.g., suggested chat response).
- Trailing icons: on filter chips optional — can open a menu or remove the chip.
- Elevation only when placed on an image or complicated background; never on the page itself, and never to indicate a pressed state (use the visual ripple effect).
- Chip label text should be 20 characters or fewer with the same typography style as buttons; skip articles to save space. The leading icon color of unselected chips defaults to primary; on surface variant is a lower-emphasis alternative.

## Token group

No `md.comp.*` token names appear in the captured specs notes. Color resolves through each variant's color roles per state folder; the specs module organizes tokens under Default, Light with Enabled / Hovered / Focused / Pressed (ripple) groupings.

## Accessibility summary

- Assistive technology users must be able to use a chip to perform an action, navigate to it, and activate it.
- The chip label needs at least 3:1 contrast with the background; high contrast helps differentiate clustered chips. A chip that performs an action should present button semantics to the platform accessibility API.
- Horizontal overflow: prefer reflow (a leading "Show all" filter chip shifts content down so every chip shows) or a menu (a leading button lists all chip options without shifting content). Don't use the menu method on chips with a second action, like a remove icon.
- Do not apply density by default: it pushes targets under the 48×48 CSS-pixel best practice; controls for choosing density must themselves stay ≥ 48×48 CSS px.
- Multi-select sets: Space or Enter selects/deselects the focused chip; while multiple chips can be selected, only one can be in focus.
- Removal: display the remove icon whenever a chip can be removed; on mobile, if remove is the chip's only action, the icon isn't required — select the chip and press Delete. A chip with only a remove icon is one focusable element; with a second action, the chip content and remove icon are two separate focusable elements.
- Show interactivity with a secondary indicator for low-vision and cognitive accessibility: an introducing label ("Select type"), page context ("Filter results"), the outline color role (not outline variant) for ≥3:1 contrast, or an interactive label / leading icon.
- Drop-down list items: the accessibility label matches the item's text label; icons accompanying text are marked decorative to avoid redundant verbalization.

## Captured spec tables

*Reproduced as captured from notes/articles/chips--*.json; sparse rows reflect the original scrape.*

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

Measurements (`specs`, shared values across variants):

| Attribute | Value |
|---|---|
| Height / Container height | 32dp |
| Shape / Container shape | 8dp corner radius |
| Icon size | 18dp |
| Avatar shape (input chip) | 12dp corner radius |
| Avatar size (input chip) | 24dp |
| Vertical label text alignment | Center-aligned |
| Horizontal label text alignment | Start-aligned |
| Left/right padding (assist, filter) | 16dp |
| Left/right padding without icon (suggestion) | 16dp |
| Left/right padding with icon | 8dp |
| Left padding for avatar (input chip) | 4dp |
| Right padding for avatar (input chip) | 8dp |
| Left/right padding for icon (input chip) | 8dp |
| Padding between elements | 8dp |
| Target size for close icon (input chip) | Min 48dp |

Keyboard navigation (`accessibility`):

| Keys | Actions |
|---|---|
| Tab | Moves focus to enabled An enabled state communicates an interactive component or element. More on enabled state chip or chip group |
| Space or Enter | Activates, selects, or deselects the focused chip |
| Backspace or Delete | Removes currently focused A focused state communicates when a user has highlighted an element, using an input method such as a keyboard or voice. More on focuse |
| Arrows | Moves focus between chips |

Labeling elements (`accessibility`):

| Element | A11y label | Role (Web) | Role (Android Views (MDC-Android)) | Role (Jetpack Compose) |
|---|---|---|---|---|
| Image / Icon within chip | Hide image | - | - | - |
| Basic chip (one action) | "{chip content}" | gridcell | button | button |
| Selectable chip | "{chip content}" | gridcell | radio button | checkbox |
| Remove icon (no other action) | "Remove {chip content}" | - | - | - |
| Two actions (e.g., select + remove) | "{chip content}." Then "Remove {chip content}". | button or checkbox | button or checkbox | button or checkbox |

The accessibility label for a chip is the chip's label text; additional actions, like remove, are labeled separately.
