import type { UserRole } from '../../core/types/common'

export type StaffMember = {
  id: string
  fullName: string
  email: string
  phone?: string
  role: UserRole
}

export type StaffDoctorProfile = {
  id: string
  username: string
  fullName: string
  email: string
  phone?: string
  gender?: string
  address?: string
  role: UserRole | string
  status: string
  active: boolean
  confirmed: boolean
  serviceId?: string
}

export type StaffDoctorCreateInput = {
  username: string
  password: string
  email: string
  fullName: string
  gender?: string
  phone?: string
  address?: string
  serviceId?: string
  isConfirmed: boolean
  isActive: boolean
}

export type StaffDoctorUpdateInput = {
  email: string
  fullName: string
  gender?: string
  phone?: string
  address?: string
  serviceId?: string
  isConfirmed: boolean
  isActive: boolean
}

export type StaffProfileUpdateInput = {
  fullName: string
  email: string
  gender?: string
  phone?: string
  address?: string
}
