# Hostinger order emails and private order links

Add these entries to the ignored backend/.env file (not frontend/.env):

```properties
SMTP_HOST=smtp.hostinger.com
SMTP_PORT=465
SMTP_USERNAME=contact@styledsomehow.in
SMTP_PASSWORD=REPLACE_WITH_MAILBOX_PASSWORD
ORDER_CONFIRMATION_EMAIL_ENABLED=true
STORE_URL=http://localhost:3000
```

Keep the existing ORDER_ACCESS_SECRET, at least 32 characters. It signs private order links. Changing it invalidates links already emailed. Set STORE_URL to the public HTTPS storefront origin on deployment, not localhost. No new credentials or configuration entries are required for the private-link update.

The project imports backend/.env as a Java properties file. Enter literal values without shell quotes; escape backslashes if present. Never commit passwords. The password is the mailbox password used in Gmail, not the Hostinger account password.

SMTP uses SSL on 465 (required STARTTLS on other ports), hostname verification and finite connection/read/write timeouts. Confirmations use HTML plus plain text with the existing logo embedded inline. TEST orders are labelled in the subject and message and never promised dispatch.

## Order access
The email View order button contains a signed, read-only link bound to the specific order, payment identity and checkout email. It expires 90 days after payment and can be reopened during that period. Possession of the link grants access: customers must keep it private. It cannot modify orders or access other orders.

The capability is in the URL fragment (#access), which is not sent in the initial page HTTP request. The client strips it from the address bar and exchanges it using a CSRF-protected POST for a server-side, 30-minute, order-specific session. It is not stored in browser localStorage or sessionStorage and is never serialized into order JSON. Responses have no-store caching and the order page uses no-referrer/noindex metadata. Email providers still see the link because it is in the email; disable third-party click tracking for these private links.

The confirmation-page View order button can also grant access from the original checkout session, without a token or extra email. A reference alone on a different browser does not grant access. Refreshing the order page can resume its existing verified session.

Public email-code and recovery forms have been removed. POST /api/order-access/request and /verify now return 410 without sending email. Lost/expired email links direct the customer to contact@styledsomehow.in. Verify ownership before providing private order information. Existing old emails do not automatically gain secure tokens; test with a newly paid TEST order. No bulk resend is triggered.

## Confirmation dispatch
A durable database queue checks every 30 seconds. Only PAID orders after commit are sent. Row locking suppresses simultaneous duplicate sends; failures preserve payment status and retry with increasing delay. SMTP delivery is at-least-once: a crash or ambiguous network acceptance may cause a duplicate. Recorded sending means provider acceptance, not inbox delivery. Flyway V9 skips historical terminal orders.

## Verification
Frontend lint and production build pass. Backend tests cannot run here because Gradle download is blocked; no backend compilation or email-client visual verification is claimed.

Local backend check:
```powershell
.\gradlew.bat test --tests "*OrderLinkServiceTests" --tests "*OrderAccessIntegrationTests" --tests "*OrderConfirmationTests"
```

1. Restart backend and frontend after pulling.
2. Place a new Razorpay TEST order and verify confirmation starts at the top.
3. Open View order on the confirmation page: it must show that order without requesting email.
4. Open the email link in a different browser/incognito: it must show only that order with no OTP.
5. Refresh; close order access; refresh again and ensure details are hidden. Reopen the original email link.
6. A changed signature, expired link or reference alone from another session must fail.
7. The retired request/verify endpoints must not send email.
8. Admin TEST courier review remains read-only; no real booking is enabled by this work.

Tests cover signature tampering, expiry, key rotation, identity changes, session scope, CSRF, closing access, retired email endpoints, failed-send retries and escaped HTML.

## Diagnosing missing confirmations
Recent fulfilment changes did not change SMTP settings. Only new paid orders trigger confirmations; preparation/tracking changes do not send emails. A sent confirmation is not sent again on page refresh or a stage update.

On startup check `Order confirmation email readiness` warnings: confirmations enabled, provider configured and order-link configured must all be true. SMTP is the only email implementation. For Hostinger use ORDER_CONFIRMATION_EMAIL_ENABLED=true, SMTP_HOST=smtp.hostinger.com, SMTP_PORT=465, SMTP_USERNAME and SMTP_PASSWORD, and an ORDER_ACCESS_SECRET of at least 32 characters. Keep an existing valid signing secret unchanged. The backend loads .env relative to its working directory; start from backend when using backend/.env. Never post the password or signing secret.

For a new PAID order, the worker scans every 30 seconds. It logs provider acceptance or a failure with exception types and a scheduled retry, without recipient addresses, raw SMTP responses, tokens or credentials. Authentication errors suggest checking mailbox credentials; socket/connection errors suggest connectivity; ResponseStatusException can indicate missing order-link configuration. These are diagnostic clues, not proof of the cause. Backoff may delay subsequent attempts for hours. Payment status is never changed by email delivery. SMTP acceptance is not proof of inbox delivery: check Spam and the mailbox provider if acceptance was logged.
