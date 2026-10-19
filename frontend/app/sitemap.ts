import type {MetadataRoute} from "next";
import {getProducts} from "../lib/catalog";
import {siteUrl} from "../lib/site";
export const dynamic="force-dynamic";
export default async function sitemap():Promise<MetadataRoute.Sitemap>{const products=await getProducts();return ["","/collections/women","/collections/men","/policies/shipping-policy","/policies/return-exchange-policy","/policies/privacy-policy","/policies/terms-of-service"].map(path=>({url:siteUrl+path})).concat(products.map(p=>({url:`${siteUrl}/products/${p.slug}`})));}
