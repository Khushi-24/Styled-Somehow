"use client";
import Image from "next/image";
import Link from "next/link";
import { useRef, useState } from "react";
const images = ["/images/bitchari-1.webp", "/images/bitchari-2.webp", "/images/bitchari-3.webp", "/images/bitchari-4.webp"];
const descriptions = ["Bitchआरी oversized black T-shirt, front view", "Bitchआरी oversized black T-shirt, alternate pose", "Bitchआरी oversized black T-shirt, surprised pose", "Close-up of yellow Bitchआरी lettering"];
const sizes = ["S", "M", "L", "XL"];
export default function BitchariProduct({ returnUrl = "/#new-in-product-bitchari" }: { returnUrl?: string }) {
 const [photo, setPhoto] = useState(0);
 const gallery = useRef<HTMLDivElement>(null);
 function movePhoto(direction: number) {
  const next = (photo + direction + 5) % 5;
  gallery.current?.scrollTo({ left: next * gallery.current.clientWidth, behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
 }
 const [size, setSize] = useState("");
 const [message, setMessage] = useState("");
 function addToCart() {
  try {
   const stored = JSON.parse(localStorage.getItem("styled-somehow-cart") || "[]");
   const cart = Array.isArray(stored) ? stored : [];
   const item = cart.find((p: { slug: string; size: string }) => p.slug === "bitchari-oversized-t-shirt" && p.size === size);
   if (item) item.quantity += 1; else cart.push({ slug: "bitchari-oversized-t-shirt", name: "Bitchआरी Oversized T-Shirt", size, price: 599, quantity: 1 });
   localStorage.setItem("styled-somehow-cart", JSON.stringify(cart));
   setMessage(`Bitchआरी · ${size} added to your cart.`);
  } catch { setMessage("Could not save your cart. Please try again."); }
 }
 return <main className="product-page">
  <nav className="product-nav" aria-label="Product navigation"><Link href="/" className="brand-logo" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><Link href={returnUrl}>← Continue shopping</Link></nav>
  <p className="product-breadcrumb"><Link href="/">Home</Link> / Bitchआरी</p>
  <div className="product-layout">
   <section className="product-gallery" aria-label="Product photos">
    <div className="product-gallery__rail" ref={gallery} onScroll={event => {const el = event.currentTarget; setPhoto(Math.round(el.scrollLeft / el.clientWidth));}}>
     {[0, 1, 2, 3, 4].map((index) => {
      const imageIndex = index < 2 ? 0 : index - 1;
      return <div className="product-gallery__slide" key={index} role="group" aria-label={`Photo ${index + 1} of 5`}>
       <Image src={images[imageIndex]} alt={index === 1 ? "Bitchआरी style highlights" : descriptions[imageIndex]} fill sizes="(max-width: 820px) 100vw, 55vw" preload={index === 0} />
       {index === 1 && <div className="gallery-highlights"><h2>Style<br />Highlights</h2><dl><div><dt>Composition</dt><dd>100% Cotton</dd></div><div><dt>GSM</dt><dd>240</dd></div><div><dt>Colour</dt><dd>Black</dd></div><div><dt>Sleeve length</dt><dd>Short Sleeve</dd></div><div><dt>Fit</dt><dd>Oversized Fit</dd></div></dl></div>}
      </div>;
     })}
    </div>
    <div className="product-gallery__controls"><button type="button" onClick={() => movePhoto(-1)} aria-label="Previous photo">‹</button><span aria-live="polite">{photo + 1} / 5</span><button type="button" onClick={() => movePhoto(1)} aria-label="Next photo">›</button></div>
   </section>
   <section className="product-information" aria-labelledby="product-title">
    <h1 id="product-title">Bitchआरी Oversized T-Shirt</h1>
    <p className="product-price product-price--large"><del>₹900</del><span>₹599</span><span className="product-saving">Save 33%</span></p>
    <p className="product-description">A little wordplay, a lot of attitude. Bold yellow Bitchआरी lettering with pink shadowing on a black oversized tee.</p>
    <dl className="product-highlights"><div><dt>Composition</dt><dd>100% Cotton</dd></div><div><dt>GSM</dt><dd>240</dd></div><div><dt>Colour</dt><dd>Black</dd></div><div><dt>Fit</dt><dd>Oversized</dd></div><div><dt>Sleeves</dt><dd>Short</dd></div></dl>
    <fieldset className="product-sizes"><legend>Size{size ? `: ${size}` : " — select your size"}</legend><div>{sizes.map(value => <button type="button" key={value} aria-pressed={size === value} onClick={() => {setSize(value);setMessage("");}}>{value}</button>)}</div></fieldset>
    <details className="product-details"><summary>Size guide</summary><p>Temporary reference: Bonkers Corner oversized T-shirt sizing. Product measurements will be updated when confirmed.</p><table className="product-size-table"><caption>Garment measurements in inches</caption><thead><tr><th>Size</th><th>Chest</th><th>Length</th></tr></thead><tbody>{[["S",44,28],["M",46,29],["L",48,30],["XL",51,30.5]].map(([label,chest,length]) => <tr key={label}><th scope="row">{label}</th><td>{chest}</td><td>{length}</td></tr>)}</tbody></table></details>
    <button className="product-add" type="button" disabled={!size} onClick={addToCart}>{size ? "Add to cart" : "Select a size"}</button>
    <p className="product-cart-message" role="status">{message}</p>
    <details className="product-details" open><summary>Product details</summary><p>Black oversized T-shirt with yellow Bitchआरी lettering and pink shadow detailing. Dropped shoulders and short sleeves. 240 GSM, 100% cotton fabric.</p></details>
   </section>
  </div>
 </main>;
}
