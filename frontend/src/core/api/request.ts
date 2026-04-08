import { httpClient, type AxiosRequestConfig } from './http-client'
import { normalizeApiError } from './errors'

type ApiRequestOptions = {
  request: AxiosRequestConfig
}

export const executeApiRequest = async <T>({ request }: ApiRequestOptions): Promise<T> => {
  try {
    const response = await httpClient.request<T>(request)
    return response.data
  } catch (error) {
    throw normalizeApiError(error)
  }
}
