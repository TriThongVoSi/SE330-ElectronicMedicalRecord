import type { UseQueryResult } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import type { PagedResult } from '../core/types/common'
import { EmptyState } from './EmptyState'
import { ErrorState } from './ErrorState'
import { LoadingState } from './LoadingState'
import { PaginationControls } from './PaginationControls'

type PaginatedQueryContentProps<TItem> = {
  query: UseQueryResult<PagedResult<TItem>, unknown>
  emptyTitle: string
  emptyMessage: string
  getErrorMessage: (error: unknown) => string
  onRetry: () => void
  onPageChange: (page: number) => void
  children: (items: TItem[]) => ReactNode
}

export const PaginatedQueryContent = <TItem,>({
  query,
  emptyTitle,
  emptyMessage,
  getErrorMessage,
  onRetry,
  onPageChange,
  children,
}: PaginatedQueryContentProps<TItem>): ReactNode => {
  if (query.isLoading) {
    return <LoadingState />
  }

  if (query.isError) {
    return <ErrorState message={getErrorMessage(query.error)} onRetry={onRetry} />
  }

  if (!query.isSuccess) {
    return null
  }

  if (query.data.items.length === 0) {
    return <EmptyState title={emptyTitle} message={emptyMessage} />
  }

  return (
    <>
      {children(query.data.items)}
      <PaginationControls
        page={query.data.page}
        totalPages={query.data.totalPages}
        totalItems={query.data.totalItems}
        onPageChange={onPageChange}
      />
    </>
  )
}
