import { executeApiRequest } from '../../core/api/request'
import type {
  StaffDoctorCreateInput,
  StaffDoctorProfile,
  StaffDoctorUpdateInput,
  StaffMember,
  StaffProfileUpdateInput,
} from './types'

const ensureArrayPayload = <T>(rawPayload: unknown, message: string): T[] => {
  if (Array.isArray(rawPayload)) {
    return rawPayload as T[]
  }

  throw {
    message,
    status: 502,
    details: rawPayload,
  }
}

export const listDoctors = async (): Promise<StaffMember[]> => {
  const rawPayload = await executeApiRequest<unknown>({
    request: {
      url: '/api/staff/doctors',
      method: 'GET',
    },
  })

  return ensureArrayPayload<StaffMember>(rawPayload, 'Invalid doctors response from backend.')
}

export const getDoctorProfile = async (doctorId: string): Promise<StaffDoctorProfile> => {
  return executeApiRequest<StaffDoctorProfile>({
    request: {
      url: `/api/staff/doctors/${doctorId}`,
      method: 'GET',
    },
  })
}

export const createDoctor = async (payload: StaffDoctorCreateInput): Promise<StaffDoctorProfile> => {
  return executeApiRequest<StaffDoctorProfile>({
    request: {
      url: '/api/staff/doctors',
      method: 'POST',
      data: payload,
    },
  })
}

export const updateDoctor = async (
  doctorId: string,
  payload: StaffDoctorUpdateInput
): Promise<StaffDoctorProfile> => {
  return executeApiRequest<StaffDoctorProfile>({
    request: {
      url: `/api/staff/doctors/${doctorId}`,
      method: 'PUT',
      data: payload,
    },
  })
}

export const deactivateDoctor = async (doctorId: string): Promise<void> => {
  await executeApiRequest<void>({
    request: {
      url: `/api/staff/doctors/${doctorId}`,
      method: 'DELETE',
    },
  })
}

export const getMyProfile = async (): Promise<StaffDoctorProfile> => {
  return executeApiRequest<StaffDoctorProfile>({
    request: {
      url: '/api/staff/profile',
      method: 'GET',
    },
  })
}

export const updateMyProfile = async (
  payload: StaffProfileUpdateInput
): Promise<StaffDoctorProfile> => {
  return executeApiRequest<StaffDoctorProfile>({
    request: {
      url: '/api/staff/profile',
      method: 'PUT',
      data: payload,
    },
  })
}
