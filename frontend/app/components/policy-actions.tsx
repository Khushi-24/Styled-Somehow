"use client";
import Link from "next/link";
import {useEffect,useState} from "react";
import {readCart} from "../../lib/cart";
import {openCart} from "./cart-drawer";
import styles from "../policies/shipping-policy/shipping-policy.module.css";
export default function PolicyActions(){
 const [count,setCount]=useState(0);
 useEffect(()=>{const update=()=>setCount(readCart().reduce((n,i)=>n+i.quantity,0));const timer=setTimeout(update,0);window.addEventListener("cart-updated",update);window.addEventListener("storage",update);return()=>{clearTimeout(timer);window.removeEventListener("cart-updated",update);window.removeEventListener("storage",update);};},[]);
 return <div className={styles.actions}><Link href="/#new-in" aria-label="Continue shopping" title="Continue shopping"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 9h18L19 3H5L3 9ZM4 9v12h16V9M9 21v-7h6v7M3 9c0 3 4 3 4 0 0 3 5 3 5 0 0 3 5 3 5 0 0 3 4 3 4 0"/></svg></Link><Link href="/orders" aria-label="Your orders" title="Your orders"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="7" r="4"/><path d="M3 22c0-11 18-11 18 0"/></svg></Link><button type="button" onClick={openCart} aria-label={`Open cart, ${count} items`} title="Cart"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 8h16l-2 13H6L4 8ZM8 8V6a4 4 0 0 1 8 0v2"/></svg><span className={styles.cartCount}>{count}</span></button></div>;
}
