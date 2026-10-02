import Image from "next/image";
import Link from "next/link";

export default function NewIn() {
 return <section className="new-in" aria-labelledby="new-in-title">
  <header><h2 id="new-in-title">New In</h2><p>Upgrade your closet with everything trendy and new</p></header>
  <div className="new-in__grid"><article className="new-in__product">
   <Link href="/products/cherry-zest-oversized-t-shirt">
    <div className="new-in__image"><Image src="/images/cherry-zest-1.webp" alt="White Cherry Zest oversized T-shirt with cherry and citrus artwork" fill sizes="(max-width: 620px) 70vw, (max-width: 980px) 45vw, 25vw" /><span className="new-in__badge">NEW</span></div>
    <h3>Cherry Zest Oversized T-Shirt</h3><p className="product-price"><del>₹999</del><span>₹599</span><span className="product-saving">Save 40%</span></p>
   </Link>
  </article></div>
 </section>;
}
