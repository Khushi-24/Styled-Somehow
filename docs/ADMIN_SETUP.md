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
