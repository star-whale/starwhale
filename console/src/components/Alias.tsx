import React from 'react'

export function Alias({ alias = '' }: { alias?: string }) {
    return (
        <div style={{ display: 'inline-flex', gap: '2px', alignItems: 'center' }}>
            {alias.split(',').map((item, index) => {
                return (
                    <span
                        key={index}
                        style={{
                            fontSize: '14px',
                            lineHeight: '14px',
                            borderRadius: '4px',
                            backgroundColor: '#F5F8FF',
                            border: '1px solid #CFD7E6 ',
                            padding: '2px 8px',
                            color: '#02102B',
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
