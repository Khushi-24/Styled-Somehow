import NewIn from "./components/new-in";
import ShopFor from "./components/shop-for";
import StorefrontHero from "./components/storefront-hero";

export default function Home() {
  return (
    <main>
      <StorefrontHero />
      <ShopFor />
      <NewIn />
    </main>
  );
}
