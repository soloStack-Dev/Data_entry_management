# UI Visual Style, Fonts, Icons and Animations

## 1. Visual Style

Use a modern enterprise/data-management dashboard style.

Design principles:

-   White or very light page background
-   Clear content hierarchy
-   Rounded cards
-   Subtle borders
-   Moderate shadows
-   Good spacing
-   Strong readability
-   Avoid excessive gradients
-   Avoid excessive animations

## 2. Typography

Recommended font:

``` text
Inter
```

Fallback:

``` text
system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif
```

Use:

``` text
Heading: 600–700
Body: 400
Labels: 500
Buttons: 500–600
Table headers: 600
```

Avoid using multiple font families.

## 3. Icons

Use one consistent icon library.

Recommended:

``` text
Bootstrap Icons
```

Use icons for:

``` text
Home
Data Entry
Collection
Search
Edit
Delete
Calendar
Clock
User
Save
Plus
Filter
```

Do not mix many unrelated icon libraries.

## 4. Button Design

Primary action:

``` text
Create Entry
Save Entry
```

Secondary action:

``` text
Cancel
Back
```

Danger action:

``` text
Delete
```

Every button should have clear text. Icons can accompany the text.

## 5. Animation Rules

Animations should be subtle.

Use:

``` text
150ms–250ms
ease-in-out
```

Recommended animations:

### Button

-   Slight hover transition
-   Slight active press effect

### Card

-   Small shadow transition on hover

### HTMX Content

When table content changes:

-   Short fade/transition

### Notifications

-   Fade in
-   Stay visible briefly
-   Fade out

Avoid:

-   Large bouncing animations
-   Constant movement
-   Long loading animations
-   Decorative animation that slows data entry

## 6. Loading State

During HTMX requests:

``` text
Saving...
Searching...
Loading...
```

Use a small spinner only where necessary.

Example:

``` html
<button
    type="submit"
    hx-indicator="#save-spinner">
    Save Entry
</button>

<span id="save-spinner" class="htmx-indicator">
    Saving...
</span>
```

## 7. Accessibility

Use:

-   Proper labels
-   Visible focus states
-   Semantic HTML
-   `aria-label` where required
-   Keyboard-accessible controls
-   Sufficient text/background contrast

Never depend on icons alone for important actions.
