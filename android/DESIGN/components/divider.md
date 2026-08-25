# Divider (M3)

> Source: https://m3.material.io/components/divider/overview
> Source: https://m3.material.io/components/divider/guidelines
> Source: https://m3.material.io/components/divider/specs
> Source: https://m3.material.io/components/divider/accessibility

## Variants/types

- **Full-width divider** — separates larger sections of unrelated content; usable on surfaces or inside cards/lists; can split interactive from non-interactive areas.
- **Inset (middle-inset) divider** — equally indented from both sides by default; separates related content (e.g., emails in a list); align with anchoring elements like icons or avatars.
- **Vertical divider** — arranges content on larger screens (e.g., text beside media).

M2→M3: new color mappings; vertical dividers added.

## Anatomy

- A simple line — no other elements.

## Key dimensions

| Attribute | Value |
|---|---|
| Divider full-width | 100% |
| Divider inset left margin | 16dp |
| Divider inset right margin | 0dp |
| Divider middle-inset left margin | 16dp |
| Divider middle-inset right margin | 16dp |
| Space between divider & supporting-text | 4dp |
| Divider right margin | 8dp |
| Divider bottom margin | 8dp |

## States

Divider is non-interactive: single enabled state only.

No state layers apply — see design.md for the general interaction-state model.

## Behavior

- Groups content and creates hierarchy; can imply nested parent/child relationships.
- If used both ways on one screen, full-width separates different kinds of content and inset separates nested items, reinforcing hierarchy.
- Repetitive list items may skip inset dividers when spacing alone suffices.
- Keep lines visible but not bold; prefer open space over dividers for grouping where possible; group things rather than separating individual items.
- Use sparingly — too many lines clutter the interface.

## Token group

Specs capture a single **Color** token group under `md.comp.divider.*`:

- Color role: outline-variant (light and dark schemes).

## Accessibility summary

- Dividers are decorative elements with no contrast minimums required.

## Captured spec tables

### Specs — Measurements

| Attribute | Value |
|---|---|
| Divider full-width | 100% |
| Divider inset left margin | 16dp |
| Divider inset right margin | 0dp |
| Divider middle-inset left margin | 16dp |
| Divider middle-inset right margin | 16dp |
| Space between divider & supporting-text | 4dp |
| Divider right margin | 8dp |
| Divider bottom margin | 8dp |

### Overview — Availability & resources

| Type | Resource | Status |
|---|---|---|
| Design | Design Kit (Figma) | Available |
| Implementation | Flutter | Available |
| Implementation | Jetpack Compose | Available |
| Implementation | Android Views (MDC-Android) | Available |
| Implementation | Web | Available |
