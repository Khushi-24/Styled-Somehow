"use client";
import Image from "next/image";
import Link from "next/link";
import { useEffect } from "react";

export default function NewIn() {
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
  <div className="new-in__grid"><article id="new-in-product-cherry-zest" className="new-in__product">
   <Link href="/products/cherry-zest-oversized-t-shirt" onClick={event => {
     if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
     window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}#new-in-product-cherry-zest`);
   }}>
    <div className="new-in__image"><Image src="/images/cherry-zest-1.webp" alt="White Cherry Zest oversized T-shirt with cherry and citrus artwork" fill sizes="(max-width: 620px) 70vw, (max-width: 980px) 45vw, 25vw" /><span className="new-in__badge">NEW</span></div>
    <h3>Cherry Zest Oversized T-Shirt</h3><p className="product-price"><del>₹999</del><span>₹599</span><span className="product-saving">Save 40%</span></p>
   </Link>
  </article><article id="new-in-product-untamed-torque" className="new-in__product">
   <Link href="/products/untamed-torque-oversized-t-shirt" onClick={event => {
     if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
     window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}#new-in-product-untamed-torque`);
   }}>
    <div className="new-in__image"><Image src="/images/untamed-torque-front.webp" alt="White Untamed Torque oversized T-shirt with split-car artwork" fill sizes="(max-width: 620px) 70vw, (max-width: 980px) 45vw, 25vw" /><span className="new-in__badge">NEW</span></div>
    <h3>Untamed Torque Oversized T-Shirt</h3><p className="product-price"><del>₹1,200</del><span>₹999</span><span className="product-saving">Save 17%</span></p>
   </Link>
  </article></div>
 </section>;
}
