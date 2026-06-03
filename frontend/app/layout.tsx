import "./styles.css";

export const metadata = {
  title: "Mini Commerce",
  description: "Separated frontend for Mini Commerce"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
