let csrf: {token: string; headerName: string} | null = null;
export class ApiError extends Error { constructor(public status: number, message: string) {super(message);} }
export async function adminRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
 const method=options.method||"GET";
 if(method!=="GET" && !csrf){const r=await fetch("/api/auth/csrf",{cache:"no-store"});if(!r.ok)throw new Error("Cannot connect to admin service");csrf=await r.json();}
 const headers=new Headers(options.headers);
 if(csrf&&method!=="GET") headers.set(csrf.headerName,csrf.token);
 const response=await fetch(path,{...options,headers,cache:"no-store"});
 if(response.status===401||response.status===403)csrf=null;
 if(!response.ok){const body=await response.json().catch(()=>null);throw new ApiError(response.status,body?.message||`Request failed (${response.status}). Please try again.`);}
 if(path==="/api/auth/login"||path==="/api/auth/logout")csrf=null;
 return response.status===204?undefined as T:response.json();
}
