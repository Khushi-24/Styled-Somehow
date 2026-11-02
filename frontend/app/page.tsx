import FounderStory from "./components/founder-story";
import NewIn from "./components/new-in";
import InteractiveProductRack from "./components/interactive-product-rack";
import ShopFor from "./components/shop-for";
import StorefrontHero from "./components/storefront-hero";

import { getProducts,getCollections } from "../lib/catalog";
export const dynamic = "force-dynamic";
export default async function Home() {
 const [products,collections] = await Promise.all([getProducts().catch(() => null),getCollections().catch(() => [])]);
  return (
    <main>
      <StorefrontHero collections={collections} />
      <ShopFor activeSlugs={collections.map(c=>c.slug)} />
      {products && <InteractiveProductRack products={products} />}
      {products ? <NewIn products={products} /> : <section className="new-in"><h2>New In</h2><p>Our collection is temporarily unavailable. Please try again shortly.</p></section>}
      <FounderStory />
    </main>
  );
}
