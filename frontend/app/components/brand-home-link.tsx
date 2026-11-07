import Link from "next/link";

export default function BrandHomeLink() {
  return (
    <Link className="brand-logo" href="/" aria-label="Styled Somehow home">
      <span>STYLED</span>
      <span>SOMEHOW</span>
    </Link>
  );
}
