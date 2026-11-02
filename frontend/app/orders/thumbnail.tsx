"use client";
import Image from "next/image";
import {useState} from "react";
import styles from "./orders.module.css";
export default function OrderThumbnail({src,name,large=false}:{src?:string|null;name:string;large?:boolean}){
 const [failed,setFailed]=useState("");
 const safe=!!src&&(src.startsWith("/images/")||src.startsWith("/api/media/"))&&!src.includes("\\");
 return <span className={`${styles.thumbnail}${large?` ${styles.thumbnailLarge}`:""}`}>{safe&&failed!==src?<Image src={src!} alt={name} fill sizes={large?"(max-width: 600px) 76px, 96px":"(max-width: 600px) 80px, 104px"} unoptimized={src!.startsWith("/api/")} onError={()=>setFailed(src!)}/>:<svg viewBox="0 0 48 48" role="img" aria-label={`Image unavailable for ${name}`}><path d="m16 7-10 7 6 9 5-3v21h14V20l5 3 6-9-10-7c-1 5-15 5-16 0Z"/><path d="M19 8c1 5 9 5 10 0"/></svg>}</span>;
}
