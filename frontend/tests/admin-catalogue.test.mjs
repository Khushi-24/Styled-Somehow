import {test} from "node:test";
import assert from "node:assert/strict";
import {filterCatalogue} from "../lib/admin-catalogue.ts";
const products=[
 {id:1,name:"Cherry Zest",slug:"cherry-zest",price:599,status:"PUBLISHED",sortOrder:2,collections:["women"],colours:["White"]},
 {id:2,name:"Karna Sun Warrior",slug:"karna-sun-warrior",price:999,status:"DRAFT",sortOrder:1,collections:["yuga"],colours:["Black"]},
 {id:3,name:"Unassigned Tee",slug:"unassigned-tee",price:499,status:"ARCHIVED",sortOrder:0,collections:[],colours:["White"]},
];
const collections=[{slug:"women",name:"Women"},{slug:"yuga",name:"Yuga – The Legacy Collection"}];
const defaults={query:"",status:"ALL",collection:"",sort:"newest"};
test("catalogue search matches collection display names, colours and product IDs",()=>{
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,query:"legacy black"}).map(p=>p.id),[2]);
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,query:"#1"}).map(p=>p.id),[1]);
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,query:"  CHERRY  "}).map(p=>p.id),[1]);
});
test("status and collection filters combine, including unassigned products",()=>{
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,status:"PUBLISHED",collection:"yuga"}),[]);
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,status:"DRAFT",collection:"yuga"}).map(p=>p.id),[2]);
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,collection:"__none"}).map(p=>p.id),[3]);
});
test("catalogue sorting never changes the original product list",()=>{
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,sort:"price"}).map(p=>p.id),[3,1,2]);
 assert.deepEqual(filterCatalogue(products,collections,{...defaults,sort:"storefront"}).map(p=>p.id),[3,2,1]);
 assert.deepEqual(filterCatalogue(products,collections,defaults).map(p=>p.id),[3,2,1]);
 assert.deepEqual(products.map(p=>p.id),[1,2,3]);
});
