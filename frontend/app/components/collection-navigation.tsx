"use client";

import {useEffect} from "react";

export default function CollectionNavigation(){
 useEffect(()=>{
  let frame=0;
  function restore(){
   cancelAnimationFrame(frame);
   frame=requestAnimationFrame(()=>{
    const id=window.location.hash.slice(1);
    const target=id ? document.getElementById(id) : null;
    if(target?.closest(".collection-products")) window.scrollTo({top:target.getBoundingClientRect().top+window.scrollY-24,behavior:"instant"});
   });
  }
  function remember(event:MouseEvent){
   if(event.button!==0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey)return;
   const link=event.target instanceof Element ? event.target.closest<HTMLAnchorElement>(".collection-grid a") : null;
   const card=link?.closest<HTMLElement>("article[id]");
   if(!card || !link || link.origin!==window.location.origin || !link.pathname.startsWith("/products/"))return;
   window.history.replaceState(window.history.state,"",`${window.location.pathname}${window.location.search}#${card.id}`);
  }
  restore();
  document.addEventListener("click",remember,true);
  window.addEventListener("popstate",restore);
  window.addEventListener("hashchange",restore);
  window.addEventListener("pageshow",restore);
  return ()=>{cancelAnimationFrame(frame);document.removeEventListener("click",remember,true);window.removeEventListener("popstate",restore);window.removeEventListener("hashchange",restore);window.removeEventListener("pageshow",restore);};
 },[]);
 return null;
}
