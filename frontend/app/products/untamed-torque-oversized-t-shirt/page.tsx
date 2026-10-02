import type { Metadata } from "next";
import UntamedProduct from "../../components/untamed-product";
export const metadata: Metadata = { title: "Untamed Torque Oversized T-Shirt | styled_somehow", description: "White oversized T-shirt with split-car artwork. 240 GSM. ₹999." };
export default async function Page({ searchParams }: { searchParams: Promise<{ from?: string }> }) {
 const params = await searchParams;
 return <UntamedProduct returnUrl={params.from === "men" ? "/collections/men#untamed-torque" : "/#new-in-product-untamed-torque"} />;
}
