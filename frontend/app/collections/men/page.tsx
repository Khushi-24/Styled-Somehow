import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
export const metadata: Metadata = { title: "Men’s Collection | styled_somehow", description: "Explore men’s oversized graphic T-shirts from styled_somehow." };
export default function MensCollection() {
 return <main className="collection-page">
  <nav className="product-nav" aria-label="Main navigation"><Link className="brand-logo" href="/" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><Link href="/">Home</Link></nav>
  <p className="product-breadcrumb"><Link href="/">Home</Link> / Men</p>
  <div className="collection-banner"><Image src="/images/men-collection-banner.webp" alt="Men’s streetwear campaign with a model in a black oversized T-shirt surrounded by speakers" width={2048} height={1143} sizes="100vw" preload /></div>
  <section className="collection-products" aria-labelledby="mens-collection-title">
   <header className="collection-heading"><h1 id="mens-collection-title">Men’s Collection</h1><p>2 products</p></header>
   <div className="collection-grid"><article id="untamed-torque" className="new-in__product"><Link href="/products/untamed-torque-oversized-t-shirt?from=men">
    <div className="new-in__image"><Image src="/images/untamed-torque-front.webp" alt="Untamed Torque white oversized T-shirt with split-car artwork" fill sizes="(max-width: 620px) 46vw, (max-width: 980px) 31vw, 23vw" /><span className="new-in__badge">NEW</span></div>
    <h2>Untamed Torque Oversized T-Shirt</h2><p className="product-price"><del>₹1,200</del><span>₹999</span><span className="product-saving">Save 17%</span></p>
   </Link></article><article id="sabr" className="new-in__product"><Link href="/products/sabr-oversized-t-shirt">
    <div className="new-in__image"><Image src="/images/sabr-black.webp" alt="Sabr black oversized T-shirt with सब्र lettering" fill sizes="(max-width: 620px) 46vw, (max-width: 980px) 31vw, 23vw" /><span className="new-in__badge">NEW</span></div>
    <h2>Sabr Oversized T-Shirt</h2><p className="product-price"><del>₹600</del><span>₹499</span><span className="product-saving">Save 17%</span></p><p className="collection-colours">Black / White</p>
   </Link></article></div>
  </section>
 </main>;
}
