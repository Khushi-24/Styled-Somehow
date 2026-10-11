import NewIn from "./components/new-in";
import ShopFor from "./components/shop-for";
import StorefrontHero from "./components/storefront-hero";

import { getProducts } from "../lib/catalog";
export const dynamic = "force-dynamic";
export default async function Home() {
 const products = await getProducts().catch(() => null);
  return (
    <main>
      <StorefrontHero />
      <ShopFor />
      {products ? <NewIn products={products} /> : <section className="new-in"><h2>New In</h2><p>Our collection is temporarily unavailable. Please try again shortly.</p></section>}
    </main>
  );
}
