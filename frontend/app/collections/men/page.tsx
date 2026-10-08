import Image from "next/image";
import Link from "next/link";
import { getProducts } from "../../../lib/catalog";
import { money, saving, productAnchor } from "../../../lib/product";
export const dynamic = "force-dynamic";
export const metadata = {title: "Men’s Collection | styled_somehow"};
export default async function Collection() {
 const all = await getProducts().catch(() => null);
 const products = all?.filter(p => p.collections.includes("men")).sort((a,b)=>a.collectionOrder-b.collectionOrder || a.sortOrder-b.sortOrder);
 return <main className="collection-page"><nav className="product-nav"><Link className="brand-logo" href="/" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><Link href="/">Home</Link></nav>
 <p className="product-breadcrumb"><Link href="/">Home</Link> / Men</p><div className="collection-banner"><Image src="/images/men-collection-banner.webp" alt="Men’s streetwear campaign" width={2048} height={1143} sizes="100vw" preload /></div>
 <section className="collection-products"><header className="collection-heading"><h1>Men’s Collection</h1><p>{products ? `${products.length} products` : "Collection temporarily unavailable"}</p></header>
 <div className="collection-grid">{products?.map(p => <article id={productAnchor(p)} key={p.id} className="new-in__product"><Link prefetch={false} href={`/products/${p.slug}?from=men`}><div className="new-in__image"><Image src={p.media[0].url} alt={p.media[0].alt} fill sizes="(max-width: 620px) 46vw, 25vw" unoptimized={p.media[0].url.startsWith("/api/")} />{p.newIn && <span className="new-in__badge">NEW</span>}</div><h2>{p.name}</h2><p className="product-price">{p.originalPrice>p.price && <del>{money(p.originalPrice)}</del>}<span>{money(p.price)}</span>{p.originalPrice>p.price && <span className="product-saving">Save {saving(p)}%</span>}</p>{p.colours.length>1 && <p className="collection-colours">{p.colours.join(" / ")}</p>}</Link></article>)}</div>{products?.length === 0 && <p>New styles are on their way.</p>}</section></main>;
}
