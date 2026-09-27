# UI Components Specification

## 1. Overall Design

Create a modern data-entry management dashboard.

Design direction:

-   Clean
-   Professional
-   Minimal
-   Business/dashboard oriented
-   Responsive
-   Easy to scan
-   Desktop-first but mobile compatible

Do not make the interface look like a social-media application.

## 2. Header

Include:

-   Application logo/icon
-   Application name
-   Navigation links
-   Optional profile/user area

Navigation:

``` text
Home | Data Entry | Collection
```

Use Bootstrap navbar utilities.

## 3. Dashboard Cards

Home page cards:

``` text
Total Entries
Today's Entries
Recent Entries
```

Each card should contain:

-   Small icon
-   Label
-   Number
-   Optional short supporting text

Use Bootstrap cards with a subtle border and shadow.

## 4. Data Entry Form

Use a centered dashboard card/panel.

Form layout:

``` text
Product Name     [_____________________]

Description      [_____________________]

Timing           [__________]

Date             [__________]

Type             [ Select Type ▼ ]

From User        [_____________________]

To User          [_____________________]

                [ Save Entry ]
```

Use responsive Bootstrap grid:

``` text
col-12
col-md-6
```

for fields where appropriate.

Description should use a textarea.

## 5. Collection Table

Top section:

``` text
Collection
-----------------------------------------
[ Search entries... ]     [New Entry]
```

Table:

``` text
Product | Description | Timing | Date | Type | From | To | Actions
```

Actions:

``` text
[Edit] [Delete]
```

Use separate buttons.

Edit:

``` text
btn-outline-primary
```

Delete:

``` text
btn-outline-danger
```

## 6. Search

Search field should:

-   Have search icon
-   Show placeholder text
-   Filter by product name and relevant text fields
-   Send HTMX request
-   Replace only the table body/fragment

Example concept:

``` html
<input
    type="search"
    name="search"
    placeholder="Search entries..."
    hx-get="/collection/search"
    hx-trigger="keyup changed delay:300ms"
    hx-target="#data-table"
    hx-swap="innerHTML">
```

## 7. Empty State

When there are no records:

``` text
No entries found

Create your first data entry to see it here.

[Create Entry]
```

## 8. Delete Confirmation

Before deleting:

``` text
Are you sure you want to delete this entry?

[Cancel] [Delete]
```

Do not delete immediately from an accidental click.

## 9. Responsive Behavior

Desktop:

-   Full table
-   Full navigation
-   Multi-column form

Mobile:

-   Stack form fields
-   Allow horizontal table scrolling or use a responsive table strategy
-   Stack action buttons where necessary
-   Keep touch targets large enough
