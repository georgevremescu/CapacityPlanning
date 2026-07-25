import { useCallback, useEffect, useRef, useState } from 'react';

// Runs an async loader on mount and whenever `deps` change; exposes a `reload`
// function so views can refresh themselves after a mutation (create/update/delete).
export function useAsync(loader, deps) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  // Guards against out-of-order responses: if `deps` change again before an
  // in-flight request resolves, its result is discarded instead of overwriting
  // the newer request's state.
  const latestRequestId = useRef(0);

  const reload = useCallback(() => {
    const requestId = ++latestRequestId.current;
    setLoading(true);
    setError(null);
    loader()
      .then((result) => {
        if (latestRequestId.current === requestId) setData(result);
      })
      .catch((err) => {
        if (latestRequestId.current === requestId) setError(err.message);
      })
      .finally(() => {
        if (latestRequestId.current === requestId) setLoading(false);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, error, loading, reload };
}
