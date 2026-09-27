# Data Entry Management Platform --- Project Specification

## 1. Project Goal

Build a data entry management web application using:

-   Java
-   Spring Boot
-   Spring MVC
-   Thymeleaf
-   HTMX
-   HTML5
-   CSS
-   Bootstrap
-   MySQL
-   Docker
-   Spring Data JPA
-   Bean Validation
-   Spring Boot Actuator

The application should provide a clean data-entry-management-dashboard
experience.

## 2. Main Pages

### Home Page

Route:

``` text
/
```

Purpose:

-   Dashboard-style landing page.
-   Show application title and short description.
-   Show summary cards:
    -   Total Entries
    -   Today's Entries
    -   Recently Added
-   Provide navigation to Data Entry and Collection.
-   Provide a quick action button to create a new entry.

### Data Entry Page

Route:

``` text
/data-entry
```

Purpose:

-   Display the data-entry form.
-   Allow the user to create a new record.
-   Submit the form using HTMX without requiring a full-page refresh.
-   Show validation errors beside the relevant inputs.
-   Show success/error feedback after submission.

Fields:

``` text
Product Name
Description
Timing
Date
Type
From User
To User
```

### Collection Page

Route:

``` text
/collection
```

Purpose:

-   Display saved records in a responsive table.
-   Include a search field.
-   Filter records using HTMX.
-   Provide Edit and Delete actions for every row.
-   Confirm deletion before removing a record.
-   Allow pagination when the dataset becomes large.

## 3. Suggested Navigation

``` text
Home
Data Entry
Collection
```

Header should remain consistent across pages.

## 4. Data Flow

``` text
User
  ↓
Thymeleaf + Bootstrap UI
  ↓
HTMX Request
  ↓
Spring MVC Controller
  ↓
Service Layer
  ↓
JPA Repository
  ↓
MySQL
```

## 5. Core Features

-   Create data entry
-   Read data
-   Search/filter data
-   Edit data
-   Delete data
-   Server-side validation
-   Client-side validation
-   HTMX partial updates
-   Responsive design
-   Success/error notifications
-   Database persistence
-   Dockerized MySQL
-   Production health endpoint

## 6. Suggested Package Structure

``` text
com.example.dataentry
├── controller
├── service
├── repository
├── entity
├── dto
├── exception
├── config
└── DataEntryApplication
```

Resources:

``` text
src/main/resources/
├── templates/
│   ├── index.html
│   ├── data-entry.html
│   ├── collection.html
│   └── fragments/
│       ├── header.html
│       ├── navbar.html
│       ├── alerts.html
│       ├── data-entry-form.html
│       └── data-table.html
├── static/
│   ├── css/
│   └── js/
└── application.properties
```
