import {test} from "node:test";
import assert from "node:assert/strict";
import {checkoutReference} from "../lib/checkout-recovery.ts";
test("a previous paid order never replaces the next nonempty cart, including after refresh",()=>{assert.equal(checkoutReference(null,"previous-confirmed-order",false),null);});
test("an empty cart can recover its confirmation and a pending checkout always wins",()=>{assert.equal(checkoutReference(null,"paid-order",true),"paid-order");assert.equal(checkoutReference("pending-order","paid-order",false),"pending-order");assert.equal(checkoutReference(null,null,true),null);});
