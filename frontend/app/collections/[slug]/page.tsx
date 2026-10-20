import CollectionPage from "../../components/collection-page";
import {getCollections} from "../../../lib/catalog";
export const dynamic="force-dynamic";
export async function generateMetadata({params}:{params:Promise<{slug:string}>}){const {slug}=await params;const c=(await getCollections()).find(c=>c.slug===slug);return {title:c?`${c.name} | styled_somehow`:"Collection | styled_somehow"};}
export default async function Page({params,searchParams}:{params:Promise<{slug:string}>;searchParams:Promise<{from?:string}>}){return <CollectionPage slug={(await params).slug} from={(await searchParams).from}/>;}
