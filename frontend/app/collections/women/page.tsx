import CollectionPage from "../../components/collection-page";
export const dynamic="force-dynamic";
export const metadata={title:"Women’s Collection | styled_somehow"};
export default async function Page({searchParams}:{searchParams:Promise<{from?:string}>}){return <CollectionPage slug="women" from={(await searchParams).from}/>;}
