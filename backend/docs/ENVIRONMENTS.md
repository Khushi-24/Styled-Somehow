# Backend environments

All commands below run with **backend/** as the working directory. Never commit real credentials.

| Profile | Secret file | Purpose |
| --- | --- | --- |
| local (default) | .env.local; legacy .env also supported | Local development |
| staging | .env.staging | Isolated rehearsal database and media |
| production | .env.production | Live deployment; readiness checks enabled |

Only the active profile imports its corresponding secret file. Local imports the legacy .env first and .env.local second, so .env.local wins. Staging and production do not load the local .env. Missing files are optional for deployments that supply environment variables instead; required credentials still have no usable defaults.

## Windows PowerShell

For local development:

```powershell
Copy-Item .env.local.example .env.local
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

For staging on your laptop:

```powershell
Copy-Item .env.staging.example .env.staging
.\gradlew.bat bootRun --args="--spring.profiles.active=staging"
```

Copy each template only once; do not overwrite an already configured secret file. Edit the copy and replace every REPLACE_WITH value. Existing local .env users can continue running without creating .env.local. Use exactly one environment profile at a time. IntelliJ: set working directory to backend and program arguments to --spring.profiles.active=staging (or local).

The database URL determines which machine/database receives writes. For staging on your laptop, create a separate local styled_somehow_staging database and grant its user access. Do not point laptop tests at the hosted production database. Migrations create tables, not the database or MySQL user. Run local and staging one at a time unless you configure different server.port values and frontend BACKEND_URL values.

## Values and format

These .env files are parsed as Java .properties, not shell dotenv: use KEY=value without surrounding quotes or export. Copy the BCrypt hash literally, including dollar signs. Escape literal backslashes as double backslashes; avoid trailing spaces. Keep these files out of screenshots and logs. Generate ORDER_ACCESS_SECRET privately (for example openssl rand -hex 32 on Linux); do not reuse the placeholder. Production needs SMTP credentials, ORDER_CONFIRMATION_EMAIL_ENABLED=true and an HTTPS STORE_URL to pass readiness checks. Live payments and courier booking stay off until explicitly enabled after verification. Google login is optional.

## Server

Store credentials outside the repository, e.g. /etc/styled-somehow/staging.properties. It uses the same KEY=value contents as .env.staging. Explicitly add it when starting the backend:

```bash
java -jar /path/to/backend.jar --spring.profiles.active=staging --spring.config.additional-location=file:/etc/styled-somehow/staging.properties
```

For production use the production profile and production.properties path. The service account must be able to read the external file; use restricted directory/file ownership, not world-readable secrets. Configure absolute persistent MEDIA_DIRECTORY paths separately for staging and production. The already-created server staging.properties can be retained; no need to duplicate it into the repository.

Staging hard-disables live-payment and booking flags through the profile even if RAZORPAY_LIVE_ENABLED or SHIPROCKET_BOOKING_ENABLED is true. Supply TEST keys. Staging email and Google login default off but can be configured deliberately for rehearsal. Production enables app.production and secure cookies regardless of APP_PRODUCTION/COOKIE_SECURE placeholders. Do not override the underlying Spring properties to bypass these safeguards.

Frontend configuration remains separate in frontend/.env.local; these profiles apply only to Spring Boot. Configure/rebuild the frontend BACKEND_URL for the selected backend.
