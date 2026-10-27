import Link from "next/link";
import PolicyActions from "./policy-actions";
export default function StoreHeader({backHref="/",backLabel="Continue shopping",dark=false}:{backHref?:string;backLabel?:string;dark?:boolean}){
 return <nav className={`store-header${dark?" store-header--dark":""}`} aria-label="Store navigation"><Link className="store-header__back" href={backHref} aria-label={backLabel}><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m14 6-6 6 6 6M8 12h13"/></svg><span className="store-header__back-label">{backLabel}</span><span className="store-header__back-short" aria-hidden="true">Back</span></Link><Link className="brand-logo" href="/" aria-label="Styled Somehow home"><span>STYLED</span><span>SOMEHOW</span></Link><PolicyActions dark={dark}/></nav>;
}
