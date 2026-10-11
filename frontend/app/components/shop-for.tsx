"use client";
import Image from "next/image";
import Link from "next/link";
import { useEffect } from "react";

const collections = [
  { audience: "women", title: "SHOP WOMEN", image: "/images/shop-women.webp", alt: "Model wearing a black oversized chilli graphic T-shirt" },
  { audience: "men", title: "SHOP MEN", image: "/images/shop-men.webp", alt: "Model wearing a white oversized Untamed car graphic T-shirt" },
] as const;

export default function ShopFor() {
  useEffect(() => {
    let frame = 0;
    function restoreShopCard() {
      const id = window.location.hash.slice(1);
      if (id !== "shop-men" && id !== "shop-women") return;
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => {
        const card = document.getElementById(id);
        if (!card) return;
        const rail = card.parentElement;
        if (rail) rail.scrollLeft = card.offsetLeft - rail.offsetLeft;
        window.scrollTo({ top: card.getBoundingClientRect().top + window.scrollY - 16, behavior: "instant" });
      });
    }
    restoreShopCard();
    window.addEventListener("popstate", restoreShopCard);
    window.addEventListener("hashchange", restoreShopCard);
    window.addEventListener("pageshow", restoreShopCard);
    return () => { cancelAnimationFrame(frame); window.removeEventListener("popstate", restoreShopCard); window.removeEventListener("hashchange", restoreShopCard); window.removeEventListener("pageshow", restoreShopCard); };
  }, []);
  return (
    <section className="shop-for" aria-label="Shop by collection">
      <div className="shop-for__grid">
        {collections.map((collection) => (
          <Link id={`shop-${collection.audience}`} onClick={event => {
            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}#shop-${collection.audience}`);
          }} className="shop-for__card" href={`/collections/${collection.audience}`} key={collection.audience} aria-label={`Explore ${collection.audience}'s collection`}>
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
