import type { Metadata } from "next";
import ChilliProduct from "../../components/chilli-product";
export const metadata: Metadata = { title: "Chilli Crush Oversized T-Shirt | styled_somehow", description: "Black oversized T-shirt with red chilli artwork. 240 GSM, 100% cotton. ₹699." };
export default async function Page({ searchParams }: { searchParams: Promise<{ from?: string }> }) {
 const params = await searchParams;
 return <ChilliProduct returnUrl={params.from === "women" ? "/collections/women#chilli-crush" : "/#new-in-product-chilli-crush"} />;
}
