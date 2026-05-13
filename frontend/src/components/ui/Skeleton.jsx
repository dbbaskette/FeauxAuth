export default function Skeleton({ width, height = 10, className = '', style, ...rest }) {
  const baseStyle = {
    width: typeof width === 'number' ? `${width}px` : width || '100%',
    height: typeof height === 'number' ? `${height}px` : height,
    ...style,
  }
  return <div className={`skeleton ${className}`} style={baseStyle} {...rest} />
}

export function SkeletonText({ lines = 3, className = '' }) {
  const widths = ['82%', '64%', '74%', '90%', '56%']
  return (
    <div className={`flex flex-col gap-2 ${className}`}>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} width={widths[i % widths.length]} height={10} />
      ))}
    </div>
  )
}
