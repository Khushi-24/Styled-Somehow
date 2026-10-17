"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

const slides = [
 {src:"/images/women-pastel-editorial.webp",alt:"Shop Women — pastel studio campaign"},
 {src:"/images/women-orange-editorial.webp",alt:"Shop the women’s collection — orange frame campaign"},
];

export default function WomenCollectionBanner(){
 const [active,setActive]=useState(0);
 const [paused,setPaused]=useState(false);
 useEffect(()=>{
  if(paused || window.matchMedia("(prefers-reduced-motion: reduce)").matches)return;
  const timer=window.setInterval(()=>setActive(i=>(i+1)%slides.length),2500);
  return ()=>window.clearInterval(timer);
 },[paused]);
 function scrollToCollection(event:React.MouseEvent<HTMLAnchorElement>){
  const target=document.getElementById("women-collection");
  if(!target || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey)return;
  event.preventDefault();
  target.scrollIntoView({behavior:window.matchMedia("(prefers-reduced-motion: reduce)").matches?"instant":"smooth",block:"start"});
 }
 return <section className="women-banner-carousel" aria-label="Women’s collection campaigns" aria-roledescription="carousel" onMouseEnter={()=>setPaused(true)} onMouseLeave={()=>setPaused(false)} onFocusCapture={()=>setPaused(true)} onBlurCapture={event=>{if(!event.currentTarget.contains(event.relatedTarget))setPaused(false);}}>
  {slides.map((slide,i)=><a key={slide.src} href="#women-collection" className={`women-banner-carousel__slide${i===active?" is-active":""}`} aria-hidden={i!==active} tabIndex={i===active?0:-1} onClick={scrollToCollection} aria-label="Shop women — scroll to the collection">
   <Image unoptimized src={slide.src} alt={slide.alt} width={1672} height={941} loading="eager" fetchPriority={i===0?"high":"auto"}/>
  </a>)}
  <div className="women-banner-carousel__controls" aria-label="Choose a banner">
   <button type="button" aria-label="Previous banner" onClick={()=>setActive(i=>(i+slides.length-1)%slides.length)}>←</button>
   {slides.map((slide,i)=><button type="button" key={slide.src} className="women-banner-carousel__dot" aria-label={`Show banner ${i+1}`} aria-current={active===i?"true":undefined} onClick={()=>setActive(i)}><span/></button>)}
   <button type="button" aria-label="Next banner" onClick={()=>setActive(i=>(i+1)%slides.length)}>→</button>
  </div>
 </section>;
}
