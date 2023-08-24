import { BoldIcon } from 'lucide-react'

export interface BubbleMenuItem {
    name: string
    isActive: () => boolean
    command: () => void
    icon: typeof BoldIcon
}
