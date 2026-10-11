"use client";

import Image from "next/image";
import { useState } from "react";

export const measurements = [
  { size: "S", chest: 40, sleeve: 9.7, opening: 7.5, length: 27.5 },
  { size: "M", chest: 43, sleeve: 10.3, opening: 7.8, length: 28.5 },
  { size: "L", chest: 45, sleeve: 10.5, opening: 8.5, length: 29.5 },
];

export default function SizeGuide({ sizes }: { sizes: string[] }) {
  const [unit, setUnit] = useState<"in" | "cm">("in");
  const dimension = (inches: number) => unit === "in" ? String(inches) : (inches * 2.54).toFixed(1);

  return (
    <details className="product-details" open>
      <summary>Size guide</summary>
      <div className="size-guide-toolbar">
        <p>Garment measurements</p>
        <div className="size-guide-units" role="group" aria-label="Measurement unit">
          <button type="button" aria-pressed={unit === "in"} onClick={() => setUnit("in")}>Inches</button>
          <button type="button" aria-pressed={unit === "cm"} onClick={() => setUnit("cm")}>CM</button>
        </div>
      </div>
      <div className="size-guide-table-wrap" role="region" aria-label="Size measurements, scroll horizontally to see all columns" tabIndex={0}>
        <table className="product-size-table">
          <thead><tr><th scope="col">Size</th><th scope="col">Chest circumference</th><th scope="col">Front length</th><th scope="col">Sleeve length</th><th scope="col">Sleeve opening (flat)</th></tr></thead>
          <tbody>{measurements.filter(m => sizes.includes(m.size)).map(m => (
            <tr key={m.size}><th scope="row">{m.size}</th><td>{dimension(m.chest)}</td><td>{dimension(m.length)}</td><td>{dimension(m.sleeve)}</td><td>{dimension(m.opening)}</td></tr>
          ))}</tbody>
        </table>
      </div>
      {sizes.includes("XL") && <p>XL measurements have not been confirmed yet.</p>}
      <Image className="size-guide-diagram" src="/images/tshirt-measurement-guide.webp" alt="Oversized T-shirt diagram showing chest circumference, front length, sleeve length and flat sleeve opening." width={1536} height={1024} sizes="(max-width: 820px) 100vw, 45vw" />
    </details>
  );
}
