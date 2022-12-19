import { useState } from 'react'

export default function useForceUpdate() {
    const [, setValue] = useState(0)
    return () => setValue((prevState) => prevState + 1)
}
