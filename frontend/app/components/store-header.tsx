import Link from "next/link";
import PolicyActions from "./policy-actions";
export default function StoreHeader({backHref="/",backLabel="Continue shopping",dark=false}:{backHref?:string;backLabel?:string;dark?:boolean}){
 return <nav className={`store-header${dark?" store-header--dark":""}`} aria-label="Store navigation"><Link className="store-header__back" href={backHref}>← {backLabel}</Link><Link className="brand-logo" href="/" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><PolicyActions dark={dark}/></nav>;
}
