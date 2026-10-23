import type {Product} from "./product";

export function productSlug(name:string):string {
 return name.normalize("NFKD").replace(/[\u0300-\u036f]/g,"").toLowerCase()
  .replace(/['’]/g,"").replace(/&/g," and ").replace(/[^a-z0-9]+/g,"-")
  .replace(/^-|-$/g,"").slice(0,100).replace(/-$/g,"");
}
export function renameProduct(product:Product,name:string,customSlug:boolean):Product {
 return {...product,name,...(!product.id&&!customSlug?{slug:productSlug(name)}:{})};
}
