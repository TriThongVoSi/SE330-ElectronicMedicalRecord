import type { UserRole } from '../core/types/common'
import type { Appointment } from '../features/appointments/types'
import type { Drug } from '../features/drugs/types'
import type { Patient } from '../features/patients/types'
import type { Prescription } from '../features/prescriptions/types'
import type { Service } from '../features/services/types'
import type { StaffMember } from '../features/staff/types'

type AuthAccount = {
  id: string
  username: string
  password: string
  fullName: string
  email: string
  role: UserRole
  active: boolean
}

const now = new Date()
const dateOnly = now.toISOString().slice(0, 10)

let idCounter = 100

export const buildId = (prefix: string): string => {
  idCounter += 1
  return `${prefix}-${idCounter}`
}

export const authAccounts: AuthAccount[] = [
  {
    id: '20000000-0000-0000-0000-000000000001',
    username: 'admin.local',
    password: 'admin123',
    fullName: 'Local Admin',
    email: 'admin.local@EMR.dev',
    role: 'ADMIN',
    active: true,
  },
  {
    id: '20000000-0000-0000-0000-000000000002',
    username: 'doctor.local',
    password: 'doctor123',
    fullName: 'Dr. Local Demo',
    email: 'doctor.local@EMR.dev',
    role: 'DOCTOR',
    active: true,
  },
]

export let services: Service[] = [
  {
    id: '10000000-0000-0000-0000-000000000001',
    serviceCode: 'SRV-GEN-CHECK',
    serviceName: 'General Psychological Checkup',
    serviceType: 'EXAMINATION',
    isActive: true,
  },
  {
    id: '10000000-0000-0000-0000-000000000002',
    serviceCode: 'SRV-CBT',
    serviceName: 'Cognitive Behavioral Therapy',
    serviceType: 'EXAMINATION',
    isActive: true,
  },
  {
    id: '10000000-0000-0000-0000-000000000003',
    serviceCode: 'SRV-BLOOD',
    serviceName: 'Basic Blood Test',
    serviceType: 'TEST',
    isActive: true,
  },
]

export let drugs: Drug[] = [
  {
    id: '60000000-0000-0000-0000-000000000001',
    drugCode: 'DRUG-PARACETAMOL-500',
    drugName: 'Paracetamol 500mg',
    manufacturer: 'ABC Pharma',
    expiryDate: '2027-12-31',
    unit: 'TABLET',
    price: 1500,
    stockQuantity: 200,
    isActive: true,
  },
  {
    id: '60000000-0000-0000-0000-000000000002',
    drugCode: 'DRUG-AMOX-250',
    drugName: 'Amoxicillin 250mg',
    manufacturer: 'XYZ Healthcare',
    expiryDate: '2027-06-30',
    unit: 'CAPSULE',
    price: 2500,
    stockQuantity: 120,
    isActive: true,
  },
]

export let patients: Patient[] = [
  {
    id: '30000000-0000-0000-0000-000000000001',
    patientCode: 'PT-0001',
    fullName: 'John Smith',
    dateOfBirth: '1980-04-15',
    email: 'john.smith@EMR.dev',
    gender: 'Male',
    phone: '0902000001',
    address: '123 Main St',
    diagnosis: 'General anxiety',
    drugAllergies: 'Penicillin',
    heightCm: 178,
    weightKg: 74.5,
    createdAt: `${dateOnly}T08:00:00.000Z`,
    updatedAt: `${dateOnly}T08:00:00.000Z`,
  },
  {
    id: '30000000-0000-0000-0000-000000000002',
    patientCode: 'PT-0002',
    fullName: 'Emily Johnson',
    dateOfBirth: '1990-07-22',
    email: 'emily.johnson@EMR.dev',
    gender: 'Female',
    phone: '0902000002',
    address: '456 Park Ave',
    diagnosis: 'Sleep disorder',
    drugAllergies: 'Pollen extract',
    heightCm: 165.5,
    weightKg: 60.2,
    createdAt: `${dateOnly}T08:30:00.000Z`,
    updatedAt: `${dateOnly}T08:30:00.000Z`,
  },
]

export let staffMembers: StaffMember[] = [
  {
    id: '20000000-0000-0000-0000-000000000001',
    fullName: 'Local Admin',
    email: 'admin.local@EMR.dev',
    role: 'ADMIN',
    phone: '0901000001',
  },
  {
    id: '20000000-0000-0000-0000-000000000002',
    fullName: 'Dr. Local Demo',
    email: 'doctor.local@EMR.dev',
    role: 'DOCTOR',
    phone: '0901000002',
  },
]

export let appointments: Appointment[] = [
  {
    id: '40000000-0000-0000-0000-000000000001',
    appointmentCode: 'AP-20260401-0001',
    appointmentTime: `${dateOnly}T09:00:00.000Z`,
    status: 'COMING',
    doctorId: '20000000-0000-0000-0000-000000000002',
    doctorName: 'Dr. Local Demo',
    patientId: '30000000-0000-0000-0000-000000000001',
    patientName: 'John Smith',
    urgencyLevel: 2,
    prescriptionStatus: 'NONE',
    isFollowup: false,
    priorityScore: 8,
    createdAt: `${dateOnly}T08:00:00.000Z`,
    updatedAt: `${dateOnly}T08:00:00.000Z`,
  },
  {
    id: '40000000-0000-0000-0000-000000000002',
    appointmentCode: 'AP-20260401-0002',
    appointmentTime: `${dateOnly}T14:30:00.000Z`,
    status: 'FINISH',
    doctorId: '20000000-0000-0000-0000-000000000002',
    doctorName: 'Dr. Local Demo',
    patientId: '30000000-0000-0000-0000-000000000002',
    patientName: 'Emily Johnson',
    urgencyLevel: 1,
    prescriptionStatus: 'CREATED',
    isFollowup: false,
    priorityScore: 6,
    createdAt: `${dateOnly}T09:00:00.000Z`,
    updatedAt: `${dateOnly}T09:00:00.000Z`,
  },
]

export let prescriptions: Prescription[] = [
  {
    id: '70000000-0000-0000-0000-000000000001',
    prescriptionCode: 'RX-20260401-0001',
    patientId: '30000000-0000-0000-0000-000000000002',
    patientName: 'Emily Johnson',
    doctorId: '20000000-0000-0000-0000-000000000002',
    doctorName: 'Dr. Local Demo',
    appointmentId: '40000000-0000-0000-0000-000000000002',
    status: 'CREATED',
    diagnosis: 'Mild respiratory infection',
    advice: 'Take medicines after meals and return in 3 days if symptoms persist.',
    items: [
      {
        drugId: '60000000-0000-0000-0000-000000000001',
        drugName: 'Paracetamol 500mg',
        quantity: 2,
        instructions: 'Take 1 tablet twice daily after meals.',
      },
      {
        drugId: '60000000-0000-0000-0000-000000000002',
        drugName: 'Amoxicillin 250mg',
        quantity: 2,
        instructions: 'Take 1 capsule twice daily for 5 days.',
      },
    ],
    createdAt: `${dateOnly}T12:00:00.000Z`,
  },
]

export const replaceCollection = <T>(
  collectionName:
    | 'patients'
    | 'appointments'
    | 'services'
    | 'drugs'
    | 'prescriptions'
    | 'staffMembers',
  value: T[]
): void => {
  if (collectionName === 'patients') {
    patients = value as Patient[]
    return
  }
  if (collectionName === 'appointments') {
    appointments = value as Appointment[]
    return
  }
  if (collectionName === 'services') {
    services = value as Service[]
    return
  }
  if (collectionName === 'drugs') {
    drugs = value as Drug[]
    return
  }
  if (collectionName === 'prescriptions') {
    prescriptions = value as Prescription[]
    return
  }

  staffMembers = value as StaffMember[]
}

export const findStaffDisplayName = (staffId: string): string => {
  return staffMembers.find((member) => member.id === staffId)?.fullName ?? 'Unknown staff'
}

export const findPatientDisplayName = (patientId: string): string => {
  return patients.find((patient) => patient.id === patientId)?.fullName ?? 'Unknown patient'
}

export const findDrugDisplayName = (drugId: string): string => {
  return drugs.find((drug) => drug.id === drugId)?.drugName ?? 'Unknown drug'
}
