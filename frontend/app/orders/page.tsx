import type {Metadata} from "next";
import OrderAccessView from "./view";
export const metadata:Metadata={title:"Your order | Styled Somehow",robots:{index:false,follow:false},referrer:"no-referrer"};
export default async function OrdersPage({searchParams}:{searchParams:Promise<{reference?:string|string[]}>}){
 const params=await searchParams;
 const reference=typeof params.reference==="string"&&/^[0-9a-f-]{36}$/i.test(params.reference)?params.reference:"";
 return <OrderAccessView initialReference={reference}/>;
}
