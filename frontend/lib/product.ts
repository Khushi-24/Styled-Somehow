export type Media = {url: string; alt: string; colour: string; kind: "image" | "video"; contain: boolean};
export type Product = {availability?: Record<string,Record<string,boolean>>;id?: number; version?: number; slug: string; name: string; price: number; originalPrice: number; description: string; details: string; composition: string; gsm: number; collections: string[]; colours: string[]; sizes: string[]; media: Media[]; newIn: boolean; sortOrder: number; collectionOrder: number; status: "DRAFT" | "PUBLISHED" | "ARCHIVED"};
export const productAnchor = (p: Product) => p.slug.replace(/-oversized-t-shirt$/, "");
export const money = (n: number) => `₹${n.toLocaleString("en-IN")}`;
export const saving = (p: Product) => Math.round((1 - p.price / p.originalPrice) * 100);
