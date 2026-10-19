"use client";
import {useEffect,useId,useRef,useState} from "react";
import {PendingOrder} from "../../../lib/cart";
import {adminRequest} from "../../../lib/admin-api";
import {money} from "../../../lib/product";
import styles from "./shipment.module.css";

type Booking={orderId:string;status:string;courier:string;quotedCost:number;providerOrderId?:number;providerShipmentId?:number;awb?:string;message?:string};
type State={bookingEnabled:boolean;booking:Booking|null};
type Parcel={weightKg:number;lengthCm:number;breadthCm:number;heightCm:number};
type Option={courierId:number;courier:string;rupees:number;transitDays:number|null;rating:number|null};
type Comparison={options:Option[];customerShipping:number;quantity:number;pickupLocation:string;deliveryPin:string;parcel:Parcel};
const fields=[{key:"weightKg",label:"Packed weight (kg)",min:"0.001",max:"50",step:"0.001"},{key:"lengthCm",label:"Length (cm)",min:"0.1",max:"300",step:"0.01"},{key:"breadthCm",label:"Width (cm)",min:"0.1",max:"300",step:"0.01"},{key:"heightCm",label:"Height (cm)",min:"0.1",max:"300",step:"0.01"}] as const;
function parcelDefaults(quantity:number){return {weightKg:String(Math.ceil(quantity/2)*0.5),lengthCm:"27.94",breadthCm:"33.02",heightCm:String(Number((quantity*2.54).toFixed(2))) };}
export default function Shipment({order,onBooked}:{order:PendingOrder;onBooked?:()=>void}){
 const id=useId();
 const reviewHeading=useRef<HTMLHeadingElement>(null);
 const [providerId,setProviderId]=useState("");
 async function reconcile(e:React.FormEvent){e.preventDefault();setBusy(true);setError("");try{const booking=await adminRequest<Booking>(`${path}/reconcile`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({providerOrderId:Number(providerId||state?.booking?.providerOrderId)})});setState(previous=>({...previous!,booking}));onBooked?.();}catch(e){setError(e instanceof Error?e.message:"Could not reconcile");}finally{setBusy(false);}}
 const [acknowledged,setAcknowledged]=useState(false);
 const [state,setState]=useState<State|null>(null),[comparison,setComparison]=useState<Comparison|null>(null),[selectedId,setSelectedId]=useState<number|null>(null),[review,setReview]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState("");
 const quantity=order.items.reduce((total,item)=>total+item.quantity,0);
 const [parcel,setParcel]=useState(()=>parcelDefaults(quantity));
 const parcelKey=`styled-somehow-admin-parcel-v2-${quantity}`;
 useEffect(()=>{const timer=setTimeout(()=>{setParcel(parcelDefaults(quantity));try{const saved=JSON.parse(localStorage.getItem(parcelKey)||"null");if(!saved)return;const next=parcelDefaults(quantity);for(const field of fields){const value=Number(saved[field.key]);if(!Number.isFinite(value)||value<Number(field.min)||value>Number(field.max))return;next[field.key]=String(value);}setParcel(next);}catch{}},0);return()=>clearTimeout(timer);},[parcelKey,quantity]);
 const path=`/api/admin/orders/${encodeURIComponent(order.id)}/shipment`;
 const eligible=order.status==="PAID"&&(order.paymentMode==="LIVE"||order.paymentMode==="TEST");
 const canBook=state?.bookingEnabled&&order.paymentMode==="LIVE"&&order.fulfilmentStage==="READY_TO_SHIP";
 useEffect(()=>{if(!eligible)return;let live=true;adminRequest<State>(path).then(data=>{if(live)setState(data);}).catch(e=>{if(live)setError(e instanceof Error?e.message:"Cannot load shipment.");});return()=>{live=false;};},[eligible,path]);
 async function refresh(){setBusy(true);setError("");try{setState(await adminRequest<State>(path));}catch(e){setError(e instanceof Error?e.message:"Cannot load shipment.");}finally{setBusy(false);}}
 async function compare(event:React.FormEvent){event.preventDefault();if(busy)return;setBusy(true);setError("");setComparison(null);setReview(false);try{const data=await adminRequest<Comparison>(`${path}/compare`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(Object.fromEntries(Object.entries(parcel).map(([key,value])=>[key,Number(value)])))});try{localStorage.setItem(parcelKey,JSON.stringify(data.parcel));}catch{}setComparison(data);setSelectedId(data.options[0]?.courierId??null);}catch(e){setError(e instanceof Error?e.message:"Cannot compare couriers.");}finally{setBusy(false);}}
 const selected=comparison?.options.find(option=>option.courierId===selectedId);
 useEffect(()=>{if(review){reviewHeading.current?.focus({preventScroll:true});reviewHeading.current?.scrollIntoView({behavior:"smooth",block:"nearest"});}},[review]);
 async function reviewCourier(){
  if(!selected||!comparison||busy)return;setBusy(true);setError("");setAcknowledged(false);
  try{
   const fresh=await adminRequest<Comparison>(`${path}/compare`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(comparison.parcel)});
   setComparison(fresh);const option=fresh.options.find(rate=>rate.courierId===selected.courierId);
   if(!option){setSelectedId(null);setReview(false);setError("This courier is no longer available. Select another option.");return;}
   if(option.rupees!==selected.rupees)setError("The courier quote has changed. Review the updated cost below.");
   setReview(true);
  }catch(e){setComparison(null);setReview(false);setError(e instanceof Error?e.message:"Could not refresh this courier. Compare prices again.");}
  finally{setBusy(false);}
 }
 async function book(){if(!selected||!comparison||busy||!review||!canBook||!acknowledged)return;setBusy(true);setError("");try{const booking=await adminRequest<Booking>(path,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({expectedCost:selected.rupees,courierId:selected.courierId,parcel:comparison.parcel})});setState({bookingEnabled:true,booking});setComparison(null);onBooked?.();}catch(e){setError(e instanceof Error?e.message:"Shipment request failed. Refresh its status before doing anything else.");setComparison(null);setState(null);}finally{setReview(false);setBusy(false);}}
 const booking=state?.booking;
 return <section className={styles.panel} aria-label="Shipment booking"><h3>Shipment booking</h3>
 {!eligible?<p className={styles.notice}>{order.paymentMode==="TEST"?"TEST ORDER — real shipment booking is blocked. No courier charges will be made.":"Only paid LIVE orders can be shipped. Unpaid, refunded and review orders are blocked."}</p>:<>
 {error&&<p role="alert" className={styles.error}>{error}</p>}
 {booking?<><p className={booking.status==="BOOKED"?styles.success:styles.notice} role="status"><strong>{booking.status.replaceAll("_"," ")}</strong> · {booking.message}</p><dl><dt>Courier</dt><dd>{booking.courier}</dd><dt>Quoted courier cost</dt><dd>{money(booking.quotedCost)}</dd>{booking.awb&&<><dt>Tracking / AWB</dt><dd>{booking.awb}</dd></>}{booking.providerOrderId&&<><dt>Shiprocket order</dt><dd>{booking.providerOrderId}</dd></>}{booking.providerShipmentId&&<><dt>Shipment ID</dt><dd>{booking.providerShipmentId}</dd></>}</dl><p>Manage labels and pickup in Shiprocket. For a pending or review status, check Shiprocket using order reference {order.id}. Do not create another shipment.</p><button disabled={busy} onClick={refresh}>Refresh shipment status</button><form onSubmit={reconcile}><label>Shiprocket internal order ID<input type="number" required min="1" value={providerId||booking.providerOrderId||""} onChange={e=>setProviderId(e.target.value)}/></label><button disabled={busy}>Verify existing shipment with Shiprocket</button><p>This only reads provider details and links a matching shipment. It does not create, charge, cancel or rebook a courier.</p></form></>:!state?<button disabled={busy} onClick={refresh}>{busy?"Loading…":"Load shipment status"}</button>:<>
 {order.paymentMode==="TEST"&&<p className={styles.notice}>TEST ORDER — courier comparison only. Real shipment booking is blocked; no shipment is created.</p>}
 {!state.bookingEnabled&&<p className={styles.notice}>You can compare couriers. Real booking remains disabled until Shiprocket KYC and live-payment setup are complete.</p>}
 <p>Default pack: 11 × 13 inches, with 1 inch height per T-shirt. Measurements below are in centimetres and remain editable. After a successful check, this browser remembers them for orders with the same number of T-shirts. Confirm the packed parcel before comparing. Delivery PIN: <strong>{order.pinCode}</strong>. Customer paid for shipping: <strong>{money(order.shipping)}</strong>.</p>
 <form onSubmit={compare}><div className={styles.parcelFields}>{fields.map(field=><label key={field.key} htmlFor={`${id}-${field.key}`}>{field.label}<input id={`${id}-${field.key}`} type="number" inputMode="decimal" min={field.min} max={field.max} step={field.step} required disabled={busy} value={parcel[field.key]} onChange={event=>{setParcel({...parcel,[field.key]:event.target.value});setComparison(null);setReview(false);}}/></label>)}</div><button disabled={busy} type="submit">{busy?"Please wait…":comparison?"Refresh courier prices":"Compare couriers"}</button></form>
 {comparison&&<><p>Pickup: <strong>{comparison.pickupLocation}</strong> · {comparison.quantity} T-shirt(s). Sorted by lowest API quote; amounts rounded up to the next rupee. Final Shiprocket charges may differ.</p>
 {comparison.options.length===0?<p className={styles.notice}>No supported courier options were returned for this parcel. Check this order in Shiprocket.</p>:<><div className={styles.tableWrap}><table className={styles.table}><caption>Courier options for this order</caption><thead><tr><th scope="col">Select courier</th><th scope="col">API quote</th><th scope="col">Estimated delivery</th><th scope="col">Rating</th></tr></thead><tbody>{comparison.options.map((option,index)=><tr key={option.courierId} className={selectedId===option.courierId?styles.selected:""}><td><label><input type="radio" name={`${id}-courier`} checked={selectedId===option.courierId} disabled={busy} onChange={()=>{setSelectedId(option.courierId);setReview(false);}}/><span>{option.courier}{index===0&&<small className={styles.badge}>Lowest quote</small>}</span></label></td><td><strong>{money(option.rupees)}</strong>{option.rupees>110&&<small className={styles.overLimit}>Above ₹110 checkout limit</small>}</td><td>{option.transitDays?`${option.transitDays} ${option.transitDays===1?"day":"days"} after dispatch`:"Not provided"}</td><td>{option.rating!=null?`${option.rating.toFixed(1)} / 5`:"Not provided"}</td></tr>)}</tbody></table></div>
 {selected&&!review&&<button disabled={busy} onClick={reviewCourier}>{busy?"Refreshing quote…":"Review selected courier"}</button>}
 {selected&&review&&<div className={styles.confirm} aria-labelledby={`${id}-review-title`}><div className={styles.reviewTitle}><h4 ref={reviewHeading} tabIndex={-1} id={`${id}-review-title`}>Review selected courier</h4><span className={order.paymentMode==="TEST"?styles.testBadge:styles.liveBadge}>{order.paymentMode==="TEST"?"TEST · Preview only":"LIVE ORDER"}</span></div>
 <div className={styles.reviewGrid}><div><span className={styles.reviewLabel}>Selected courier</span><strong>{selected.courier}</strong><p>{selected.transitDays?`${selected.transitDays} ${selected.transitDays===1?"day":"days"} after dispatch`:"Delivery estimate not provided"}{selected.rating!=null?` · Rating ${selected.rating.toFixed(1)} / 5`:""}</p></div><div><span className={styles.reviewLabel}>Current courier quote</span><strong className={styles.reviewPrice}>{money(selected.rupees)}</strong><p>Refreshed for this parcel. Final Shiprocket charges may differ.</p></div></div>
 <dl><dt>Order reference</dt><dd>{order.id}</dd><dt>Delivery</dt><dd>{order.city}, {order.state} · <strong>{comparison.deliveryPin}</strong></dd><dt>Pickup location</dt><dd>{comparison.pickupLocation}</dd><dt>Parcel</dt><dd>{comparison.quantity} T-shirt(s) · {comparison.parcel.weightKg} kg<br/>{comparison.parcel.lengthCm} × {comparison.parcel.breadthCm} × {comparison.parcel.heightCm} cm</dd><dt>Customer paid shipping</dt><dd>{comparison.customerShipping===0?"Free shipping":money(comparison.customerShipping)}</dd><dt>Shipping difference</dt><dd>{selected.rupees>comparison.customerShipping?`${money(selected.rupees-comparison.customerShipping)} absorbed by Styled Somehow`:"Covered by the customer’s shipping payment"}</dd></dl>
 {selected.rupees>110&&<p className={styles.notice}>This courier quote exceeds your ₹110 checkout limit.</p>}
 {order.paymentMode==="TEST"?<p className={styles.notice}>This is a TEST preview. No Shiprocket order, AWB, pickup or courier charge will be created.</p>:!state?.bookingEnabled?<p className={styles.notice}>Booking is disabled. Complete Shiprocket verification and enable live booking before creating this shipment.</p>:order.fulfilmentStage!=="READY_TO_SHIP"?<p className={styles.notice}>Mark this order Ready to ship in Fulfilment before booking this courier.</p>:<><p>Confirming creates a real Shiprocket shipment and requests an AWB. Wallet charges may apply. Labels and pickup are managed in Shiprocket; pickup is not scheduled automatically. The customer’s order total will not change.</p><label className={styles.acknowledgement}><input type="checkbox" checked={acknowledged} disabled={busy} onChange={e=>setAcknowledged(e.target.checked)}/><span>I have checked the packed parcel, delivery details and courier cost, and approve creating this shipment.</span></label><button className={styles.bookButton} disabled={busy||!canBook||!acknowledged} onClick={book}>{busy?"Booking…":"Confirm & create shipment"}</button></>}
 <button disabled={busy} onClick={()=>{setReview(false);setAcknowledged(false);}}>Back to couriers</button></div>}

 </>}
 </>}
 </>}
 </>}
 </section>;
}
