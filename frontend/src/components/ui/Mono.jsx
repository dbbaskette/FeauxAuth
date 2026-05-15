export default function Mono({ as: Tag = 'span', className = '', children, ...rest }) {
  return <Tag className={`mono ${className}`} {...rest}>{children}</Tag>
}
