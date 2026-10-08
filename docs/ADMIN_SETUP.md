# Admin catalogue — local setup and acceptance checks

The first admin slice is `/admin`. It uses Spring Boot authentication and a MySQL catalogue. Product data is no longer duplicated in frontend components. Run both apps; without the backend, the storefront explicitly shows catalogue unavailability rather than an outdated hardcoded copy.

## Requirements

Java 21, the repository Gradle wrapper, MySQL 8+, Node/npm. The current execution environment has Java 17 and cannot download Gradle, so backend compilation/integration checks have not been run here. Frontend lint and production build passed. HTTP rendering checks for home, admin, both collections, existing product pages and unknown-product 404 passed against an isolated catalogue fixture server. Those checks do not verify the Spring backend or browser interactions.

1. Create an empty `styled_somehow` database and a dedicated local database user with permissions for this database. Do not use root for the application.
2. Export the variables below in your backend terminal; Spring does not load `.env` files automatically. Keep actual secrets out of Git and shell history where possible.

```bash
export DB_URL='jdbc:mysql://localhost:3306/styled_somehow'
export DB_USERNAME='your_app_user'
export DB_PASSWORD='your_database_password'
export ADMIN_USERNAME='your_owner_username'
export ADMIN_PASSWORD_HASH='your_bcrypt_hash'
export SESSION_COOKIE_SECURE=false
export MEDIA_DIRECTORY="$PWD/storage/media"
bash gradlew bootRun
```

`ADMIN_PASSWORD_HASH` must be a BCrypt hash of a strong owner password. Generate it locally with a trusted BCrypt tool (cost 12 recommended), or use the password helper:

```bash
java -cp "$(bash gradlew -q adminRuntimeClasspath)" com.styledsomehow.backend.security.PasswordHash
```

The helper reads the password without echoing it. The hash itself is printed for placement in the private environment. There is no default admin account/password. Production must use HTTPS and `SESSION_COOKIE_SECURE=true` (the default).

3. In the frontend directory, copy `.env.example` to `.env.local`, then:

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
