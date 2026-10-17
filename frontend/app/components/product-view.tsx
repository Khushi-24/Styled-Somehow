"use client";
import ProductDelivery from "./product-delivery";
import StoreHeader from "./store-header";
import Image from "next/image";
import SizeGuide, {measurements} from "./size-guide";
import Link from "next/link";
import {openCart} from "./cart-drawer";
import {useRouter} from "next/navigation";
import {useRef,useState} from "react";
import {writeCart,readCart} from "../../lib/cart";
import {Product,money,saving} from "../../lib/product";
export default function ProductView({product:p,returnUrl}: {product:Product;returnUrl:string}) {
 const router=useRouter();const [busy,setBusy]=useState(false);
 const [colour,setColour]=useState(p.media[0].colour);
 const [size,setSize]=useState(()=>p.sizes.find(s=>p.availability?.[p.media[0].colour]?.[s]===true)??"");const [message,setMessage]=useState("");const [photo,setPhoto]=useState(0);
 const gallery=useRef<HTMLDivElement>(null);
 const selectedMeasurement=measurements.find(m=>m.size===size);
 const images=p.media.filter(m=>m.colour===colour);
 const slides=[{media:images[0],highlights:false},{media:images[1]?.kind==="image"?images[1]:images[0],highlights:true},...images.slice(1).map(media=>({media,highlights:false}))];
 function move(direction:number){gallery.current?.scrollTo({left:((photo+direction+slides.length)%slides.length)*gallery.current.clientWidth,behavior:window.matchMedia("(prefers-reduced-motion: reduce)").matches?"auto":"smooth"});}
 function available(s:string){return p.availability?.[colour]?.[s]===true;}
 async function add(buyNow=false){if(busy)return;if(!size||!available(size)){setMessage("This size is out of stock.");return;}setBusy(true);setMessage("");try{
  const response=await fetch(`/api/products/${encodeURIComponent(p.slug)}`,{cache:"no-store"});if(!response.ok)throw new Error("Stock check failed");const fresh:Product=await response.json();if(fresh.availability?.[colour]?.[size]!==true){setMessage("This size is now out of stock. Please refresh the page.");return;}
  const cart=readCart();
  const item=cart.find((v:{slug:string;size:string;colour:string})=>v.slug===p.slug&&v.size===size&&v.colour===colour);
  if(item&&item.quantity>=20){setMessage("Maximum 20 of this variant per checkout.");return;}if(item)item.quantity+=1;else cart.push({slug:p.slug,name:p.name,size,colour,price:p.price,quantity:1});
  writeCart(cart);if(buyNow){router.push("/checkout");}else{setMessage(`${p.name} · ${colour} · ${size} added to your cart.`);openCart();}
 }catch{setMessage("Could not save your cart. Please try again.");}finally{setBusy(false);}}
 return <main className="product-page"><StoreHeader backHref={returnUrl}/><p className="product-breadcrumb"><Link href="/">Home</Link> / {p.name}</p>
 <div className="product-layout"><section className="product-gallery" aria-label="Product photos"><div className="product-gallery__rail" ref={gallery} onScroll={e=>setPhoto(Math.round(e.currentTarget.scrollLeft/e.currentTarget.clientWidth))}>
 {slides.map(({media:m,highlights},i)=><div key={`${colour}-${i}`} className={`product-gallery__slide${m.contain&&!highlights?" product-gallery__slide--detail":""}`} role="group" aria-label={`Photo ${i+1} of ${slides.length}`}>
 {m.kind==="video"?<video className="product-video" src={m.url} controls playsInline preload="metadata" aria-label={m.alt}/>:<Image src={m.url} alt={highlights?`${p.name} style highlights`:m.alt} fill sizes="(max-width: 820px) 100vw, 55vw" preload={i===0} unoptimized={m.url.startsWith("/api/")} />}
 {highlights&&<div className="gallery-highlights"><h2>Style<br/>Highlights</h2><dl><div><dt>Composition</dt><dd>{p.composition}</dd></div><div><dt>GSM</dt><dd>{p.gsm}</dd></div><div><dt>Colour</dt><dd>{colour}</dd></div><div><dt>Sleeve length</dt><dd>Short Sleeve</dd></div><div><dt>Fit</dt><dd>Oversized Fit</dd></div></dl></div>}</div>)}
 </div><div className="product-gallery__controls"><button onClick={()=>move(-1)} aria-label="Previous photo">‹</button><span aria-live="polite">{photo+1} / {slides.length}</span><button onClick={()=>move(1)} aria-label="Next photo">›</button></div></section>
 <section className="product-information" aria-labelledby="product-title"><h1 id="product-title">{p.name}</h1><p className="product-price product-price--large">{p.originalPrice>p.price&&<del>{money(p.originalPrice)}</del>}<span>{money(p.price)}</span>{p.originalPrice>p.price&&<span className="product-saving">Save {saving(p)}%</span>}</p><p className="product-shipping">Free shipping on orders ₹999 &amp; above. See our <Link href="/policies/shipping-policy" style={{textDecoration:"underline",textUnderlineOffset:"3px"}}>shipping policy</Link> for details.</p>
 {p.colours.length>1&&<fieldset className="product-sizes"><legend>Colour: {colour}</legend><div>{p.colours.map(c=><button key={c} disabled={busy} aria-pressed={c===colour} onClick={()=>{setColour(c);setSize(p.sizes.find(s=>p.availability?.[c]?.[s]===true)??"");setPhoto(0);setMessage("");gallery.current?.scrollTo({left:0,top:0,behavior:"instant"});}}>{c}</button>)}</div></fieldset>}
 <fieldset className="product-sizes"><legend><span className="product-size-heading"><span>Size{size?`: ${size}`:" — select your size"}</span>{selectedMeasurement&&<span className="product-size-measurements"><span>Chest {selectedMeasurement.chest} inches</span><span>Length {selectedMeasurement.length} inches</span></span>}</span></legend><div>{p.sizes.map(s=><button key={s} disabled={busy||!available(s)} title={!available(s)?"Out of stock":undefined} aria-label={`${s}${!available(s)?" — out of stock":""}`} aria-pressed={size===s} onClick={()=>{setSize(s);setMessage("");}}>{s}</button>)}</div></fieldset>
 {!p.sizes.some(available)&&<p className="stock-availability">This colour is currently out of stock.</p>}
 <div className="product-purchase-actions"><button className="product-add" disabled={busy||!size||!available(size)} onClick={()=>add()}>{busy?"Please wait…":size?"Add to cart":"Select a size"}</button><button className="product-buy-now" disabled={busy||!size||!available(size)} onClick={()=>add(true)}>Buy It Now</button></div><p className="product-cart-message" role="status">{message}</p>
 <ProductDelivery key={`${p.slug}-${colour}-${size}`} slug={p.slug} colour={colour} size={size}/>
 <SizeGuide sizes={p.sizes}/>
 <details className="product-details" open><summary>Product details</summary><p className="product-description">{p.description}</p><dl className="product-specifications"><div><dt>Composition</dt><dd>{p.composition}</dd></div><div><dt>Fabric weight</dt><dd>{p.gsm} GSM</dd></div><div><dt>Colour</dt><dd>{colour}</dd></div><div><dt>Fit</dt><dd>Oversized</dd></div><div><dt>Sleeves</dt><dd>Short</dd></div></dl><p>{p.details}</p></details></section></div></main>;
}
