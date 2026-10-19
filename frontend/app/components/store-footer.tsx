"use client";
import Link from "next/link";
import {usePathname} from "next/navigation";
import styles from "./store-footer.module.css";

export default function StoreFooter(){
 const path=usePathname();
 if(path.startsWith("/admin"))return null;
 return <footer className={styles.footer}><div className={styles.grid}>
  <div className={styles.brand}><Link href="/" className="brand-logo" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><p>Style you don’t have to think about.<br/>Comfort you never want to take off.</p></div>
  <nav aria-label="Shop collections"><h2>Shop</h2><Link href="/collections/women">Women’s collection</Link><Link href="/collections/men">Men’s collection</Link><Link href="/#new-in">New In</Link></nav>
  <nav aria-label="Policies"><h2>Information</h2><Link href="/policies/shipping-policy">Shipping policy</Link><Link href="/policies/return-exchange-policy">Return &amp; exchange policy</Link><Link href="/policies/privacy-policy">Privacy policy</Link><Link href="/policies/terms-of-service">Shopping terms</Link></nav>
  <div><h2>Contact us</h2><Link href="/orders">My orders</Link><a href="mailto:contact@styledsomehow.in">contact@styledsomehow.in</a></div>
 </div><div className={styles.bottom}>© {new Date().getFullYear()} Styled Somehow</div></footer>;
}
