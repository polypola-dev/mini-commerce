import { createClient } from "@/lib/supabase/server";
import { NextRequest, NextResponse } from "next/server";

// order-admin 전용 호스트(MSA S4, ADR-005) — order-api(고객)/shop-api와 분리된 관리자 전용 서비스.
const BACKEND_URL = process.env.ORDER_ADMIN_SERVICE_URL ?? "http://localhost:18082";
const BFF_SECRET_KEY = process.env.BFF_SECRET_KEY;

export async function GET(request: NextRequest) {
  if (!BFF_SECRET_KEY) return new NextResponse("Server misconfiguration", { status: 500 });

  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();
  if (!session) return new NextResponse("Unauthorized", { status: 401 });

  const { searchParams } = request.nextUrl;
  const backendUrl = new URL(`${BACKEND_URL}/api/admin/orders`);
  searchParams.forEach((value, key) => backendUrl.searchParams.set(key, value));

  const backendResponse = await fetch(backendUrl.toString(), {
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
