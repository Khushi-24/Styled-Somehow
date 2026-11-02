/** A completed order never replaces a nonempty new cart. Pending checkout ownership is checked by the API. */
export function checkoutReference(pending:string|null,confirmed:string|null,cartEmpty:boolean):string|null{return pending||(cartEmpty?confirmed:null);}
