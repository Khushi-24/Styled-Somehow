export const fulfilmentLabels:Record<string,string>={UNFULFILLED:"Awaiting payment",PAID:"Order confirmed",PACKING:"Packing",PRINTING:"Packing",PACKED:"Packing",READY_TO_SHIP:"Ready to ship",COURIER_BOOKED:"Ready to ship",SHIPPED:"Shipped",IN_TRANSIT:"In transit",DELIVERED:"Delivered",DELIVERY_ISSUE:"Delivery issue",RETURNED_TO_ORIGIN:"Returned to sender"};
export const fulfilmentSteps=["PAID","PACKING","READY_TO_SHIP","SHIPPED","IN_TRANSIT","DELIVERED"];
export type Fulfilment={orderId:string;stage:string;test:boolean;onHold:boolean;deliveryMethod:string;courier:string|null;awb:string|null;courierStatus:string|null;trackingUrl:string|null;trackingCheckedAt:string|null;trackingFailed:boolean;timeline:{stage:string;occurredAt:string}[]};
export type AdminFulfilment={fulfilment:Fulfilment;allowedNext:string[];history:{id:string;stage:string;actor:string;note:string;occurredAt:string}[]};

export const displayStage=(stage:string)=>({PRINTING:"PACKING",PACKED:"PACKING",COURIER_BOOKED:"READY_TO_SHIP"}[stage]||stage);
export const fulfilmentDescriptions:Record<string,string>={PAID:"Great choice. We’ll take it from here.",PACKING:"Getting your tee ready for you.",READY_TO_SHIP:"All packed and waiting for pickup.",SHIPPED:"Your parcel is with the courier.",IN_TRANSIT:"Making its way to your doorstep.",DELIVERED:"Let the outfit plans begin."};

export function fulfilmentDescription(stage:string,method?:string){if(method==="PERSONAL"){if(displayStage(stage)==="READY_TO_SHIP")return "All packed and ready for personal delivery by Styled Somehow.";if(stage==="SHIPPED"||stage==="IN_TRANSIT")return "Your order is on its way with Styled Somehow for personal delivery.";}return fulfilmentDescriptions[displayStage(stage)];}
