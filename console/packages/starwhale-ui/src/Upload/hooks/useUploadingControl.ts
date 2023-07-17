import { useEffect, useRef } from 'react'
import { Subject, Subscription, catchError, mergeMap, throwError } from 'rxjs'

function useUploadingControl<FileUpload>({
    concurrent = 4,
    onUpload,
    onDone,
    onError,
}: {
    concurrent?: number
    onUpload?: (args: FileUpload) => Promise<any>
    onDone?: (args: FileUpload & { resp: any }) => void
    onError?: (args: FileUpload & { resp: any }, error: any) => void
}) {
    const uploadQRef = useRef<Subject<FileUpload>>(new Subject<FileUpload>())

    useEffect(() => {
        const subscription = new Subscription()
        const uploadQ = uploadQRef.current
        const uploadQSubscription = uploadQ
            .asObservable()
            .pipe(
                mergeMap(async (args: FileUpload) => {
                    const resp = await onUpload?.(args)
                    return {
                        ...args,
                        resp,
                    }
                    // return new Promise((resolve, reject) => {
                    //     setTimeout(() => {
                    //         resolve(fu)
                    //     }, 2000)
                    // })
                }, concurrent),
                catchError((error) => {
                    return throwError(() => error)
                })
            )
            .subscribe({
                next: (res: any) => {
                    // console.log(res, 'done')
                    onDone?.(res)
                },
                error: (error) => {
                    // console.log('ERROR >>>>', error)
                    onError?.(error?.config?.data, error)
                },
            })

        subscription.add(uploadQSubscription)

        // new Array(concurrent).fill(100).map(() => uploadQ.next({ test: new Date() }))

        return () => {
            subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    return {
        uploadQueue: uploadQRef.current,
    }
}

export { useUploadingControl }
export default useUploadingControl
