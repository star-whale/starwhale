import React, { useMemo } from 'react'
import Card from '@/components/Card'
import yaml from 'js-yaml'
import { JSONTree } from 'react-json-tree'
import { useRuntime } from '@/domain/runtime/hooks/useRuntime'

const theme = {
    scheme: 'bright',
    author: 'chris kempson (http://chriskempson.com)',
    base00: '#000000',
    base01: '#303030',
    base02: '#505050',
    base03: '#b0b0b0',
    base04: '#d0d0d0',
    base05: '#e0e0e0',
    base06: '#f5f5f5',
    base07: '#ffffff',
    base08: '#fb0120',
    base09: '#fc6d24',
    base0A: '#fda331',
    base0B: '#a1c659',
    base0C: '#76c7b7',
    base0D: '#6fb3d2',
    base0E: '#d381c3',
    base0F: '#be643c',
}

export default function RuntimeVersionOverviewMeta() {
    const { runtime } = useRuntime()

    const jsonData = useMemo(() => {
        if (!runtime?.versionMeta) return {}
        return yaml.load(runtime?.versionMeta)
    }, [runtime?.versionMeta])

    return (
        <Card
            style={{
                fontSize: '14px',
            }}
        >
            <div
                style={{
                    paddingLeft: '10px',
                }}
            >
                <JSONTree data={jsonData} theme={theme} hideRoot shouldExpandNode={() => true} />
            </div>
        </Card>
    )
}
