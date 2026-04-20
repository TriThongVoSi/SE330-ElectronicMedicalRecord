import type { ApiError } from '../core/api/errors'
import { buildPagedResult, defaultPageSize } from '../core/utils/pagination'
import type { PagedResult } from '../core/types/common'
import type {
  Appointment,
  AppointmentListQuery,
  AppointmentUpsertInput,
} from '../features/appointments/types'
import type { DashboardSummary } from '../features/dashboard/types'
import type { Drug, DrugListQuery, DrugUpsertInput } from '../features/drugs/types'
import type {
  AuthTokenResponse,
  ForgotPasswordPayload,
  ForgotPasswordVerifyOtpResponse,
  LoginPayload,
  OtpChallengeResponse,
  ResetPasswordPayload,
  ResetPasswordResponse,
  SignUpPayload,
  SignUpVerifyOtpResponse,
  VerifyOtpPayload,
} from '../features/auth/types'
import type { Patient, PatientListQuery, PatientUpsertInput } from '../features/patients/types'
import type {
  Prescription,
  PrescriptionItem,
  PrescriptionListQuery,
  PrescriptionUpsertInput,
} from '../features/prescriptions/types'
import type { Service, ServiceListQuery, ServiceUpsertInput } from '../features/services/types'
import type { StaffMember } from '../features/staff/types'
import {
  appointments,
  authAccounts,
  drugs,
  findDrugDisplayName,
  findPatientDisplayName,
  findStaffDisplayName,
  patients,
  prescriptions,
  replaceCollection,
  services,
  staffMembers,
  buildId,
} from './db'

type RefreshTokenIndex = Record<string, string>

const refreshTokenIndex: RefreshTokenIndex = {}
const signUpOtpIndex: Record<string, { otp: string; payload: SignUpPayload }> = {}
const passwordResetOtpIndex: Record<string, { otp: string; accountId: string }> = {}
const passwordResetTokenIndex: Record<string, string> = {}

const defaultOtpCode = '123456'

const artificialDelay = async (): Promise<void> => {
  await new Promise((resolve) => {
    window.setTimeout(resolve, 120)
  })
}

const createMockError = (message: string, status: number): ApiError => ({
  message,
  status,
})

const nowIso = (): string => new Date().toISOString()

const normalizedSearch = (value: string | undefined): string => (value ?? '').trim().toLowerCase()

const pickPage = (queryPage: number | undefined): number => {
  if (typeof queryPage !== 'number' || Number.isNaN(queryPage) || queryPage < 1) {
    return 1
  }
  return queryPage
}

const pickSize = (querySize: number | undefined): number => {
  if (typeof querySize !== 'number' || Number.isNaN(querySize) || querySize < 1) {
    return defaultPageSize
  }
  return querySize
}

const authUserFromAccount = (
  account: (typeof authAccounts)[number]
): AuthTokenResponse['user'] => ({
  id: account.id,
  username: account.username,
  fullName: account.fullName,
  email: account.email,
  role: account.role,
  mustChangePassword: false,
})

const toMaskedEmail = (email: string): string => {
  const [local, domain] = email.split('@')
  if (!local || !domain) {
    return ''
  }
  if (local.length <= 2) {
    return `*@${domain}`
  }
  return `${local[0]}***${local[local.length - 1]}@${domain}`
}

const issueSessionForAccount = (account: (typeof authAccounts)[number]): AuthTokenResponse => {
  const accessToken = `mock-access::${account.id}::${Date.now()}`
  const refreshToken = `mock-refresh::${account.id}::${Date.now()}`
  refreshTokenIndex[refreshToken] = account.id

  return {
    accessToken,
    refreshToken,
    user: authUserFromAccount(account),
  }
}

const resolveUserFromAccessToken = (accessToken: string): AuthTokenResponse['user'] | null => {
  if (!accessToken.startsWith('mock-access::')) {
    return null
  }

  const tokenParts = accessToken.split('::')
  if (tokenParts.length < 3) {
    return null
  }

  const accountId = tokenParts[1]
  const account = authAccounts.find((item) => item.id === accountId)
  return account ? authUserFromAccount(account) : null
}

const normalizePrescriptionItems = (items: PrescriptionItem[]): PrescriptionItem[] =>
  items.map((item) => ({
    ...item,
    drugName: item.drugName ?? findDrugDisplayName(item.drugId),
    quantity: Number(item.quantity),
  }))

export const mockApi = {
  async login(payload: LoginPayload): Promise<AuthTokenResponse> {
    await artificialDelay()
    const identifier = payload.identifier.trim().toLowerCase()
    const account = authAccounts.find(
      (item) =>
        (item.username.toLowerCase() === identifier || item.email.toLowerCase() === identifier) &&
        item.password === payload.password &&
        item.active
    )

    if (!account) {
      throw createMockError('Invalid username or password.', 401)
    }

    return issueSessionForAccount(account)
  },

  async refresh(refreshToken: string): Promise<AuthTokenResponse> {
    await artificialDelay()
    const accountId = refreshTokenIndex[refreshToken]
    if (!accountId) {
      throw createMockError('Refresh token expired.', 401)
    }

    const account = authAccounts.find((item) => item.id === accountId)
    if (!account) {
      throw createMockError('Account not found for refresh token.', 401)
    }

    return issueSessionForAccount(account)
  },

  async logout(refreshToken: string | null): Promise<void> {
    await artificialDelay()
    if (refreshToken) {
      delete refreshTokenIndex[refreshToken]
    }
  },

  async me(accessToken: string | null): Promise<AuthTokenResponse['user']> {
    await artificialDelay()

    if (!accessToken) {
      throw createMockError('Not authenticated.', 401)
    }

    const user = resolveUserFromAccessToken(accessToken)
    if (!user) {
      throw createMockError('Session is invalid.', 401)
    }

    return user
  },

  async signUp(payload: SignUpPayload): Promise<OtpChallengeResponse> {
    await artificialDelay()
    const username = payload.username.trim()
    const email = payload.email.trim().toLowerCase()

    if (authAccounts.some((account) => account.email.toLowerCase() === email)) {
      throw createMockError('Email already exists.', 409)
    }
    if (authAccounts.some((account) => account.username.toLowerCase() === username.toLowerCase())) {
      throw createMockError('Username already exists.', 409)
    }

    signUpOtpIndex[email] = {
      otp: defaultOtpCode,
      payload: {
        ...payload,
        username,
        email,
      },
    }

    return {
      message: 'OTP sent.',
      nextStep: 'VERIFY_OTP',
      emailMasked: toMaskedEmail(email),
      expiresInSeconds: 300,
    }
  },

  async verifySignUpOtp(payload: VerifyOtpPayload): Promise<SignUpVerifyOtpResponse> {
    await artificialDelay()
    const email = payload.email.trim().toLowerCase()
    const challenge = signUpOtpIndex[email]
    if (!challenge) {
      throw createMockError('OTP is invalid or unavailable.', 400)
    }
    if (payload.otp !== challenge.otp) {
      throw createMockError('OTP is invalid.', 400)
    }

    const role: AuthTokenResponse['user']['role'] =
      challenge.payload.role === 'ADMIN' ? 'ADMIN' : 'DOCTOR'
    const account = {
      id: buildId('auth'),
      username: challenge.payload.username.trim(),
      password: challenge.payload.password,
      fullName: challenge.payload.fullName?.trim() || challenge.payload.username.trim(),
      email,
      role,
      active: true,
    }

    authAccounts.push(account)

    if (role === 'DOCTOR') {
      replaceCollection('staffMembers', [
        ...staffMembers,
        {
          id: account.id,
          fullName: account.fullName,
          email: account.email,
          role: 'DOCTOR',
          phone: null,
        },
      ])
    }

    delete signUpOtpIndex[email]

    return {
      message: 'Verified',
      user: authUserFromAccount(account),
    }
  },

  async requestPasswordReset(payload: ForgotPasswordPayload): Promise<OtpChallengeResponse> {
    await artificialDelay()
    const email = payload.email.trim().toLowerCase()
    const account = authAccounts.find((item) => item.email.toLowerCase() === email && item.active)

    if (account) {
      passwordResetOtpIndex[email] = { otp: defaultOtpCode, accountId: account.id }
    }

    return {
      message: 'If account exists, OTP has been sent.',
      nextStep: 'VERIFY_OTP',
      emailMasked: account ? toMaskedEmail(email) : '',
      expiresInSeconds: 300,
    }
  },

  async verifyForgotPasswordOtp(
    payload: VerifyOtpPayload
  ): Promise<ForgotPasswordVerifyOtpResponse> {
    await artificialDelay()
    const email = payload.email.trim().toLowerCase()
    const challenge = passwordResetOtpIndex[email]
    if (!challenge || payload.otp !== challenge.otp) {
      throw createMockError('OTP is invalid.', 400)
    }

    const tempResetToken = `mock-reset::${challenge.accountId}::${Date.now()}`
    passwordResetTokenIndex[tempResetToken] = challenge.accountId
    delete passwordResetOtpIndex[email]

    return {
      tempResetToken,
      expiresInSeconds: 600,
    }
  },

  async resetPassword(payload: ResetPasswordPayload): Promise<ResetPasswordResponse> {
    await artificialDelay()
    const accountId = passwordResetTokenIndex[payload.tempResetToken]
    if (!accountId) {
      throw createMockError('Reset token is invalid.', 401)
    }

    const account = authAccounts.find((item) => item.id === accountId)
    if (!account) {
      throw createMockError('Reset token is invalid.', 401)
    }

    account.password = payload.newPassword
    delete passwordResetTokenIndex[payload.tempResetToken]

    return {
      message: 'Password updated successfully.',
    }
  },

  async dashboardSummary(): Promise<DashboardSummary> {
    await artificialDelay()
    const today = new Date().toISOString().slice(0, 10)
    const todayAppointments = appointments.filter((appointment) =>
      appointment.appointmentTime.startsWith(today)
    )
    const finishedToday = todayAppointments.filter((item) => item.status === 'FINISH')
    const cancelledToday = todayAppointments.filter((item) => item.status === 'CANCEL')
    const comingToday = todayAppointments.filter((item) => item.status === 'COMING')

    return {
      totalAppointmentsToday: todayAppointments.length,
      comingAppointmentsToday: comingToday.length,
      finishedAppointmentsToday: finishedToday.length,
      cancelledAppointmentsToday: cancelledToday.length,
      newPatientsToday: patients.filter((patient) => patient.createdAt?.startsWith(today)).length,
      upcomingAppointments: appointments
        .slice()
        .sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime))
        .slice(0, 5)
        .map((item) => ({
          id: item.id,
          appointmentTime: item.appointmentTime,
          patientName: item.patientName ?? findPatientDisplayName(item.patientId),
          doctorName: item.doctorName ?? findStaffDisplayName(item.doctorId),
          status: item.status,
        })),
    }
  },

  async listPatients(query: PatientListQuery): Promise<PagedResult<Patient>> {
    await artificialDelay()
    const page = pickPage(query.page)
    const size = pickSize(query.size)
    const search = normalizedSearch(query.search)
    const phone = normalizedSearch(query.phone)
    const code = normalizedSearch(query.code)

    const filtered = patients.filter((item) => {
      const searchable = `${item.patientCode} ${item.fullName} ${item.phone}`.toLowerCase()
      if (search && !searchable.includes(search)) {
        return false
      }
      if (phone && !item.phone.toLowerCase().includes(phone)) {
        return false
      }
      if (code && !item.patientCode.toLowerCase().includes(code)) {
        return false
      }
      return true
    })

    return buildPagedResult(filtered, page, size)
  },

  async getPatient(id: string): Promise<Patient> {
    await artificialDelay()
    const record = patients.find((item) => item.id === id)
    if (!record) {
      throw createMockError('Patient not found.', 404)
    }
    return record
  },

  async createPatient(input: PatientUpsertInput): Promise<Patient> {
    await artificialDelay()
    const duplicatedCode = patients.some((item) => item.patientCode === input.patientCode)
    if (duplicatedCode) {
      throw createMockError('Patient code already exists.', 409)
    }

    const record: Patient = {
      id: buildId('patient'),
      ...input,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }

    replaceCollection('patients', [record, ...patients])
    return record
  },

  async updatePatient(id: string, input: PatientUpsertInput): Promise<Patient> {
    await artificialDelay()
    const target = patients.find((item) => item.id === id)
    if (!target) {
      throw createMockError('Patient not found.', 404)
    }

    if (patients.some((item) => item.id !== id && item.patientCode === input.patientCode)) {
      throw createMockError('Patient code already exists.', 409)
    }

    const updated: Patient = {
      ...target,
      ...input,
      updatedAt: nowIso(),
    }

    replaceCollection(
      'patients',
      patients.map((item) => (item.id === id ? updated : item))
    )
    return updated
  },

  async listAppointments(query: AppointmentListQuery): Promise<PagedResult<Appointment>> {
    await artificialDelay()
    const page = pickPage(query.page)
    const size = pickSize(query.size)
    const search = normalizedSearch(query.search)

    const filtered = appointments.filter((item) => {
      const matchesSearch =
        !search ||
        `${item.appointmentCode} ${item.patientName ?? ''} ${item.doctorName ?? ''}`
          .toLowerCase()
          .includes(search)

      const matchesStatus = !query.status || item.status === query.status
      const matchesDoctor = !query.doctorId || item.doctorId === query.doctorId
      const matchesPatient = !query.patientId || item.patientId === query.patientId
      const matchesDate = !query.date || item.appointmentTime.startsWith(query.date)

      return matchesSearch && matchesStatus && matchesDoctor && matchesPatient && matchesDate
    })

    return buildPagedResult(filtered, page, size)
  },

  async createAppointment(input: AppointmentUpsertInput): Promise<Appointment> {
    await artificialDelay()

    const patient = patients.find((item) => item.id === input.patientId)
    const doctor = staffMembers.find((item) => item.id === input.doctorId)

    if (!patient || !doctor) {
      throw createMockError('Doctor or patient does not exist.', 400)
    }

    const conflicted = appointments.some(
      (item) => item.doctorId === input.doctorId && item.appointmentTime === input.appointmentTime
    )

    if (conflicted) {
      throw createMockError('Doctor already has another appointment at selected time.', 409)
    }

    const record: Appointment = {
      id: buildId('appointment'),
      ...input,
      doctorName: doctor.fullName,
      patientName: patient.fullName,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }

    replaceCollection('appointments', [record, ...appointments])
    return record
  },

  async updateAppointment(id: string, input: AppointmentUpsertInput): Promise<Appointment> {
    await artificialDelay()
    const current = appointments.find((item) => item.id === id)
    if (!current) {
      throw createMockError('Appointment not found.', 404)
    }

    const conflicted = appointments.some(
      (item) =>
        item.id !== id &&
        item.doctorId === input.doctorId &&
        item.appointmentTime === input.appointmentTime
    )

    if (conflicted) {
      throw createMockError('Doctor already has another appointment at selected time.', 409)
    }

    const updated: Appointment = {
      ...current,
      ...input,
      doctorName: findStaffDisplayName(input.doctorId),
      patientName: findPatientDisplayName(input.patientId),
      updatedAt: nowIso(),
    }

    replaceCollection(
      'appointments',
      appointments.map((item) => (item.id === id ? updated : item))
    )
    return updated
  },

  async getAppointment(id: string): Promise<Appointment> {
    await artificialDelay()
    const record = appointments.find((item) => item.id === id)
    if (!record) {
      throw createMockError('Appointment not found.', 404)
    }
    return record
  },

  async listServices(query: ServiceListQuery): Promise<PagedResult<Service>> {
    await artificialDelay()
    const page = pickPage(query.page)
    const size = pickSize(query.size)
    const search = normalizedSearch(query.search)

    const filtered = services.filter((item) => {
      const matchesSearch =
        !search ||
        `${item.serviceCode} ${item.serviceName} ${item.serviceType}`.toLowerCase().includes(search)
      const matchesActive =
        typeof query.isActive === 'boolean' ? item.isActive === query.isActive : true
      return matchesSearch && matchesActive
    })

    return buildPagedResult(filtered, page, size)
  },

  async createService(input: ServiceUpsertInput): Promise<Service> {
    await artificialDelay()
    if (services.some((item) => item.serviceCode === input.serviceCode)) {
      throw createMockError('Service code already exists.', 409)
    }

    const record: Service = {
      id: buildId('service'),
      ...input,
    }

    replaceCollection('services', [record, ...services])
    return record
  },

  async updateService(id: string, input: ServiceUpsertInput): Promise<Service> {
    await artificialDelay()
    const current = services.find((item) => item.id === id)
    if (!current) {
      throw createMockError('Service not found.', 404)
    }

    if (services.some((item) => item.id !== id && item.serviceCode === input.serviceCode)) {
      throw createMockError('Service code already exists.', 409)
    }

    const updated: Service = { ...current, ...input }
    replaceCollection(
      'services',
      services.map((item) => (item.id === id ? updated : item))
    )
    return updated
  },

  async listDrugs(query: DrugListQuery): Promise<PagedResult<Drug>> {
    await artificialDelay()
    const page = pickPage(query.page)
    const size = pickSize(query.size)
    const search = normalizedSearch(query.search)

    const filtered = drugs.filter((item) => {
      const matchesSearch =
        !search ||
        `${item.drugCode} ${item.drugName} ${item.manufacturer} ${item.unit}`
          .toLowerCase()
          .includes(search)
      const matchesActive =
        typeof query.isActive === 'boolean' ? item.isActive === query.isActive : true
      return matchesSearch && matchesActive
    })

    return buildPagedResult(filtered, page, size)
  },

  async createDrug(input: DrugUpsertInput): Promise<Drug> {
    await artificialDelay()
    if (drugs.some((item) => item.drugCode === input.drugCode)) {
      throw createMockError('Drug code already exists.', 409)
    }

    const record: Drug = {
      id: buildId('drug'),
      ...input,
    }

    replaceCollection('drugs', [record, ...drugs])
    return record
  },

  async updateDrug(id: string, input: DrugUpsertInput): Promise<Drug> {
    await artificialDelay()
    const current = drugs.find((item) => item.id === id)
    if (!current) {
      throw createMockError('Drug not found.', 404)
    }

    if (drugs.some((item) => item.id !== id && item.drugCode === input.drugCode)) {
      throw createMockError('Drug code already exists.', 409)
    }

    const updated: Drug = { ...current, ...input }
    replaceCollection(
      'drugs',
      drugs.map((item) => (item.id === id ? updated : item))
    )
    return updated
  },

  async listPrescriptions(query: PrescriptionListQuery): Promise<PagedResult<Prescription>> {
    await artificialDelay()
    const page = pickPage(query.page)
    const size = pickSize(query.size)
    const search = normalizedSearch(query.search)

    const filtered = prescriptions.filter((item) => {
      const matchesSearch =
        !search ||
        `${item.prescriptionCode} ${item.patientName ?? ''} ${item.doctorName ?? ''}`
          .toLowerCase()
          .includes(search)
      const matchesStatus = !query.status || item.status === query.status
      const matchesPatient = !query.patientId || item.patientId === query.patientId
      return matchesSearch && matchesStatus && matchesPatient
    })

    return buildPagedResult(filtered, page, size)
  },

  async getPrescription(id: string): Promise<Prescription> {
    await artificialDelay()
    const record = prescriptions.find((item) => item.id === id)
    if (!record) {
      throw createMockError('Prescription not found.', 404)
    }
    return record
  },

  async createPrescription(input: PrescriptionUpsertInput): Promise<Prescription> {
    await artificialDelay()
    if (prescriptions.some((item) => item.prescriptionCode === input.prescriptionCode)) {
      throw createMockError('Prescription code already exists.', 409)
    }

    const normalizedItems = normalizePrescriptionItems(input.items)
    const record: Prescription = {
      id: buildId('prescription'),
      ...input,
      patientName: findPatientDisplayName(input.patientId),
      doctorName: findStaffDisplayName(input.doctorId),
      items: normalizedItems,
      createdAt: nowIso(),
    }

    replaceCollection('prescriptions', [record, ...prescriptions])
    return record
  },

  async updatePrescription(id: string, input: PrescriptionUpsertInput): Promise<Prescription> {
    await artificialDelay()
    const current = prescriptions.find((item) => item.id === id)
    if (!current) {
      throw createMockError('Prescription not found.', 404)
    }

    if (
      prescriptions.some(
        (item) => item.id !== id && item.prescriptionCode === input.prescriptionCode
      )
    ) {
      throw createMockError('Prescription code already exists.', 409)
    }

    const normalizedItems = normalizePrescriptionItems(input.items)
    const updated: Prescription = {
      ...current,
      ...input,
      patientName: findPatientDisplayName(input.patientId),
      doctorName: findStaffDisplayName(input.doctorId),
      items: normalizedItems,
      createdAt: current.createdAt,
    }

    replaceCollection(
      'prescriptions',
      prescriptions.map((item) => (item.id === id ? updated : item))
    )
    return updated
  },

  async listDoctors(): Promise<StaffMember[]> {
    await artificialDelay()
    return staffMembers.filter((item) => item.role === 'DOCTOR')
  },

  async lookupPatients(): Promise<Patient[]> {
    await artificialDelay()
    return patients
  },

  async lookupDrugs(): Promise<Drug[]> {
    await artificialDelay()
    return drugs
  },

  async lookupAppointments(): Promise<Appointment[]> {
    await artificialDelay()
    return appointments
  },
}
