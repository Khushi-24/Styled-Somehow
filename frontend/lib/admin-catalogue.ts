import type {Product,ShopCollection} from "./product";
export type CatalogueFilters={query:string;status:string;collection:string;sort:string};
const normalize=(text:string)=>text.normalize("NFKD").replace(/[\u0300-\u036f]/g,"").toLowerCase();
export function filterCatalogue(products:Product[],collections:ShopCollection[],filters:CatalogueFilters):Product[]{
 const names=new Map(collections.map(c=>[c.slug,c.name]));
 const terms=normalize(filters.query.trim()).split(/\s+/).filter(Boolean);
 return products.filter(p=>{
  if(filters.status!=="ALL"&&p.status!==filters.status)return false;
  if(filters.collection==="__none"&&p.collections.length)return false;
  if(filters.collection&&filters.collection!=="__none"&&!p.collections.includes(filters.collection))return false;
  const text=normalize([p.name,p.slug,String(p.id),`#${p.id}`,...p.colours,...p.collections.map(c=>names.get(c)||c)].join(" "));
  return terms.every(term=>text.includes(term));
 }).sort((a,b)=>{
  if(filters.sort==="name")return a.name.localeCompare(b.name,"en",{numeric:true})||(b.id||0)-(a.id||0);
  if(filters.sort==="price")return a.price-b.price||(b.id||0)-(a.id||0);
  if(filters.sort==="storefront")return a.sortOrder-b.sortOrder||(a.id||0)-(b.id||0);
  return (b.id||0)-(a.id||0);
 });
}
