function ButtonGroup({ children }) {
    return (
        <div key='action' className='flex items-center gap-16px '>
            {children}
        </div>
    )
}

export { ButtonGroup }
export default ButtonGroup
