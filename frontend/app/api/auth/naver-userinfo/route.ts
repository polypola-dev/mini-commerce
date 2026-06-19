import { NextRequest, NextResponse } from "next/server";

const NAVER_USERINFO_URL = "https://openapi.naver.com/v1/nid/me";

export async function GET(request: NextRequest) {
  const authorization = request.headers.get("authorization");
  if (!authorization) {
    return NextResponse.json({ error: "missing_authorization" }, { status: 401 });
  }

  const naverResponse = await fetch(NAVER_USERINFO_URL, {
    headers: { Authorization: authorization },
  });

  if (!naverResponse.ok) {
    return NextResponse.json({ error: "naver_userinfo_request_failed" }, { status: 502 });
  }

  const body = await naverResponse.json();

  if (body.resultcode !== "00" || !body.response) {
    return NextResponse.json({ error: "naver_userinfo_invalid_response" }, { status: 502 });
  }

  const { id, email, nickname, profile_image } = body.response;

  return NextResponse.json({
    sub: id,
    email,
    email_verified: Boolean(email),
    name: nickname,
    picture: profile_image,
  });
}
