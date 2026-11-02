type Csrf={token:string;headerName:string};
let csrf:Csrf|null=null;
let csrfFlight:Promise<Csrf>|null=null;
let generation=0;
function clearCsrf(){csrf=null;csrfFlight=null;generation++;}
async function getCsrf():Promise<Csrf>{
 if(csrf)return csrf;
 if(!csrfFlight){
  const epoch=generation;
  const flight=fetch("/api/auth/csrf",{cache:"no-store"}).then(async r=>{if(!r.ok)throw new Error("Cannot connect to admin service");const value:Csrf=await r.json();if(epoch===generation)csrf=value;return value;});
  csrfFlight=flight;
  void flight.finally(()=>{if(csrfFlight===flight)csrfFlight=null;}).catch(()=>{});
 }
 return csrfFlight;
}
export class ApiError extends Error { status:number; constructor(status:number,message:string){super(message);this.status=status;} }
export async function adminRequest<T>(path:string,options:RequestInit={}):Promise<T>{
 const method=(options.method||"GET").toUpperCase();
 const headers=new Headers(options.headers);
 if(!["GET","HEAD","OPTIONS"].includes(method)){const token=await getCsrf();headers.set(token.headerName,token.token);}
 const response=await fetch(path,{...options,method,headers,cache:"no-store"});
 if(response.status===401||response.status===403)clearCsrf();
 if(!response.ok){const body=await response.json().catch(()=>null);const fallback=response.status===401?"Your admin session expired. Sign in through Products and retry.":response.status===403?"Access denied or the security token expired. Sign in through Products and retry.":`Request failed (${response.status}). Please try again.`;throw new ApiError(response.status,body?.message||body?.detail||fallback);}
 if(path==="/api/auth/login"||path==="/api/auth/logout")clearCsrf();
 return response.status===204?undefined as T:response.json();
}
