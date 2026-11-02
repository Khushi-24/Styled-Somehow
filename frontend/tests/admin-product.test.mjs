import {test} from "node:test";
import assert from "node:assert/strict";
import {productSlug,renameProduct} from "../lib/admin-product.ts";
import {uploadDimensions} from "../lib/product-image-upload.ts";

test("new product URLs follow name edits and handle punctuation without breaking URL validation",()=>{
 const p={name:"",slug:""};
 const first=renameProduct(p,"Cherry Zest Oversized T-Shirt",false);
 assert.equal(first.slug,"cherry-zest-oversized-t-shirt");
 assert.equal(renameProduct(first,"She’s Winning & Chilli Crush!",false).slug,"shes-winning-and-chilli-crush");
 assert.equal(renameProduct(first,"",false).slug,"");
 const long=productSlug("Cotton Tee ".repeat(30));
 assert.ok(long.length<=100);assert.match(long,/^[a-z0-9]+(-[a-z0-9]+)*$/);
 assert.equal(productSlug("  Café — Oversized  "),"cafe-oversized");
});
test("manual new-product URLs and all saved product URLs survive name changes",()=>{
 assert.equal(renameProduct({name:"Old",slug:"my-own-url"},"New Name",true).slug,"my-own-url");
 assert.equal(renameProduct({id:12,name:"Old",slug:"permanent-url"},"New Name",false).slug,"permanent-url");
});
test("upscaled and oversized-byte photos are prepared within server limits without stretching",()=>{
 for(const [w,h,bytes] of [[8192,12288,2_000_000],[4096,4096,1_000_000],[3000,4000,9*1024*1024]]){
  const size=uploadDimensions(w,h,bytes);
  assert.equal(size.resize,true);assert.ok(size.width*size.height<=12_000_000);
  assert.ok(size.width<=3200&&size.height<=3200);
  assert.ok(Math.abs(size.width/size.height-w/h)<.001);
 }
});
test("already valid photos keep their original resolution",()=>{
 assert.deepEqual(uploadDimensions(3000,4000,8*1024*1024),{width:3000,height:4000,resize:false});
 assert.deepEqual(uploadDimensions(1200,1600,500_000),{width:1200,height:1600,resize:false});
 assert.throws(()=>uploadDimensions(0,1600,100));
});
