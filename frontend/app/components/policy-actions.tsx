"use client";
import Link from "next/link";
import {useEffect,useState} from "react";
import {readCart} from "../../lib/cart";
import {openCart} from "./cart-drawer";
import styles from "../policies/shipping-policy/shipping-policy.module.css";
export default function PolicyActions({dark=false}:{dark?:boolean}={}){
 const [count,setCount]=useState(0);
 useEffect(()=>{const update=()=>setCount(readCart().reduce((n,i)=>n+i.quantity,0));const timer=setTimeout(update,0);window.addEventListener("cart-updated",update);window.addEventListener("storage",update);return()=>{clearTimeout(timer);window.removeEventListener("cart-updated",update);window.removeEventListener("storage",update);};},[]);
 return <div className={styles.actions} style={{color:dark?"#fff":"#211c19"}}><Link href="/" aria-label="Home" title="Home"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 10 12 3l9 7M5 9v12h14V9M9 21v-7h6v7"/></svg></Link><Link href="/orders" aria-label="Your orders" title="Your orders"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="7" r="4"/><path d="M3 22c0-11 18-11 18 0"/></svg></Link><button type="button" onClick={openCart} aria-label={`Open cart, ${count} items`} title="Cart"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 8h16l-2 13H6L4 8ZM8 8V6a4 4 0 0 1 8 0v2"/></svg><span className={styles.cartCount}>{count}</span></button></div>;
}
