import { useState } from 'react';

// Renders the value for a controlled input/select without ever going
// uncontrolled: null/undefined (e.g. an optional field that's never been set)
// becomes '', matching how the individual forms handled it before.
function displayValue(value) {
  return value ?? '';
}

// Turns a field's raw (string, from the DOM) value back into the type the
// API expects, based on the field descriptor - the one bit of per-field
// logic each hand-rolled form used to duplicate.
function coerce(field, rawValue) {
  if (field.type === 'number') {
    if (rawValue === '') return field.nullable ? null : 0;
    return Number(rawValue);
  }
  if (field.type === 'select' && field.nullable) {
    if (rawValue === '') return null;
    return field.numeric ? Number(rawValue) : rawValue;
  }
  if (field.type === 'date') {
    return rawValue || null;
  }
  return rawValue;
}

// A single generic form for the app's create/edit dialogs (initiatives,
// people, epics), driven by a `fields` descriptor instead of each view
// hand-rolling its own useState/onChange/coercion/markup boilerplate.
//
// Field shape: { name, label, type: 'text'|'textarea'|'number'|'date'|'select',
//   required, min, max, step, rows, options: [{value, label}],
//   nullable (select/number: '' becomes null instead of 0/first option),
//   nullableLabel (placeholder option text), numeric (select: coerce value to Number) }
export default function CrudForm({ fields, initialValues, onSubmit, onCancel, inline = false }) {
  const [values, setValues] = useState(initialValues);

  const update = (name) => (e) =>
    setValues((prev) => ({ ...prev, [name]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    const payload = {};
    for (const field of fields) {
      payload[field.name] = coerce(field, values[field.name]);
    }
    onSubmit(payload);
  };

  return (
    <form className={inline ? 'form form--inline' : 'form'} onSubmit={handleSubmit}>
      {fields.map((field) => (
        <label key={field.name}>
          {field.label}
          {field.type === 'textarea' ? (
            <textarea
              value={displayValue(values[field.name])}
              onChange={update(field.name)}
              rows={field.rows ?? 2}
            />
          ) : field.type === 'select' ? (
            <select value={displayValue(values[field.name])} onChange={update(field.name)}>
              {field.nullable && <option value="">{field.nullableLabel}</option>}
              {field.options.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          ) : (
            <input
              type={field.type ?? 'text'}
              min={field.min}
              max={field.max}
              step={field.step}
              required={field.required}
              value={displayValue(values[field.name])}
              onChange={update(field.name)}
            />
          )}
        </label>
      ))}
      <div className="form-actions">
        <button type="submit">Save</button>
        <button type="button" className="button-secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}
