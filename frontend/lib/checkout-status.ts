// Unresolved payment/review states must still resume rather than silently create another checkout.
export function finishedCheckout(status:string):boolean{
 return ["PAID","CANCELLED","EXPIRED","REFUNDED"].includes(status);
}
