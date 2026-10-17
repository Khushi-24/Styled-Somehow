import OrderAccessView from "./view";
export default async function OrdersPage({searchParams}:{searchParams:Promise<{reference?:string|string[]}>}){
 const params=await searchParams;
 const reference=typeof params.reference==="string"&&/^[0-9a-f-]{36}$/i.test(params.reference)?params.reference:"";
 return <OrderAccessView initialReference={reference}/>;
}
