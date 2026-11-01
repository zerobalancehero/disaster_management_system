import type { GeoPointDto } from './api.types'

export type WeatherCondition =
  | 'CLEAR'
  | 'CLOUDY'
  | 'OVERCAST'
  | 'LIGHT_RAIN'
  | 'HEAVY_RAIN'
  | 'THUNDERSTORM'
  | 'TORNADO'
  | 'HEATWAVE'
  | 'FOG'

export interface CurrentWeatherResponse {
  source: string
  location: GeoPointDto
  observationTime: string
  temperatureCelsius: number
  humidityPercent: number
  rainfallMm: number
  windSpeedKmh: number
  weatherCondition: WeatherCondition
  isStale: boolean
  freshnessNote: string
}

export interface WeatherForecastResponse {
  id?: string
  source?: string
  coordinates: GeoPointDto
  regionId?: string
  forecastIssueTime: string
  forecastTargetTime: string
  temperatureCelsius: number
  rainfallMm: number
  windSpeedKmh: number
  weatherCondition: WeatherCondition
}
