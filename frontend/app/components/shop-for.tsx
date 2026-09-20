"use client";

import Link from "next/link";
import { CSSProperties, useState } from "react";

type Audience = "women" | "men";

const products = {
  women: [
    { name: "She Can. She Will. Oversized Tee", price: "₹799", compareAt: "₹999", art: "SHE CAN\nSHE WILL", tone: "#ead9ed", ink: "#cc3d78" },
    { name: "Bitchआरी Graphic Tee", price: "₹799", compareAt: "₹999", art: "BITCHआरी", tone: "#221b1c", ink: "#f3c94c" },
    { name: "I Don't Care Oversized Tee", price: "₹849", compareAt: "₹1,099", art: "I DON'T\nCARE", tone: "#f1e9de", ink: "#151515" },
    { name: "Her Era Statement Tee", price: "₹799", compareAt: "₹999", art: "HER ERA", tone: "#181818", ink: "#f2eee8" },
  ],
  men: [
    { name: "Core Black Oversized Tee", price: "₹749", compareAt: "₹949", art: "OFF DUTY", tone: "#171717", ink: "#f4f1eb" },
    { name: "No Pressure Essential Tee", price: "₹799", compareAt: "₹999", art: "NO\nPRESSURE", tone: "#e8e2d7", ink: "#111111" },
    { name: "Styled Somehow Logo Tee", price: "₹749", compareAt: "₹949", art: "STYLED\nSOMEHOW", tone: "#d8d2c5", ink: "#111111" },
    { name: "After Hours Graphic Tee", price: "₹849", compareAt: "₹1,099", art: "AFTER\nHOURS", tone: "#302c2b", ink: "#d9c5e8" },
  ],
} as const;

export default function ShopFor() {
  const [audience, setAudience] = useState<Audience>("women");
  const activeProducts = products[audience];

  return (
    <section className="shop-for" aria-labelledby="shop-for-title">
      <div className="shop-for__header">
        <div>
          <p className="shop-for__eyebrow">FIND YOUR FIT</p>
          <div className="shop-for__title-row">
            <h2 id="shop-for-title">Shop for</h2>
            <div className="audience-tabs" role="tablist" aria-label="Choose collection">
              {(["women", "men"] as const).map((value, index) => (
                <span key={value} className="audience-tab-wrap">
                  {index > 0 && <span className="audience-divider" aria-hidden="true">/</span>}
                  <button
                    type="button"
                    role="tab"
                    aria-selected={audience === value}
                    className={audience === value ? "is-active" : ""}
                    onClick={() => setAudience(value)}
                  >
                    {value[0].toUpperCase() + value.slice(1)}
                  </button>
                </span>
              ))}
            </div>
          </div>
        </div>
        <Link className="shop-for__all" href={`/collections/${audience}`}>
          Shop {audience} <span aria-hidden="true">↗</span>
        </Link>
      </div>

      <div className="product-rail" role="tabpanel" aria-live="polite">
        {activeProducts.map((product) => (
          <article className="product-card" key={product.name}>
            <Link href={`/collections/${audience}`} aria-label={product.name}>
              <div
                className="product-card__visual"
                style={{ "--card-tone": product.tone, "--card-ink": product.ink } as CSSProperties}
              >
                <span className="sample-badge">COLLECTION PREVIEW</span>
                <div className="tee-mockup" aria-hidden="true">
                  <span>{product.art}</span>
                </div>
                <span className="quick-view">QUICK VIEW</span>
              </div>
              <div className="product-card__info">
                <h3>{product.name}</h3>
                <p><span>{product.compareAt}</span> {product.price}</p>
              </div>
            </Link>
          </article>
        ))}
      </div>
    </section>
  );
}
