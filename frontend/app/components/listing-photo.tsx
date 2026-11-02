"use client";
import Image from "next/image";
import {useState} from "react";
import type {Product} from "../../lib/product";
import styles from "./listing-photo.module.css";

export default function ListingPhoto({product,sizes,unoptimized=false}:{product:Product;sizes:string;unoptimized?:boolean}){
 const [loadedPreview,setLoadedPreview]=useState("");
 const photos=product.media.filter(m=>m.kind==="image");
 const first=photos[0];
 const others=photos.filter(m=>m.url!==first?.url);
 const preview=others.find(m=>m.colour===first?.colour)||others[0];
 return <div className={`new-in__image ${styles.photo}`} data-preview-ready={!!preview&&loadedPreview===preview.url}>
  {first&&<Image src={first.url} alt={first.alt||product.name} fill sizes={sizes} unoptimized={unoptimized}/>}
  {preview&&<span className={styles.alternate} aria-hidden="true"><Image src={preview.url} alt="" fill sizes={sizes} loading="lazy" unoptimized={unoptimized} style={{objectFit:preview.contain?"contain":"cover"}} onLoad={()=>setLoadedPreview(preview.url)} onError={()=>setLoadedPreview("")}/></span>}
  {product.newIn&&<span className="new-in__badge">NEW</span>}
 </div>;
}
