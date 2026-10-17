import styles from "./shipping-policy.module.css";
import StoreHeader from "../../components/store-header";

export const metadata = { title: "Shipping policy | Styled Somehow" };

export default function ShippingPolicy() {
  return (
    <main className={styles.page}><div className={styles.shell}>
      <StoreHeader dark/>
      <header className={styles.hero}><h1>Shipping policy</h1></header>
      <div className={styles.content}>
        <section aria-labelledby="shipping-charges">
          <h2 id="shipping-charges">What are the shipping charges?</h2>
          <p>Enjoy free shipping when your product subtotal is ₹999 or more, after product discounts. For orders below ₹999, charges depend on your delivery PIN code and are displayed at checkout before you pay.</p>
        </section>
        <section aria-labelledby="delivery-times">
          <h2 id="delivery-times">When will my order arrive?</h2>
          <p>Once your order has been dispatched, delivery usually takes 7–8 business days in metro cities and 10–12 business days in other parts of India. These estimates begin from dispatch, not from the date you place your order.</p>
          <p>Your location, public holidays, weather conditions and courier delays may affect the delivery timeline.</p>
        </section>
        <section aria-labelledby="order-tracking">
          <h2 id="order-tracking">Where can I track my shipment?</h2>
          <p>Tracking becomes available after your parcel is dispatched. We’ll share a tracking link by email and SMS so you can follow its journey.</p>
          <p>You can also open Orders on our website, find your order using the email address used at checkout, and select Track Order to view shipment updates.</p>
        </section>
        <section aria-labelledby="undelivered-orders">
          <h2 id="undelivered-orders">What happens if delivery is unsuccessful?</h2>
          <p>If a prepaid parcel cannot be delivered and is returned to us by the courier, we’ll contact you to confirm your delivery details and arrange another shipment once the parcel reaches us.</p>
        </section>
        <section aria-labelledby="shipping-support">
          <h2 id="shipping-support">Need help with your delivery?</h2>
          <p>Email <a href="mailto:contact@styledsomehow.in" style={{ textDecoration: "underline" }}>contact@styledsomehow.in</a> with your order reference so we can help.</p>
        </section>
      </div>
    </div></main>
  );
}
