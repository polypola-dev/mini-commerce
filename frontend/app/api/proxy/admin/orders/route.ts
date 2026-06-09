import { createClient } from "@/lib/supabase/server";
import { NextResponse } from "next/server";

const BACKEND_URL = process.env.API_BASE_URL ?? "http://localhost:18080";
const BFF_SECRET_KEY = process.env.BFF_SECRET_KEY;

export async function GET() {
  if (!BFF_SECRET_KEY) return new NextResponse("Server misconfiguration", { status: 500 });

  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();
  if (!session) return new NextResponse("Unauthorized", { status: 401 });

  const backendResponse = await fetch(`${BACKEND_URL}/api/admin/orders`, {
    headers: {
      "X-Internal-BFF-Key": BFF_SECRET_KEY,
      "Authorization": `Bearer ${session.access_token}`,
    },
    cache: "no-store",
  });

  const body = await backendResponse.text();
  return new NextResponse(body, {
    status: backendResponse.status,
    headers: { "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json" },
  });
}
