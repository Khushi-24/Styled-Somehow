"use client";
import StoreHeader from "../components/store-header";
import {useEffect,useRef,useState} from "react";
import {adminRequest,ApiError} from "../../lib/admin-api";
import {PendingOrder} from "../../lib/cart";
import {money} from "../../lib/product";
import styles from "./orders.module.css";
export default function OrderAccessView({initialReference=""}:{initialReference?:string}){
 const [order,setOrder]=useState<PendingOrder|null>(null),[loading,setLoading]=useState(true),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 const linkToken=useRef("");
 useEffect(()=>{
  let live=true;
  const fragment=new URLSearchParams(window.location.hash.slice(1));
  if(fragment.has("access")){linkToken.current=fragment.get("access")||"";window.history.replaceState(window.history.state,"",window.location.pathname+window.location.search);}
  async function load(){
   try{
    if(linkToken.current){
     const result=await adminRequest<{orderId:string}>("/api/order-access/link",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({token:linkToken.current})});
     const data=await adminRequest<PendingOrder>(`/api/order-access/order/${encodeURIComponent(result.orderId)}`);
     if(live)setOrder(data);
    }else if(initialReference){
     const result=await adminRequest<{orderId:string}>("/api/order-access/checkout",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({orderId:initialReference})});
     const data=await adminRequest<PendingOrder>(`/api/order-access/order/${encodeURIComponent(result.orderId)}`);
     if(live)setOrder(data);
    }else{
     const data=await adminRequest<PendingOrder>("/api/order-access/current");if(live)setOrder(data);
    }
   }catch(e){if(live&&(!(e instanceof ApiError)||e.status!==401||linkToken.current||initialReference))setError("We couldn’t open this order. Use the secure link in your confirmation email, or contact us for help.");}
   finally{if(live)setLoading(false);}
  }
  void load();return()=>{live=false;};
 },[initialReference]);
 async function close(){setBusy(true);setError("");try{await adminRequest<void>("/api/order-access/close",{method:"POST"});linkToken.current="";setOrder(null);window.history.replaceState(window.history.state,"",window.location.pathname);}catch{setError("Could not close order access. Please try again.");}finally{setBusy(false);}}
 return <main className="shopping-page"><StoreHeader/><section className={styles.panel}><h1>Your order</h1>
 {error&&<p role="alert" className="shopping-error">{error}</p>}
 {loading?<p role="status">Opening your order…</p>:order?<><div className={styles.status}>{order.status.replaceAll("_"," ")}</div><p className={styles.reference}>Order reference<br/><strong>{order.id}</strong></p>
 {order.paymentMode==="TEST"&&<p className={styles.test}>TEST ORDER · No real money was charged. This order will not be dispatched.</p>}
 <div className={styles.items}>{order.items.map((item,index)=><div key={index}><div><strong>{item.name}</strong><p>{item.colour} · {item.size} · Qty {item.quantity}</p></div><strong>{money(item.unitPrice*item.quantity)}</strong></div>)}</div>
 <dl className={styles.totals}><div><dt>Shipping</dt><dd>{order.shipping===0?"Free":money(order.shipping)}</dd></div><div><dt>Total</dt><dd>{money(order.total)}</dd></div></dl>
 <h2>Delivery address</h2><p>{order.name}<br/>{order.line1}{order.line2&&<><br/>{order.line2}</>}<br/>{order.city}, {order.state} {order.pinCode}</p>
 {order.status.startsWith("REFUND")&&<p>This order is not being fulfilled. Contact us if you need help with its refund status.</p>}
 {order.status==="PAYMENT_REVIEW"&&<p>We’re reviewing this payment. Please contact us before making another payment.</p>}
 <button disabled={busy} onClick={close}>{busy?"Closing…":"Close order access"}</button><p className={styles.muted}>On a shared device, close access when finished. Access expires after 30 minutes. You can reopen the link from your email.</p>
 </>:<><p>Open the <strong>View order</strong> link in your confirmation email to see your order status.</p><p>Lost the email or need help? Contact <a href="mailto:contact@styledsomehow.in">contact@styledsomehow.in</a>{initialReference&&<> and include reference <strong>{initialReference}</strong></>}.</p></>}
 {order&&<p className={styles.muted}>Need help? <a href="mailto:contact@styledsomehow.in">contact@styledsomehow.in</a></p>}
 </section></main>;
}
