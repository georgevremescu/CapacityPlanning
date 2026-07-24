function levelFor(utilization) {
  if (utilization > 1.0) return 'danger';
  if (utilization >= 0.85) return 'warning';
  return 'healthy';
}

export default function UtilizationBar({ utilization, allocatedSp, capacitySp }) {
  const level = levelFor(utilization);
  const widthPercent = Math.min(utilization, 1.5) / 1.5 * 100;

  return (
    <div className="utilization-bar">
      <div className="utilization-bar-track">
        <div
          className={`utilization-bar-fill utilization-bar-fill--${level}`}
          style={{ width: `${widthPercent}%` }}
        />
      </div>
      <div className="utilization-bar-label">
        {allocatedSp.toFixed(0)} / {capacitySp.toFixed(0)} sp &middot;{' '}
        {(utilization * 100).toFixed(0)}%
      </div>
    </div>
  );
}
