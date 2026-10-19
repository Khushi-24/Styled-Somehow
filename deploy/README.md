# Production rehearsal and recovery

Do not enable paid launch merely because code builds. Use an isolated staging database/media folder and TEST credentials for rehearsal. Never use the same inventory database for staging and production.

## Required setup

- Run Java 21, MySQL and Next.js under restart-managed services or containers on the selected host. Bind backend/MySQL privately; expose only the HTTPS storefront/reverse proxy. Set request-body limits and edge rate limits for login, OTP, checkout, PIN and payment-start routes.
- Persist the MySQL data and `MEDIA_DIRECTORY` independently of application releases. A deploy must not replace these folders. Take consistent backups before migrations; retain the previous application release for rollback. Do not blindly reverse Flyway migrations after writes.
- Backend: set `COOKIE_SECURE=true`, correct `STORE_URL=https://...`, database credentials, BCrypt admin hash, fresh `ORDER_ACCESS_SECRET`, SMTP and provider credentials. Set `ORDER_CONFIRMATION_EMAIL_ENABLED=true` after verifying SMTP. Leave `RAZORPAY_LIVE_ENABLED=false` and `SHIPROCKET_BOOKING_ENABLED=false` until controlled account verification is complete. `APP_PRODUCTION=true` enforces secure-cookie/HTTPS/private-link/email readiness at startup; it does not perform paid provider actions.
- Frontend: set `STORE_URL` and `BACKEND_URL` for the deployment. Rebuild when changing `BACKEND_URL` because Next rewrites are generated during the build. Configure the exact public Google callback in Google Cloud and the Razorpay webhook at `/api/payments/razorpay/webhook` with its matching secret. Do not put credentials in NEXT_PUBLIC variables.
- Validate client IP handling with your chosen trusted proxy. Never enable arbitrary X-Forwarded-For trust. Current application limits use the observed remote address; a proxy shared by everyone requires correctly configured edge limits and trusted forwarding before launch.
- Monitor `/api/health` for liveness and `/api/health/ready` for DB readiness, HTTPS expiry, process restarts, HTTP failures, disk capacity, payment/refund backlog, failed mail and tracking jobs. Alert delivery must be tested.
- Verify seller legal/contact address, invoice/tax treatment and published policy wording before public launch. No personal address or tax registration has been invented in code.

## Encrypted off-server backups

`backup.sh` expects `mysqldump`, `gpg`, `tar`, `rclone`, `sha256sum` and the environment variable names at the top of the script. Keep the private MySQL option file outside the repository with owner-only permissions; its `[client]` section supplies host/user/password. Use a public GPG recipient whose private decryption key is backed up separately. Configure an off-server rclone destination. Backups are encrypted before leaving the host. No backup values are committed.

Example schedule after a successful manual rehearsal: hourly database/media backup using a systemd timer or cron under a dedicated account; alert on nonzero exit. This script makes a MySQL-consistent snapshot but does not provide a cross-filesystem atomic snapshot with concurrent media uploads. It retains media rather than deleting it, and a restore must check all product/order image references. Add upload-triggered copies or pause writes for a coordinated pre-release snapshot. Define retention and storage budgets on the backup destination; nothing is deleted automatically.

## Mandatory restore drill (isolated environment only)

1. Download one backup into a private temporary directory and run `sha256sum -c SHA256SUMS`.
2. Decrypt `database.sql.gpg` and restore into a NEW isolated MySQL database; never target production during a drill. Decrypt/extract `media.tar.gpg` into a NEW empty media directory.
3. Start a staging backend against those restored paths with payments, booking and real email disabled. Ensure background jobs cannot call production accounts.
4. Verify catalogue, historical images, order snapshots, shared stock/reservations, migration version and admin access. Record backup time, restoration time, missing assets and any recovery steps.
5. Only after a successful drill approve the backup schedule. Deployment, scheduler installation, off-server credentials and actual restore cannot be verified from source alone.

## Launch switches

- The admin Orders page can pause NEW checkouts/payment starts. Existing callbacks, verification, refunds and tracking continue. This is not infrastructure shutdown.
- LIVE mode requires a matching `rzp_live_` key AND `RAZORPAY_LIVE_ENABLED=true`; TEST remains the default. Match webhook credentials to that account/mode and verify a controlled small payment/refund before broad launch.
- Shipping comparison/booking uses the same default parcel formula: 11 x 13 inches, 1 inch height per tee, 0.5 kg for each started pair, up to 20 tees. These are defaults, not measured proof for every design/quantity. Verify packed dimensions and weight before accepting larger orders; override admin measurements when necessary and absorb any agreed merchant-side quote difference.
- Labels, pickup scheduling, exchanges, partial refunds and courier cancellation/rebooking remain reviewed operations in provider dashboards. The portal can reconcile a matching existing provider shipment; it does not blindly recreate ambiguous bookings.
