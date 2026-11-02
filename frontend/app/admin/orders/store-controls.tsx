"use client";
import {useEffect,useState} from "react";
import {adminRequest} from "../../../lib/admin-api";
export default function StoreControls(){
 const [paused,setPaused]=useState<boolean|null>(null),[busy,setBusy]=useState(false),[message,setMessage]=useState("");
 useEffect(()=>{adminRequest<{ordersPaused:boolean}>("/api/admin/settings").then(s=>setPaused(s.ordersPaused)).catch(()=>setMessage("Could not load store settings."));},[]);
 async function toggle(){setBusy(true);try{const s=await adminRequest<{ordersPaused:boolean}>("/api/admin/settings",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({ordersPaused:!paused})});setPaused(s.ordersPaused);setMessage(s.ordersPaused?"New orders paused. Existing payments, refunds and tracking continue.":"New orders enabled.");}catch(e){setMessage(e instanceof Error?e.message:"Could not save settings");}finally{setBusy(false);}}
 return <section className="ops-section"><h2>Store ordering</h2><p>{paused===null?"Loading…":paused?"New orders are paused":"Accepting new orders"}</p><button disabled={busy||paused===null} onClick={toggle}>{paused?"Resume orders":"Pause new orders"}</button>{message&&<p role="status">{message}</p>}</section>;
}
