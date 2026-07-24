export function currentQuarter(date = new Date()) {
  const quarterOfYear = Math.floor(date.getMonth() / 3) + 1;
  return `${date.getFullYear()}-Q${quarterOfYear}`;
}

// dateStr is a 'YYYY-MM-DD' string as returned by the backend for LocalDate fields.
export function quarterOfDateString(dateStr) {
  if (!dateStr) return null;
  const [year, month] = dateStr.split('-').map(Number);
  const quarterOfYear = Math.floor((month - 1) / 3) + 1;
  return `${year}-Q${quarterOfYear}`;
}

// Generates a small window of quarters around the current one, for a dropdown.
export function nearbyQuarters(before = 1, after = 3, date = new Date()) {
  const quarterOfYear = Math.floor(date.getMonth() / 3) + 1;
  const startIndex = date.getFullYear() * 4 + (quarterOfYear - 1) - before;
  const endIndex = date.getFullYear() * 4 + (quarterOfYear - 1) + after;

  const quarters = [];
  for (let index = startIndex; index <= endIndex; index += 1) {
    const year = Math.floor(index / 4);
    const q = (index % 4) + 1;
    quarters.push(`${year}-Q${q}`);
  }
  return quarters;
}

// Returns `count` consecutive quarters starting at quarterStr (e.g. a 4-quarter
// business-plan-year outlook starting from whichever quarter is selected).
export function quartersFrom(quarterStr, count) {
  const [yearPart, qPart] = quarterStr.split('-Q').map(Number);
  const startIndex = yearPart * 4 + (qPart - 1);

  const quarters = [];
  for (let i = 0; i < count; i += 1) {
    const index = startIndex + i;
    const year = Math.floor(index / 4);
    const q = (index % 4) + 1;
    quarters.push(`${year}-Q${q}`);
  }
  return quarters;
}
