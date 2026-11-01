import { api } from './client'
import type { CurrentWeatherResponse, WeatherForecastResponse } from '../types'

export const getCurrentWeather = async (lat: number, lon: number): Promise<CurrentWeatherResponse> => {
  const r = await api.get<CurrentWeatherResponse>('/weather/current', {
    params: { lat, lon },
  })
  return r.data
}

export const getWeatherForecast = async (lat: number, lon: number): Promise<WeatherForecastResponse[]> => {
  const r = await api.get<WeatherForecastResponse[]>('/weather/forecast', {
    params: { lat, lon },
  })
  return Array.isArray(r.data) ? r.data : []
}
