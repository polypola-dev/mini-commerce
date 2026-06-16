import { createClient } from "@/lib/supabase/server";
import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.API_BASE_URL ?? "http://localhost:18080";
const BFF_SECRET_KEY = process.env.BFF_SECRET_KEY;

async function getAdminSession() {
  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();
  return session;
}

export async function GET(request: NextRequest) {
  if (!BFF_SECRET_KEY) return new NextResponse("Server misconfiguration", { status: 500 });
  const session = await getAdminSession();
  if (!session) return new NextResponse("Unauthorized", { status: 401 });

  const { searchParams } = request.nextUrl;
  const backendUrl = new URL(`${BACKEND_URL}/api/admin/products`);
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

export async function POST(request: NextRequest) {
  if (!BFF_SECRET_KEY) return new NextResponse("Server misconfiguration", { status: 500 });
  const session = await getAdminSession();
  if (!session) return new NextResponse("Unauthorized", { status: 401 });

  const body = await request.text();
  const backendResponse = await fetch(`${BACKEND_URL}/api/admin/products`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Internal-BFF-Key": BFF_SECRET_KEY,
      "Authorization": `Bearer ${session.access_token}`,
    },
    body,
  });

  const responseBody = await backendResponse.text();
  return new NextResponse(responseBody, {
    status: backendResponse.status,
    headers: { "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json" },
  });
}
