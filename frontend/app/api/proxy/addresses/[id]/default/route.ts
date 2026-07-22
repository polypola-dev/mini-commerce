import { createClient } from "@/lib/supabase/server";
import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL = process.env.API_BASE_URL ?? "http://localhost:18080";
const BFF_SECRET_KEY = process.env.BFF_SECRET_KEY;

export async function PUT(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  if (!BFF_SECRET_KEY) {
    return new NextResponse("Server misconfiguration", { status: 500 });
  }

  const supabase = await createClient();
  const { data: { session } } = await supabase.auth.getSession();

  if (!session) {
    return new NextResponse("Unauthorized", { status: 401 });
  }

  const { id } = await params;

  const backendResponse = await fetch(`${BACKEND_URL}/api/addresses/${id}/default`, {
    method: "PUT",
    headers: {
      "X-Internal-BFF-Key": BFF_SECRET_KEY,
      "Authorization": `Bearer ${session.access_token}`,
    },
  });

  return new NextResponse(null, { status: backendResponse.status });
}
