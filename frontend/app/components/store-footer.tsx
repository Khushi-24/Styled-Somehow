"use client";
import Link from "next/link";
import {usePathname} from "next/navigation";
import {socialLinks} from "../../lib/social-links";
import styles from "./store-footer.module.css";

const shopLinks=[{href:"/collections/women",label:"Women’s collection"},{href:"/collections/men",label:"Men’s collection"},{href:"/#new-in",label:"New In"}];
const helpLinks=[{href:"/orders",label:"My orders"},{href:"/policies/shipping-policy",label:"Shipping policy"},{href:"/policies/return-exchange-policy",label:"Return & exchange policy"},{href:"/policies/privacy-policy",label:"Privacy policy"},{href:"/policies/terms-of-service",label:"Shopping terms"}];
function FooterLinks({links}:{links:typeof shopLinks}){return <>{links.map(link=><Link key={link.href} href={link.href}>{link.label}</Link>)}</>;}
function InstagramIcon(){return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4"/><circle className={styles.iconDot} cx="17.5" cy="6.5" r="1"/></svg>;}
function YoutubeIcon(){return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="2" y="5" width="20" height="14" rx="4"/><path d="m10 9 5 3-5 3Z"/></svg>;}
export default function StoreFooter(){
 const path=usePathname();
 if(path.startsWith("/admin"))return null;
 return <footer className={styles.footer}><div className={styles.inner}>
  <div className={styles.main}>
   <div className={styles.brand}><Link href="/" className="brand-logo" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><p>Style you don’t have to think about.<br/>Comfort you never want to take off.</p></div>
   <nav className={styles.desktopLinks} aria-label="Shop collections"><h2>Shop</h2><FooterLinks links={shopLinks}/></nav>
   <nav className={styles.desktopLinks} aria-label="Customer care"><h2>Customer care</h2><FooterLinks links={helpLinks}/></nav>
   <div className={styles.contact}><h2>Get in touch</h2><a className={styles.email} href="mailto:contact@styledsomehow.in">contact@styledsomehow.in <span aria-hidden="true">↗</span></a></div>
  <div className={styles.mobileLinks}>
   <details><summary>Shop <span aria-hidden="true">+</span></summary><nav aria-label="Shop collections"><FooterLinks links={shopLinks}/></nav></details>
   <details><summary>Customer care <span aria-hidden="true">+</span></summary><nav aria-label="Customer care"><FooterLinks links={helpLinks}/></nav></details>
  </div></div>
  <div className={styles.socialRow}><div className={styles.brandSocial}><h2>Follow Styled Somehow</h2><div className={styles.socialLinks}><a href={socialLinks.brandInstagram} target="_blank" rel="noopener noreferrer" aria-label="Styled Somehow on Instagram (opens in a new tab)"><InstagramIcon/><span>Instagram</span></a>{socialLinks.brandYoutube&&<a href={socialLinks.brandYoutube} target="_blank" rel="noopener noreferrer" aria-label="Styled Somehow on YouTube (opens in a new tab)"><YoutubeIcon/><span>YouTube</span></a>}</div></div>
   {socialLinks.founderInstagram&&<a className={styles.founder} href={socialLinks.founderInstagram} target="_blank" rel="noopener noreferrer"><InstagramIcon/><span>Khushi · Founder</span><span aria-hidden="true">↗</span><span className={styles.srOnly}> on Instagram (opens in a new tab)</span></a>}
  </div>
  <div className={styles.bottom}>© {new Date().getFullYear()} Styled Somehow</div>
 </div></footer>;
}
