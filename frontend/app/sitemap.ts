import type {MetadataRoute} from "next";
import {getProducts,getCollections} from "../lib/catalog";
import {siteUrl} from "../lib/site";
export const dynamic="force-dynamic";
export default async function sitemap():Promise<MetadataRoute.Sitemap>{const [products,collections]=await Promise.all([getProducts(),getCollections()]);return ["",...collections.map(c=>`/collections/${c.slug}`),"/policies/shipping-policy","/policies/return-exchange-policy","/policies/privacy-policy","/policies/terms-of-service"].map(path=>({url:siteUrl+path})).concat(products.map(p=>({url:`${siteUrl}/products/${p.slug}`})));}
