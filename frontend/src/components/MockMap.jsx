export default function MockMap({ station }) {
  return (
    <div className="mock-map">
      <div className="map-marker">
        <span>{station.name}</span>
      </div>
      <div className="map-coordinates">
        <p>Mock map preview</p>
        <strong>{station.latitude}, {station.longitude}</strong>
      </div>
    </div>
  );
}
