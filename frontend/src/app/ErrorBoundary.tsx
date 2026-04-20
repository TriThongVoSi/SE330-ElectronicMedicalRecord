import React from 'react'

type ErrorBoundaryState = {
  hasError: boolean
}

type ErrorBoundaryProps = {
  children: React.ReactNode
}

export class AppErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: Error): void {
    console.error('Unhandled UI error', error)
  }

  render(): React.ReactNode {
    if (this.state.hasError) {
      return (
        <div className="full-screen-loader">
          <strong>Unexpected UI error</strong>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => window.location.reload()}
          >
            Reload application
          </button>
        </div>
      )
    }

    return this.props.children
  }
}
