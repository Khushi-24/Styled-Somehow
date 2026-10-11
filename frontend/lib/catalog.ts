import "server-only";
import type { Product } from "./product";
const backend = process.env.BACKEND_URL || "http://127.0.0.1:8080";
export async function getProducts(): Promise<Product[]> {
 const response = await fetch(`${backend}/api/products`, {cache: "no-store", signal: AbortSignal.timeout(5000)});
 if (!response.ok) throw new Error("Catalogue unavailable");
 return response.json();
}
export async function getProduct(slug: string): Promise<Product | null> {
 const response = await fetch(`${backend}/api/products/${encodeURIComponent(slug)}`, {cache: "no-store", signal: AbortSignal.timeout(5000)});
 if (response.status === 404) return null;
 if (!response.ok) throw new Error("Catalogue unavailable");
 return response.json();
}
