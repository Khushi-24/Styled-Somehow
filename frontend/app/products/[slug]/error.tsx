"use client";
import Link from "next/link";
export default function ErrorPage({reset}: {reset: () => void}) {return <main className="new-in"><h1>Product temporarily unavailable</h1><p>Please try again shortly.</p><button onClick={reset}>Try again</button> <Link href="/">Home</Link></main>;}
