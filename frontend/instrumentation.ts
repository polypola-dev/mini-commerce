import { registerOTel, OTLPHttpJsonTraceExporter } from "@vercel/otel";

// @vercel/otel의 fetch 계측은 기본적으로 Vercel 배포 URL에만 traceparent를 전파한다.
// 우리 백엔드는 Vercel 배포 URL이 아니므로 명시적으로 propagateContextUrls에 등록해야 한다.
const BACKEND_URLS = [
  process.env.API_BASE_URL ?? "http://localhost:18080",
  process.env.ORDER_SERVICE_URL ?? "http://localhost:18081",
  process.env.ORDER_ADMIN_SERVICE_URL ?? "http://localhost:18082",
];

export function register() {
  const otlpEndpoint = process.env.OTLP_TRACING_ENDPOINT ?? "http://localhost:4318";

  registerOTel({
    serviceName: "mini-commerce-bff",
    traceExporter: new OTLPHttpJsonTraceExporter({
      url: `${otlpEndpoint}/v1/traces`,
    }),
    instrumentationConfig: {
      fetch: {
        propagateContextUrls: BACKEND_URLS,
      },
    },
  });
}
