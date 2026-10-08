"use client";
import Image from "next/image";
import Link from "next/link";
import {useRef,useState} from "react";
import {writeCart,readCart} from "../../lib/cart";
import {Product,money,saving} from "../../lib/product";
export default function ProductView({product:p,returnUrl}: {product:Product;returnUrl:string}) {
 const [colour,setColour]=useState(p.media[0].colour);
 const [size,setSize]=useState("");const [message,setMessage]=useState("");const [photo,setPhoto]=useState(0);
 const gallery=useRef<HTMLDivElement>(null);
 const images=p.media.filter(m=>m.colour===colour);
 const slides=[{media:images[0],highlights:false},{media:images[1]?.kind==="image"?images[1]:images[0],highlights:true},...images.slice(1).map(media=>({media,highlights:false}))];
 function move(direction:number){gallery.current?.scrollTo({left:((photo+direction+slides.length)%slides.length)*gallery.current.clientWidth,behavior:window.matchMedia("(prefers-reduced-motion: reduce)").matches?"auto":"smooth"});}
 function available(s:string){return p.availability?.[colour]?.[s]===true;}
 async function add(){if(!size||!available(size)){setMessage("This size is out of stock.");return;}try{
  const response=await fetch(`/api/products/${encodeURIComponent(p.slug)}`,{cache:"no-store"});if(!response.ok)throw new Error("Stock check failed");const fresh:Product=await response.json();if(fresh.availability?.[colour]?.[size]!==true){setMessage("This size is now out of stock. Please refresh the page.");return;}
  const cart=readCart();
  const item=cart.find((v:{slug:string;size:string;colour:string})=>v.slug===p.slug&&v.size===size&&v.colour===colour);
  if(item&&item.quantity>=20){setMessage("Maximum 20 of this variant per checkout.");return;}if(item)item.quantity+=1;else cart.push({slug:p.slug,name:p.name,size,colour,price:p.price,quantity:1});
  writeCart(cart);setMessage(`${p.name} · ${colour} · ${size} added to your cart.`);
 }catch{setMessage("Could not save your cart. Please try again.");}}
 return <main className="product-page"><nav className="product-nav" aria-label="Product navigation"><Link className="brand-logo" href="/" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><Link href={returnUrl}>← Continue shopping</Link><Link href="/cart">Cart</Link></nav><p className="product-breadcrumb"><Link href="/">Home</Link> / {p.name}</p>
 <div className="product-layout"><section className="product-gallery" aria-label="Product photos"><div className="product-gallery__rail" ref={gallery} onScroll={e=>setPhoto(Math.round(e.currentTarget.scrollLeft/e.currentTarget.clientWidth))}>
 {slides.map(({media:m,highlights},i)=><div key={`${colour}-${i}`} className={`product-gallery__slide${m.contain&&!highlights?" product-gallery__slide--detail":""}`} role="group" aria-label={`Photo ${i+1} of ${slides.length}`}>
 {m.kind==="video"?<video className="product-video" src={m.url} controls playsInline preload="metadata" aria-label={m.alt}/>:<Image src={m.url} alt={highlights?`${p.name} style highlights`:m.alt} fill sizes="(max-width: 820px) 100vw, 55vw" preload={i===0} unoptimized={m.url.startsWith("/api/")} />}
 {highlights&&<div className="gallery-highlights"><h2>Style<br/>Highlights</h2><dl><div><dt>Composition</dt><dd>{p.composition}</dd></div><div><dt>GSM</dt><dd>{p.gsm}</dd></div><div><dt>Colour</dt><dd>{colour}</dd></div><div><dt>Sleeve length</dt><dd>Short Sleeve</dd></div><div><dt>Fit</dt><dd>Oversized Fit</dd></div></dl></div>}</div>)}
 </div><div className="product-gallery__controls"><button onClick={()=>move(-1)} aria-label="Previous photo">‹</button><span aria-live="polite">{photo+1} / {slides.length}</span><button onClick={()=>move(1)} aria-label="Next photo">›</button></div></section>
 <section className="product-information" aria-labelledby="product-title"><h1 id="product-title">{p.name}</h1><p className="product-price product-price--large">{p.originalPrice>p.price&&<del>{money(p.originalPrice)}</del>}<span>{money(p.price)}</span>{p.originalPrice>p.price&&<span className="product-saving">Save {saving(p)}%</span>}</p><p className="product-description">{p.description}</p><dl className="product-highlights"><div><dt>Composition</dt><dd>{p.composition}</dd></div><div><dt>GSM</dt><dd>{p.gsm}</dd></div><div><dt>Colour</dt><dd>{colour}</dd></div><div><dt>Fit</dt><dd>Oversized</dd></div><div><dt>Sleeves</dt><dd>Short</dd></div></dl>
 {p.colours.length>1&&<fieldset className="product-sizes"><legend>Colour: {colour}</legend><div>{p.colours.map(c=><button key={c} aria-pressed={c===colour} onClick={()=>{setColour(c);setSize("");setPhoto(0);setMessage("");gallery.current?.scrollTo({left:0,top:0,behavior:"instant"});}}>{c}</button>)}</div></fieldset>}
 <fieldset className="product-sizes"><legend>Size{size?`: ${size}`:" — select your size"}</legend><div>{p.sizes.map(s=><button key={s} disabled={!available(s)} title={!available(s)?"Out of stock":undefined} aria-label={`${s}${!available(s)?" — out of stock":""}`} aria-pressed={size===s} onClick={()=>{setSize(s);setMessage("");}}>{s}</button>)}</div></fieldset>
 <p className="stock-availability">{p.sizes.some(available)?"Unavailable sizes are marked as out of stock.":"This colour is currently out of stock."}</p>
 <details className="product-details"><summary>Size guide</summary><p>Temporary reference: Bonkers Corner oversized T-shirt sizing. Product measurements will be updated when confirmed.</p><table className="product-size-table"><caption>Garment measurements in inches</caption><thead><tr><th>Size</th><th>Chest</th><th>Length</th></tr></thead><tbody>{[["S",44,28],["M",46,29],["L",48,30],["XL",51,30.5]].filter(([s])=>p.sizes.includes(String(s))).map(([s,c,l])=><tr key={s}><th scope="row">{s}</th><td>{c}</td><td>{l}</td></tr>)}</tbody></table></details>
 <button className="product-add" disabled={!size||!available(size)} onClick={add}>{size?"Add to cart":"Select a size"}</button><p className="product-cart-message" role="status">{message}</p><details className="product-details" open><summary>Product details</summary><p>{p.details}</p></details></section></div></main>;
}
