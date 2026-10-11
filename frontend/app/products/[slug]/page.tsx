import { notFound } from "next/navigation";
import { getProduct } from "../../../lib/catalog";
import { productAnchor } from "../../../lib/product";
import ProductView from "../../components/product-view";
export const dynamic = "force-dynamic";
export async function generateMetadata({params}: {params: Promise<{slug: string}>}) {
 const p = await getProduct((await params).slug).catch(() => null);
 return {title: p ? `${p.name} | styled_somehow` : "Product | styled_somehow"};
}
export default async function Page({params,searchParams}: {params: Promise<{slug: string}>; searchParams: Promise<{from?: string}>}) {
 const product = await getProduct((await params).slug);
 if (!product) notFound();
 const {from} = await searchParams;
 const anchor = productAnchor(product);
 const returnUrl = from === "men" || from === "women" ? `/collections/${from}#${anchor}` : `/#new-in-product-${anchor}`;
 return <ProductView product={product} returnUrl={returnUrl} />;
}
