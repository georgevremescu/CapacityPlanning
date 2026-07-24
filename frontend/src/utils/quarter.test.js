import { describe, expect, it } from 'vitest';
import { currentQuarter, nearbyQuarters, quarterOfDateString, quartersFrom } from './quarter.js';

describe('currentQuarter', () => {
  it('maps each month to the right quarter', () => {
    expect(currentQuarter(new Date(2026, 0, 1))).toBe('2026-Q1');
    expect(currentQuarter(new Date(2026, 2, 31))).toBe('2026-Q1');
    expect(currentQuarter(new Date(2026, 3, 1))).toBe('2026-Q2');
    expect(currentQuarter(new Date(2026, 6, 23))).toBe('2026-Q3');
    expect(currentQuarter(new Date(2026, 11, 31))).toBe('2026-Q4');
  });
});

describe('quarterOfDateString', () => {
  it('parses a YYYY-MM-DD string into its quarter', () => {
    expect(quarterOfDateString('2026-08-15')).toBe('2026-Q3');
    expect(quarterOfDateString('2026-01-01')).toBe('2026-Q1');
    expect(quarterOfDateString('2026-12-31')).toBe('2026-Q4');
  });

  it('returns null for missing dates', () => {
    expect(quarterOfDateString(null)).toBeNull();
    expect(quarterOfDateString(undefined)).toBeNull();
    expect(quarterOfDateString('')).toBeNull();
  });
});

describe('nearbyQuarters', () => {
  it('generates a window around the current quarter, including year rollover', () => {
    // 2026-Q3, 1 before, 3 after -> Q2'26, Q3'26, Q4'26, Q1'27, Q2'27
    expect(nearbyQuarters(1, 3, new Date(2026, 6, 23))).toEqual([
      '2026-Q2',
      '2026-Q3',
      '2026-Q4',
      '2027-Q1',
      '2027-Q2',
    ]);
  });

  it('rolls backward across a year boundary too', () => {
    // 2026-Q1, 2 before, 0 after -> Q3'25, Q4'25, Q1'26
    expect(nearbyQuarters(2, 0, new Date(2026, 1, 1))).toEqual([
      '2025-Q3',
      '2025-Q4',
      '2026-Q1',
    ]);
  });

  it('returns just the current quarter when before and after are 0', () => {
    expect(nearbyQuarters(0, 0, new Date(2026, 6, 23))).toEqual(['2026-Q3']);
  });
});

describe('quartersFrom', () => {
  it('returns count consecutive quarters starting at the given quarter', () => {
    expect(quartersFrom('2026-Q3', 4)).toEqual(['2026-Q3', '2026-Q4', '2027-Q1', '2027-Q2']);
  });

  it('rolls over the year boundary', () => {
    expect(quartersFrom('2026-Q4', 3)).toEqual(['2026-Q4', '2027-Q1', '2027-Q2']);
  });

  it('returns a single-element array when count is 1', () => {
    expect(quartersFrom('2026-Q1', 1)).toEqual(['2026-Q1']);
  });
});
