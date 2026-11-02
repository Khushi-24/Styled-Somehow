"use client";
import {useEffect,useRef,useState} from "react";
import CartView from "../cart/view";
export function openCart(){window.dispatchEvent(new Event("cart-open"));}
export default function CartDrawer(){
 const dialog=useRef<HTMLDialogElement>(null);
 const [visible,setVisible]=useState(false);
 useEffect(()=>{function open(){setVisible(true);dialog.current?.showModal();}window.addEventListener("cart-open",open);return()=>window.removeEventListener("cart-open",open);},[]);
 useEffect(()=>{if(!visible)return;const previous=document.body.style.overflow;document.body.style.overflow="hidden";return()=>{document.body.style.overflow=previous;};},[visible]);
 function close(){dialog.current?.close();setVisible(false);}
 return <dialog className="cart-drawer" ref={dialog} aria-label="Your shopping cart" onClose={()=>setVisible(false)} onClick={e=>{if(e.target===e.currentTarget)close();}}><div className="cart-drawer-header"><h2>Your cart</h2><button type="button" onClick={close} aria-label="Close cart">×</button></div>{visible&&<CartView drawer onClose={close}/>}</dialog>;
}
