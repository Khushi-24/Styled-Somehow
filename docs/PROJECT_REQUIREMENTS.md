# styled_somehow

> A modern, scalable e-commerce platform for a Gen-Z-focused clothing brand.

**styled_somehow** is a production-oriented e-commerce application being built for a modern clothing brand, initially focused on T-shirts.

The project consists of two primary applications:

1. **Customer Storefront** — for browsing products, purchasing products, managing accounts, and tracking orders.
2. **Admin Panel** — for managing products, inventory, orders, customers, and categories.

The system is intentionally designed to be **simple enough for the initial MVP while remaining extensible for future features**.

---

# 1. Project Vision

The goal of styled_somehow is to build a fast, modern, visually appealing, and reliable e-commerce platform that provides a smooth shopping experience across desktop and mobile devices.

The platform should support the complete customer journey:

```text
Discover
   ↓
Browse Products
   ↓
View Product
   ↓
Select Size
   ↓
Add to Cart
   ↓
Checkout
   ↓
Payment
   ↓
Order Confirmation
   ↓
Order Tracking
```

The platform should also provide administrators with the tools required to operate the store efficiently.

---

# 2. Target Audience

### Primary Audience

* Gen-Z girls
* Young women
* Fashion-conscious customers
* Customers interested in quirky and expressive clothing

### Brand Characteristics

The website's visual and UX direction should communicate:

* Quirky
* Aesthetic
* Gen-Z
* Comfortable
* Stylish
* Modern
* Effortless
* Expressive

The design should feel like a **fashion brand**, not a generic e-commerce template.

---

# 3. Project Goals

The application should prioritize:

* Excellent user experience
* Responsive design
* Fast page performance
* Clean architecture
* Maintainable code
* Secure APIs
* Reliable inventory management
* Simple administration
* Future extensibility
* Production readiness

The project should avoid unnecessary complexity during the MVP phase.

---

# 4. Technology Stack

## Frontend

* Next.js
* TypeScript
* Tailwind CSS
* Next.js App Router

## Backend

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* REST APIs

## Database

* MySQL

## Architecture

```text
┌─────────────────────┐
│     Next.js         │
│     Frontend        │
└──────────┬──────────┘
           │
           │ REST APIs
           ↓
┌─────────────────────┐
│    Spring Boot      │
│      Backend        │
└──────────┬──────────┘
           │
           │ JPA / Hibernate
           ↓
┌─────────────────────┐
│       MySQL         │
└─────────────────────┘
```

### Important Architecture Rule

The frontend must **never connect directly to MySQL**.

All database operations must go through the Spring Boot backend.

---

# 5. Repository Structure

```text
styled-somehow/
│
├── frontend/              # Next.js application
│
├── backend/               # Spring Boot application
│
├── docs/                  # Architecture and project documentation
│
├── .gitignore
│
└── README.md
```

The repository may evolve as the project grows, but changes to the structure should be made intentionally.

---

# 6. Customer Features

## 6.1 Homepage

The homepage should include:

* Navigation / header
* Hero section
* Featured products
* New arrivals
* Categories
* Brand / story section
* Promotional section
* Footer

The homepage should be visually focused and optimized for the target audience.

---

## 6.2 Shop / Product Listing

Customers should be able to:

* Browse products
* Search products
* Filter by category
* Filter by size
* Filter by price
* Sort products
* Navigate through products using pagination when required

---

## 6.3 Product Details

Each product page should support:

* Product name
* Price
* Description
* Product images
* Available sizes
* Size selection
* Variant stock availability
* Quantity selection
* Add to cart
* Wishlist
* Product information
* Shipping information
* Return information

The UI should clearly communicate when a selected size is unavailable.

---

## 6.4 Cart

The cart should display:

* Products
* Selected size / variant
* Quantity
* Individual product price
* Remove item
* Update quantity
* Subtotal
* Shipping charges
* Total
* Proceed to checkout

Cart calculations must be validated by the backend.

The frontend must not be treated as the source of truth for prices, discounts, inventory, or totals.

---

# 7. Checkout

Checkout should support:

### Customer Information

* Name
* Email
* Phone number

### Shipping Address

* Address
* City
* State
* Postal code
* Country

### Order Summary

* Products
* Selected variants
* Quantities
* Subtotal
* Shipping charges
* Discounts
* Final total

### Payment

Payment integration will be implemented using a suitable payment provider.

Payment status must be verified by the backend rather than trusting client-side payment information.

### Order Confirmation

After a successful order:

* Create the order
* Record order items
* Record payment status
* Update inventory
* Display order confirmation
* Provide order details to the customer

---

# 8. Authentication

The application should support:

* Sign up
* Login
* Logout
* Password reset

Authentication and authorization must be handled securely by the backend.

Protected resources must not rely solely on frontend route protection.

---

# 9. Customer Account

Authenticated customers should be able to access:

### Profile

* View profile
* Update profile

### Addresses

* View addresses
* Add address
* Edit address
* Delete address

### Orders

* View orders
* View order details
* View order status

### Wishlist

* View wishlist
* Add products
* Remove products

---

# 10. Admin Panel

The admin panel will provide store-management functionality.

## 10.1 Dashboard

The dashboard should display relevant business information such as:

* Total orders
* Total sales
* Customers
* Products
* Low-stock products

The initial dashboard should remain simple.

Advanced analytics are outside the MVP scope.

---

## 10.2 Product Management

Admins should be able to:

* Add products
* Edit products
* Delete products
* Upload product images
* Manage prices
* Manage sizes / variants
* Manage inventory
* Activate / deactivate products

---

## 10.3 Category Management

Admins should be able to:

* Create categories
* Edit categories
* Delete categories

Category deletion must account for products currently associated with the category.

---

## 10.4 Order Management

Admins should be able to:

* View orders
* View order details
* View customer information
* View payment status
* Update order status

Order status transitions should follow defined business rules rather than allowing arbitrary invalid states.

---

## 10.5 Customer Management

Admins should be able to:

* View customers
* View customer details
* View customer orders

Sensitive customer information must not be unnecessarily exposed.

---

# 11. Product & Inventory Model

A product may contain multiple variants.

Example:

```text
Product
└── Whatever Oversized Tee
    │
    ├── S   → stock: 10
    ├── M   → stock: 15
    ├── L   → stock: 8
    └── XL  → stock: 5
```

Inventory must be tracked **independently for every variant / size**.

For example:

```text
T-Shirt A
S  = 10
M  = 20
L  = 5
XL = 0
```

If XL inventory reaches zero, XL must be unavailable while the other sizes remain purchasable.

Inventory operations must be handled safely to prevent overselling.

---

# 12. Product Images

Product images must **not** be stored directly inside MySQL.

Images should eventually be stored using an external object/image storage provider such as:

* Amazon S3
* Cloudinary
* Another suitable storage provider

The database should store only the relevant image references / URLs and metadata.

Example:

```text
Product
   ↓
ProductImage
   ↓
image_url
```

The exact storage provider should be selected when image infrastructure is implemented.

---

# 13. MVP Scope

The MVP should prioritize the functionality required to operate a real online store.

### Must Have

* Product browsing
* Product listing
* Product details
* Search
* Basic filtering
* Cart
* Customer authentication
* Checkout
* Payment
* Orders
* Inventory
* Customer account
* Admin product management
* Admin order management
* Basic customer management
* Category management

### MVP Principle

Only features required to operate the initial store should be implemented.

Do not build future functionality simply because the architecture could support it.

---

# 14. Future Features

The following features should **not** be implemented during the initial MVP unless explicitly requested:

* Product reviews
* Product ratings
* Loyalty points
* Referral system
* AI recommendations
* Advanced analytics
* Advanced personalization
* Other non-essential features

The architecture should allow these capabilities to be added later without requiring a complete rewrite.

---

# 15. Responsive Design

The website must follow a responsive-first approach.

It should work correctly across:

* Desktop
* Laptop
* iPhone
* iPad
* Android phones
* Android tablets

Supported modern browsers should include:

* Chrome
* Safari
* Edge
* Firefox

There should be **one responsive web application**, not separate mobile and desktop applications.

---

# 16. Backend Development Principles

The Spring Boot backend should follow a clean layered architecture.

A typical structure may be:

```text
controller
    ↓
service
    ↓
repository
    ↓
database
```

### Guidelines

* Use REST APIs.
* Keep business logic out of controllers.
* Use DTOs where appropriate.
* Validate incoming requests.
* Use centralized exception handling.
* Return appropriate HTTP status codes.
* Use meaningful API responses.
* Use database constraints where appropriate.
* Keep transactions clearly defined.
* Avoid unnecessary abstractions.
* Avoid unnecessary design patterns.
* Keep services focused and maintainable.

---

# 17. Frontend Development Principles

The frontend should use:

* Next.js App Router
* TypeScript
* Tailwind CSS
* Reusable components

### Guidelines

* Keep components small and maintainable.
* Prefer server components where appropriate.
* Use client components only when client-side functionality is required.
* Avoid unnecessary client-side rendering.
* Handle loading states.
* Handle error states.
* Handle empty states.
* Provide useful user feedback.
* Keep API communication organized.
* Avoid duplicating business logic unnecessarily.

---

# 18. Database Principles

The database will use MySQL.

Database design should:

* Use proper relational modeling.
* Define relationships explicitly.
* Use appropriate primary keys.
* Use foreign keys where appropriate.
* Use database constraints where appropriate.
* Avoid unnecessary duplication.
* Avoid storing calculated values unless there is a clear reason.
* Track inventory at the variant level.
* Store timestamps for important entities.
* Design for future extensibility without over-engineering.

---

# 19. Security Principles

Security is a core requirement.

The application should:

* Authenticate users securely.
* Authorize admin-only operations.
* Validate and sanitize incoming data.
* Protect sensitive endpoints.
* Never expose passwords.
* Never store plaintext passwords.
* Avoid trusting prices or totals sent by the client.
* Verify payment status on the backend.
* Prevent unauthorized access to other customers' data.
* Use secure configuration for secrets.
* Never commit passwords, API keys, tokens, or other secrets to Git.

Environment-specific secrets should be stored using environment variables or an appropriate secret-management mechanism.

---

# 20. API Principles

REST APIs should follow predictable conventions.

Example:

```text
GET    /api/products
GET    /api/products/{id}
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}

GET    /api/categories
POST   /api/categories

GET    /api/orders
GET    /api/orders/{id}
POST   /api/orders
```

Exact endpoints may evolve as implementation progresses.

API design decisions should prioritize:

* Consistency
* Security
* Maintainability
* Clear request/response contracts
* Appropriate HTTP methods
* Appropriate HTTP status codes

---

# 21. Error Handling

The application must handle errors intentionally.

### Backend

Use centralized exception handling for API errors.

Errors should provide useful information without exposing internal implementation details.

### Frontend

The UI should handle:

* Loading
* API failures
* Empty results
* Invalid input
* Authentication failures
* Payment failures
* Out-of-stock situations

Users should receive understandable feedback instead of raw technical errors.

---

# 22. Testing

Testing should be introduced alongside functionality rather than being postponed until the end.

The project should eventually include appropriate tests for:

### Backend

* Unit tests
* Service-layer tests
* Controller/API tests
* Repository/integration tests where appropriate
* Security-related tests

### Frontend

* Component tests where valuable
* Important user-flow tests
* API integration testing where appropriate

Critical business logic such as:

* Pricing
* Inventory
* Order creation
* Authentication
* Authorization
* Payment handling

should receive particular attention.

---

# 23. Development Strategy

Development will be incremental.

The planned development sequence is:

```text
Phase 1
Project Foundation
        ↓
Phase 2
Architecture & Database Design
        ↓
Phase 3
Spring Boot Backend Foundation
        ↓
Phase 4
Next.js Frontend Foundation
        ↓
Phase 5
Product / Catalog
        ↓
Phase 6
Authentication
        ↓
Phase 7
Cart
        ↓
Phase 8
Checkout & Payment
        ↓
Phase 9
Orders & Inventory
        ↓
Phase 10
Admin Panel
        ↓
Phase 11
Testing & Refinement
        ↓
Phase 12
Deployment
```

Features from later phases should **not be implemented prematurely** unless there is a specific reason.

Each phase should leave the project in a stable and understandable state.

---

# 24. Git & Version Control

Git should be used throughout development.

Commits should:

* Represent logical changes.
* Be reasonably small.
* Have meaningful commit messages.
* Avoid committing secrets.
* Avoid committing generated build artifacts.
* Avoid unrelated changes.

Example:

```text
feat: add product entity
feat: implement product listing API
feat: add product listing page
fix: prevent duplicate cart items
refactor: simplify product service
```

---

# 25. AI Coding Agent Rules

AI coding agents such as Cline may be used during development.

The AI agent is an implementation assistant, **not the final decision-maker**.

Before modifying the project, the AI agent must:

1. Understand the existing repository.
2. Inspect relevant files.
3. Understand the current architecture.
4. Check existing implementations before creating new ones.
5. Follow the documented requirements.
6. Avoid unnecessary technologies and dependencies.

### The AI agent must NOT:

* Rewrite the entire application without instruction.
* Create unnecessary files.
* Create unnecessary abstractions.
* Introduce unnecessary libraries.
* Modify unrelated files.
* Change architecture without justification.
* Assume undefined business requirements.
* Implement future features prematurely.
* Ignore errors.
* Hide failing tests.
* Remove existing functionality simply to make code compile.

### When implementing a feature

The AI agent should:

```text
Understand
   ↓
Inspect
   ↓
Plan
   ↓
Explain significant decisions
   ↓
Implement a small change
   ↓
Run relevant tests
   ↓
Review result
   ↓
Fix issues
```

The AI agent should make **small, reviewable changes** rather than attempting to build the entire application in one operation.

If a requirement materially affects architecture, security, database design, or business logic and has not been defined, the agent should ask for clarification before proceeding.

The human developer retains final control over all architectural and product decisions.

---

# 26. Documentation

The `docs/` directory should contain important project documentation as the system grows.

Potential documentation includes:

```text
docs/
├── architecture/
├── database/
├── api/
├── decisions/
└── setup/
```

Important architectural decisions should be documented when they are significant enough to affect future development.

---

# 27. Environment Configuration

Environment-specific configuration must not be hardcoded into source code.

Examples include:

* Database credentials
* API keys
* Payment credentials
* Authentication secrets
* Cloud storage credentials
* External service configuration

Use environment variables or appropriate configuration mechanisms.

Example:

```text
.env
.env.local
```

Environment files containing secrets must be excluded from Git.

A safe example configuration file may be provided when necessary:

```text
.env.example
```

---

# 28. Performance Principles

Performance should be considered from the beginning without premature optimization.

The application should prioritize:

* Efficient database queries
* Appropriate indexing
* Optimized images
* Efficient API responses
* Minimal unnecessary client-side JavaScript
* Appropriate caching when required
* Pagination for large datasets

Performance optimizations should be based on actual requirements rather than unnecessary complexity.

---

# 29. Scalability Philosophy

The application should be **extensible, not over-engineered**.

The MVP does not require:

* Microservices
* Kubernetes
* Event-driven architecture everywhere
* Complex distributed systems
* Multiple databases
* Advanced caching infrastructure

unless a real requirement emerges.

The initial architecture should remain straightforward:

```text
Next.js
   ↓
Spring Boot
   ↓
MySQL
```

The system can evolve as the business grows.

---

# 30. Definition of Done

A feature should not be considered complete simply because the code compiles.

A feature is considered complete when:

* The requirement is implemented.
* Relevant edge cases are handled.
* Validation is implemented where required.
* Security implications are considered.
* Errors are handled.
* Relevant tests pass.
* The UI handles loading/error/empty states where applicable.
* No unrelated files were unnecessarily modified.
* The implementation follows the existing architecture.
* The feature is manually reviewed when appropriate.

---

# 31. Current Project Status

The project is being developed incrementally.

### Foundation

* [x] Development environment setup
* [x] Git repository initialized
* [x] Repository structure created
* [ ] Backend project
* [ ] Frontend project
* [ ] Database design

### Backend

* [ ] Spring Boot foundation
* [ ] Database connection
* [ ] Entity model
* [ ] Repository layer
* [ ] Service layer
* [ ] REST API
* [ ] Authentication
* [ ] Authorization
* [ ] Product APIs
* [ ] Cart APIs
* [ ] Order APIs
* [ ] Inventory APIs
* [ ] Admin APIs

### Frontend

* [ ] Next.js foundation
* [ ] Design system
* [ ] Homepage
* [ ] Product listing
* [ ] Product details
* [ ] Authentication
* [ ] Cart
* [ ] Checkout
* [ ] Customer account
* [ ] Admin panel

### Launch

* [ ] Testing
* [ ] Security review
* [ ] Performance review
* [ ] Production configuration
* [ ] Deployment
* [ ] Domain configuration
* [ ] Production verification

---

# 32. Core Principle

> **Build only what is needed now, but structure the system so it can grow later.**

The priority is not to build the largest possible system.

The priority is to build a **clean, reliable, production-ready e-commerce MVP** that can evolve as styled_somehow grows.
