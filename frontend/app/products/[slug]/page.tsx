import { notFound } from "next/navigation";
import { getProduct } from "../../../lib/catalog";
import { productAnchor } from "../../../lib/product";
import {siteUrl} from "../../../lib/site";
import ProductView from "../../components/product-view";
export const dynamic = "force-dynamic";
export async function generateMetadata({params}: {params: Promise<{slug: string}>}) {
 const p = await getProduct((await params).slug).catch(() => null);
 return {title:p?`${p.name} | styled_somehow`:"Product | styled_somehow",description:p?.description,alternates:p?{canonical:`${siteUrl}/products/${p.slug}`}:undefined,robots:p?.status==="ARCHIVED"?{index:false,follow:true}:undefined,openGraph:p?{title:p.name,description:p.description,images:p.media.filter(m=>m.kind==="image").slice(0,1).map(m=>siteUrl+m.url)}:undefined};
}
export default async function Page({params,searchParams}: {params: Promise<{slug: string}>; searchParams: Promise<{from?: string; origin?: string}>}) {
 const product = await getProduct((await params).slug);
 if (!product) notFound();
 const {from,origin} = await searchParams;
 const collectionQuery = origin === `shop-${from}` ? `?from=${origin}` : "";
 const anchor = productAnchor(product);
 const returnUrl = from === "men" || from === "women" ? `/collections/${from}${collectionQuery}#${anchor}` : `/#new-in-product-${anchor}`;
 const data={"@context":"https://schema.org","@type":"Product",name:product.name,description:product.description,image:product.media.filter(m=>m.kind==="image").map(m=>siteUrl+m.url),brand:{"@type":"Brand",name:"Styled Somehow"},offers:{"@type":"Offer",url:`${siteUrl}/products/${product.slug}`,priceCurrency:"INR",price:product.price,availability:product.status==="PUBLISHED"&&Object.values(product.availability||{}).some(sizes=>Object.values(sizes).some(Boolean))?"https://schema.org/InStock":"https://schema.org/OutOfStock"}};
 return <><script type="application/ld+json" dangerouslySetInnerHTML={{__html:JSON.stringify(data).replace(/</g,"\\u003c")}}/><ProductView product={product} returnUrl={returnUrl} /></>;
}
