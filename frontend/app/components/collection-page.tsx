import CollectionNavigation from "./collection-navigation";
import WomenCollectionBanner from "./women-collection-banner";
import MenCollectionBanner from "./men-collection-banner";
import {notFound} from "next/navigation";
import StoreHeader from "./store-header";
import Image from "next/image";
import Link from "next/link";
import { getProducts, getCollections } from "../../lib/catalog";
import { money, saving, productAnchor } from "../../lib/product";
export default async function CollectionPage({slug,from}: {slug:string;from?:string}) {
 const collections=await getCollections();
 const collection=collections.find(c=>c.slug===slug);
 if(!collection)notFound();
 const source = from === `shop-${slug}` ? from : "";
 const backHref = source ? `/#${source}` : "/";
 const all = await getProducts().catch(() => null);
 const products = all?.filter(p => p.collections.includes(slug)).sort((a,b)=>a.collectionOrder-b.collectionOrder || a.sortOrder-b.sortOrder);
 return <main className="collection-page"><StoreHeader backHref={backHref}/><CollectionNavigation/><nav className="collection-links" aria-label="Collections">{collections.map(c=><Link key={c.slug} href={`/collections/${c.slug}`} aria-current={c.slug===slug?"page":undefined}>{c.name}</Link>)}</nav>
 {slug==="women"?<WomenCollectionBanner/>:slug==="men"?<MenCollectionBanner/>:null}
 <section id={`${slug}-collection`} className="collection-products"><header className="collection-heading"><h1>{slug==="women"?"Women’s Collection":slug==="men"?"Men’s Collection":collection.name}</h1><p>{products ? `${products.length} products` : "Collection temporarily unavailable"}</p></header>
 <div className="collection-grid">{products?.map(p => <article id={productAnchor(p)} key={p.id} className="new-in__product"><Link prefetch={false} href={`/products/${p.slug}?from=${slug}${source ? `&origin=${source}` : ""}`}><div className="new-in__image"><Image src={p.media[0].url} alt={p.media[0].alt} fill sizes="(max-width: 620px) 46vw, 25vw" />{p.newIn && <span className="new-in__badge">NEW</span>}</div><h2>{p.name}</h2><p className="product-price">{p.originalPrice>p.price && <del>{money(p.originalPrice)}</del>}<span>{money(p.price)}</span>{p.originalPrice>p.price && <span className="product-saving">Save {saving(p)}%</span>}</p>{p.colours.length>1 && <p className="collection-colours">{p.colours.join(" / ")}</p>}</Link></article>)}</div>{products?.length === 0 && <p>New styles are on their way.</p>}</section></main>;
}
