import styles from "../shipping-policy/shipping-policy.module.css";
import StoreHeader from "../../components/store-header";

export const metadata = {title:"Return & Exchange Policy | Styled Somehow"};

export default function ReturnExchangePolicy(){
 return <main className={styles.page}><div className={styles.shell}>
  <StoreHeader dark/>
  <header className={styles.hero}><h1>Return &amp; exchange policy</h1></header>
  <div className={styles.content}>
   <section aria-labelledby="return-window"><h2 id="return-window">01 / Easy 5-day returns</h2><p>Request a return within 5 calendar days of delivery. Items must be unused, unwashed and unaltered, with the original brand tag attached and intact.</p><p><strong>Shipping charges are non-refundable for change-of-mind returns.</strong> Both outward shipping and reverse pickup costs apply, including the actual outward cost on orders delivered free. We will confirm all deductions before arranging pickup.</p></section>
   <section aria-labelledby="exchanges"><h2 id="exchanges">02 / Exchanges &amp; replacements</h2><p>Request an exchange within 5 calendar days of delivery. The same item-condition and tag requirements apply. Exchanges depend on stock availability; any price difference and shipping charges will be confirmed before approval.</p></section>
   <section aria-labelledby="request-return"><h2 id="request-return">03 / How to raise a request</h2><p>Email <a href="mailto:contact@styledsomehow.in?subject=Return%20or%20exchange%20request">contact@styledsomehow.in</a> with your order reference, product, reason for the request and preferred replacement, if applicable. Include clear photos for defects, damage or incorrect items.</p><p>Our team typically reviews requests within 48 hours. Reverse pickup is arranged only after approval. Please do not send items back without confirmation.</p></section>
   <section aria-labelledby="pickup-packaging"><h2 id="pickup-packaging">04 / Packaging &amp; pickup</h2><p>Pack items securely as they were delivered, with all original tags and the original packaging where available. Our team will confirm pickup availability and instructions.</p></section>
   <section aria-labelledby="refunds"><h2 id="refunds">05 / Inspection &amp; refunds</h2><p>Refunds are approved after we receive and inspect the returned item. We will confirm the eligible amount, refund method and processing timeline with you.</p></section>
   <section aria-labelledby="defect-conditions"><h2 id="defect-conditions">06 / Defects &amp; exclusions</h2><p>Manufacturing defects, damage on arrival and incorrect items are reviewed for replacement or refund. Damage caused by improper washing, alterations, misuse or normal wear is not covered.</p><p>Change-of-mind shipping deductions do not automatically apply to confirmed defective or incorrect items. This policy, including its 5-day window and tag requirements, does not limit your rights under applicable consumer law.</p></section>
  </div>
 </div></main>;
}
