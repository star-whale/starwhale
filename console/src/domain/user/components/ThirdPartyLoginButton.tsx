import React, { useCallback } from 'react'
import Button from '@/components/Button'
import IconFont from '@/components/IconFont'
import useTranslation from '@/hooks/useTranslation'

export interface IThirdPartyLoginButtonProps {
    isLogin: boolean
    vendorName: string
    vendor: string
    icon: React.ReactNode
}

export default function ThirdPartyLoginButton({ isLogin, vendorName, vendor, icon }: IThirdPartyLoginButtonProps) {
    const [t] = useTranslation()

    const handleClick = useCallback(
        (e) => {
            e.preventDefault()
            const base = `${window.location.protocol}//${window.location.host}`
            const feUrl = encodeURIComponent(`${base}/loginnew`)
            const baseBe = `${base}/swcloud/api/v1/redirect/thirdparty`
            // redirect to our server and pass the front-end url to redirect when user auth done.
            window.location.href = `${baseBe}?callback=${feUrl}&vendor=${encodeURIComponent(vendor)}`
        },
        [vendor]
    )

    return (
        <Button
            isFull
            startEnhancer={icon}
            kind='secondary'
            endEnhancer={<IconFont type='arrow_right' />}
            overrides={{
                BaseButton: {
                    style: { justifyContent: 'space-between', paddingLeft: '20px' },
                    // make a button type, prevent triggering click event when we press enter in from
                    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#implicit-submission
                    props: { type: 'button' },
                },
            }}
            onClick={handleClick}
        >
            {t(isLogin ? 'Log In With' : 'Sign Up With', [vendorName])}
        </Button>
    )
}
