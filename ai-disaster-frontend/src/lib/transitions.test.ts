import { describe, it, expect } from 'vitest'
import { canTransition } from './transitions'

describe('assignment transitions', () => {
  it('allows the forward path', () => {
    expect(canTransition('APPROVED', 'EN_ROUTE')).toBe(true)
    expect(canTransition('ON_SCENE', 'RESOLVED')).toBe(true)
  })
  it('blocks skipping and going back', () => {
    expect(canTransition('APPROVED', 'RESOLVED')).toBe(false)
    expect(canTransition('RESOLVED', 'EN_ROUTE')).toBe(false)
  })
})
