import React from 'react'

export function Alias({ alias = '' }: { alias?: string }) {
    return (
        <div style={{ display: 'inline-flex', gap: '4px', alignItems: 'center' }}>
            {alias.split(',').map((item, index) => {
                return (
                    <span
                        key={index}
                        style={{
                            fontSize: '14px',
                            lineHeight: '16px',
                            borderRadius: '4px',
                            backgroundColor: '#EBF1FF',
                            border: '1px solid #CFD7E5',
                            padding: '2px 4px',
                        }}
                    >
                        {item}
                    </span>
                )
            })}
        </div>
    )
}
export default Alias
