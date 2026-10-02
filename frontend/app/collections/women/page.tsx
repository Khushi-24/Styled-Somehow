import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
export const metadata: Metadata = { title: "Women’s Collection | styled_somehow", description: "Explore women’s oversized graphic T-shirts from styled_somehow." };
const products = [
 {slug: "chilli-crush", name: "Chilli Crush Oversized T-Shirt", image: "/images/chilli-crush-1.webp", alt: "Black Chilli Crush oversized T-shirt with red chilli artwork on pink", original: "900", price: "699", saving: "22"},
 {slug: "cherry-zest", name: "Cherry Zest Oversized T-Shirt", image: "/images/cherry-zest-1.webp", alt: "White Cherry Zest oversized T-shirt with cherry and citrus artwork", original: "999", price: "599", saving: "40"},
 {slug: "shes-winning", name: "She’s Winning Oversized T-Shirt", image: "/images/shes-winning-1.webp", alt: "Black She’s Winning oversized T-shirt with pink statement artwork", original: "900", price: "599", saving: "33"},
 {slug: "bitchari", name: "Bitchआरी Oversized T-Shirt", image: "/images/bitchari-1.webp", alt: "Black Bitchआरी oversized T-shirt with yellow lettering and pink shadow", original: "900", price: "599", saving: "33"},
];
export default function WomensCollection() {
 return <main className="collection-page">
  <nav className="product-nav" aria-label="Main navigation"><Link className="brand-logo" href="/" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><Link href="/">Home</Link></nav>
  <p className="product-breadcrumb"><Link href="/">Home</Link> / Women</p>
  <div className="collection-banner collection-banner--women"><Image src="/images/shop-women.webp" alt="Women’s streetwear campaign with a model wearing a black oversized chilli graphic T-shirt" fill sizes="100vw" preload /></div>
  <section className="collection-products" aria-labelledby="womens-collection-title">
   <header className="collection-heading"><h1 id="womens-collection-title">Women’s Collection</h1><p>{products.length} products</p></header>
   <div className="collection-grid">{products.map(product => <article id={product.slug} className="new-in__product" key={product.slug}><Link href={`/products/${product.slug}-oversized-t-shirt?from=women`}>
    <div className="new-in__image"><Image src={product.image} alt={product.alt} fill sizes="(max-width: 620px) 46vw, (max-width: 980px) 31vw, 23vw" /><span className="new-in__badge">NEW</span></div>
    <h2>{product.name}</h2><p className="product-price"><del>₹{product.original}</del><span>₹{product.price}</span><span className="product-saving">Save {product.saving}%</span></p>
   </Link></article>)}</div>
  </section>
 </main>;
}
