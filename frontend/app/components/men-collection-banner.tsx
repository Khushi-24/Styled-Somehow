"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

const slides = [
 {src:"/images/men-burgundy-editorial.webp",alt:"Shop Men — burgundy editorial campaign"},
 {src:"/images/men-purple-editorial.webp",alt:"Shop Men — purple torn-paper campaign"},
];

export default function MenCollectionBanner(){
 const [active,setActive]=useState(0);
 const [paused,setPaused]=useState(false);
 const [manualPause,setManualPause]=useState(false);
 useEffect(()=>{
  if(paused || manualPause || window.matchMedia("(prefers-reduced-motion: reduce)").matches)return;
  const timer=window.setInterval(()=>setActive(i=>(i+1)%slides.length),2500);
  return ()=>window.clearInterval(timer);
 },[paused,manualPause]);
 function scrollToCollection(event:React.MouseEvent<HTMLAnchorElement>){
  const target=document.getElementById("men-collection");
  if(!target || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey)return;
  event.preventDefault();
  target.scrollIntoView({behavior:window.matchMedia("(prefers-reduced-motion: reduce)").matches?"instant":"smooth",block:"start"});
 }
 return <section className="women-banner-carousel" aria-label="Men’s collection campaigns" aria-roledescription="carousel" onMouseEnter={()=>setPaused(true)} onMouseLeave={()=>setPaused(false)} onFocusCapture={()=>setPaused(true)} onBlurCapture={event=>{if(!event.currentTarget.contains(event.relatedTarget))setPaused(false);}}>
  {slides.map((slide,i)=><a key={slide.src} href="#men-collection" className={`women-banner-carousel__slide${i===active?" is-active":""}`} aria-hidden={i!==active} tabIndex={i===active?0:-1} onClick={scrollToCollection} aria-label="Shop men — scroll to the collection">
   <Image sizes="100vw" quality={95} src={slide.src} alt={slide.alt} width={1672} height={941} loading={i===0?"eager":"lazy"} fetchPriority={i===0?"high":"auto"}/>
  </a>)}
  <div className="women-banner-carousel__controls" aria-label="Choose a banner"><button type="button" aria-label={manualPause?"Play banners":"Pause banners"} onClick={()=>setManualPause(v=>!v)}>{manualPause?"▶":"Ⅱ"}</button>
   <button type="button" aria-label="Previous banner" onClick={()=>setActive(i=>(i+slides.length-1)%slides.length)}>←</button>
   {slides.map((slide,i)=><button type="button" key={slide.src} className="women-banner-carousel__dot" aria-label={`Show banner ${i+1}`} aria-current={active===i?"true":undefined} onClick={()=>setActive(i)}><span/></button>)}
   <button type="button" aria-label="Next banner" onClick={()=>setActive(i=>(i+1)%slides.length)}>→</button>
  </div>
 </section>;
}
