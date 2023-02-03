import React from 'react'
import { Accordion as BaseAccordion, type AccordionProps } from 'baseui/accordion'
import { mergeOverrides } from '@/utils/baseui'

export interface IAccordionProps extends Partial<AccordionProps> {
    className?: string
}

/* eslint-disable react/jsx-props-no-spreading */
export default function Accordion({ children, ...props }: IAccordionProps) {
    const overrides = mergeOverrides(
        {
            PanelContainer: {
                style: {
                    borderTop: '1px solid #CFD7E6',
                    borderLeft: '1px solid #CFD7E6',
                    borderRight: '1px solid #CFD7E6',
                    borderRadius: '3px',
                    borderBottomColor: '#CFD7E6',
                    display: 'flex',
                    flexDirection: 'column',
                },
            },
            Header: {
                style: {
                    backgroundColor: '#F7F8FA',
                    paddingTop: '9px',
                    paddingBottom: '9px',
                    fontSize: '14px',
                },
            },
            Content: {
                style: {
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    paddingTop: '20px',
                    paddingBottom: '20px',
                    paddingLeft: '20px',
                    paddingRight: '20px',
                },
            },
            Root: {
                style: {
                    flex: 1,
                    display: 'flex',
                    fontSize: '14px',
                },
            },
        },
        props.overrides
    )

    return (
        <BaseAccordion {...props} overrides={overrides}>
            {children}
        </BaseAccordion>
    )
}
