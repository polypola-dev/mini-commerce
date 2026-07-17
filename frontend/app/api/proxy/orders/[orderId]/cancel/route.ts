import { createClient } from "@/lib/supabase/server";
import { NextRequest, NextResponse } from "next/server";

// order-api 전용 호스트(MSA S3-3b, ADR-005) — shop-api(API_BASE_URL)와 분리된 서비스.
const BACKEND_URL = process.env.ORDER_SERVICE_URL ?? "http://localhost:18081";
const BFF_SECRET_KEY = process.env.BFF_SECRET_KEY;

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ orderId: string }> }
) {
  if (!BFF_SECRET_KEY) {
    return new NextResponse("Server misconfiguration", { status: 500 });
  }

  const { orderId } = await params;
  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();

  if (!session) {
    return new NextResponse("Unauthorized", { status: 401 });
  }

  const body = await request.text();

  const backendResponse = await fetch(
    `${BACKEND_URL}/api/orders/${orderId}/cancel`,
    {
      method: "POST",
      headers: {
        "X-Internal-BFF-Key": BFF_SECRET_KEY,
        "Authorization": `Bearer ${session.access_token}`,
        "Content-Type": "application/json",
      },
      body,
    }
  );

  const responseBody = await backendResponse.text();

  return new NextResponse(responseBody, {
    status: backendResponse.status,
    headers: { "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json" },
  });
}
