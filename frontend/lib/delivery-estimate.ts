// Customer estimates include two days for preparation; courier transit stays unchanged in admin.
export function deliveryEstimate(days:number|null|undefined):string|null{
 return typeof days==="number"&&Number.isInteger(days)&&days>0&&days<=90?`Estimated delivery in ${days+2} days`:null;
}
