"use client";
import {useEffect,useId,useState} from "react";
import {PendingOrder} from "../../../lib/cart";
import {adminRequest} from "../../../lib/admin-api";
import {money} from "../../../lib/product";
import ShippingDiagnostic from "./shipping-diagnostic";
import styles from "./shipment.module.css";

type Booking={orderId:string;status:string;courier:string;quotedCost:number;providerOrderId?:number;providerShipmentId?:number;awb?:string;message?:string};
type State={bookingEnabled:boolean;booking:Booking|null};
type Parcel={weightKg:number;lengthCm:number;breadthCm:number;heightCm:number};
type Option={courierId:number;courier:string;rupees:number;transitDays:number|null;rating:number|null};
type Comparison={options:Option[];customerShipping:number;quantity:number;pickupLocation:string;deliveryPin:string;parcel:Parcel};
const fields=[{key:"weightKg",label:"Packed weight (kg)",min:"0.001",max:"50",step:"0.001"},{key:"lengthCm",label:"Length (cm)",min:"0.1",max:"300",step:"0.01"},{key:"breadthCm",label:"Width (cm)",min:"0.1",max:"300",step:"0.01"},{key:"heightCm",label:"Height (cm)",min:"0.1",max:"300",step:"0.01"}] as const;
function parcelDefaults(quantity:number){return {weightKg:quantity<=2?"0.5":"",lengthCm:"27.94",breadthCm:"33.02",heightCm:String(Number((quantity*2.54).toFixed(2))) };}
export default function Shipment({order}:{order:PendingOrder}){
 const id=useId();
 const [state,setState]=useState<State|null>(null),[comparison,setComparison]=useState<Comparison|null>(null),[selectedId,setSelectedId]=useState<number|null>(null),[review,setReview]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState("");
 const quantity=order.items.reduce((total,item)=>total+item.quantity,0);
 const [parcel,setParcel]=useState(()=>parcelDefaults(quantity));
 const parcelKey=`styled-somehow-admin-parcel-v2-${quantity}`;
 useEffect(()=>{const timer=setTimeout(()=>{setParcel(parcelDefaults(quantity));try{const saved=JSON.parse(localStorage.getItem(parcelKey)||"null");if(!saved)return;const next=parcelDefaults(quantity);for(const field of fields){const value=Number(saved[field.key]);if(!Number.isFinite(value)||value<Number(field.min)||value>Number(field.max))return;next[field.key]=String(value);}setParcel(next);}catch{}},0);return()=>clearTimeout(timer);},[parcelKey,quantity]);
 const path=`/api/admin/orders/${encodeURIComponent(order.id)}/shipment`;
 const eligible=order.status==="PAID"&&(order.paymentMode==="LIVE"||order.paymentMode==="TEST");
 const canBook=state?.bookingEnabled&&order.paymentMode==="LIVE";
 useEffect(()=>{if(!eligible)return;let live=true;adminRequest<State>(path).then(data=>{if(live)setState(data);}).catch(e=>{if(live)setError(e instanceof Error?e.message:"Cannot load shipment.");});return()=>{live=false;};},[eligible,path]);
 async function refresh(){setBusy(true);setError("");try{setState(await adminRequest<State>(path));}catch(e){setError(e instanceof Error?e.message:"Cannot load shipment.");}finally{setBusy(false);}}
 async function compare(event:React.FormEvent){event.preventDefault();if(busy)return;setBusy(true);setError("");setComparison(null);setReview(false);try{const data=await adminRequest<Comparison>(`${path}/compare`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(Object.fromEntries(Object.entries(parcel).map(([key,value])=>[key,Number(value)])))});try{localStorage.setItem(parcelKey,JSON.stringify(data.parcel));}catch{}setComparison(data);setSelectedId(data.options[0]?.courierId??null);}catch(e){setError(e instanceof Error?e.message:"Cannot compare couriers.");}finally{setBusy(false);}}
 const selected=comparison?.options.find(option=>option.courierId===selectedId);
 async function book(){if(!selected||!comparison||busy||!review)return;setBusy(true);setError("");try{const booking=await adminRequest<Booking>(path,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({expectedCost:selected.rupees,courierId:selected.courierId,parcel:comparison.parcel})});setState({bookingEnabled:true,booking});setComparison(null);}catch(e){setError(e instanceof Error?e.message:"Shipment request failed. Refresh its status before doing anything else.");setComparison(null);setState(null);}finally{setReview(false);setBusy(false);}}
 const booking=state?.booking;
 return <section className={styles.panel} aria-label="Shipment booking"><h3>Shipment booking</h3>
 {!eligible?<p className={styles.notice}>{order.paymentMode==="TEST"?"TEST ORDER — real shipment booking is blocked. No courier charges will be made.":"Only paid LIVE orders can be shipped. Unpaid, refunded and review orders are blocked."}</p>:<>
 {error&&<p role="alert" className={styles.error}>{error}</p>}
 {booking?<><p className={booking.status==="BOOKED"?styles.success:styles.notice} role="status"><strong>{booking.status.replaceAll("_"," ")}</strong> · {booking.message}</p><dl><dt>Courier</dt><dd>{booking.courier}</dd><dt>Quoted courier cost</dt><dd>{money(booking.quotedCost)}</dd>{booking.awb&&<><dt>Tracking / AWB</dt><dd>{booking.awb}</dd></>}{booking.providerOrderId&&<><dt>Shiprocket order</dt><dd>{booking.providerOrderId}</dd></>}{booking.providerShipmentId&&<><dt>Shipment ID</dt><dd>{booking.providerShipmentId}</dd></>}</dl><p>Manage labels and pickup in Shiprocket. For a pending or review status, check Shiprocket using order reference {order.id}. Do not create another shipment.</p><button disabled={busy} onClick={refresh}>Refresh shipment status</button></>:!state?<button disabled={busy} onClick={refresh}>{busy?"Loading…":"Load shipment status"}</button>:<>
 {order.paymentMode==="TEST"&&<p className={styles.notice}>TEST ORDER — courier comparison only. Real shipment booking is blocked; no shipment is created.</p>}
 {!state.bookingEnabled&&<p className={styles.notice}>You can compare couriers. Real booking remains disabled until Shiprocket KYC and live-payment setup are complete.</p>}
 <p>Default pack: 11 × 13 inches, with 1 inch height per T-shirt. Measurements below are in centimetres and remain editable. After a successful check, this browser remembers them for orders with the same number of T-shirts. Confirm the packed parcel before comparing. Delivery PIN: <strong>{order.pinCode}</strong>. Customer paid for shipping: <strong>{money(order.shipping)}</strong>.</p>
 <form onSubmit={compare}><div className={styles.parcelFields}>{fields.map(field=><label key={field.key} htmlFor={`${id}-${field.key}`}>{field.label}<input id={`${id}-${field.key}`} type="number" inputMode="decimal" min={field.min} max={field.max} step={field.step} required disabled={busy} value={parcel[field.key]} onChange={event=>{setParcel({...parcel,[field.key]:event.target.value});setComparison(null);setReview(false);}}/></label>)}</div><button disabled={busy} type="submit">{busy?"Please wait…":comparison?"Refresh courier prices":"Compare couriers"}</button></form>
 <ShippingDiagnostic path={path} parcel={parcel} disabled={busy}/>
 {comparison&&<><p>Pickup: <strong>{comparison.pickupLocation}</strong> · {comparison.quantity} T-shirt(s). Sorted by lowest API quote; amounts rounded up to the next rupee. Final Shiprocket charges may differ.</p>
 {comparison.options.length===0?<p className={styles.notice}>No supported courier options were returned for this parcel. Check this order in Shiprocket.</p>:<><div className={styles.tableWrap}><table className={styles.table}><caption>Courier options for this order</caption><thead><tr><th scope="col">Select courier</th><th scope="col">API quote</th><th scope="col">Estimated delivery</th><th scope="col">Rating</th></tr></thead><tbody>{comparison.options.map((option,index)=><tr key={option.courierId} className={selectedId===option.courierId?styles.selected:""}><td><label><input type="radio" name={`${id}-courier`} checked={selectedId===option.courierId} disabled={busy} onChange={()=>{setSelectedId(option.courierId);setReview(false);}}/><span>{option.courier}{index===0&&<small className={styles.badge}>Lowest quote</small>}</span></label></td><td><strong>{money(option.rupees)}</strong>{option.rupees>110&&<small className={styles.overLimit}>Above ₹110 checkout limit</small>}</td><td>{option.transitDays?`${option.transitDays} ${option.transitDays===1?"day":"days"} after dispatch`:"Not provided"}</td><td>{option.rating!=null?`${option.rating.toFixed(1)} / 5`:"Not provided"}</td></tr>)}</tbody></table></div>
 {selected&&!review&&<button disabled={busy||!canBook} onClick={()=>setReview(true)}>Review selected courier</button>}
 {selected&&review&&<div className={styles.confirm}><h4>Confirm real shipment</h4><p><strong>{selected.courier} · {money(selected.rupees)}</strong></p><p>Packed parcel: {comparison.parcel.weightKg} kg · {comparison.parcel.lengthCm} × {comparison.parcel.breadthCm} × {comparison.parcel.heightCm} cm.</p><p>Customer shipping payment remains {money(comparison.customerShipping)}. Any additional courier cost is paid by Styled Somehow. {selected.rupees>110?"This selection exceeds your ₹110 checkout limit. ":""}Confirming creates a real shipment and assigns an AWB. Shiprocket wallet charges may apply. Pickup is not scheduled automatically.</p><button disabled={busy||!canBook} onClick={book}>{busy?"Booking…":"Confirm & create shipment"}</button><button disabled={busy} onClick={()=>setReview(false)}>Cancel</button></div>}
 </>}
 </>}
 </>}
 </>}
 </section>;
}
