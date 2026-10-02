import Image from "next/image";
import Link from "next/link";

const collections = [
  { audience: "women", title: "SHOP WOMEN", image: "/images/shop-women.webp", alt: "Model wearing a black oversized chilli graphic T-shirt" },
  { audience: "men", title: "SHOP MEN", image: "/images/shop-men.webp", alt: "Model wearing a white oversized Untamed car graphic T-shirt" },
] as const;

export default function ShopFor() {
  return (
    <section className="shop-for" aria-label="Shop by collection">
      <div className="shop-for__grid">
        {collections.map((collection) => (
          <Link className="shop-for__card" href={`/collections/${collection.audience}`} key={collection.audience} aria-label={`Explore ${collection.audience}'s collection`}>
            <Image src={collection.image} alt={collection.alt} fill sizes="(max-width: 620px) calc(100vw - 24px), calc(50vw - 36px)" />
            <div className="shop-for__caption">
              <h2>{collection.title}</h2>
              <span className="shop-for__explore">Explore</span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}
