export const costLabels={OUTER_PACKAGING:"Outer packaging",INNER_PACKAGING:"Inner packaging",THANK_YOU_CARD:"Thank-you card",CLOTH_TAG:"Cloth tag",FREEBIES:"Freebies",SHIPPING:"Shipping / personal delivery",DTF_PRINT:"DTF printing",PAYMENT_FEE:"Payment fees",EXTRA:"Extra costs"};
export type CostKey=keyof typeof costLabels;
export type CostEntry={key:CostKey;amount:number|null;included:boolean};
export type CostProfile={version:number;items:CostEntry[];customerShipping:number;allocateMonthly:boolean};
export type MonthlyBudget={version:number;electricity:number;website:number;ai:number;other:number;expectedUnits:number};
export type CostEstimate={complete:boolean;missing:string[];revenue:number;directCost:number;overhead:number;totalCost:number;profit:number|null;margin:number|null};
export type CostRow={productId:number;name:string;slug:string;status:string;image:string|null;sellingPrice:number;profile:CostProfile;estimate:CostEstimate};
export type CostDashboard={month:string;fixedTshirtCost:number;budget:MonthlyBudget;products:CostRow[]};
const paise=(n:number)=>Math.round(n*100);
export function estimateCost(price:number,fixedCost:number,profile:CostProfile,budget:MonthlyBudget):CostEstimate{
 const missing:string[]=[];let direct=paise(fixedCost);
 for(const item of profile.items)if(item.included){if(item.amount===null||!Number.isFinite(item.amount)||item.amount<0)missing.push(item.key);else direct+=paise(item.amount);}
 let overhead=0;
 if(profile.allocateMonthly){if(budget.expectedUnits<=0)missing.push("EXPECTED_MONTHLY_UNITS");else overhead=Math.round((paise(budget.electricity)+paise(budget.website)+paise(budget.ai)+paise(budget.other))/budget.expectedUnits);}
 const revenue=paise(price)+paise(profile.customerShipping),total=direct+overhead,profit=missing.length?null:revenue-total;
 return {complete:!missing.length,missing,revenue:revenue/100,directCost:direct/100,overhead:overhead/100,totalCost:total/100,profit:profit===null?null:profit/100,margin:profit===null?null:Math.sign(profit)*Math.round(Math.abs(profit)*10000/revenue)/100};
}
export const monthlyTotal=(budget:MonthlyBudget)=>(paise(budget.electricity)+paise(budget.website)+paise(budget.ai)+paise(budget.other))/100;
export const costMoney=(n:number)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR",minimumFractionDigits:2,maximumFractionDigits:2}).format(n);
export function costingMonth(){const parts=new Intl.DateTimeFormat("en-CA",{timeZone:"Asia/Kolkata",year:"numeric",month:"2-digit"}).formatToParts(new Date());return `${parts.find(p=>p.type==="year")?.value}-${parts.find(p=>p.type==="month")?.value}`;}
