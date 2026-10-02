import type { Metadata } from "next";
import BitchariProduct from "../../components/bitchari-product";
export const metadata: Metadata = { title: "Bitchआरी Oversized T-Shirt | styled_somehow", description: "Black oversized T-shirt with yellow Bitchआरी lettering. 240 GSM, 100% cotton. ₹599." };
export default async function Page({ searchParams }: { searchParams: Promise<{ from?: string }> }) {
 const params = await searchParams;
 return <BitchariProduct returnUrl={params.from === "women" ? "/collections/women#bitchari" : "/#new-in-product-bitchari"} />;
}
