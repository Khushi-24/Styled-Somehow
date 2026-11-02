"use client";
import Image from "next/image";

export default function MenCollectionBanner(){
 function scrollToCollection(event:React.MouseEvent<HTMLAnchorElement>){
  const target=document.getElementById("men-collection");
  if(!target||event.ctrlKey||event.metaKey||event.shiftKey||event.altKey)return;
  event.preventDefault();
  target.scrollIntoView({behavior:window.matchMedia("(prefers-reduced-motion: reduce)").matches?"instant":"smooth",block:"start"});
 }
 return <section className="women-banner-carousel" aria-label="Men’s collection campaign">
  <a href="#men-collection" className="women-banner-carousel__slide is-active" onClick={scrollToCollection} aria-label="Shop men — scroll to the collection">
   <Image sizes="100vw" quality={95} src="/images/men-burgundy-editorial.webp" alt="Shop Men — burgundy editorial campaign" width={1672} height={941} loading="eager" fetchPriority="high"/>
  </a>
 </section>;
}
