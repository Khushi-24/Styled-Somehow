"use client";
import StoreHeader from "../components/store-header";
import {useEffect,useRef,useState} from "react";
import {adminRequest,ApiError} from "../../lib/admin-api";
import {PendingOrder} from "../../lib/cart";
import {Fulfilment,fulfilmentLabels,fulfilmentSteps} from "../../lib/fulfilment";
import {money} from "../../lib/product";
import styles from "./orders.module.css";
export default function OrderAccessView({initialReference=""}:{initialReference?:string}){
 const [order,setOrder]=useState<PendingOrder|null>(null),[loading,setLoading]=useState(true),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 const linkToken=useRef("");
 const [tracking,setTracking]=useState<Fulfilment|null>(null),[trackingError,setTrackingError]=useState(""),[refreshing,setRefreshing]=useState(false);
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
 const orderId=order?.id;
 useEffect(()=>{
  if(!orderId)return;let live=true;
  async function check(){try{const data=await adminRequest<Fulfilment>(`/api/order-access/order/${encodeURIComponent(orderId!)}/fulfilment`);if(live){setTracking(data);setTrackingError("");}}catch(e){if(live){if(e instanceof ApiError&&e.status===401){setOrder(null);setTracking(null);setError("Order access has expired. Reopen the secure link in your confirmation email.");}else setTrackingError("Could not refresh delivery progress. Please try again.");}}}
  void check();const timer=setInterval(()=>void check(),60000);return()=>{live=false;clearInterval(timer);};
 },[orderId]);
 async function refreshStatus(){
  if(!order||refreshing)return;setRefreshing(true);setTrackingError("");
  try{const data=await adminRequest<PendingOrder>(`/api/order-access/order/${encodeURIComponent(order.id)}`);setOrder(data);setTracking(await adminRequest<Fulfilment>(`/api/order-access/order/${encodeURIComponent(order.id)}/fulfilment`));}
  catch{setOrder(null);setTracking(null);setError("Order access may have expired. Reopen the secure link in your confirmation email.");}
  finally{setRefreshing(false);}
 }
 async function close(){setBusy(true);setError("");try{await adminRequest<void>("/api/order-access/close",{method:"POST"});linkToken.current="";setOrder(null);setTracking(null);window.history.replaceState(window.history.state,"",window.location.pathname);}catch{setError("Could not close order access. Please try again.");}finally{setBusy(false);}}
 return <main className="shopping-page"><StoreHeader/><section className={styles.panel}><h1>Your order</h1>
 {error&&<p role="alert" className="shopping-error">{error}</p>}
 {loading?<p role="status">Opening your order…</p>:order?<><div className={styles.status}>{order.status.replaceAll("_"," ")}</div><p className={styles.reference}>Order reference<br/><strong>{order.id}</strong></p>
 {order.paymentMode==="TEST"&&<p className={styles.test}>TEST ORDER · No real money was charged. This order will not be dispatched.</p>}
 {tracking?.orderId===order.id&&<section className={styles.delivery} aria-label="Delivery progress"><div className={styles.deliveryHeading}><h2>{tracking.onHold?"Order needs attention":fulfilmentLabels[tracking.stage]||tracking.stage}</h2><button disabled={refreshing} onClick={refreshStatus}>{refreshing?"Refreshing…":"Refresh status"}</button></div>
 {tracking.onHold?<p>Contact us if you need help with this order.</p>:<ol className={styles.progress}>{fulfilmentSteps.map((stage,index)=><li key={stage} aria-current={stage===tracking.stage?"step":undefined} className={index<=fulfilmentSteps.indexOf(tracking.stage)?styles.reached:""}>{fulfilmentLabels[stage]}</li>)}</ol>}
 {tracking.test?<p className={styles.muted}>Preparation updates are for testing only. This order will not be shipped.</p>:tracking.awb?<><p><strong>{tracking.courier}</strong><br/>Tracking number: <strong>{tracking.awb}</strong></p><p>{tracking.courierStatus||"Your courier is booked. We’re awaiting its first tracking update."}</p>{tracking.stage==="COURIER_BOOKED"&&<p className={styles.muted}>A tracking number has been assigned. Courier collection is still pending.</p>}{tracking.trackingFailed&&<p className={styles.muted}>Courier tracking is temporarily unavailable. Showing the last saved update.</p>}{tracking.trackingCheckedAt&&<p className={styles.muted}>Last checked: {new Date(tracking.trackingCheckedAt).toLocaleString("en-IN")}</p>}{tracking.trackingUrl&&<a href={tracking.trackingUrl} target="_blank" rel="noopener noreferrer" className={styles.trackLink}>Track with courier ↗</a>}</>:<p className={styles.muted}>Tracking details will appear here once your courier is booked.</p>}
 {!!tracking.timeline.length&&<details className={styles.timeline}><summary>Order updates</summary><ol>{tracking.timeline.map((event,index)=><li key={index}><strong>{fulfilmentLabels[event.stage]||event.stage}</strong><span>{new Date(event.occurredAt).toLocaleString("en-IN")}</span></li>)}</ol></details>}
 </section>}
 {trackingError&&<p className={styles.muted} role="status">{trackingError}</p>}
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
