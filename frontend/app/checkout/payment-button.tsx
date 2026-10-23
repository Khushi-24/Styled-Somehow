"use client";
import Script from "next/script";
import {useEffect,useRef,useState} from "react";
import {adminRequest} from "../../lib/admin-api";
import {PendingOrder} from "../../lib/cart";
type Result={razorpay_payment_id:string;razorpay_signature:string};
type Options={key:string;order_id:string;amount:number;currency:string;name:string;description:string;prefill:{name:string;email:string;contact:string};handler:(result:Result)=>void;modal:{ondismiss:()=>void}};
declare global{interface Window{Razorpay?:new(options:Options)=>{open:()=>void;on:(event:string,handler:()=>void)=>void}}}
export default function PaymentButton({order,prepareOrder,onOrder,onBusy,disabled=false,label="Pay securely"}:{order:PendingOrder|null;prepareOrder?:()=>Promise<PendingOrder|null>;onOrder:(order:PendingOrder)=>void;onBusy:(busy:boolean)=>void;disabled?:boolean;label?:string}){
 const [enabled,setEnabled]=useState(false),[ready,setReady]=useState(false),[busy,setBusy]=useState(false),[message,setMessage]=useState("Loading payment options…");
 const locked=useRef(false);
 useEffect(()=>{let live=true;adminRequest<{enabled:boolean}>("/api/payments/config").then(c=>{if(live){setEnabled(c.enabled);setMessage(c.enabled?"":"Payments are temporarily unavailable. Please try again later.");}}).catch(()=>{if(live)setMessage("Cannot load payment options. Please refresh this page.");});return()=>{live=false;};},[]);
 function finish(){locked.current=false;setBusy(false);onBusy(false);}
 async function pay(){if(!window.Razorpay||!enabled||disabled||locked.current)return;locked.current=true;setBusy(true);onBusy(true);setMessage("");try{
  const payable=order??await prepareOrder?.();if(!payable){finish();return;}
  const start=await adminRequest<{keyId:string;gatewayOrderId:string;amount:number;currency:string;mode:string}>(`/api/checkout/${payable.id}/payment`,{method:"POST"});
  const popup=new window.Razorpay({key:start.keyId,order_id:start.gatewayOrderId,amount:start.amount,currency:start.currency,name:"Styled Somehow",description:start.mode==="TEST"?"TEST checkout":"Styled Somehow order",prefill:{name:payable.name,email:payable.email,contact:payable.phone},handler:result=>{void adminRequest<PendingOrder>(`/api/checkout/${payable.id}/payment/verify`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({paymentId:result.razorpay_payment_id,signature:result.razorpay_signature})}).then(onOrder).catch(e=>setMessage(e instanceof Error?e.message:"We’re verifying your payment. Check its status before retrying.")).finally(finish);},modal:{ondismiss:()=>{setMessage("Payment window closed. Check payment status before another attempt.");finish();}}});
  popup.on("payment.failed",()=>setMessage("Payment attempt failed. Close the payment window to check status or retry."));popup.open();
 }catch(e){setMessage(e instanceof Error?e.message:"Cannot open payment. Please retry.");finish();}}
 async function check(){if(!order||locked.current)return;locked.current=true;setBusy(true);onBusy(true);try{onOrder(await adminRequest<PendingOrder>(`/api/checkout/${order.id}/payment/check`,{method:"POST"}));setMessage("Payment status refreshed.");}catch(e){setMessage(e instanceof Error?e.message:"Cannot check payment status.");}finally{finish();}}
 return <div>{enabled&&<Script src="https://checkout.razorpay.com/v1/checkout.js" onReady={()=>setReady(true)} onError={()=>setMessage("Payment checkout could not load. Refresh and retry.")}/>}{message&&<p role="status">{message}</p>}{enabled&&<><button type="button" className="shopping-primary" disabled={!ready||busy||disabled} onClick={pay}>{busy?"Preparing payment…":label}</button>{order&&<button type="button" disabled={busy} onClick={check}>Check payment status</button>}</>}</div>;
}
