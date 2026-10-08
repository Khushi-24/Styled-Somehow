"use client";
import Image from "next/image";
import Link from "next/link";
import { useEffect } from "react";

import { Product, productAnchor, money, saving } from "../../lib/product";
export default function NewIn({products}: {products: Product[]}) {
 useEffect(() => {
  let frame = 0;
  function restoreProduct() {
   const id = window.location.hash.slice(1);
   if (!id.startsWith("new-in-product-")) return;
   cancelAnimationFrame(frame);
   frame = requestAnimationFrame(() => {
    const card = document.getElementById(id);
    if (!card) return;
    const rail = card.closest(".new-in__grid");
    if (rail) rail.scrollLeft = (card as HTMLElement).offsetLeft - (rail as HTMLElement).offsetLeft;
    window.scrollTo({ top: card.getBoundingClientRect().top + window.scrollY - 24, behavior: "instant" });
   });
  }
  restoreProduct();
  window.addEventListener("popstate", restoreProduct);
  window.addEventListener("hashchange", restoreProduct);
  window.addEventListener("pageshow", restoreProduct);
  return () => { cancelAnimationFrame(frame); window.removeEventListener("popstate", restoreProduct); window.removeEventListener("hashchange", restoreProduct); window.removeEventListener("pageshow", restoreProduct); };
 }, []);
 return <section id="new-in" className="new-in" aria-labelledby="new-in-title">
  <header><h2 id="new-in-title">New In</h2><p>Upgrade your closet with everything trendy and new</p></header>
  <div className="new-in__grid">{products.filter(p => p.newIn).map(product => {
   const anchor = productAnchor(product);
   return <article key={product.id} id={`new-in-product-${anchor}`} className="new-in__product"><Link prefetch={false} href={`/products/${product.slug}`} onClick={event => {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}#new-in-product-${anchor}`);
   }}><div className="new-in__image"><Image src={product.media[0].url} alt={product.media[0].alt} fill sizes="(max-width: 620px) 70vw, (max-width: 980px) 45vw, 25vw" unoptimized={product.media[0].url.startsWith("/api/")} /><span className="new-in__badge">NEW</span></div>
    <h3>{product.name}</h3><p className="product-price">{product.originalPrice > product.price && <del>{money(product.originalPrice)}</del>}<span>{money(product.price)}</span>{product.originalPrice > product.price && <span className="product-saving">Save {saving(product)}%</span>}</p>
   </Link></article>;
  })}</div>{products.filter(p => p.newIn).length === 0 && <p>New arrivals are on their way.</p>}
 </section>;
}
