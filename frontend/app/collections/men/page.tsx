import CollectionNavigation from "../../components/collection-navigation";
import MenCollectionBanner from "../../components/men-collection-banner";
import StoreHeader from "../../components/store-header";
import Image from "next/image";
import Link from "next/link";
import { getProducts } from "../../../lib/catalog";
import { money, saving, productAnchor } from "../../../lib/product";
export const dynamic = "force-dynamic";
export const metadata = {title: "Men’s Collection | styled_somehow"};
export default async function Collection({searchParams}: {searchParams: Promise<{from?:string}>}) {
 const {from} = await searchParams;
 const source = from === "shop-men" ? from : "";
 const backHref = source ? `/#${source}` : "/";
 const all = await getProducts().catch(() => null);
 const products = all?.filter(p => p.collections.includes("men")).sort((a,b)=>a.collectionOrder-b.collectionOrder || a.sortOrder-b.sortOrder);
 return <main className="collection-page"><StoreHeader backHref={backHref}/><CollectionNavigation/>
 <MenCollectionBanner/>
 <section id="men-collection" className="collection-products" style={{scrollMarginTop:24}}><header className="collection-heading"><h1>Men’s Collection</h1><p>{products ? `${products.length} products` : "Collection temporarily unavailable"}</p></header>
 <div className="collection-grid">{products?.map(p => <article id={productAnchor(p)} key={p.id} className="new-in__product"><Link prefetch={false} href={`/products/${p.slug}?from=men${source ? `&origin=${source}` : ""}`}><div className="new-in__image"><Image src={p.media[0].url} alt={p.media[0].alt} fill sizes="(max-width: 620px) 46vw, 25vw" />{p.newIn && <span className="new-in__badge">NEW</span>}</div><h2>{p.name}</h2><p className="product-price">{p.originalPrice>p.price && <del>{money(p.originalPrice)}</del>}<span>{money(p.price)}</span>{p.originalPrice>p.price && <span className="product-saving">Save {saving(p)}%</span>}</p>{p.colours.length>1 && <p className="collection-colours">{p.colours.join(" / ")}</p>}</Link></article>)}</div>{products?.length === 0 && <p>New styles are on their way.</p>}</section></main>;
}
