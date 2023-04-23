import React from 'react'

function useGridQueryText() {
    const [textQuery, setTextQuery] = React.useState('')

    return {
        textQuery,
        setTextQuery,
    }
}

export { useGridQueryText }

export default useGridQueryText
