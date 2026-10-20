import CollectionPage from "../../components/collection-page";
export const dynamic="force-dynamic";
export const metadata={title:"Men’s Collection | styled_somehow"};
export default async function Page({searchParams}:{searchParams:Promise<{from?:string}>}){return <CollectionPage slug="men" from={(await searchParams).from}/>;}
