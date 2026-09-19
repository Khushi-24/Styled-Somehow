"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";

const slides = [
  { image: "/images/feel-the-vibe.webp", alt: "Styled Somehow Feel the Vibe campaign", href: "/collections/new-drop", position: "center" },
  { image: "/images/the-lineup.webp", alt: "Styled Somehow graphic tees and everyday basics lineup", href: "/collections/all", position: "center" },
] as const;

const collectionLinks = [
  ["New Drop", "/collections/new-drop"], ["Graphic Tees", "/collections/graphic-tees"],
  ["Plain Tees", "/collections/plain-tees"], ["Joggers", "/collections/joggers"],
  ["Best Sellers", "/collections/best-sellers"],
] as const;

function Icon({ name }: { name: "search" | "user" | "bag" | "menu" | "close" }) {
  const paths = {
    search: <><circle cx="11" cy="11" r="6.5"/><path d="m16 16 4.25 4.25"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M4.75 21c.7-4.25 3.12-6.5 7.25-6.5s6.55 2.25 7.25 6.5"/></>,
    bag: <><path d="M4.5 8.5h15l-1 12h-13l-1-12Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></>,
    menu: <path d="M3 7h18M3 12h18M3 17h18"/>,
    close: <path d="m5 5 14 14M19 5 5 19"/>,
  };
  return <svg aria-hidden="true" viewBox="0 0 24 24">{paths[name]}</svg>;
}

export default function StorefrontHero() {
  const [activeSlide, setActiveSlide] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const timer = window.setInterval(() => setActiveSlide((current) => (current + 1) % slides.length), 6500);
    return () => window.clearInterval(timer);
  }, []);

  const moveSlide = (direction: number) => setActiveSlide((current) => (current + direction + slides.length) % slides.length);

  return (
    <section className="storefront" aria-label="Styled Somehow featured collections">
      <header className="site-header">
        <Link className="brand-logo" href="/" aria-label="Styled Somehow home">
          <span>STYLED</span>
          <span>SOMEHOW</span>
        </Link>
        <nav className="desktop-nav" aria-label="Main navigation">
          <div className="nav-dropdown">
            <button type="button" className="nav-link">COLLECTIONS <span aria-hidden="true">⌄</span></button>
            <div className="dropdown-menu">{collectionLinks.map(([label, href]) => <Link key={label} href={href}>{label}</Link>)}</div>
          </div>
          <Link className="nav-link" href="/collections/men">MEN</Link>
          <Link className="nav-link" href="/collections/women">WOMEN</Link>
          <Link className="nav-link" href="/collections/couple-tshirts">COUPLE T-SHIRTS</Link>
        </nav>
        <div className="header-actions">
          <button className="icon-button desktop-action" type="button" aria-label="Search"><Icon name="search" /></button>
          <Link className="icon-button desktop-action" href="/account" aria-label="Account"><Icon name="user" /></Link>
          <Link className="icon-button bag-button" href="/cart" aria-label="Cart, 0 items"><Icon name="bag" /><span className="cart-count">0</span></Link>
          <button className="icon-button menu-button" type="button" aria-label={menuOpen ? "Close menu" : "Open menu"} aria-expanded={menuOpen} onClick={() => setMenuOpen((open) => !open)}><Icon name={menuOpen ? "close" : "menu"} /></button>
        </div>
      </header>

      <div className={`mobile-panel ${menuOpen ? "is-open" : ""}`} aria-hidden={!menuOpen}>
        <p className="mobile-menu-label">SHOP</p>
        <Link href="/collections/new-drop" onClick={() => setMenuOpen(false)}>New Drop</Link>
        <Link href="/collections/graphic-tees" onClick={() => setMenuOpen(false)}>Graphic Tees</Link>
        <Link href="/collections/plain-tees" onClick={() => setMenuOpen(false)}>Plain Tees</Link>
        <Link href="/collections/joggers" onClick={() => setMenuOpen(false)}>Joggers</Link>
        <Link href="/collections/men" onClick={() => setMenuOpen(false)}>Men</Link>
        <Link href="/collections/women" onClick={() => setMenuOpen(false)}>Women</Link>
        <Link href="/collections/couple-tshirts" onClick={() => setMenuOpen(false)}>Couple T-shirts</Link>
        <div className="mobile-secondary-links"><Link href="/account">Account</Link><button type="button">Search</button></div>
      </div>

      <div className="slides">
        {slides.map((slide, index) => (
          <Link key={slide.image} href={slide.href} className={`hero-slide ${index === activeSlide ? "is-active" : ""}`} aria-hidden={index !== activeSlide} tabIndex={index === activeSlide ? 0 : -1}>
            <Image src={slide.image} alt={slide.alt} fill priority={index === 0} sizes="100vw" style={{ objectPosition: slide.position }} />
            {index === 1 && <span className="lineup-cta">EXPLORE THE LINEUP <span aria-hidden="true">↗</span></span>}
          </Link>
        ))}
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
