# Admin costing and profit estimates

Open **Admin → Costing & profit** (`/admin/costing`). Cost profiles and monthly budgets are stored in the backend database and are never added to public product responses.

- Blank T-shirt: fixed ₹280.00, enforced by the backend.
- Each design has editable outer/inner packaging, thank-you card, cloth tag, freebies, shipping/personal-delivery, DTF print, payment fee and extra costs. All start selected. A selected blank amount is unknown; enter zero explicitly for free items, or untick to exclude them. Unticking preserves the amount.
- Selling price comes from the catalogue, not MRP. Customer shipping collected is separate revenue and defaults to zero. Enter per-T-shirt shares for multi-item packaging and shipments.
- Save updates the profile, not the product price, stock or existing orders. Profiles are current planning estimates shared across months, not historical accounting snapshots. Search and thumbnail listing cover published, draft and archived products.
- Monthly electricity, website/hosting, AI subscription and other expenses are saved by month. Forecast unit sales determine the optional allocation. Product estimates use only the saved budget. Selecting a different month does not replace product costs.

## Calculation

`Revenue = current selling price + customer shipping collected per T-shirt`

`Direct cost = ₹280 + selected cost amounts`

`Optional overhead = monthly total / forecast T-shirts sold` (rounded to paise)

`Estimated profit = revenue - direct cost - optional overhead`

`Margin = estimated profit / revenue × 100`

Any unknown selected cost, or enabled allocation without a positive sales forecast, withholds profit and margin. Negative values remain visible as losses. Calculations use decimal money in Java and integer paise for the client preview.

This screen does not claim actual monthly net profit. Actual sales, advertising, returns/refunds, taxes and actual expenses require reconciliation. Do not subtract monthly expenses twice if they have already been allocated. No shipping provider, payment or email action is performed by costing.

## Checks

Backend tests cover monetary arithmetic, missing values, disabled items, monthly allocation, losses, admin authorization, CSRF, database persistence, product isolation, stale versions and public API privacy. Frontend tests cover calculator parity; browser checks cover save/reload, toggles and monthly allocation at phone, tablet and desktop widths using fixtures.
