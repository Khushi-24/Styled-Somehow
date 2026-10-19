"use client";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { Product } from "../../lib/product";
import { money } from "../../lib/product";

function audience(p: Product) {
  const labels = p.collections.map(c => c.toLowerCase());
  if (labels.some(c => c === "women" || c.includes("women"))) return "WOMEN";
  if (labels.some(c => c === "men" || c.includes("men"))) return "MEN";
  return "COLLECTION";
}
function interleave(products: Product[]) {
  const women = products.filter(p => audience(p) === "WOMEN");
  const men = products.filter(p => audience(p) === "MEN");
  const other = products.filter(p => audience(p) === "COLLECTION");
  const output: Product[] = [];
  while ((women.length || men.length) && output.length < 9) {
    if (women.length) output.push(women.shift()!);
    if (men.length && output.length < 9) output.push(men.shift()!);
  }
  return output.concat(other).slice(0,9);
}
export default function InteractiveProductRack({ products }: { products: Product[] }) {
  const items = useMemo(() => interleave(products.filter(p => p.status === "PUBLISHED" && p.media?.some(m => m.kind === "image"))), [products]);
  const [active,setActive] = useState(0);
  const [manual,setManual] = useState(false);
  const [narrow,setNarrow] = useState(false);
  useEffect(() => {
    const mq = window.matchMedia("(max-width: 680px)");
    const sync = () => setNarrow(mq.matches);
    sync(); mq.addEventListener("change",sync);
    return () => mq.removeEventListener("change",sync);
  }, []);
  useEffect(() => {
    if (manual || items.length < 2 || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const timer = window.setInterval(() => setActive(v => (v + 1) % items.length), 3000);
    return () => window.clearInterval(timer);
  }, [manual,items.length]);
  if (!items.length) return null;
  const choose = (i:number) => {setManual(true);setActive((i+items.length)%items.length);};
  const selected = items[active] ?? items[0];
  return <section className="ss-rack" aria-labelledby="ss-rack-title">
    <header className="ss-rack-intro"><h2 id="ss-rack-title">PICK YOUR ENERGY.</h2><p>Find the fit that feels like you.</p></header>
    <div className="ss-rack-stage">
      <div className="ss-rack-rail" aria-hidden="true" />
      <div className="ss-rack-garments">
        {items.map((p,i) => {
          const delta=i-active, dist=Math.abs(delta);
          const pitch=narrow?65:Math.min(128,880/Math.max(items.length-1,1));
          const center=(i-(items.length-1)/2)*pitch;
          const shift=(delta>0?1:delta<0?-1:0)*(narrow?16:37);
          const media=p.media.find(m=>m.kind==="image")!;
          return <Link prefetch={false} href={`/products/${p.slug}`} key={p.slug} className={"ss-rack-item"+(i===active?" is-selected":"")}
            style={{transform:`translateX(calc(-50% + ${center+shift}px)) scale(${i===active?(narrow?1.14:1.23):1})`,zIndex:i===active?30:20-dist}}
            aria-label={`View ${p.name}`}
            onPointerEnter={e=>{if(e.pointerType==="mouse")choose(i);}} onFocus={()=>choose(i)}>
            <svg className="ss-rack-hook" viewBox="0 0 110 45" aria-hidden="true"><path d="M55 21c-10-1-10-17 0-17 12 0 13 13 2 18L11 42h88L57 22" fill="none" stroke="#8f8b83" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
            <span className="ss-rack-photo"><Image src={media.url} alt={media.alt || p.name} fill sizes="(max-width:680px) 165px, 220px" unoptimized={media.url.startsWith("/api/")} style={{objectFit:media.contain?"contain":"cover"}} /></span>
          </Link>;
        })}
      </div>
    </div>
    <div className="ss-rack-details" aria-live="polite"><h3>{selected.name}</h3><p>{money(selected.price)}</p>
      <div className="ss-rack-actions"><button type="button" onClick={()=>choose(active-1)} aria-label="Previous product">←</button><Link prefetch={false} href={`/products/${selected.slug}`}>EXPLORE DESIGN ↗</Link><button type="button" onClick={()=>choose(active+1)} aria-label="Next product">→</button></div>
      <small>{manual?"HOVER TO PREVIEW — CLICK OR TAP TO SHOP":"AUTO PREVIEW — CLICK OR TAP TO SHOP"}</small>
    </div>
    <style jsx global>{`
      .ss-rack{background:#f5f3ef;color:#191919;overflow:hidden;padding:48px 0 32px;isolation:isolate}
      .ss-rack-intro{text-align:center;padding:0 18px}
      .ss-rack-intro>span{font-size:10px;letter-spacing:.28em;color:#777}
      .ss-rack-intro h2{font:900 clamp(36px,6vw,76px)/1 Arial,Helvetica,sans-serif;letter-spacing:-.065em;margin:12px 0}
      .ss-rack-intro p{color:#777;font-size:14px;margin:0}
      .ss-rack-stage{position:relative;height:clamp(340px,43vw,535px);max-width:1350px;margin:12px auto 0}
      .ss-rack-rail{position:absolute;left:6%;right:6%;top:38px;height:9px;background:linear-gradient(#777,#eee 31%,#aaa 67%,#777);border-radius:10px;box-shadow:0 4px 5px #0002}
      .ss-rack-rail:before,.ss-rack-rail:after{content:"";position:absolute;top:-2px;width:10px;height:14px;background:#999}
      .ss-rack-rail:before{left:0}.ss-rack-rail:after{right:0}
      .ss-rack-garments{position:relative;height:100%}
      .ss-rack-item{position:absolute;left:50%;top:34px;width:clamp(125px,16vw,215px);height:82%;background:none;border:0;padding:0;cursor:pointer;transform-origin:50% 5%;transition:transform .75s cubic-bezier(.18,.8,.23,1),filter .65s;filter:brightness(.87) drop-shadow(0 9px 9px #0002)}
      .ss-rack-item:focus-visible{outline:2px solid #191919;outline-offset:6px}
      .ss-rack-item.is-selected{filter:brightness(1) drop-shadow(0 20px 22px #0003)}
      .ss-rack-hook{position:relative;display:block;height:45px;width:90px;margin:0 auto -4px}
      .ss-rack-photo{display:block;position:relative;height:calc(100% - 45px);width:100%;border-radius:3px;background:#e0dbd4;overflow:hidden;box-shadow:0 2px 6px #0002}
      .ss-rack-photo img{transition:filter .5s}
      .ss-rack-item:not(.is-selected) .ss-rack-photo img{filter:saturate(.8)}
      .ss-rack-details{text-align:center;min-height:165px;padding:0 16px}
      .ss-rack-details h3{font-size:clamp(19px,2.4vw,26px);letter-spacing:-.04em;margin:8px 0}
      .ss-rack-details p{font-size:13px;margin:5px}
      .ss-rack-actions{display:flex;align-items:center;justify-content:center;gap:14px;margin:15px 0}
      .ss-rack-actions button{border:1px solid #ccc;background:transparent;width:44px;height:42px;cursor:pointer}
      .ss-rack-actions a{background:#181818;color:white;font-size:11px;letter-spacing:.1em;padding:16px 26px;font-weight:700}
      .ss-rack-details small{font-size:9px;color:#888;letter-spacing:.11em}
      @media(max-width:680px){.ss-rack{padding:32px 0 24px}.ss-rack-stage{height:345px}.ss-rack-rail{left:4%;right:4%}.ss-rack-item{width:156px;height:78%}.ss-rack-intro h2{font-size:39px}}
      @media(prefers-reduced-motion:reduce){.ss-rack-item{transition:none}}
    `}</style>
  </section>;
}
