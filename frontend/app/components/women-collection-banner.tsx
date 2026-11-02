"use client";
import Image from "next/image";

export default function WomenCollectionBanner(){
 function scrollToCollection(event:React.MouseEvent<HTMLAnchorElement>){
  const target=document.getElementById("women-collection");
  if(!target||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;
  event.preventDefault();
  target.scrollIntoView({behavior:window.matchMedia("(prefers-reduced-motion: reduce)").matches?"instant":"smooth",block:"start"});
 }
 return <section className="women-banner-carousel" aria-label="Women’s collection campaign">
  <a href="#women-collection" className="women-banner-carousel__slide is-active" onClick={scrollToCollection} aria-label="Shop women — scroll to the collection">
   <Image sizes="100vw" quality={95} src="/images/women-pastel-editorial.webp" alt="Shop Women — pastel studio campaign" width={1672} height={941} loading="eager" fetchPriority="high"/>
  </a>
 </section>;
}
