import type { Metadata } from "next";
import CherryProduct from "../../components/cherry-product";
export const metadata: Metadata = { title: "Cherry Zest Oversized T-Shirt | styled_somehow", description: "White oversized T-shirt with cherry and citrus artwork. 240 GSM. ₹599." };
export default async function Page({ searchParams }: { searchParams: Promise<{ from?: string }> }) {
 const params = await searchParams;
 return <CherryProduct returnUrl={params.from === "women" ? "/collections/women#cherry-zest" : "/#new-in-product-cherry-zest"} />;
}
