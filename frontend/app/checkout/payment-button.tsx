"use client";
import Script from "next/script";
import {useEffect,useState} from "react";
import {adminRequest} from "../../lib/admin-api";
import {PendingOrder} from "../../lib/cart";
type Result={razorpay_payment_id:string;razorpay_signature:string};
type Options={key:string;order_id:string;amount:number;currency:string;name:string;description:string;prefill:{name:string;email:string;contact:string};handler:(result:Result)=>void;modal:{ondismiss:()=>void}};
declare global{interface Window{Razorpay?:new(options:Options)=>{open:()=>void;on:(event:string,handler:()=>void)=>void}}}
export default function PaymentButton({order,onOrder,onBusy}:{order:PendingOrder;onOrder:(order:PendingOrder)=>void;onBusy:(busy:boolean)=>void}){
 const [enabled,setEnabled]=useState(false),[ready,setReady]=useState(false),[busy,setBusy]=useState(false),[message,setMessage]=useState("Loading payment availability…");
 useEffect(()=>{let live=true;adminRequest<{enabled:boolean}>("/api/payments/config").then(c=>{if(live){setEnabled(c.enabled);setMessage(c.enabled?"Test mode: no real money is charged.":"Test payment keys are not configured yet.");}}).catch(()=>{if(live)setMessage("Cannot load payment availability. Refresh this page.");});return()=>{live=false;};},[]);
 function finish(){setBusy(false);onBusy(false);}
 async function pay(){if(!window.Razorpay||busy)return;setBusy(true);onBusy(true);setMessage("");try{
  const start=await adminRequest<{keyId:string;gatewayOrderId:string;amount:number;currency:string}>(`/api/checkout/${order.id}/payment`,{method:"POST"});
  const popup=new window.Razorpay({key:start.keyId,order_id:start.gatewayOrderId,amount:start.amount,currency:start.currency,name:"Styled Somehow",description:"TEST checkout",prefill:{name:order.name,email:order.email,contact:order.phone},handler:result=>{void adminRequest<PendingOrder>(`/api/checkout/${order.id}/payment/verify`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({paymentId:result.razorpay_payment_id,signature:result.razorpay_signature})}).then(onOrder).catch(e=>setMessage(e instanceof Error?e.message:"Verification pending. Check payment status before retrying.")).finally(finish);},modal:{ondismiss:()=>{setMessage("Checkout closed. Check payment status before another attempt.");finish();}}});
  popup.on("payment.failed",()=>setMessage("Payment attempt failed. You can retry within the reservation time."));popup.open();
 }catch(e){setMessage(e instanceof Error?e.message:"Cannot open payment checkout");finish();}}
 async function check(){setBusy(true);onBusy(true);try{onOrder(await adminRequest<PendingOrder>(`/api/checkout/${order.id}/payment/check`,{method:"POST"}));setMessage("Payment status refreshed from Razorpay.");}catch(e){setMessage(e instanceof Error?e.message:"Cannot check payment");}finally{finish();}}
 return <div>{enabled&&<Script src="https://checkout.razorpay.com/v1/checkout.js" onReady={()=>setReady(true)} onError={()=>setMessage("Payment checkout could not load. Refresh and retry.")}/>}<p role="status">{message}</p>{enabled&&<><button className="shopping-primary" disabled={!ready||busy} onClick={pay}>{busy?"Checking payment…":"Pay in Razorpay TEST mode"}</button><button disabled={busy} onClick={check}>Check payment status</button></>}</div>;
}
