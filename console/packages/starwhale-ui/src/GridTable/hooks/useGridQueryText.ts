import React from 'react'
import { useStoreApi } from './useStore'

function useGridQueryText() {
    const store = useStoreApi().getState()

    const [textQuery, setTextQuery] = React.useState('')

    return {
        textQuery,
        setTextQuery,
    }
}

export { useGridQueryText }

export default useGridQueryText
