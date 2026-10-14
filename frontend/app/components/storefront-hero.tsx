"use client";

import {getImageProps} from "next/image";
import Link from "next/link";
import PolicyActions from "./policy-actions";
import { useEffect, useState } from "react";

const slides = [
  { image: "/images/red-streetwear-hero-v3.webp", mobileImage:"/images/red-streetwear-hero-mobile-v3.webp", alt: "Styled Somehow red streetwear campaign: Style you don’t have to think about. Comfort you never want to take off. Shop now.", href: "/#new-in", position: "center" },
  { image: "/images/the-lineup.webp", mobileImage:"/images/the-lineup-mobile.webp", alt: "Styled Somehow graphic tees and everyday basics lineup", href: "/#new-in", position: "center" },
] as const;

const collectionLinks = [
  ["New Drop", "/collections/new-drop"], ["Graphic Tees", "/collections/graphic-tees"],
  ["Plain Tees", "/collections/plain-tees"], ["Joggers", "/collections/joggers"],
  ["Best Sellers", "/collections/best-sellers"],
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

export default function StorefrontHero() {
  function rememberHome() {
    window.history.replaceState(window.history.state, "", `${window.location.pathname}${window.location.search}`);
  }
  const [activeSlide, setActiveSlide] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const timer = window.setInterval(() => setActiveSlide((current) => (current + 1) % slides.length), 6500);
    return () => window.clearInterval(timer);
  }, []);

  const moveSlide = (direction: number) => setActiveSlide((current) => (current + direction + slides.length) % slides.length);

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
          <Link className="nav-link" href="/collections/men" onClick={rememberHome}>MEN</Link>
          <Link className="nav-link" href="/collections/women" onClick={rememberHome}>WOMEN</Link>
          <Link className="nav-link" href="/collections/couple-tshirts">COUPLE T-SHIRTS</Link>
        </nav>
        <div className="header-actions"><PolicyActions dark={activeSlide===0||menuOpen}/>          <button className="icon-button menu-button" type="button" aria-label={menuOpen ? "Close menu" : "Open menu"} aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}><Icon name={menuOpen ? "close" : "menu"} /></button>
        </div>
      </header>

      <div className={`mobile-panel ${menuOpen ? "is-open" : ""}`} aria-hidden={!menuOpen}>
        <p className="mobile-menu-label">SHOP</p>
        <Link href="/collections/new-drop" onClick={() => setMenuOpen(false)}>New Drop</Link>
        <Link href="/collections/graphic-tees" onClick={() => setMenuOpen(false)}>Graphic Tees</Link>
        <Link href="/collections/plain-tees" onClick={() => setMenuOpen(false)}>Plain Tees</Link>
        <Link href="/collections/joggers" onClick={() => setMenuOpen(false)}>Joggers</Link>
        <Link href="/collections/men" onClick={() => { rememberHome(); setMenuOpen(false); }}>Men</Link>
        <Link href="/collections/women" onClick={() => { rememberHome(); setMenuOpen(false); }}>Women</Link>
        <Link href="/collections/couple-tshirts" onClick={() => setMenuOpen(false)}>Couple T-shirts</Link>
        <div className="mobile-secondary-links"><Link href="/orders">View your order</Link><button type="button">Search</button></div>
      </div>

      <div className="slides">
        {slides.map((slide, index) => {
          const desktop=index===0?{src:slide.image,alt:slide.alt,width:1672,height:941}:getImageProps({src:slide.image,alt:slide.alt,width:1672,height:941,quality:index===0?95:75,sizes:"100vw"}).props;
          const mobile=index===0?{srcSet:slide.mobileImage,sizes:"100vw"}:getImageProps({src:slide.mobileImage,alt:slide.alt,width:index===0?1174:1122,height:index===0?1340:1402,quality:index===0?95:75,sizes:"100vw"}).props;
          return <Link key={slide.image} href={slide.href} className={`hero-slide ${index === activeSlide ? "is-active" : ""}`} aria-hidden={index !== activeSlide} tabIndex={index === activeSlide ? 0 : -1}>
            <picture><source media="(max-width: 620px)" srcSet={mobile.srcSet} sizes={mobile.sizes}/>{/* Art-directed sources share one responsive image element. */}
            <img {...desktop} alt={slide.alt} loading={index===0?"eager":"lazy"} fetchPriority={index===0?"high":"auto"} style={{objectPosition:slide.position}}/></picture>
            {index === 1 && <span className="lineup-cta">EXPLORE THE LINEUP <span aria-hidden="true">↗</span></span>}
          </Link>;
        })}
      </div>

      <div className="carousel-controls">
        <button type="button" onClick={() => moveSlide(-1)} aria-label="Previous slide">←</button>
        <div className="carousel-dots" role="group" aria-label="Choose a slide">
          {slides.map((slide, index) => <button key={slide.image} className={index === activeSlide ? "is-active" : ""} type="button" aria-label={`Show slide ${index + 1}`} aria-current={index === activeSlide ? "true" : undefined} onClick={() => setActiveSlide(index)} />)}
        </div>
        <button type="button" onClick={() => moveSlide(1)} aria-label="Next slide">→</button>
      </div>
    </section>
  );
}
