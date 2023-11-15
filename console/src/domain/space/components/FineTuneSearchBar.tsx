import Search from 'baseui/icon/search'

function FineTuneSearchBar() {
    return (
        <Search
            value={queries}
            getFilters={(key) => (attrs.find((v) => v.key === key) || attrs[0])?.getFilters()}
            onChange={setQueries as any}
        />
    )
}
export { FineTuneSearchBar }
export default FineTuneSearchBar
