"use client";
import {useState} from "react";
import {adminRequest} from "../../../lib/admin-api";
import {PendingOrder} from "../../../lib/cart";
import {money} from "../../../lib/product";
export default function Refund({order,onRefunded}:{order:PendingOrder;onRefunded:(status:string)=>void}){
 const [open,setOpen]=useState(false),[reason,setReason]=useState(""),[returned,setReturned]=useState(false),[approved,setApproved]=useState(false),[busy,setBusy]=useState(false),[message,setMessage]=useState("");
 if(order.status!=="PAID")return null;
 async function submit(e:React.FormEvent){e.preventDefault();setBusy(true);try{const result=await adminRequest<{status:string}>(`/api/admin/orders/${order.id}/refund`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({reason,returnReceived:returned})});setMessage("Full refund queued. Refresh orders to see progress. Do not issue another refund manually.");setOpen(false);onRefunded(result.status);}catch(e){setMessage(e instanceof Error?e.message:"Could not queue refund");}finally{setBusy(false);}}
 return <section className="ops-section"><h3>Cancellation / approved return</h3><p>Full refund: {money(order.total)}. No blank stock is restored automatically. Inspect stock and record any justified adjustment separately. Partial refunds and exchanges require a reviewed manual process.</p><button disabled={busy} onClick={()=>setOpen(v=>!v)}>Review full refund</button>{open&&<form onSubmit={submit}><label>Reason<textarea required maxLength={400} value={reason} onChange={e=>setReason(e.target.value)}/></label><label><input type="checkbox" checked={returned} onChange={e=>setReturned(e.target.checked)}/>Returned items received and inspected (required after delivery)</label><label><input type="checkbox" required checked={approved} onChange={e=>setApproved(e.target.checked)}/>I approve a full {order.paymentMode} refund of {money(order.total)} and have resolved any courier booking.</label><button disabled={busy||!approved}>Queue full refund</button></form>}{message&&<p role="status">{message}</p>}</section>;
}
