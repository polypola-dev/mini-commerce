import { createClient } from "@/lib/supabase/server";
import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.API_BASE_URL ?? "http://localhost:18080";
const BFF_SECRET_KEY = process.env.BFF_SECRET_KEY;

export async function GET() {
  if (!BFF_SECRET_KEY) {
    return new NextResponse("Server misconfiguration", { status: 500 });
  }

  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();

  // 위시리스트는 홈/목록 등 비로그인 화면에서도 훅이 로드된다.
  // 비로그인은 오류가 아니라 "빈 목록"이므로 200 + []로 응답한다.
  if (!session) {
    return NextResponse.json([]);
  }

  const backendResponse = await fetch(`${BACKEND_URL}/api/wishlist`, {
    headers: {
      "X-Internal-BFF-Key": BFF_SECRET_KEY,
      "Authorization": `Bearer ${session.access_token}`,
    },
  });

  const responseBody = await backendResponse.text();

  return new NextResponse(responseBody, {
    status: backendResponse.status,
    headers: { "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json" },
  });
}

export async function POST(request: NextRequest) {
  if (!BFF_SECRET_KEY) {
    return new NextResponse("Server misconfiguration", { status: 500 });
  }

  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();

  if (!session) {
    return new NextResponse("Unauthorized", { status: 401 });
  }

  const body = await request.text();

  const backendResponse = await fetch(`${BACKEND_URL}/api/wishlist`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Internal-BFF-Key": BFF_SECRET_KEY,
      "Authorization": `Bearer ${session.access_token}`,
    },
    body,
  });

  return new NextResponse(null, { status: backendResponse.status });
}
