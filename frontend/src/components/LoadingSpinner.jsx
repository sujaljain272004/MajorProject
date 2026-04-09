export default function LoadingSpinner({ label = "Loading..." }) {
  return (
    <div className="loading-box">
      <div className="spinner" />
      <p>{label}</p>
    </div>
  );
}
