import TabBar from "./tab-bar";
import SearchOverlayPanel from "./search-overlay-panel";
import { SearchOverlayProvider } from "@/lib/search-overlay";

export default function ShopLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="mcPage">
      <SearchOverlayProvider>
        <div className="mcShell">
          <div className="mcContent">{children}</div>
          <TabBar />
          <SearchOverlayPanel />
        </div>
      </SearchOverlayProvider>
    </div>
  );
}
