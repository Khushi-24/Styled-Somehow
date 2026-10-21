"use client";
import Link from "next/link";
import OrderThumbnail from "./thumbnail";
import OrderLogin from "./login";
import StoreHeader from "../components/store-header";
import {useCallback,useEffect,useRef,useState} from "react";
import {adminRequest,ApiError} from "../../lib/admin-api";
import {PendingOrder} from "../../lib/cart";
import {Fulfilment,fulfilmentLabels,fulfilmentSteps,displayStage,fulfilmentDescription} from "../../lib/fulfilment";
import {money} from "../../lib/product";
import styles from "./orders.module.css";
export default function OrderAccessView({initialReference=""}:{initialReference?:string}){
 const [order,setOrder]=useState<PendingOrder|null>(null),[loading,setLoading]=useState(true),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 const linkToken=useRef("");
 const heading=useRef<HTMLHeadingElement>(null),listScroll=useRef(0),restoreList=useRef(false);
 const [signedIn,setSignedIn]=useState(false);
 const sessionChanged=useCallback((value:boolean)=>{setSignedIn(value);if(value){setError("");if(restoreList.current){restoreList.current=false;requestAnimationFrame(()=>{window.scrollTo({top:listScroll.current,behavior:"instant"});heading.current?.focus({preventScroll:true});});}}},[]);
 async function selectOrder(id:string){listScroll.current=window.scrollY;setLoading(true);setError("");try{setOrder(await adminRequest<PendingOrder>(`/api/order-access/order/${encodeURIComponent(id)}`));setTracking(null);window.history.replaceState(window.history.state,"",`/orders?reference=${encodeURIComponent(id)}`);}catch(e){setError(e instanceof Error?e.message:"Could not open order.");}finally{setLoading(false);}}
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
     if(live){setOrder(data);window.history.replaceState(window.history.state,"",`/orders?reference=${encodeURIComponent(result.orderId)}`);}
    }else if(initialReference){
     try{const data=await adminRequest<PendingOrder>(`/api/order-access/order/${encodeURIComponent(initialReference)}`);if(live)setOrder(data);}catch{
      const result=await adminRequest<{orderId:string}>("/api/order-access/checkout",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({orderId:initialReference})});
      const data=await adminRequest<PendingOrder>(`/api/order-access/order/${encodeURIComponent(result.orderId)}`);if(live)setOrder(data);
     }
    }else{
     if(live)setOrder(null);
    }
   }catch(e){if(live&&(!(e instanceof ApiError)||e.status!==401||linkToken.current||initialReference))setError("We couldn’t open this order. Use the secure link in your confirmation email, or contact us for help.");}
   finally{if(live)setLoading(false);}
  }
  void load();return()=>{live=false;};
 },[initialReference]);
 const orderId=order?.id;
 useEffect(()=>{if(!orderId||loading)return;const frame=requestAnimationFrame(()=>{window.scrollTo({top:0,behavior:"instant"});heading.current?.focus({preventScroll:true});});return()=>cancelAnimationFrame(frame);},[orderId,loading]);
 useEffect(()=>{
  if(!orderId)return;let live=true;
  async function check(){try{const data=await adminRequest<Fulfilment>(`/api/order-access/order/${encodeURIComponent(orderId!)}/fulfilment`);if(live){setTracking(data);setTrackingError("");}}catch(e){if(live){if(e instanceof ApiError&&e.status===401){setOrder(null);setTracking(null);setError("Order access has expired. Reopen the secure link in your confirmation email.");}else setTrackingError("Could not refresh delivery progress. Please try again.");}}}
  void check();const timer=setInterval(()=>void check(),60000);return()=>{live=false;clearInterval(timer);};
 },[orderId]);
 async function refreshStatus(){
  if(!order||refreshing)return;setRefreshing(true);setTrackingError("");
  try{const data=await adminRequest<PendingOrder>(`/api/order-access/order/${encodeURIComponent(order.id)}`);setOrder(data);setTracking(await adminRequest<Fulfilment>(`/api/order-access/order/${encodeURIComponent(order.id)}/fulfilment`));}
  catch(e){if(e instanceof ApiError&&(e.status===401||e.status===403)){setOrder(null);setTracking(null);setError("Order access has expired. Log in again or reopen your email link.");}else setTrackingError("Could not refresh right now. Showing your last saved update. Please retry.");}
  finally{setRefreshing(false);}
 }
 async function close(){if(signedIn){setOrder(null);setTracking(null);setError("");linkToken.current="";window.history.replaceState(window.history.state,"","/orders");restoreList.current=true;return;}setBusy(true);setError("");try{await adminRequest<void>("/api/order-access/close",{method:"POST"});linkToken.current="";setOrder(null);setTracking(null);window.history.replaceState(window.history.state,"",window.location.pathname);}catch{setError("Could not close order access. Please try again.");}finally{setBusy(false);}}
 const currentStage=tracking&&tracking.orderId===order?.id?tracking.stage:order?.fulfilmentStage||"PAID";
 const itemCount=order?.items.reduce((sum,item)=>sum+item.quantity,0)||0;
 return <main className={styles.page}><StoreHeader/><div className={styles.content}><header className={`${styles.intro}${!order&&!signedIn?` ${styles.loginIntro}`:""}`}><div>{order&&<p className={styles.eyebrow}>{`ORDER #${order.id.slice(0,8).toUpperCase()}`}</p>}<h1 ref={heading} tabIndex={-1}>{order?"Order details":"My orders"}</h1></div>{order&&<button className={styles.textButton} disabled={busy} onClick={close}>{busy?"Closing…":signedIn?"← All orders":"← Back to my orders"}</button>}</header>
 {error&&<p role="alert" className={styles.error}>{error}</p>}
 {loading?<div className={styles.loading} role="status">Opening your order…</div>:order?<>
 <div className={styles.detailMeta}><span>Placed {new Date(order.createdAt).toLocaleDateString("en-IN",{day:"numeric",month:"long",year:"numeric"})}</span><span>{itemCount} {itemCount===1?"T-shirt":"T-shirts"}</span><span className={styles.status} data-status={order.status}>{order.status==="PAID"?"Payment received":order.status.replaceAll("_"," ")}</span></div>
 {order.paymentMode==="TEST"&&<p className={styles.test}><strong>TEST ORDER</strong> · No real money was charged. This order will not be dispatched.</p>}
 <div className={styles.detailLayout}><div className={styles.detailMain}>
 <section className={styles.delivery} aria-label="Delivery progress"><div className={styles.deliveryHeading}><div><h2>{tracking?.onHold?"Order needs attention":fulfilmentLabels[currentStage]||currentStage}</h2><p className={styles.stageDescription}>{tracking?.onHold?"Contact us if you need help with this order.":fulfilmentDescription(currentStage,tracking?.deliveryMethod)}</p></div><button className={styles.refreshButton} disabled={refreshing} onClick={refreshStatus}>{refreshing?"Refreshing…":"↻ Refresh"}</button></div>
 {!tracking?.onHold&&fulfilmentSteps.includes(displayStage(currentStage))&&<ol className={styles.progress}>{fulfilmentSteps.map((stage,index)=><li key={stage} aria-current={stage===displayStage(currentStage)?"step":undefined} className={index<=fulfilmentSteps.indexOf(displayStage(currentStage))?styles.reached:""}><span className={styles.stepDot} aria-hidden="true">{index<fulfilmentSteps.indexOf(displayStage(currentStage))?"✓":index+1}</span><strong>{fulfilmentLabels[stage]}</strong></li>)}</ol>}
 {tracking?.awb&&<div className={styles.courier}><div><strong>{tracking.courier}</strong><p>Tracking number: {tracking.awb}</p><p>{tracking.courierStatus||"Awaiting the first courier update."}</p>{tracking.stage==="COURIER_BOOKED"&&<p>Courier collection is still pending.</p>}</div>{tracking.trackingUrl&&<a href={tracking.trackingUrl} target="_blank" rel="noopener noreferrer" className={styles.trackLink}>Track parcel ↗</a>}</div>}
 {tracking?.trackingFailed&&<p className={styles.muted}>Courier tracking is temporarily unavailable. Showing the last saved update.</p>}{tracking?.trackingCheckedAt&&<p className={styles.muted}>Last checked: {new Date(tracking.trackingCheckedAt).toLocaleString("en-IN")}</p>}
 {trackingError&&<p className={styles.error} role="status">{trackingError}</p>}
 {!!tracking?.timeline.length&&<details className={styles.timeline}><summary>View order updates</summary><ol>{tracking.timeline.filter((event,index,all)=>event.stage!=="COURIER_BOOKED"&&(index===0||displayStage(event.stage)!==displayStage(all[index-1].stage))).map((event,index)=><li key={index}><strong>{fulfilmentLabels[event.stage]||event.stage}</strong><span>{new Date(event.occurredAt).toLocaleString("en-IN")}</span></li>)}</ol></details>}
 </section>
 <section className={styles.itemsSection}><div className={styles.sectionHeading}><h2>In your order</h2><span>{itemCount} {itemCount===1?"T-shirt":"T-shirts"}</span></div><div className={styles.items}>{order.items.map((item,index)=><Link prefetch={false} href={`/products/${encodeURIComponent(item.slug)}`} className={styles.item} key={`${item.slug}-${item.colour}-${item.size}-${index}`}><OrderThumbnail src={item.image} name={item.name} large/><div className={styles.itemCopy}><h3>{item.name}</h3><p>{item.colour} <span>·</span> Size {item.size} <span>·</span> Qty {item.quantity}</p><span className={styles.productLink}>View product ↗</span></div><strong className={styles.itemPrice}>{money(item.unitPrice*item.quantity)}</strong></Link>)}</div></section>
 </div><aside className={styles.summary} aria-label="Order summary"><section><h2>Payment summary</h2><dl className={styles.totals}><div><dt>Subtotal</dt><dd>{money(order.subtotal)}</dd></div><div><dt>Shipping</dt><dd>{order.shipping===0?"Free":money(order.shipping)}</dd></div><div className={styles.total}><dt>{order.status==="REFUNDED"?"Original payment (refunded)":"Total paid"}</dt><dd>{money(order.total)}</dd></div></dl></section><section className={styles.address}><h2>Delivering to</h2><strong>{order.name}</strong><p>{order.line1}{order.line2&&<><br/>{order.line2}</>}<br/>{order.city}, {order.state}<br/>{order.pinCode}</p></section><details className={styles.reference}><summary>Full order reference</summary><p>{order.id}</p></details><div className={styles.help}><h3>Need help?</h3><a href="mailto:contact@styledsomehow.in">contact@styledsomehow.in ↗</a></div></aside></div>
 {order.status.startsWith("REFUND")&&<p className={styles.error}>This order is not being fulfilled. Contact us if you need help with its refund status.</p>}
 {order.status==="PAYMENT_REVIEW"&&<p className={styles.error}>We’re reviewing this payment. Please contact us before making another payment.</p>}
 </>:<OrderLogin onSelect={selectOrder} onSession={sessionChanged}/>}
 </div></main>;
}
