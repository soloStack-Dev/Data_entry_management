# Background Process and Application Flow

## 1. Create Entry

Flow:

``` text
User opens Data Entry page
        ↓
User fills form
        ↓
Browser validation
        ↓
HTMX POST request
        ↓
Spring Controller
        ↓
DTO validation
        ↓
Service
        ↓
Repository
        ↓
MySQL
        ↓
Success response
        ↓
HTMX updates notification/table
```

## 2. Controller Responsibility

Controller should:

-   Receive HTTP request
-   Bind form data
-   Validate DTO
-   Call service
-   Return Thymeleaf view/fragment

Controller should not contain database logic.

## 3. DTO

Use a request DTO for form input.

Example:

``` text
DataEntryRequest
```

Suggested fields:

``` text
productName
description
timing
date
type
fromUser
toUser
```

## 4. Service Responsibility

Service should:

-   Apply business rules
-   Create entity
-   Update entity
-   Delete entity
-   Search records
-   Call repository

Example methods:

``` text
createEntry()
getAllEntries()
searchEntries()
getEntryById()
updateEntry()
deleteEntry()
```

## 5. Repository

Use:

``` text
JpaRepository<DataEntry, Long>
```

Add search methods using Spring Data JPA.

For example:

``` text
findByProductNameContainingIgnoreCase(...)
```

For multi-field searching, use a JPQL query or specification strategy.

## 6. Collection Search

User enters:

``` text
keyboard
```

HTMX sends:

``` text
GET /collection/search?search=keyboard
```

Backend:

``` text
Controller
   ↓
Service
   ↓
Repository
   ↓
MySQL
```

Return only:

``` text
data-table.html
```

HTMX replaces the table content.

## 7. Edit

Flow:

``` text
Click Edit
   ↓
GET /data-entry/edit/{id}
   ↓
Load existing data
   ↓
Display form
   ↓
User modifies
   ↓
HTMX POST/PUT
   ↓
Validate
   ↓
Update database
   ↓
Return updated UI
```

## 8. Delete

Flow:

``` text
Click Delete
   ↓
Confirmation
   ↓
DELETE request
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
MySQL
   ↓
Refresh/remove row
```

## 9. HTMX Fragments

Use separate Thymeleaf fragments for partial updates.

Recommended:

``` text
fragments/
├── data-entry-form.html
├── data-table.html
├── alerts.html
└── pagination.html
```

Avoid returning an entire HTML document for every small HTMX operation.

## 10. Error Handling

Handle:

-   Validation errors
-   Record not found
-   Database errors
-   Unexpected server errors

Provide user-friendly messages.

Do not expose stack traces or database credentials to users.

## 11. Logging

Use Spring Boot logging for:

-   Application startup
-   Important operations
-   Errors
-   Unexpected exceptions

Never log:

-   Passwords
-   Database credentials
-   Sensitive user data unnecessarily
