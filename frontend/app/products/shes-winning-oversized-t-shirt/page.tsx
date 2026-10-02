import type { Metadata } from "next";
import ShesWinningProduct from "../../components/shes-winning-product";
export const metadata: Metadata = { title: "She's Winning Oversized T-Shirt | styled_somehow", description: "Black oversized T-shirt with pink statement artwork. 240 GSM, 100% cotton. ₹599." };
export default async function Page({ searchParams }: { searchParams: Promise<{ from?: string }> }) {
 const params = await searchParams;
 return <ShesWinningProduct returnUrl={params.from === "women" ? "/collections/women#shes-winning" : "/#new-in-product-shes-winning"} />;
}
