import { executeApiRequest } from '../../core/api/request'
import type { DashboardSummary } from './types'

export const getDashboardSummary = async (): Promise<DashboardSummary> => {
  return executeApiRequest<DashboardSummary>({
    request: {
      url: '/api/dashboard/summary',
      method: 'GET',
    },
  })
}
