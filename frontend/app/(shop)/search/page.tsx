import SearchBar from "../search-bar";
import SearchContent from "./search-content";
import SearchChrome from "./search-chrome";

export default async function SearchPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const { q } = await searchParams;
  const query = (q ?? "").trim();

  return (
    <SearchChrome searchBar={<SearchBar initialQuery={query} />}>
      <SearchContent query={query} />
    </SearchChrome>
  );
}
