import TabBar from "./tab-bar";

export default function ShopLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="mcPage">
      <div className="mcShell">
        <div className="mcContent">{children}</div>
        <TabBar />
      </div>
    </div>
  );
}
