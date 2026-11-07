import Link from "next/link";
import type { ComponentProps } from "react";

function BrandLetters() {
  return <><span>STYLED</span><span>SOMEHOW</span></>;
}

export function BrandWordmark() {
  return <div className="brand-logo"><BrandLetters /></div>;
}

export default function BrandHomeLink({onNavigate}: Pick<ComponentProps<typeof Link>, "onNavigate">) {
  return (
    <Link className="brand-logo" href="/" aria-label="Styled Somehow home" onNavigate={onNavigate}>
      <BrandLetters />
    </Link>
  );
}
