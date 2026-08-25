type ContextualPageHeadingProps = {
  trail: string[]
  title: string
}

export function ContextualPageHeading({ trail, title }: ContextualPageHeadingProps) {
  return <div className="context-page-heading">
    {trail.map(item => <span key={item}>{item}<i aria-hidden="true">·</i></span>)}
    <h1>{title}</h1>
  </div>
}
