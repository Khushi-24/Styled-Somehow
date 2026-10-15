"use client";
import {useEffect,useState} from "react";
import {PendingOrder} from "../../../lib/cart";
import {adminRequest} from "../../../lib/admin-api";
import {money} from "../../../lib/product";
import styles from "./shipment.module.css";

type Booking={orderId:string;status:string;courier:string;quotedCost:number;providerOrderId?:number;providerShipmentId?:number;awb?:string;message?:string};
type State={bookingEnabled:boolean;booking:Booking|null};
type Preview={courierId:number;courier:string;quotedCost:number;customerShipping:number;quantity:number;weight:string;dimensions:string;pickupLocation:string};
export default function Shipment({order}:{order:PendingOrder}){
 const [state,setState]=useState<State|null>(null),[quote,setQuote]=useState<Preview|null>(null),[busy,setBusy]=useState(false),[error,setError]=useState("");
 const path=`/api/admin/orders/${encodeURIComponent(order.id)}/shipment`;
 const eligible=order.status==="PAID"&&order.paymentMode==="LIVE";
 useEffect(()=>{if(!eligible)return;let live=true;adminRequest<State>(path).then(data=>{if(live)setState(data);}).catch(e=>{if(live)setError(e instanceof Error?e.message:"Cannot load shipment.");});return()=>{live=false;};},[eligible,path]);
 async function refresh(){setBusy(true);setError("");try{setState(await adminRequest<State>(path));}catch(e){setError(e instanceof Error?e.message:"Cannot load shipment.");}finally{setBusy(false);}}
 async function preview(){setBusy(true);setError("");try{setQuote(await adminRequest<Preview>(`${path}/preview`,{method:"POST"}));}catch(e){setError(e instanceof Error?e.message:"Cannot check shipping.");}finally{setBusy(false);}}
 async function book(){if(!quote||busy)return;setBusy(true);setError("");try{const booking=await adminRequest<Booking>(path,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({expectedCost:quote.quotedCost,courierId:quote.courierId})});setState({bookingEnabled:true,booking});setQuote(null);}catch(e){setError(e instanceof Error?e.message:"Shipment request failed. Refresh its status before doing anything else.");setQuote(null);setState(null);}finally{setBusy(false);}}
 const booking=state?.booking;
 return <section className={styles.panel} aria-label="Shipment booking"><h3>Shipment · Delhivery</h3>
 {!eligible?<p className={styles.notice}>{order.paymentMode==="TEST"?"TEST ORDER — real shipment booking is blocked. No courier charges will be made.":"Only paid LIVE orders can be shipped. Unpaid, refunded and review orders are blocked."}</p>:<>
 {error&&<p role="alert" className={styles.error}>{error}</p>}
 {booking?<><p className={booking.status==="BOOKED"?styles.success:styles.notice} role="status"><strong>{booking.status.replaceAll("_"," ")}</strong> · {booking.message}</p><dl><dt>Courier</dt><dd>{booking.courier}</dd><dt>Quoted courier cost</dt><dd>{money(booking.quotedCost)}</dd>{booking.awb&&<><dt>Tracking / AWB</dt><dd>{booking.awb}</dd></>}{booking.providerOrderId&&<><dt>Shiprocket order</dt><dd>{booking.providerOrderId}</dd></>}{booking.providerShipmentId&&<><dt>Shipment ID</dt><dd>{booking.providerShipmentId}</dd></>}</dl><p>Pickup and label generation are managed in Shiprocket for now. For a pending or review status, check Shiprocket using order reference {order.id}. Do not create another shipment.</p><button disabled={busy} onClick={refresh}>Refresh shipment status</button></>:!state?<button disabled={busy} onClick={refresh}>{busy?"Loading…":"Load shipment status"}</button>:!state.bookingEnabled?<p className={styles.notice}>Booking is disabled while Shiprocket KYC and live payments are being prepared.</p>:quote?<div className={styles.confirm}><h4>Review before booking</h4><dl><dt>Courier</dt><dd>{quote.courier}</dd><dt>Pickup location</dt><dd>{quote.pickupLocation}</dd><dt>Parcel</dt><dd>{quote.quantity} T-shirt(s) · {quote.weight} · {quote.dimensions}</dd><dt>Courier quote</dt><dd><strong>{money(quote.quotedCost)}</strong></dd><dt>Customer paid for shipping</dt><dd>{money(quote.customerShipping)}</dd></dl><p>Confirming creates a real shipment and assigns an AWB. Shiprocket wallet charges may apply; the courier quote is separate from customer shipping. Pickup is not scheduled automatically.</p><button disabled={busy} onClick={book}>{busy?"Booking…":"Confirm & create shipment"}</button><button disabled={busy} onClick={()=>setQuote(null)}>Cancel</button></div>:<button disabled={busy} onClick={preview}>{busy?"Checking courier…":"Create shipment — review quote"}</button>}
 </>}
 </section>;
}
