import {test} from "node:test";
import assert from "node:assert/strict";
const ok=(body)=>new Response(JSON.stringify(body),{headers:{"Content-Type":"application/json"}});

test("parallel admin writes share one CSRF request",async()=>{
 const {adminRequest}=await import("../lib/admin-api.ts?parallel");
 const original=globalThis.fetch;let tokenRequests=0;const writes=[];
 globalThis.fetch=async(path,options)=>{
  if(path==="/api/auth/csrf"){tokenRequests++;await new Promise(r=>setTimeout(r,10));return ok({token:"shared-token",headerName:"X-CSRF-TOKEN"});}
  writes.push({path,token:options.headers.get("X-CSRF-TOKEN")});return ok({saved:true});
 };
 try{await Promise.all([adminRequest("/api/admin/settings",{method:"POST"}),adminRequest("/api/admin/mail/job/retry",{method:"POST"})]);assert.equal(tokenRequests,1);assert.equal(writes.length,2);assert.ok(writes.every(r=>r.token==="shared-token"));}finally{globalThis.fetch=original;}
});
test("a rejected admin write is never replayed and the next explicit request gets a fresh token",async()=>{
 const {adminRequest,ApiError}=await import("../lib/admin-api.ts?denied");
 const original=globalThis.fetch;let tokenRequests=0,writes=0;
 globalThis.fetch=async(path)=>{
  if(path==="/api/auth/csrf")return ok({token:`token-${++tokenRequests}`,headerName:"X-CSRF-TOKEN"});
  writes++;return writes===1?new Response("{}",{status:403}):ok({saved:true});
 };
 try{await assert.rejects(()=>adminRequest("/api/admin/inventory/1/adjustments",{method:"POST"}),e=>e instanceof ApiError&&e.status===403);assert.equal(writes,1);await adminRequest("/api/admin/inventory/1/adjustments",{method:"POST"});assert.equal(writes,2);assert.equal(tokenRequests,2);}finally{globalThis.fetch=original;}
});
test("successful sign-in clears the pre-login CSRF token before uploads",async()=>{
 const {adminRequest}=await import("../lib/admin-api.ts?login");
 const original=globalThis.fetch;let tokenRequests=0,uploadToken;
 globalThis.fetch=async(path,options)=>{
  if(path==="/api/auth/csrf")return ok({token:`token-${++tokenRequests}`,headerName:"X-CSRF-TOKEN"});
  if(path==="/api/auth/login")return new Response(null,{status:204});
  uploadToken=options.headers.get("X-CSRF-TOKEN");return ok({url:"/api/media/fixture.png"});
 };
 try{await adminRequest("/api/auth/login",{method:"POST"});await adminRequest("/api/admin/media",{method:"POST",body:new FormData()});assert.equal(tokenRequests,2);assert.equal(uploadToken,"token-2");}finally{globalThis.fetch=original;}
});
