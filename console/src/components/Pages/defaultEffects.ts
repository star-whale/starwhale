import { PageEffectInfo, DefaultEffect, PageEffect } from './Page'
import { transform } from 'framer-motion'

export const clamp = (num: number, min: number, max: number) => Math.max(Math.min(num, max), min)

function titlesEffect(info: PageEffectInfo) {
    const { index, normalizedOffset, size } = info
    return {
        style: {
            x: normalizedOffset * -size.width * (0.8 + normalizedOffset * 0.015),
            y: normalizedOffset * 16,
            scale: transform(normalizedOffset, [0, 1], [1, 0.85], {
                clamp: false,
            }),
            opacity: normalizedOffset + 1,
            zIndex: -index,
        },
    }
}

function cubeEffect(info: PageEffectInfo) {
    const { normalizedOffset, direction } = info
    const isHorizontal = direction === 'horizontal'

    return {
        style: {
            originX: normalizedOffset < 0 ? 1 : 0,
            originY: normalizedOffset < 0 ? 1 : 0,
            rotateY: isHorizontal ? clamp(normalizedOffset * 90, -90, 90) : 0,
            rotateX: isHorizontal ? 0 : clamp(normalizedOffset * 90, -90, 90),
            backfaceVisibility: 'hidden' as any,
            WebkitBackfaceVisibility: 'hidden' as any,
        },
    }
}

function coverflowEffect(info: PageEffectInfo) {
    const { normalizedOffset, direction } = info
    const isHorizontal = direction === 'horizontal'

    return {
        style: {
            rotateY: isHorizontal ? clamp(normalizedOffset * -45, -45, 45) : 0,
            rotateX: isHorizontal ? 0 : clamp(normalizedOffset * -45, -45, 45),
            originX: isHorizontal ? (normalizedOffset < 0 ? 1 : 0) : 0.5,
            originY: isHorizontal ? 0.5 : normalizedOffset < 0 ? 0 : 1,
            z: -Math.abs(normalizedOffset),
            scale: 1 - Math.abs(normalizedOffset / 10),
        },
    }
}

function pileEffect(info: PageEffectInfo) {
    const { normalizedOffset, index, direction } = info
    const isHorizontal = direction === 'horizontal'
    const offset = `calc(${Math.abs(normalizedOffset) * 100}% - ${Math.abs(normalizedOffset) * 8}px)`
    return {
        style: {
            x: normalizedOffset < 0 && isHorizontal ? offset : 0,
            y: normalizedOffset < 0 && !isHorizontal ? offset : 0,
            scale: normalizedOffset < 0 ? 1 - Math.abs(normalizedOffset) / 50 : 1,
            zIndex: index,
        },
    }
}

function wheelEffect(info: PageEffectInfo) {
    const { normalizedOffset, direction, size } = info
    const isHorizontal = direction === 'horizontal'

    const rotateX = isHorizontal ? 0 : normalizedOffset * -20
    const rotateY = isHorizontal ? normalizedOffset * 20 : 0
    const y = isHorizontal ? 0 : normalizedOffset * -size.height
    const x = isHorizontal ? normalizedOffset * -size.width : 0
    const z = ((isHorizontal ? size.width : size.height) * 18) / (2 * Math.PI)

    return {
        style: {
            opacity: 1 - Math.abs(normalizedOffset) / 4,
            transform: `translate(${x}px, ${y}px) translateZ(-${z}px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(${z}px)`,
        },
    }
}

export const defaultEffects: Record<DefaultEffect, PageEffect> = {
    cube: cubeEffect,
    wheel: wheelEffect,
    pile: pileEffect,
    coverflow: coverflowEffect,
    titles: titlesEffect,
}
