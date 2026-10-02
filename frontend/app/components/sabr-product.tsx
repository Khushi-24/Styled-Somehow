"use client";
import Image from "next/image";
import Link from "next/link";
import { useRef, useState } from "react";
const sizes = ["S", "M", "L", "XL"];
export default function SabrProduct({ returnUrl = "/collections/men#sabr" }: { returnUrl?: string }) {
 const [colour, setColour] = useState("Black");
 const images = colour === "Black" ? ["/images/sabr-black.webp", "/images/sabr-black-seated.webp"] : ["/images/sabr-white.webp"];
 const slideCount = images.length + 1 + (colour === "White" ? 1 : 0);
 const [photo, setPhoto] = useState(0);
 const gallery = useRef<HTMLDivElement>(null);
 function movePhoto(direction: number) {
  const next = (photo + direction + slideCount) % slideCount;
  gallery.current?.scrollTo({ left: next * gallery.current.clientWidth, behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
 }
 const [size, setSize] = useState("");
 const [message, setMessage] = useState("");
 function addToCart() {
  try {
   const stored = JSON.parse(localStorage.getItem("styled-somehow-cart") || "[]");
   const cart = Array.isArray(stored) ? stored : [];
   const item = cart.find((p: { slug: string; size: string; colour?: string }) => p.slug === "sabr-oversized-t-shirt" && p.size === size && p.colour === colour);
   if (item) item.quantity += 1; else cart.push({ slug: "sabr-oversized-t-shirt", name: "Sabr Oversized T-Shirt", size, colour, price: 499, quantity: 1 });
   localStorage.setItem("styled-somehow-cart", JSON.stringify(cart));
   setMessage(`Sabr · ${colour} · ${size} added to your cart.`);
  } catch { setMessage("Could not save your cart. Please try again."); }
 }
 return <main className="product-page">
  <nav className="product-nav" aria-label="Product navigation"><Link href="/" className="brand-logo" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><Link href={returnUrl}>← Continue shopping</Link></nav>
  <p className="product-breadcrumb"><Link href="/">Home</Link> / Sabr</p>
  <div className="product-layout">
   <section className="product-gallery" aria-label="Product photos">
    <div className="product-gallery__rail" ref={gallery} onScroll={event => {const el = event.currentTarget; setPhoto(Math.round(el.scrollLeft / el.clientWidth));}}>
     {Array.from({ length: slideCount }, (_, index) => {
      const isVideo = colour === "White" && index === slideCount - 1;
      const imageIndex = index < 2 ? 0 : index - 1;
      return <div className="product-gallery__slide" key={colour + index} role="group" aria-label={`Slide ${index + 1} of ${slideCount}`}>
       {isVideo ? <video className="product-video" controls playsInline preload="metadata" poster="/images/sabr-white.webp" aria-label="Sabr White T-shirt video"><source src="/videos/sabr-white.mp4" type="video/mp4" /></video> : <Image src={images[imageIndex]} alt={`Sabr oversized T-shirt in ${colour}${index === 1 ? ", style highlights" : ""}`} fill sizes="(max-width: 820px) 100vw, 55vw" preload={index === 0} />}
       {index === 1 && <div className="gallery-highlights"><h2>Style<br />Highlights</h2><dl><div><dt>Composition</dt><dd>100% Cotton</dd></div><div><dt>GSM</dt><dd>240</dd></div><div><dt>Colour</dt><dd>{colour}</dd></div><div><dt>Fit</dt><dd>Oversized Fit</dd></div></dl></div>}
      </div>;
     })}
    </div>
    <div className="product-gallery__controls"><button type="button" onClick={() => movePhoto(-1)} aria-label="Previous photo">‹</button><span aria-live="polite">{photo + 1} / {slideCount}</span><button type="button" onClick={() => movePhoto(1)} aria-label="Next photo">›</button></div>
   </section>
   <section className="product-information" aria-labelledby="product-title">
    <h1 id="product-title">Sabr Oversized T-Shirt</h1>
    <p className="product-price product-price--large"><del>₹600</del><span>₹499</span><span className="product-saving">Save 17%</span></p>
    <p className="product-description">Wear your calm. Minimal सब्र lettering meets a relaxed oversized silhouette, available in Black and White.</p>
    <dl className="product-highlights"><div><dt>Composition</dt><dd>100% Cotton</dd></div><div><dt>GSM</dt><dd>240</dd></div><div><dt>Colour</dt><dd>{colour}</dd></div><div><dt>Fit</dt><dd>Oversized</dd></div><div><dt>Sleeves</dt><dd>Short</dd></div></dl>
    <fieldset className="product-colours"><legend>Colour: {colour}</legend><div>{["Black", "White"].map(value => <button type="button" key={value} aria-pressed={colour === value} onClick={() => {setColour(value);setPhoto(0);setMessage("");gallery.current?.scrollTo({left: 0, behavior: "instant"});gallery.current?.parentElement?.scrollTo({top: 0, behavior: "instant"});}}><span className="colour-swatch" style={{backgroundColor: value.toLowerCase()}} />{value}</button>)}</div></fieldset>
    <fieldset className="product-sizes"><legend>Size{size ? `: ${size}` : " — select your size"}</legend><div>{sizes.map(value => <button type="button" key={value} aria-pressed={size === value} onClick={() => {setSize(value);setMessage("");}}>{value}</button>)}</div></fieldset>
    <details className="product-details"><summary>Size guide</summary><p>Temporary reference: Bonkers Corner oversized T-shirt sizing. Product measurements will be updated when confirmed.</p><table className="product-size-table"><caption>Garment measurements in inches</caption><thead><tr><th>Size</th><th>Chest</th><th>Length</th></tr></thead><tbody>{[["S",44,28],["M",46,29],["L",48,30],["XL",51,30.5]].map(([label,chest,length]) => <tr key={label}><th scope="row">{label}</th><td>{chest}</td><td>{length}</td></tr>)}</tbody></table></details>
    <button className="product-add" type="button" disabled={!size} onClick={addToCart}>{size ? "Add to cart" : "Select a size"}</button>
    <p className="product-cart-message" role="status">{message}</p>
    <details className="product-details" open><summary>Product details</summary><p>Oversized T-shirt with सब्र chest lettering, dropped shoulders and short sleeves. Available in Black and White. 240 GSM, 100% cotton fabric.</p></details>
   </section>
  </div>
 </main>;
}
