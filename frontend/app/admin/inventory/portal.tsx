"use client";
import Link from "next/link";
import {useEffect,useRef,useState} from "react";
import {adminRequest,ApiError} from "../../../lib/admin-api";
type Stock={id:number;version:number;colour:string;size:string;quantity:number;reserved:number;lowStockThreshold:number};
type Movement={id:number;colour:string;size:string;delta:number;resultingQuantity:number;reason:string;actor:string;occurredAt:string};

function failure(label:string,path:string,error:unknown){
 if(error instanceof ApiError){
  const help=error.status===401?"Your session expired. Sign in through Products.":error.status===403?"Access denied. Sign in with the owner account.":error.status===404?"This API is missing. Pull the latest code and restart the backend.":error.status>=500?"Check the backend terminal for the corresponding error.":error.message;
  return `${label} failed (${error.status}) at ${path}. ${help}`;
 }
 return `${label} failed at ${path}. ${error instanceof Error?error.message:"Cannot connect to the backend."}`;
}
async function loadInventory(){
 const [stock,movements]=await Promise.allSettled([adminRequest<Stock[]>("/api/admin/inventory"),adminRequest<Movement[]>("/api/admin/inventory/movements")]);
 return {stock,movements};
}
export default function InventoryPortal(){
 const [stocks,setStocks]=useState<Stock[]|null>(null),[history,setHistory]=useState<Movement[]|null>(null),[selected,setSelected]=useState<Stock|null>(null),[delta,setDelta]=useState(""),[reason,setReason]=useState(""),[error,setError]=useState(""),[notice,setNotice]=useState(""),[busy,setBusy]=useState(false),[loading,setLoading]=useState(true);
 const pending=useRef<{signature:string;key:string}|null>(null);
 function apply(result:Awaited<ReturnType<typeof loadInventory>>){
  const errors:string[]=[];
  if(result.stock.status==="fulfilled"&&Array.isArray(result.stock.value))setStocks(result.stock.value);
  else{setStocks(null);errors.push(failure("Stock","/api/admin/inventory",result.stock.status==="rejected"?result.stock.reason:new Error("Unexpected API response")));}
  if(result.movements.status==="fulfilled"&&Array.isArray(result.movements.value))setHistory(result.movements.value);
  else{setHistory(null);errors.push(failure("Stock history","/api/admin/inventory/movements",result.movements.status==="rejected"?result.movements.reason:new Error("Unexpected API response")));}
  setError(errors.join(" "));
 }
 async function refresh(){apply(await loadInventory());}
 useEffect(()=>{let live=true;loadInventory().then(result=>{if(live)apply(result);}).finally(()=>{if(live)setLoading(false);});return()=>{live=false;};},[]);
 useEffect(()=>{if(!notice)return;const timer=setTimeout(()=>setNotice(""),6000);return()=>clearTimeout(timer);},[notice]);
 async function submit(e:React.FormEvent<HTMLFormElement>){e.preventDefault();if(!selected)return;const signature=JSON.stringify([selected.id,selected.version,Number(delta),reason.trim()]);if(pending.current?.signature!==signature)pending.current={signature,key:crypto.randomUUID()};setBusy(true);setError("");setNotice("");try{await adminRequest(`/api/admin/inventory/${selected.id}/adjustments`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({version:selected.version,delta:Number(delta),reason:reason.trim(),requestId:pending.current.key})});pending.current=null;setSelected(null);setDelta("");setReason("");await refresh();setNotice("Stock adjustment saved across all designs using this colour and size.");}catch(e){setError(e instanceof Error?e.message:"Could not adjust stock");}finally{setBusy(false);}}
 return <main className="admin-page"><header className="admin-header"><Link href="/admin">← Products / sign in</Link><h1>Blank T-shirt inventory</h1><button disabled={busy} onClick={()=>refresh().catch(()=>setError("Refresh failed"))}>Refresh</button></header>{(error||notice)&&<div className={`admin-toast ${error?"admin-toast--error":"admin-toast--success"}`} role={error?"alert":"status"}><p>{error||notice}</p><button aria-label="Dismiss notification" onClick={()=>{setError("");setNotice("");}}>×</button></div>}
 <section className="admin-editor"><h2>Shared oversized blanks · {stocks?`${stocks.reduce((n,s)=>n+s.quantity-s.reserved,0)} available`:loading?"Loading…":"Stock unavailable"}</h2><p>Every design uses the same blank garment type. Stock is shared by colour and size. Low stock means 3 or fewer blanks.</p>{loading?<p>Loading…</p>:<div className="inventory-table-wrap"><table className="inventory-table"><thead><tr><th>Colour</th><th>Size</th><th>On hand</th><th>Reserved</th><th>Available</th><th>Status</th><th>Action</th></tr></thead><tbody>{stocks?.map(s=><tr key={s.id}><td>{s.colour}</td><td>{s.size}</td><td>{s.quantity}</td><td>{s.reserved}</td><td>{s.quantity-s.reserved}</td><td>{s.quantity-s.reserved===0?"Out of stock":s.quantity-s.reserved<=s.lowStockThreshold?"Low stock":"In stock"}</td><td><button disabled={busy} onClick={()=>{setSelected(s);setDelta("");setReason("");}}>Adjust</button></td></tr>)}</tbody></table>{stocks===null&&<p>Stock could not be loaded. See the error message for details.</p>}{stocks?.length===0&&<p>No stock records were returned. Check the backend migration log.</p>}</div>}</section>
 {selected&&<form className="admin-editor inventory-adjust" onSubmit={submit}><h2>Adjust {selected.colour} · {selected.size}</h2><p>Current quantity: {selected.quantity}. Reserved: {selected.reserved}. Enter a positive number for restock or a negative number for damage/removal.</p><fieldset disabled={busy}><label>Quantity change<input autoFocus type="number" required min={selected.reserved-selected.quantity} max={100000} step={1} value={delta} onChange={e=>setDelta(e.target.value)}/></label><label>Reason<textarea required maxLength={500} value={reason} onChange={e=>setReason(e.target.value)}/></label><p>Result: {selected.quantity+Number(delta)} blanks</p><div className="admin-actions"><button disabled={!Number(delta)||!reason.trim()}>Save adjustment</button><button type="button" onClick={()=>setSelected(null)}>Cancel</button></div></fieldset></form>}
 <section className="admin-editor inventory-history"><h2>Recent stock changes</h2><p>Latest 100 entries. Opening quantities are imported once; restarting does not reset stock.</p><div className="inventory-table-wrap"><table className="inventory-table"><thead><tr><th>When</th><th>Blank</th><th>Change</th><th>Balance</th><th>Reason</th><th>By</th></tr></thead><tbody>{history?.map(m=><tr key={m.id}><td>{new Date(m.occurredAt).toLocaleString()}</td><td>{m.colour} · {m.size}</td><td>{m.delta>0?"+":""}{m.delta}</td><td>{m.resultingQuantity}</td><td>{m.reason}</td><td>{m.actor}</td></tr>)}</tbody></table>{history===null&&<p>Stock history is unavailable. Successfully loaded stock remains usable.</p>}{history?.length===0&&<p>No stock changes recorded.</p>}</div></section></main>;
}
