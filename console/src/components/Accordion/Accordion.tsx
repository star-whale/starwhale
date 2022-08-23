import React from 'react'
import { Accordion as BaseAccordion, type AccordionProps } from 'baseui/accordion'
import { mergeOverrides } from '@/utils/baseui'

export interface IAccordionProps extends AccordionProps {
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
                },
            },
            Header: {
                style: {
                    backgroundColor: '#F7F8FA',
                },
            },
            Content: {
                style: {
                    paddingTop: '20px',
                    paddingBottom: '20px',
                    paddingLeft: '20px',
                    paddingRight: '20px',
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
