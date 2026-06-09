import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.API_BASE_URL ?? "http://localhost:18080";

export async function GET(request: NextRequest) {
  const { searchParams } = request.nextUrl;
  const url = new URL(`${BACKEND_URL}/api/products`);
  const q = searchParams.get("q");
  if (q) url.searchParams.set("q", q);

  const backendResponse = await fetch(url.toString(), { cache: "no-store" });

  const responseBody = await backendResponse.text();
  return new NextResponse(responseBody, {
    status: backendResponse.status,
    headers: { "Content-Type": backendResponse.headers.get("Content-Type") ?? "application/json" },
  });
}
