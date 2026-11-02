"use client";

import Link from "next/link";
import {getImageProps} from "next/image";
import type {ShopCollection} from "../../lib/product";
import PolicyActions from "./policy-actions";
import { useEffect, useState } from "react";

const slides = [
  { image: "/images/red-streetwear-hero.webp", mobileImage:"/images/red-streetwear-hero-mobile-refined.webp", alt: "Styled Somehow red streetwear campaign: Style you don’t have to think about. Comfort you never want to take off. Shop now.", href: "/#pick-your-style", position: "center" },
  { image: "/images/the-lineup-approved.webp", mobileImage:"/images/the-lineup-mobile-approved.webp", alt: "Styled Somehow graphic tees and everyday basics lineup", href: "/#pick-your-style", position: "center" },
] as const;


function Icon({ name }: { name: "home" | "search" | "user" | "bag" | "menu" | "close" }) {
  const paths = {
    home: <path d="M3 10 12 3l9 7M5 9v12h14V9M9 21v-7h6v7"/>,
    search: <><circle cx="11" cy="11" r="6.5"/><path d="m16 16 4.25 4.25"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4.75 21c.7-4.25 3.12-6.5 7.25-6.5s6.55 2.25 7.25 6.5"/></>,
    bag: <><path d="M4.5 8.5h15l-1 12h-13l-1-12Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></>,
    menu: <path d="M3 7h18M3 12h18M3 17h18"/>,
    close: <path d="m5 5 14 14M19 5 5 19"/>,
  };
  return <svg aria-hidden="true" viewBox="0 0 24 24">{paths[name]}</svg>;
}

export default function StorefrontHero({collections}:{collections:ShopCollection[]}) {
  const collectionLinks = [["New In","/#new-in"],...collections.map(c=>[c.name,`/collections/${c.slug}`])];
  function rememberHome() {
    window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}`);
  }
  function shopNow(event:React.MouseEvent<HTMLAnchorElement>){
    if(event.button!==0||event.metaKey||event.ctrlKey||event.shiftKey||event.altKey)return;
    const target=document.getElementById("pick-your-style")||document.getElementById("new-in");
    if(!target)return;
    event.preventDefault();
    const hash=`#${target.id}`;
    if(window.location.hash!==hash)window.history.pushState(window.history.state,"",hash);
    target.scrollIntoView({behavior:window.matchMedia("(prefers-reduced-motion: reduce)").matches?"instant":"smooth",block:"start"});
  }
  const [activeSlide, setActiveSlide] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (menuOpen || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const timer = window.setInterval(() => setActiveSlide((current) => (current + 1) % slides.length), 2500);
    return () => window.clearInterval(timer);
  }, [menuOpen]);

  useEffect(() => {
    if (!menuOpen) return;
    const toggle=document.querySelector<HTMLButtonElement>(".menu-button");
    const panel=document.querySelector<HTMLDivElement>(".mobile-panel");
    panel?.querySelector<HTMLAnchorElement>("a")?.focus();
    const previous=document.body.style.overflow;document.body.style.overflow="hidden";
    const escape = (event: KeyboardEvent) => {
      if (event.key === "Escape") { setMenuOpen(false); toggle?.focus(); }
      if(event.key==="Tab"&&panel){const controls=[toggle,...panel.querySelectorAll<HTMLElement>("a,button")].filter((node):node is HTMLElement=>!!node);const index=controls.indexOf(document.activeElement as HTMLElement);if(event.shiftKey&&index<=0){event.preventDefault();controls.at(-1)?.focus();}else if(!event.shiftKey&&(index===controls.length-1||index<0)){event.preventDefault();controls[0]?.focus();}}
    };
    document.addEventListener("keydown", escape);
    return () => {document.removeEventListener("keydown", escape);document.body.style.overflow=previous;};
  }, [menuOpen]);

  return (
    <section className="storefront storefront--campaign" aria-label="Styled Somehow featured collections">
      <header className={`site-header ${activeSlide === 0 ? "header-on-dark" : "header-on-light"} ${menuOpen ? "menu-is-open" : ""}`}>
        <Link className="brand-logo" href="/" aria-label="Styled Somehow home">
          <span>STYLED</span>
          <span>SOMEHOW</span>
        </Link>
        <nav className="desktop-nav" aria-label="Main navigation">
          <div className="nav-dropdown">
            <button type="button" className="nav-link">COLLECTIONS <span aria-hidden="true">⌄</span></button>
            <div className="dropdown-menu">{collectionLinks.map(([label, href]) => <Link key={label} href={href}>{label}</Link>)}</div>
          </div>
          {collections.some(c=>c.slug==="men")&&<Link className="nav-link" href="/collections/men" onClick={rememberHome}>MEN</Link>}
          {collections.some(c=>c.slug==="women")&&<Link className="nav-link" href="/collections/women" onClick={rememberHome}>WOMEN</Link>}

        </nav>
        <div className="header-actions"><PolicyActions dark={activeSlide===0||menuOpen}/>          <button className="icon-button menu-button" type="button" aria-label={menuOpen ? "Close menu" : "Open menu"} aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}><Icon name={menuOpen ? "close" : "menu"} /></button>
        </div>
      </header>

      <div className={`mobile-panel ${menuOpen ? "is-open" : ""}`} aria-hidden={!menuOpen} role="dialog" aria-modal={menuOpen?true:undefined} aria-label="Shop menu">
        <p className="mobile-menu-label">SHOP</p>
        <Link href="/#new-in" onClick={() => setMenuOpen(false)}>New In</Link>
        {collections.map(c=><Link key={c.slug} href={`/collections/${c.slug}`} onClick={()=>{rememberHome();setMenuOpen(false);}}>{c.name}</Link>)}

        <div className="mobile-secondary-links"><Link href="/orders">My orders</Link></div>
      </div>

      <div className="slides">
        {slides.map((slide, index) => {
          const {props:desktop}=getImageProps({src:slide.image,alt:slide.alt,width:index===0?1672:2048,height:index===0?941:1024,sizes:"100vw",quality:95});
          const {props:mobile}=getImageProps({src:slide.mobileImage,alt:slide.alt,width:1122,height:1402,sizes:"100vw",quality:95});
          return <Link key={slide.image} href={slide.href} onClick={shopNow} aria-label="Shop now — explore Pick Your Energy" className={`hero-slide ${index === activeSlide ? "is-active" : ""}`} aria-hidden={index !== activeSlide} tabIndex={index === activeSlide ? 0 : -1}>
            <picture><source media="(max-width: 620px)" srcSet={mobile.srcSet} sizes={mobile.sizes}/>{/* Art-directed sources share one responsive image element. */}
            <img {...desktop} alt={slide.alt} loading={index===0?"eager":"lazy"} fetchPriority={index===0?"high":"auto"} style={{objectPosition:slide.position}}/></picture>
            <span className="hero-shop-now">SHOP NOW <span aria-hidden="true">→</span></span>
          </Link>;
        })}
      </div>


    </section>
  );
}
