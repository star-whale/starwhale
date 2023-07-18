import { useEffect, useRef } from 'react'
import { Subject, Subscription, catchError, from, mergeMap, of, takeUntil } from 'rxjs'

function useUploadingControl<T>({
    concurrent = 4,
    onUpload,
    onDone,
    onError,
}: {
    concurrent?: number
    onUpload: (args: T) => Promise<any>
    onDone?: (args: any) => void
    onError?: (args: any, error: any) => void
}) {
    const uploadQRef = useRef<Subject<T>>(new Subject<T>())
    const stopSignalRef = useRef<Subject<void>>(new Subject<void>())

    useEffect(() => {
        const subscription = new Subscription()
        const uploadQ = uploadQRef.current
        const uploadQSubscription = uploadQ
            .asObservable()
            .pipe(
                mergeMap((args: T) => {
                    return from(onUpload(args)).pipe(catchError((error) => of(error)))
                }, concurrent),
                takeUntil(stopSignalRef.current)
            )
            .subscribe({
                next: (res: any) => {
                    const file = res.config?.data
                    if (!file) return

                    if (res instanceof Error) {
                        onError?.(file, res)
                        return
                    }

                    onDone?.(file)
                },
            })

        subscription.add(uploadQSubscription)

        // new Array(concurrent).fill(100).map(() => uploadQ.next({ test: new Date() }))

        return () => {
            subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [stopSignalRef.current])

    return {
        uploadQueue: uploadQRef.current,
        cancel: () => {
            stopSignalRef.current.next()
            stopSignalRef.current.complete()
            stopSignalRef.current = new Subject<void>()
        },
    }
}

export { useUploadingControl }
export default useUploadingControl
