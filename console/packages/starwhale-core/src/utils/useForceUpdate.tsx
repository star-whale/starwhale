import { useState } from 'react'

export function useForceUpdate() {
    const [, setValue] = useState(0)
    return () => setValue((prevState) => prevState + 1)
}

export default useForceUpdate
