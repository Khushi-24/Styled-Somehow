# Hostinger email login and order updates

Put these entries in ignored backend/.env, never frontend/.env or a committed file:

```properties
SMTP_HOST=smtp.hostinger.com
SMTP_PORT=465
SMTP_USERNAME=contact@styledsomehow.in
SMTP_PASSWORD=REPLACE_WITH_MAILBOX_PASSWORD
ORDER_CONFIRMATION_EMAIL_ENABLED=true
STORE_URL=http://localhost:3000
ORDER_ACCESS_SECRET=REPLACE_WITH_PRIVATE_RANDOM_SECRET_AT_LEAST_32_CHARACTERS
```

Preserve an existing valid ORDER_ACCESS_SECRET. Generate a new value only if none exists, using PowerShell:

```powershell
[guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N")
```

Use literal values without shell quotes; this .env is imported as Java properties relative to the backend working directory. Password means the mailbox password, not Hostinger account password. On deployment use the public HTTPS storefront origin and secure cookies. SMTP is the sole provider: no provider selector or API key is needed. SSL/TLS uses port 465; other ports require STARTTLS, with identity checking and bounded network timeouts.

## Log in to see orders

The account icon opens /orders. Enter the Gmail checkout address, request a six-digit code, verify, and see completed purchases newest first by createdAt (UUIDs are not chronological). The list loads 20 orders at a time; pending unpaid checkouts are excluded. Each order opens its existing detail/tracking page. There are no passwords, customer registration, or changes to guest checkout.

Limits are enforced in the database under the existing budget lock:

- Three requests per canonical Gmail inbox per rolling hour; dots and +tags share the quota.
- Sixty seconds between requests for an inbox.
- Twenty requests per observed source IP per rolling hour; five per browser per hour.
- Two hundred total code requests per rolling day to protect the mailbox allowance.
- Ten-minute code lifetime, five guesses per challenge, single-use, bound to the browser session.

Requests send a login code to any valid Gmail address, irrespective of order existence. A generic code request cannot reveal purchase history. Only after verification does a server session grant access to that exact, case-insensitive checkout email (Gmail alias canonicalization is used for limits, not broadening order ownership). A failed send still consumes quota and cannot grant access. Codes are HMAC hashed, not stored in plaintext; expired challenge data is removed after 30 days during requests. Existing older OTP/recovery endpoints remain disabled; new endpoints live under /api/order-access/login.

Login sessions last thirty minutes. Verification rotates the session ID, preserving the checkout session key. Logout clears customer and private-order grants without granting admin access or changing orders. Every list/detail/tracking request checks ownership and expiry and uses no-store caching. CSRF protection applies to code, verify and logout. IP limits use request.getRemoteAddr, not arbitrary forwarded headers: configure trusted reverse-proxy client IP handling before launch; otherwise all visitors may share the proxy IP allowance. No proxy trust was added here.

## Private email links

Confirmation and status emails retain signed, read-only View order links. These grant access only to that order, expire 90 days after payment and cannot modify orders. Keep them private. The URL fragment is stripped and exchanged for a thirty-minute server session; tokens are never stored in browser local/session storage or order JSON. The nonsecret reference may remain in the URL so refresh can resume an authorized session. A reference alone does not grant access. The confirmation page also permits access from the original checkout browser. Email providers see links in messages; do not enable click tracking for private links.

## Confirmation and status emails

Confirmed PAID orders enter a durable queue checked every 30 seconds. After confirmation is accepted by SMTP, new eligible fulfilment events queue updates for Packing, Ready to ship, Shipped, In transit and Delivered. The internal Courier booked event does not generate another Ready to ship email. Repeated polling and repeated stages do not normally generate duplicate stage notifications. One eligible event per stage is created under the order lock. Changes are committed before scheduled sending; failed email delivery never rolls back payment or preparation.

Status mail has persisted attempts, sent timestamp and retry time. Backoff is bounded; no automatic bulk resend of old events occurs. V12 marks pre-existing events ineligible to prevent historical spam. Refunded/unpaid orders are excluded. TEST preparation updates are explicitly labelled TEST and cannot book a real courier. Existing delivered-history events are not replayed.

New confirmations and updates share a unique per-order subject and deterministic Message-ID. Updates include In-Reply-To and References pointing to their confirmation; IDs survive JavaMail saveChanges and retries. This encourages threading, which each email client controls. Old confirmations sent before this change lack the new thread IDs and cannot be retroactively threaded reliably. SMTP delivery is at-least-once: ambiguous acceptance or a crash after sending may still produce a duplicate. Acceptance means provider acceptance, not inbox delivery.

## Local verification

Restart both services after pulling. Flyway V12 must apply before JPA validation. Keep ORDER_ACCESS_SECRET configured.

Run backend tests locally with Java 21:

```powershell
.\gradlew.bat test --tests "*EmailLoginIntegrationTests" --tests "*OrderEmailThreadTests" --tests "*OrderStatusEmailTests" --tests "*StatusMailIntegrationTests" --tests "*OrderConfirmationTests" --tests "*OrderAccessIntegrationTests"
```

1. Make a new TEST payment and receive the confirmation.
2. In incognito open /orders, request a code, verify and check newest-first ordering. No orders should be visible before verification.
3. Check a different email cannot read the first email’s order/detail/tracking. Test wrong/expired/reused codes and logout.
4. Verify resend cooldown, third/fourth hourly requests, IP/browser limits, and failed SMTP delivery retaining limits.
5. In admin advance the new TEST order through Packing and Ready to ship. Confirm one update per stage with the same thread headers/subject. Refreshing a stage should not resend it. No shipping is booked by this test.
6. Check history/details at 320px, 375px and desktop widths; real courier updates still need a LIVE shipment test.

Frontend lint/build are checked here; backend compilation/tests remain blocked by the unavailable Gradle distribution, and live mailbox/mobile rendering are not verified here.

## Missing mail diagnostics

Startup readiness must show enabled=true, providerConfigured=true, orderLinkConfigured=true. Check SMTP credentials/connectivity and the unchanged signing secret if not ready. The backend logs confirmation provider acceptance or failure types/retry time, without addresses, passwords, raw SMTP responses, codes or tokens. Login send failures and status failures also log safe types. Check Spam if SMTP accepted. Existing queued failures may wait until their scheduled retry. Do not post secrets when sharing logs.
