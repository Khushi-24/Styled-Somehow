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

## Continue with Google (optional)

The login screen offers Google and email OTP; both grant the same thirty-minute, email-scoped order session. Google uses Spring Security's server-side OpenID Connect authorization-code flow, with its session-bound state/nonce and ID-token validation. Only Google-verified Gmail email claims are accepted, preserving the current Gmail-only checkout rule. The Google identity is not an admin login; its access token is discarded after login and no Google token is sent to the frontend or saved in browser storage. Order ownership rules and OTP limits are unchanged.

Google is off by default. Add these to backend/.env after creating a Google Cloud OAuth web client:

```properties
GOOGLE_LOGIN_ENABLED=true
GOOGLE_CLIENT_ID=REPLACE_WITH_GOOGLE_WEB_CLIENT_ID
GOOGLE_CLIENT_SECRET=REPLACE_WITH_GOOGLE_WEB_CLIENT_SECRET
```

Leave GOOGLE_LOGIN_ENABLED=false until both real credentials are present; enabling with blank values deliberately fails startup with a configuration message. Keep secrets in backend only. No frontend env changes are needed. STORE_URL must be the storefront origin, without path/query/credentials, HTTPS in production (HTTP is accepted only for local development).

Setup:

1. In Google Cloud / Google Auth Platform choose or create a project and configure branding/consent. Add your Gmail account as a test user while the app is in Testing mode if required by the console.
2. Create an OAuth client of type Web application. Register this exact Authorized redirect URI for local development: `http://localhost:3000/api/order-access/google/callback/google`. Use the same hostname as STORE_URL: localhost and 127.0.0.1 are not interchangeable.
3. For production add `https://styledsomehow.in/api/order-access/google/callback/google` if that is the exact deployed STORE_URL. Match www/non-www exactly. This is a server-redirect flow, so no JavaScript SDK origin is required by this implementation.
4. Put the client ID/secret in backend/.env, enable the flag, reload Gradle dependencies and restart backend/frontend.
5. Open /orders, click Continue with Google, select the checkout Gmail account and verify its order history. Cancel consent, try an unverified/non-Gmail identity where available, test logout/expiry, and ensure admin access is not granted.

Next.js already proxies /api to the backend; Google authorization and callback paths deliberately stay under /api. The session cookie uses SameSite=Lax so the Google top-level GET callback can carry the state session cookie. CSRF remains enabled for mutations, with state/nonce protecting the OAuth response. In production keep COOKIE_SECURE=true and trusted proxy configuration. The Google chain does not load/save a customer OAuth principal into the admin security context.

Run additional tests locally:

```powershell
.\gradlew.bat test --tests "*GoogleCustomerLoginTests" --tests "*GoogleCustomerLoginIntegrationTests" --tests "*EmailLoginIntegrationTests"
```

Google OAuth cannot be exercised here without your client credentials and a local browser callback. Tests and backend compilation are also blocked here by the Gradle distribution download.

Official implementation references: [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect), [Google OAuth client setup](https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid), [Spring Security OAuth2 login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html), [Spring Security context persistence](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html).


## Order-page presentation
History cards now include one saved order-item image (first nonblank snapshot), item quantity, line count, date, status and total. The summary endpoint remains verified-email scoped and does not add addresses or other customers’ data to list responses. Opening a card reveals every ordered item with its saved thumbnail and a product link. Long references are available in order details instead of dominating cards. Missing images fall back to a neutral T-shirt outline.

The customer pages use a full-width storefront header, white background, black text and restrained red accents. Details have tracking/items alongside an order summary on desktop and stacked content on mobile. Opening an order scrolls to the heading; returning to the order list preserves its scroll position after loading. Product links use a reference URL to let browser Back reopen the authorized order. Confirming a reference still requires the existing session/link authorization.


## Automated message presentation and replies
Login codes use branded HTML plus a plain-text fallback, a prominent six-digit code, a ten-minute expiry note and the existing embedded logo. Status updates use a shared, table-based layout with a centred logo, clear stage title, short description, order-reference panel and View order button. The approved confirmation receipt stays intact.

All variants say they are automated, request no replies and provide a mailto link with a fresh support subject to start a new message to contact@styledsomehow.in. An explicit Reply-To directing responses to support has been removed. Mail still sends from the configured, authenticated Hostinger mailbox; no fictitious no-reply address has been introduced. Auto-Submitted: auto-generated and X-Auto-Response-Suppress: All discourage automated responses. They do not suppress human replies or remove the Gmail/Outlook Reply button. Enforcing rejection of incoming replies requires a real mailbox/server rule, not an HTML template.

Per-order subjects, Message-ID, In-Reply-To and References are preserved; these concern grouping outgoing updates, not enabling/disabling the recipient’s Reply button. Existing emails and their inbox threads cannot be restyled retrospectively. Test a fresh OTP and a new stage transition after restarting the backend. Run AutomatedEmailTemplateTests, OrderConfirmationTests and OrderEmailThreadTests locally; real email-client rendering remains a manual check.
