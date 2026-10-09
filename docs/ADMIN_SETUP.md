# Admin catalogue — local setup and acceptance checks

The first admin slice is `/admin`. It uses Spring Boot authentication and a MySQL catalogue. Product data is no longer duplicated in frontend components. Run both apps; without the backend, the storefront explicitly shows catalogue unavailability rather than an outdated hardcoded copy.

## Requirements

Java 21, the repository Gradle wrapper, MySQL 8+, Node/npm. The current execution environment has Java 17 and cannot download Gradle, so backend compilation/integration checks have not been run here. Frontend lint and production build passed. HTTP rendering checks for home, admin, both collections, existing product pages and unknown-product 404 passed against an isolated catalogue fixture server. Those checks do not verify the Spring backend or browser interactions.

1. Create an empty `styled_somehow` database and a dedicated local database user with permissions for this database. Do not use root for the application.
2. Temporary local settings now live directly in `backend/src/main/resources/application.properties`. Replace `your_database_username` and `your_database_password` with your local MySQL credentials. No environment setup is needed for this local configuration.

The local admin username is `admin`. Set `admin.password-hash` in `application.properties` to a BCrypt hash of a password you choose. No shared password is published. Generate the hash locally in an interactive terminal from the backend folder:

```powershell
$classpath = .\gradlew.bat -q adminRuntimeClasspath
java -cp "$classpath" com.styledsomehow.backend.security.PasswordHash
```

The helper prompts for your password without echoing it. Copy the resulting hash into `admin.password-hash`. Keep the completed credentials local. The session cookie is temporarily configured for local HTTP (`secure=false`); production needs HTTPS and secure cookies.

From the backend directory run:

```powershell
.\gradlew.bat bootRun
```

On Linux/macOS use `bash gradlew bootRun`. Do not commit your real database password after replacing the placeholder. Before deployment, move credentials into private environment configuration, set private admin credentials and enable secure cookies.

3. In the frontend directory, no `.env.local` is required for localhost. The frontend already defaults to `http://127.0.0.1:8080`. Run:

```bash
npm install
npm run dev
```

Open `http://localhost:3000/admin`. The browser uses same-origin `/api` requests proxied to `BACKEND_URL`, default `http://127.0.0.1:8080`. Keep the backend and database private in production. Changing `BACKEND_URL` requires restarting Next; for a production build, rebuild when changing rewrite configuration.

Flyway creates catalogue tables and imports existing products once. Use an empty development database initially; do not enable automatic Flyway baselining on an existing populated database. Existing health-only databases need a reviewed migration plan if they contain other data. Hibernate validates the schema instead of creating/updating it.

The previous committed database credentials have been removed from configuration. Rotate that database password because it remains in Git history; deletion from the current file does not revoke it.

## Adding products from now on

1. Sign in, choose **Add product**, enter name, a unique lowercase URL, descriptions and prices.
2. Choose Women/Men, New In, colours and S–XL availability. Availability here is catalogue selection, not stock quantity.
3. Upload ordered photos and assign each to a colour. The first must be an image and becomes the listing photo. Use **Show complete artwork** for detail images.
4. Save as DRAFT. Drafts require complete core fields and at least one photo per colour, but remain hidden.
5. Select PUBLISHED and save. Open the storefront in a fresh tab/reload to verify listings, the detail page, prices and gallery.
6. Later edits go through the same editor. ARCHIVED hides listings and the public detail URL; records and media are retained. Existing product URLs cannot change. New In and collection order are managed separately.

New photos accept JPEG/PNG/WebP up to 8 MB; type is checked from file signatures. Originals are stored locally without optimization. Existing Sabr video is preserved; new video uploads/transcoding are a future feature. Removing a photo from a product does not delete its disk file. Back up the database and the entire media directory together; storage cleanup must only remove unreferenced files after review.

This is a local first slice. Inventory allocation, checkout, payments, orders, automatic courier integration, MFA and full audit history are not implemented. Do not launch live sales yet.

## Tests and manual acceptance

```bash
# backend
bash gradlew test
# frontend
npm run lint
npm run build
```

Backend tests use an isolated H2 database in MySQL compatibility mode and Flyway seed data; test credentials are fixtures only. Also run against real MySQL before deploying.

Acceptance: unauthorized access rejected; CSRF-free mutations rejected; incorrect login rejected; real login works; upload photo; create draft hidden publicly; publish visible in the chosen collection/New In/detail; edit price; archive hidden; stale concurrent edit rejected; logout rejects mutations; restart preserves edits/media. On 390px mobile and desktop verify original logo, gallery swipe/rotation, full artwork, fixed desktop details, colour photos/video, and return to the exact originating card.

Automatic shipping is the chosen plan, but its provider remains unselected; no courier account is connected or booking request sent by this code.

## Shared blank inventory

Open `/admin/inventory` from **Manage stock** after signing in. All products use one confirmed oversized blank type. Stock is shared by colour and size, not duplicated per artwork. Exact colour names `White` and `Black` map to those stock pools; other colours are unavailable until their inventory support is added.

V2 imports White and Black S/M/L at 12 each, XL at zero, once via Flyway. Do not edit V1 or rerun opening quantities manually. Restarting does not reset inventory. Future garment types require separate pools/mapping before use.

Use **Adjust**, enter a positive/negative quantity change and a reason, then save. Negative stock is rejected. The history shows the last 100 changes; older entries remain in the database. Version checks reject stale edits and request identifiers prevent duplicate retries. Low stock threshold is 3; XL remains visible but unavailable until restocked.

Public product availability is shared, and add-to-cart rechecks it. Cart additions do not consume or reserve stock. Reservation/consumption and order concurrency protection will be implemented with checkout; this slice does not make live ordering safe yet.

Check locally: initial total 72; XL disabled on product pages; subtract White S until zero and verify all White designs disable S after reload; restock and verify re-enabled; reason/history shown; restart preserves updates. Backend tests were added but still cannot run in this workspace without Java 21 and reachable Gradle dependencies.

## Cart and guest checkout — free shipping

Homepage bag and product-page Cart links open `/cart`. Cart quantities/removal persist in this browser. Names/prices are refreshed from the catalogue, and `/api/cart/quote` recalculates totals from database prices. Shipping is always ₹0, with no minimum order value.

`/checkout` collects an Indian delivery address, email, 10-digit mobile and six-digit PIN. Format validation does not yet verify courier serviceability. The server creates an UNPAID `PENDING_PAYMENT` record with immutable line prices and an address snapshot; this is not a confirmed sale. Do not print or dispatch these records. Payment and email integrations are still pending.

Checkout reserves shared colour/size blanks for 15 minutes. Expiry runs every minute and also before checkout operations; cancellation releases immediately. On-hand quantities do not decrease on reservation. Admin inventory shows on-hand, reserved and available separately. Stock adjustments cannot reduce on-hand below existing reservations. Paid-order consumption will be added with payment verification.

Database locks serialize checkout reservation changes across the eight current pools, including different designs using the same blank. READ_COMMITTED transactions avoid stale stock reads. This intentionally conservative small-store approach needs capacity testing before live launch. Retried checkout requests reuse an idempotency identifier; the backend rejects changed payloads and allows one active checkout per browser session. Customer reads/cancellation require that same browser session and an opaque order reference. Logging into admin preserves the guest reference through session rotation; logging out/session expiry can lose access until reservation expiry. Admin `/admin/orders` lists the most recent 100 records for inspection.

Cart remains intact until a future verified payment flow clears it. No pay button, gateway request, customer email or shipment is sent yet. Do not put real customer data into test fixtures or Git. Before live use add payment verification, checkout abuse limits, serviceability, approved policies/tax settings and reservation reconciliation after payment.

Acceptance checks:
1. Add a tee, open cart, change quantity and remove an item; reload and verify persistence/count.
2. Check ₹0 shipping and current server prices. Combined White S quantities across Cherry/Untamed must not exceed the shared available blanks.
3. Create pending checkout; inspect `/admin/orders` and reserved/available inventory.
4. Repeat submission/reload: no duplicate reservation. Another browser session must not read its reference.
5. Cancel: available stock returns. Create another and let it expire: stock returns within the next expiry sweep, or immediately on checkout refresh.
6. Verify simultaneous attempts at the last blank allow only one reservation. Test on real MySQL, not only H2.

Frontend lint/build checks run locally. Java integration and concurrency tests have been added but cannot execute in this workspace with its Java 17 and unreachable Gradle download. Run `./gradlew.bat test` using Java 21 locally before proceeding to payments. V3 migration adds reservation/order tables; do not alter already applied V1/V2 migrations.


## Secure guest order access (email OTP)

Customers can use `/orders` from another browser with the order reference and checkout email. This is read-only order access, not customer registration or a full account. Homepage account icons now lead to this page. Courier tracking is still pending integration.

Brevo is the initial transactional email provider. Its currently advertised free plan is 300 sends/day shared across all emails (verified 8 October 2026). The application conservatively caps OTP requests at 200 in any rolling 24 hours, five per canonical Gmail inbox per rolling 24 hours, five per browser per hour, twenty per observed client IP per hour, and one per email per minute. Requests for nonexistent/mismatched orders also consume limits. Behind a reverse proxy configure trusted forwarded client IP handling before launch; otherwise users share the proxy IP limit. Do not trust arbitrary forwarded headers from the public internet.

Create a Brevo account, authenticate the sending domain / verify the sender, and create a transactional API key. In your local `backend/src/main/resources/application.properties` add:

```properties
email.brevo-api-key=YOUR_PRIVATE_BREVO_API_KEY
email.sender=YOUR_VERIFIED_SENDER_EMAIL
order-access.secret=YOUR_PRIVATE_RANDOM_SECRET_AT_LEAST_32_CHARACTERS
```

Generate the last value locally in PowerShell with:

```powershell
[guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N")
```

Never commit these filled values, show them in screenshots, or send them in chat. Production should supply these through private configuration/environment references. Blank/missing settings keep the backend running but disable email order access with a clear unavailable message. No emails are actually sent until configured. No paid provider subscriptions are activated by this code.

Codes expire after ten minutes, permit five attempts and are single-use, keyed with a private HMAC secret, and bound to the requesting browser session. Verification grants read access to exactly one order for thirty minutes, with a Close order access button. A verified guest cannot cancel another browser's checkout through this lookup. Changing the secret invalidates outstanding codes. Failed delivery does not expose provider errors or undo abuse limits. Customers receive a generic response so unmatched details don't disclose orders. Challenge records contain hashes and are removed after thirty days during requests.

Test using a real email address: create a pending checkout, save the reference, open another browser/private window, visit `/orders`, enter the reference and email, retrieve the real email OTP, and verify. Confirm wrong/reused codes fail, a code cannot verify in another browser, and Close removes access. Pending checkouts remain explicitly unpaid; viewing one is not payment confirmation. Actual provider delivery, domain DNS, MySQL migration V4 and mobile layout still need manual verification.

Automatic purchase confirmation emails and customer accounts remain future work alongside verified payment integration.


### Gmail-only and daily request blocks

Checkout and order lookup accept only the exact `gmail.com` domain (case-insensitive), with visible field hints and backend validation. This excludes legitimate customers using other providers and does not guarantee a Gmail account is not disposable. Existing non-Gmail orders cannot use the new OTP lookup; contact the store instead.

Gmail dotted addresses and +tags share one inbox limit. Five accepted OTP requests are allowed per rolling 24 hours. The sixth starts a persisted three-hour block on requests and code verification for that inbox, across browser sessions/IP addresses. Repeated requests during the block do not extend it. After three hours, the five-per-24-hour allowance still applies until an older request ages out; the block does not grant five extra requests. Once quota is available and the block has ended, requests resume normally. Failed delivery and unmatched references count as accepted requests; invalid-domain submissions and attempts rejected by the minute/IP/browser/global limits do not create a sent-code request.

V5 clears pre-existing short-lived OTP challenges to apply canonical inbox hashing consistently. Existing orders and stock are preserved. Apply migrations by restarting the backend after pulling. Private keys/secrets remain unchanged.


## Razorpay test payments

This release accepts only `rzp_test_` keys. It cannot accept live payments. Keep real credentials private and out of Git. Add these entries only to your local `backend/src/main/resources/application.properties`:

```properties
payments.razorpay.key-id=YOUR_TEST_KEY_ID
payments.razorpay.key-secret=YOUR_TEST_KEY_SECRET
payments.razorpay.webhook-secret=YOUR_PRIVATE_RANDOM_SECRET_AT_LEAST_32_CHARACTERS
```

Generate the separate webhook secret in PowerShell with `[guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N")`. It is not the Razorpay API secret. Restart the backend (Flyway applies V6), then restart the frontend. In the Razorpay TEST dashboard, enable automatic capture. The backend confirms only captured payments, not authorized payments or an unverified browser success message.

Create a checkout, click **Pay in Razorpay TEST mode**, and complete a Razorpay sandbox payment. Verify PAID/TEST in admin Orders, quantity reduced once in Inventory, and the original reservation released. Retry refreshing/verification and confirm there is no second deduction. Also test failure, modal dismissal, cancellation and payment after the 15-minute reservation expires. A late payment uses available unreserved stock, or enters the persistent refund queue. Refund retries reuse the same idempotency key. Unexpected amounts/currencies or refunds made directly in Razorpay require manual review.

Admin Orders includes payment references, refund references, retry counts and review reasons. Test payments reduce your development inventory; restore quantities through Inventory after testing. Do not dispatch any TEST order.

For webhooks, the backend must have a public HTTPS address; Razorpay cannot call localhost. When that address exists, configure the TEST webhook URL `https://YOUR_BACKEND/api/payments/razorpay/webhook`, with the same local webhook secret, and events `payment.captured`, `payment.authorized`, `payment.failed`, `refund.processed`, `refund.failed`. The webhook verifies the raw request signature and fetches payment details from Razorpay. Duplicate deliveries cannot consume stock twice.

Local testing does not require a public webhook: while the backend is running, a scheduled job rotates through gateway orders and reconciles pending refunds every minute. Large queues can take multiple cycles. **Check payment status** also fetches current gateway data immediately. Background reconciliation continues if the browser closes; it stops when the backend stops.

Automated tests mock the provider. Real sandbox checkout, dashboard capture settings, webhook delivery, refunds, mobile popup behavior and MySQL migration must still be verified locally. Live launch requires a separate review, production credential handling, policies, monitoring, refund operations and disabling test mode; simply replacing keys will not enable live payments.

References: https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps/ and https://github.com/razorpay/markdown-docs/blob/master/api/refunds/normal-refunds-idempotent.md
