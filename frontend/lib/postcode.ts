"use client";

// 카카오(다음) 우편번호 서비스 — 무료·승인 불필요. 실제 도로명주소만 선택 가능해
// 사용자 임의 주소 입력을 막고 우편번호를 확보한다.
// https://postcode.map.daum.net/guide

const SCRIPT_ID = "daum-postcode-script";
const SCRIPT_SRC = "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";

export type PostcodeResult = {
  zipCode: string; // 5자리 신우편번호
  roadAddress: string; // 도로명주소
  jibunAddress: string; // 지번주소
};

function loadScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (typeof window === "undefined") {
      reject(new Error("브라우저에서만 사용할 수 있습니다."));
      return;
    }
    const daum = (window as unknown as { daum?: { Postcode?: unknown } }).daum;
    if (daum?.Postcode) {
      resolve();
      return;
    }
    const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("우편번호 서비스를 불러오지 못했어요.")));
      return;
    }
    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.src = SCRIPT_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("우편번호 서비스를 불러오지 못했어요."));
    document.body.appendChild(script);
  });
}

/** 우편번호 검색 팝업을 열고, 사용자가 주소를 고르면 콜백으로 결과를 넘긴다. */
export async function openPostcodeSearch(onComplete: (result: PostcodeResult) => void): Promise<void> {
  await loadScript();
  const daum = (window as unknown as {
    daum: { Postcode: new (opts: { oncomplete: (data: Record<string, string>) => void }) => { open: () => void } };
  }).daum;

  new daum.Postcode({
    oncomplete: (data) => {
      onComplete({
        zipCode: data.zonecode ?? "",
        roadAddress: data.roadAddress ?? "",
        jibunAddress: data.jibunAddress ?? "",
      });
    },
  }).open();
}
