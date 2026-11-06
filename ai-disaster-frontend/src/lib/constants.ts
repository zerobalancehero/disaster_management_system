import type { DisasterType, Severity, UserRole } from '../types'

export const DEFAULT_COORDINATES = {
  latitude: 13.0827,
  longitude: 80.2707,
  label: 'Chennai Central Operations Area',
}

export const CHENNAI_LOCATION_PRESETS = [
  { name: 'Central Command', lat: 13.0827, lon: 80.2707 },
  { name: 'Velachery Basin', lat: 12.9784, lon: 80.2185 },
  { name: 'Marina Coastal Strip', lat: 13.0500, lon: 80.2824 },
  { name: 'Tambaram South Zone', lat: 12.9249, lon: 80.1000 },
  { name: 'Ennore Port Sector', lat: 13.2000, lon: 80.3200 },
]

export const SEVERITY_CONFIG: Record<
  Severity,
  { label: string; color: string; bg: string; border: string; text: string; glow: string }
> = {
  CRITICAL: {
    label: 'Critical / Life Threat',
    color: '#DC2626',
    bg: 'bg-red-600/20',
    border: 'border-red-600/60',
    text: 'text-red-300',
    glow: 'shadow-[0_0_15px_rgba(220,38,38,0.45)]',
  },
  SEVERE: {
    label: 'Critical / Severe',
    color: '#EF4444',
    bg: 'bg-red-500/15',
    border: 'border-red-500/50',
    text: 'text-red-400',
    glow: 'shadow-[0_0_15px_rgba(239,68,68,0.35)]',
  },
  HIGH: {
    label: 'High Urgency',
    color: '#F97316',
    bg: 'bg-orange-500/15',
    border: 'border-orange-500/50',
    text: 'text-orange-400',
    glow: 'shadow-[0_0_15px_rgba(249,115,22,0.35)]',
  },
  MEDIUM: {
    label: 'Medium Urgency',
    color: '#F59E0B',
    bg: 'bg-amber-500/15',
    border: 'border-amber-500/50',
    text: 'text-amber-300',
    glow: 'shadow-[0_0_15px_rgba(245,158,11,0.25)]',
  },
  MODERATE: {
    label: 'Moderate',
    color: '#F59E0B',
    bg: 'bg-amber-500/15',
    border: 'border-amber-500/50',
    text: 'text-amber-300',
    glow: 'shadow-[0_0_15px_rgba(245,158,11,0.25)]',
  },
  LOW: {
    label: 'Low / Advisory',
    color: '#38BDF8',
    bg: 'bg-sky-500/15',
    border: 'border-sky-500/40',
    text: 'text-sky-300',
    glow: 'shadow-[0_0_15px_rgba(56,189,248,0.2)]',
  },
}

export const DISASTER_TYPES_CONFIG: Record<
  DisasterType,
  { label: string; icon: string; color: string; badgeClass: string }
> = {
  FLOOD: {
    label: 'Flash Flood & Inundation',
    icon: 'Waves',
    color: '#38BDF8',
    badgeClass: 'bg-blue-500/20 text-blue-300 border-blue-500/40',
  },
  CYCLONE: {
    label: 'Tropical Cyclone / Hurricane',
    icon: 'Wind',
    color: '#818CF8',
    badgeClass: 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40',
  },
  EARTHQUAKE: {
    label: 'Seismic Shock / Earthquake',
    icon: 'Activity',
    color: '#F43F5E',
    badgeClass: 'bg-rose-500/20 text-rose-300 border-rose-500/40',
  },
  LANDSLIDE: {
    label: 'Landslide / Mudflow',
    icon: 'Mountain',
    color: '#EAB308',
    badgeClass: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/40',
  },
  WILDFIRE: {
    label: 'Wildfire & Bushfire',
    icon: 'Flame',
    color: '#F97316',
    badgeClass: 'bg-orange-500/20 text-orange-300 border-orange-500/40',
  },
  GENERAL_EMERGENCY: {
    label: 'General Emergency',
    icon: 'AlertTriangle',
    color: '#A855F7',
    badgeClass: 'bg-purple-500/20 text-purple-300 border-purple-500/40',
  },
}

export const ROLE_HOMEPATH: Record<UserRole, string> = {
  USER: '/user',
  RESPONDER: '/responder',
  ADMIN: '/admin',
}
