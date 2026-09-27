# Input Validation Specification

## 1. Validation Strategy

Use two levels:

``` text
Browser validation
       +
Spring Boot Bean Validation
```

Server-side validation is authoritative.

Never trust only browser-side validation.

## 2. Product Name

Rules:

-   Required
-   Minimum: 2 characters
-   Maximum: 150 characters
-   Trim leading/trailing whitespace

Example:

``` java
@NotBlank
@Size(min = 2, max = 150)
private String productName;
```

## 3. Description

Rules:

-   Required
-   Maximum: 2000 characters
-   Trim unnecessary whitespace

Example:

``` java
@NotBlank
@Size(max = 2000)
private String description;
```

## 4. Timing

Rules:

-   Required
-   Use a clear time format
-   Prefer HTML time input

Example:

``` html
<input type="time" name="timing">
```

Java:

``` java
@NotNull
private LocalTime timing;
```

## 5. Date

Rules:

-   Required
-   Use HTML date input

``` html
<input type="date" name="date">
```

Java:

``` java
@NotNull
private LocalDate date;
```

If the business rule requires only current/future dates, implement that
rule explicitly in the service/validation layer.

## 6. Type

Use a controlled list rather than allowing arbitrary values.

Example:

``` text
Purchase
Sale
Transfer
Return
Other
```

Use an enum in Java if the values are fixed.

## 7. From User

Rules:

-   Required
-   Maximum length: 100
-   Trim whitespace

## 8. To User

Rules:

-   Required
-   Maximum length: 100
-   Trim whitespace

Optional business rule:

``` text
From User and To User should not be identical
```

Implement only if this matches the application's actual requirement.

## 9. Validation Error UI

Display the error near the corresponding field.

Example:

``` text
Product Name
[________________]

Product name is required.
```

Use Bootstrap validation classes.

## 10. Duplicate Submission Protection

Prevent accidental repeated submission.

During HTMX submission:

``` text
Save Entry
     ↓
Saving...
     ↓
Disable submit button
```

Re-enable after completion.

## 11. Security Validation

Server-side validation should also protect against:

-   Unexpectedly large input
-   Invalid dates
-   Invalid enum values
-   Malformed requests

Do not concatenate raw user input into SQL.

Use Spring Data JPA parameters/repositories.
