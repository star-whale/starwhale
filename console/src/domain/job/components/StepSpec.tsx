function StepSpec({stepSource, rawType}) {
    return (
            {stepSource &&
                stepSource?.length > 0 &&
                !rawType &&
                stepSource?.map((spec, i) => {
                    return (
                        <div key={[spec?.name, i].join('')}>
                            <div
                                style={{
                                    display: 'flex',
                                    minWidth: '280px',
                                    lineHeight: '1',
                                    alignItems: 'stretch',
                                    gap: '20px',
                                    marginBottom: '10px',
                                }}
                            >
                                <div
                                    style={{
                                        padding: '5px 20px',
                                        minWidth: '280px',
                                        background: '#EEF1F6',
                                        borderRadius: '4px',
                                    }}
                                >
                                    <span style={{ color: 'rgba(2,16,43,0.60)' }}>{t('Step')}:&nbsp;</span>
                                    <span>{spec?.name}</span>
                                    <div style={{ marginTop: '3px' }} />
                                    <span style={{ color: 'rgba(2,16,43,0.60)' }}>{t('Task Amount')}:&nbsp;</span>
                                    <span>{spec?.replicas}</span>
                                </div>
                                {spec.resources &&
                                    spec.resources?.length > 0 &&
                                    spec.resources?.map((resource, j) => (
                                        <div
                                            key={j}
                                            style={{
                                                padding: '5px 20px',
                                                borderRadius: '4px',
                                                border: '1px solid #E2E7F0',
                                                // display: 'flex',
                                                alignItems: 'center',
                                            }}
                                        >
                                            <span style={{ color: 'rgba(2,16,43,0.60)' }}>{t('Resource')}:&nbsp;</span>
                                            <span> {resource?.type}</span>
                                            <div style={{ marginTop: '3px' }} />
                                            <span style={{ color: 'rgba(2,16,43,0.60)' }}>
                                                {t('Resource Amount')}:&nbsp;
                                            </span>
                                            <span>{resource?.request}</span>
                                            <br />
                                        </div>
                                    ))}
                            </div>
                        </div>
                    )
                })}
    )
}
