import { ResourceType } from '@/schemas/resource'
import type { IconType } from 'react-icons/lib'
import { GrOrganization, GrServerCluster, GrDeploy, GrUser } from 'react-icons/gr'
import { AiOutlineDashboard, AiOutlineCodeSandbox } from 'react-icons/ai'
import { HiOutlineUserGroup, HiOutlineKey } from 'react-icons/hi'
import { BiRevision, BiExtension } from 'react-icons/bi'
import { GoPackage } from 'react-icons/go'
import { VscFileBinary } from 'react-icons/vsc'
import { RiMistFill } from 'react-icons/ri'

export const headerHeight = 70
export const sidebarExpandedWidth = 200
export const sidebarFoldedWidth = 68
export const textVariant = 'smallPlus'
export const dateFormat = 'YYYY-MM-DD'
export const dateWithZeroTimeFormat = 'YYYY-MM-DD 00:00:00'
export const dateTimeFormat = 'YYYY-MM-DD HH:mm:ss'

export const resourceIconMapping: Record<ResourceType, IconType> = {
    user: GrUser,
    user_group: HiOutlineUserGroup,
    organization: GrOrganization,
}
