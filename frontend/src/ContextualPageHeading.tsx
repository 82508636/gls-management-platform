type ContextualPageHeadingProps = {
  trail: string[]
  title: string
}

export function ContextualPageHeading({ trail, title }: ContextualPageHeadingProps) {
  return <div className="context-page-heading">
    <h1>{title}</h1>
    <div className="context-page-trail">{trail.map((item, index) => <span key={item}>{item}{index < trail.length - 1 && <i aria-hidden="true">·</i>}</span>)}</div>
  </div>
}
