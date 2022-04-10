/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable no-case-declarations */
import React, { useEffect, useState } from 'react'
import { createUseStyles } from 'react-jss'
import { useQuery } from 'react-query'
import useTranslation from '@/hooks/useTranslation'
import { useStyletron } from 'baseui'

const useStyles = createUseStyles({
    root: {},
    notification: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
})

export default function Home() {
    const styles = useStyles()
    const [t] = useTranslation()
    const [, theme] = useStyletron()

    return <div className={styles.root}></div>
}
