import { useCallback, useEffect, useState } from 'react';

// Runs an async loader on mount and whenever `deps` change; exposes a `reload`
// function so views can refresh themselves after a mutation (create/update/delete).
export function useAsync(loader, deps) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(() => {
    setLoading(true);
    setError(null);
    loader()
      .then((result) => setData(result))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, loading, reload };
}
