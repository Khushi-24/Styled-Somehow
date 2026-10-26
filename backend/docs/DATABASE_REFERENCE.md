# Database table reference

Schema baseline: Flyway migrations **V1–V20**. There are **22 application tables**, plus Flyway's `flyway_schema_history` system table. An existing local database has this full schema only after all these migrations have run.

This is a developer reference, not a database reset script. Table names below are the actual database names. PK means primary key; FK means database-enforced foreign key. Names such as product slugs and hashed emails can also connect records logically without an FK.

## 1. Products and collections

| Table | Purpose / important data | Primary key | Relationships | Introduced |
| --- | --- | --- | --- | --- |
| `products` | Product name, slug/URL, selling price, original price, descriptions, composition, GSM, New In flag, display ordering and DRAFT/PUBLISHED/ARCHIVED status. | `id` | Parent of the four product list tables below and `costing_profiles`. | V1 |
| `product_collections` | Collection slug assignments for a product, in order. Stores the slug in `value`. | `(product_id, position)` | FK `product_id` → `products.id`. `value` logically identifies `shop_collections.slug`, but has no FK to it. | V1 |
| `product_colours` | Colours offered for a product, in order. | `(product_id, position)` | FK `product_id` → `products.id`. | V1 |
| `product_sizes` | Sizes offered for a product, in order. | `(product_id, position)` | FK `product_id` → `products.id`. | V1 |
| `product_media` | Image/video URL paths, descriptions, colour, media type, ordering and full-artwork display flag. Stores paths/metadata, not image/video bytes. | `(product_id, position)` | FK `product_id` → `products.id`. | V1 |
| `shop_collections` | Managed collection slug, display name, active flag and ordering. | `slug` | Collection assignments use its slug logically through `product_collections.value`. | V17 |

Products can share the same colour/size stock. A separate inventory row is not created for each design.

## 2. Inventory

| Table | Purpose / important data | Primary key | Relationships | Introduced |
| --- | --- | --- | --- | --- |
| `blank_stock` | Shared blank T-shirt pool per colour/size: on-hand `quantity`, `reserved`, low-stock threshold and optimistic-lock `version`. | `id` | Referenced by `stock_movements.stock_id` and `pending_order_lines.stock_id`. | V2; reservations added in V3 |
| `stock_movements` | Audit ledger of stock changes: positive/negative `delta`, resulting quantity, reason, actor, date and unique adjustment request reference. | `id` | FK `stock_id` → `blank_stock.id`. | V2 |

- **Available stock = on-hand quantity − reserved quantity.**
- Creating a pending checkout reserves stock. Expiry/cancellation releases the reservation.
- Captured payments deduct on-hand stock; payment processing records a stock movement.
- Designs using Black M all consume the same Black M pool, regardless of their product names.
- Deleting orders or refunding a payment does not automatically restore physical blank stock. Restore justified quantities through a recorded adjustment; printed/used garments may no longer be usable blanks.
- Order references in `stock_movements.reason` are text, not FKs. Keep the ledger even when intentionally cleaning up dummy orders.

## 3. Orders, payments and delivery

| Table | Purpose / important data | Primary key | Relationships | Introduced |
| --- | --- | --- | --- | --- |
| `pending_orders` | Main order record: customer name/email/phone, delivery address snapshot, totals, payment status/mode/references, paid time, fulfilment stage, courier/personal delivery method, confirmation-email attempts and processing leases. **Includes paid and completed orders despite its name.** | `id` | Parent of lines, payment receipts, shipment booking and fulfilment events. OTP challenges can refer to it logically. | V3; extended through V18 |
| `pending_order_lines` | Purchased item snapshots: product slug/name, colour, size, quantity, unit price, image and stock row. Keeps purchase details independent of later catalogue edits. | `(order_id, position)` | FK `order_id` → `pending_orders.id`; FK `stock_id` → `blank_stock.id`. Product `slug` is a snapshot, not a product FK. | V3 |
| `payment_receipts` | Gateway payment identity and amount, processing status, refund reference/idempotency key, reason, retries, timestamps and refund-worker lease. | `id` | FK `order_id` → `pending_orders.id`. | V6; worker lease added V15 |
| `shipment_bookings` | Booking status, courier ID/name, quoted merchant shipping cost, provider order/shipment IDs, AWB, provider message, tracking status/URL/check time and terminal/failure flags. | `order_id` | FK `order_id` → `pending_orders.id`. At most one booking row per local order. | V8; tracking fields added V10 |
| `fulfilment_events` | Stage-change audit history with actor, internal note and occurrence time. Also tracks status-email eligibility, sent time, retries and next attempt. | `id` | FK `order_id` → `pending_orders.id` **ON DELETE CASCADE**. | V10; email fields V12; timestamp precision V16 |

### Order and customer information

Customer details currently live in `pending_orders`; there is no separate persistent customer profile table. Multiple orders may have the same email. Email login uses that email to find the customer's eligible order history.

Deleting an order removes its stored customer/address snapshot. If that person has another order, details in the other order remain. Marketing consent is not recorded by the current schema; a checkout email is not a marketing opt-in.

Order confirmation email delivery fields are on `pending_orders`; stage-update email delivery fields are on `fulfilment_events`. There is no standalone email queue table.

## 4. Login, OTP and request protection

| Table | Purpose / important data | Primary key | Relationships | Introduced |
| --- | --- | --- | --- | --- |
| `order_access_challenges` | OTP verification challenges: code hash, email/IP/browser hashes, creation/expiry times, attempt count and consumed flag. Optional `order_id` supports the original order-specific access flow; `login_email` supports email login. | `id` | Optional logical `order_id` → `pending_orders.id`; **no FK**, so order deletion does not remove these records automatically. | V4; login email added V12 |
| `order_access_budget` | Single coordination row locked during quota checks so simultaneous OTP requests cannot bypass limits. **Not a financial budget or customer balance.** | `id` | No FK. Used by OTP services. | V4 |
| `order_access_email_blocks` | Inbox-hash-based temporary blocking records for the original order-access OTP service. Current email-login request quotas are checked through challenge history; do not assume this table represents every login restriction. | `email_hash` | No FK. | V5 |
| `commerce_attempts` | Recent request timestamps and scope with hashed IP/browser/email identifiers. Used for checkout/payment-related request limits. | `id` | No FK. | V13 |
| `commerce_control` | Shared store ordering pause/resume flag and who last changed it, with timestamp. Also supplies a coordination lock for commerce checks. | `id` | No FK. | V13 |

Admin credentials are configured outside these tables. Customer access uses sessions/verified email; this schema has no separate `users`, `customers`, saved-address or persisted session table. Carts are stored in browser storage.

## 5. T-shirt costing and monthly expenses

| Table | Purpose / important data | Primary key | Relationships | Introduced |
| --- | --- | --- | --- | --- |
| `costing_profiles` | Product costing settings: customer shipping collected per T-shirt, monthly-allocation flag, update time and version. | `product_id` | FK `product_id` → `products.id`. At most one saved profile per product. | V19 |
| `costing_items` | Selected/unselected cost entries and amounts: outer/inner packaging, thank-you card, cloth tag, freebies, shipping/personal delivery, DTF printing, payment fees and extras. Null amounts mean not entered; zero is explicitly free. | `(product_id, position)` | FK `product_id` → `costing_profiles.product_id`; unique `(product_id, cost_key)`. | V19 |
| `costing_months` | Monthly electricity, website/hosting, AI subscription, other expenses and expected T-shirt sales volume, with version. | `month_key` (`YYYY-MM`) | Independent monthly budget; product settings opt into the selected month's allocation. No FK to a profile/order. | V19 |

The fixed blank T-shirt cost of **₹280** is enforced in `CostingCalculator`, not stored as an editable cost entry. Cost profiles are private current planning estimates; they are not historical cost snapshots or actual order profit records. See [Costing and profit estimates](../../docs/COSTING_AND_PROFIT.md) for formulas and limitations.

### Test order cleanup audit

| Table | Purpose / important data | Primary key | Relationships | Introduced |
| --- | --- | --- | --- | --- |
| `test_order_deletions` | Records the deleted TEST order reference, previous status, actor, reason, timestamp, optional restored stock units and released reservations. Prevents repeated deletion requests from restoring stock twice. No customer email/address is copied here. | `order_id` | Deliberately has no FK to the deleted order; the audit survives cleanup. | V20 |

Admin cleanup accepts only explicitly marked TEST orders. LIVE/unassigned orders, active payment/refund processing and any shipment record are protected. A pending test checkout releases only its own reservations. On-hand restoration is unchecked by default and requires matching recorded test-payment stock deductions plus explicit physical-stock confirmation. Changes since the preview invalidate it. Original stock movements are retained; restored units get a new movement. Provider records and already-sent emails are unaffected. Manual stock restorations made before preview cannot reliably be inferred, so the owner must leave restoration disabled for already-corrected stock.

## 6. Flyway system table

| Table | Purpose | Managed by |
| --- | --- | --- |
| `flyway_schema_history` | Installed migration versions/scripts, checksums, execution times and success state. | Flyway automatically |

Do not delete migration history to clear order data. Do not edit already-applied migration files; introduce a new migration when changing the schema.

## 7. Foreign keys and deletion behaviour

These are **database rules for direct SQL**, taken from the migrations. Application-managed collection cleanup is a separate concern.

| Parent being deleted | Child table / FK | Database behaviour while child rows exist |
| --- | --- | --- |
| `products` | `product_collections.product_id` | Blocks deletion; no cascade declared. |
| `products` | `product_colours.product_id` | Blocks deletion; no cascade declared. |
| `products` | `product_sizes.product_id` | Blocks deletion; no cascade declared. |
| `products` | `product_media.product_id` | Blocks deletion; no cascade declared. |
| `products` | `costing_profiles.product_id` | Blocks deletion; no cascade declared. |
| `costing_profiles` | `costing_items.product_id` | Blocks deletion; no cascade declared. |
| `blank_stock` | `stock_movements.stock_id` | Blocks deletion; no cascade declared. |
| `blank_stock` | `pending_order_lines.stock_id` | Blocks deletion; no cascade declared. |
| `pending_orders` | `pending_order_lines.order_id` | Blocks deletion; no cascade declared. |
| `pending_orders` | `payment_receipts.order_id` | Blocks deletion; no cascade declared. |
| `pending_orders` | `shipment_bookings.order_id` | Blocks deletion; no cascade declared. |
| `pending_orders` | `fulfilment_events.order_id` | Automatically deletes matching events: `ON DELETE CASCADE`. |
| `pending_orders` | `order_access_challenges.order_id` | Logical link only: records remain unless explicitly cleaned up. |
| `shop_collections` | `product_collections.value` | Logical link only: raw SQL deletion does not remove product assignments. |

For an intentionally reviewed dummy-order cleanup, related challenges, lines, payment receipts and shipment rows must be handled before removing parent orders; events can cascade. Preserve products, inventory rows, costing data and stock history. Reconcile stock separately to confirmed physical quantities. Local SQL deletion does not cancel shipments or delete payment history in provider dashboards.

## 8. Index reference

Every application table has the PK shown above; a primary key creates an index. The following additional indexes/unique constraints are declared in migrations:

| Table | Index name or constraint | Columns | Purpose |
| --- | --- | --- | --- |
| `products` | Unique constraint | `slug` | Product URL lookup and uniqueness. |
| `blank_stock` | Unique constraint | `(colour, size)` | One stock pool per variant. |
| `stock_movements` | Unique constraint | `request_id` | Prevent duplicate adjustments. |
| `pending_orders` | Unique constraint | `(session_key, request_id)` | Checkout idempotency. |
| `pending_orders` | `pending_order_expiry` | `(status, expires_at)` | Expired reservation processing. |
| `pending_orders` | `order_gateway_reference` (unique) | `gateway_order_id` | Match gateway orders and prevent duplicate references. |
| `pending_orders` | `idx_order_confirmation_queue` | `(status, confirmation_sent_at, confirmation_next_attempt_at)` | Pending confirmation-email work. |
| `pending_orders` | `orders_email_history` | `(email, created_at)` | Support customer email/history queries. |
| `payment_receipts` | `refund_work_queue` | `(status, updated_at)` | Pending refund work. |
| `shipment_bookings` | Unique constraints | `provider_shipment_id`; `awb` | Prevent duplicate shipment/AWB assignments. |
| `shipment_bookings` | `idx_shipment_tracking_due` | `(status, tracking_terminal, tracking_checked_at)` | Tracking refresh selection. |
| `fulfilment_events` | `idx_fulfilment_events_order` | `(order_id, occurred_at)` | Order event timeline. |
| `fulfilment_events` | `fulfilment_mail_queue` | `(notification_eligible, notification_sent_at, notification_next_attempt_at)` | Pending status emails. |
| `order_access_challenges` | `order_access_email_limit` | `(email_hash, created_at)` | Inbox request limits. |
| `order_access_challenges` | `order_access_ip_limit` | `(ip_hash, created_at)` | IP request limits. |
| `order_access_challenges` | `order_access_browser_limit` | `(browser_hash, created_at)` | Browser request limits. |
| `order_access_challenges` | `order_access_daily_limit` | `created_at` | Global request windows and expiry cleanup support. |
| `order_access_challenges` | `order_access_order_cleanup` | `order_id` | Find order-specific challenges during cleanup. |
| `commerce_attempts` | `commerce_attempts_window` | `(scope, created_at)` | Recent attempts by request scope. |
| `costing_items` | Unique constraint | `(product_id, cost_key)` | One entry per cost category per profile. |

MySQL/InnoDB also creates supporting FK indexes when no suitable index already exists. Their generated names can vary; inspect the installed database rather than assuming names. Index presence does not prove every query is optimized: `LOWER(column)`, leading-wildcard searches, sorting and additional filters can require scans. Use `EXPLAIN` with representative data before changing indexes.

## 9. Read-only database inspection

List the tables installed in the currently selected database:

```sql
SHOW TABLES;
```

Inspect a table's exact installed definition, including FKs/deletion rules:

```sql
SHOW CREATE TABLE pending_orders;
SHOW CREATE TABLE fulfilment_events;
```

Inspect installed indexes:

```sql
SHOW INDEX FROM pending_orders;
```

Check applied migrations:

```sql
SELECT version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

List FK relationships and deletion rules for this database:

```sql
SELECT
    k.TABLE_NAME AS child_table,
    k.COLUMN_NAME AS child_column,
    k.REFERENCED_TABLE_NAME AS parent_table,
    k.REFERENCED_COLUMN_NAME AS parent_column,
    r.DELETE_RULE
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.REFERENTIAL_CONSTRAINTS r
  ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
 AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
 AND r.TABLE_NAME = k.TABLE_NAME
WHERE k.TABLE_SCHEMA = DATABASE()
  AND k.REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY parent_table, child_table, k.ORDINAL_POSITION;
```

## 10. Keeping this reference current

Update this file when a migration adds/removes a table, changes relationships/deletion behaviour or adds an index. The authoritative definitions are in [the migrations folder](../src/main/resources/db/migration).
