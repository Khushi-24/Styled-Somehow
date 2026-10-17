"use client";
import {useEffect,useId,useRef,useState} from "react";
import {adminRequest,ApiError} from "../../lib/admin-api";
import {deliveryEstimate} from "../../lib/delivery-estimate";
import {Quote} from "../../lib/cart";
import styles from "./product-delivery.module.css";

export const DELIVERY_PIN_KEY="styled-somehow-delivery-pin";
export default function ProductDelivery({slug,colour,size}:{slug:string;colour:string;size:string}){
 const id=useId();const [pin,setPin]=useState("");const [busy,setBusy]=useState(false);
 const [result,setResult]=useState<{ok:boolean;text:string}|null>(null);const attempt=useRef(0);
 useEffect(()=>{const requests=attempt;const timer=setTimeout(()=>{try{const saved=localStorage.getItem(DELIVERY_PIN_KEY);if(saved&&/^[1-9][0-9]{5}$/.test(saved))setPin(saved);}catch{}},0);return()=>{clearTimeout(timer);requests.current++;};},[]);
 async function check(event:React.FormEvent){
  event.preventDefault();if(busy||!size)return;
  const request=++attempt.current;setBusy(true);setResult(null);
  try{
   const quote=await adminRequest<Quote>("/api/checkout/shipping-quote",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({items:[{slug,colour,size,quantity:1}],pinCode:pin})});
   if(request!==attempt.current)return;
   setResult({ok:true,text:deliveryEstimate(quote.deliveryDays)??"Delivery available. We’ll confirm the delivery estimate when your order is dispatched."});
   try{localStorage.setItem(DELIVERY_PIN_KEY,pin);}catch{}
  }catch(error){if(request!==attempt.current)return;setResult({ok:false,text:error instanceof ApiError&&error.status===422?error.message:error instanceof Error?error.message:"Could not check delivery. Please try again."});}
  finally{if(request===attempt.current)setBusy(false);}
 }
 return <section className={styles.section} aria-labelledby={`${id}-title`}><h2 id={`${id}-title`}>Check delivery</h2>
  <form onSubmit={check} className={styles.form}><label htmlFor={`${id}-pin`} className={styles.srOnly}>Delivery PIN code</label><input id={`${id}-pin`} value={pin} onChange={event=>{attempt.current++;setBusy(false);setResult(null);setPin(event.target.value.replace(/\D/g,"").slice(0,6));}} placeholder="Enter delivery PIN code" pattern="[1-9][0-9]{5}" required maxLength={6} inputMode="numeric" autoComplete="postal-code" aria-describedby={`${id}-hint`}/><button disabled={busy||!size||!/^[1-9][0-9]{5}$/.test(pin)}>{busy?"Checking…":"Check"}</button></form>
  {!size&&<p>Select an available size to check delivery.</p>}
  {result&&<p className={result.ok?styles.success:styles.error} role="status">{result.text}</p>}
  <p id={`${id}-hint`}>Enter your PIN code to check delivery availability.</p>
 </section>;
}
