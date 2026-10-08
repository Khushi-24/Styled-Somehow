export type CartItem={slug:string;colour:string;size:string;quantity:number;name?:string;price?:number};
export const CART_KEY="styled-somehow-cart";
export function readCart():CartItem[]{
 try{const data=JSON.parse(localStorage.getItem(CART_KEY)||"[]");if(!Array.isArray(data))return [];return data.filter(v=>v&&typeof v.slug==="string"&&["S","M","L","XL"].includes(v.size)&&Number.isInteger(v.quantity)&&v.quantity>0&&v.quantity<=1000).map(v=>({...v,colour:typeof v.colour==="string"?v.colour:""}));}catch{return [];}
}
export function writeCart(items:CartItem[]){localStorage.setItem(CART_KEY,JSON.stringify(items));window.dispatchEvent(new Event("cart-updated"));}
export const checkoutItems=(items:CartItem[])=>items.map(({slug,colour,size,quantity})=>({slug,colour,size,quantity}));
export type Quote={items:{slug:string;name:string;colour:string;size:string;quantity:number;unitPrice:number;image:string;availableQuantity:number}[];subtotal:number;shipping:number;total:number};
export type PendingOrder={id:string;status:string;createdAt:string;expiresAt:string;subtotal:number;shipping:number;total:number;name:string;email:string;phone:string;line1:string;line2:string;city:string;state:string;pinCode:string;items:{slug:string;name:string;colour:string;size:string;quantity:number;unitPrice:number;image:string}[]};
