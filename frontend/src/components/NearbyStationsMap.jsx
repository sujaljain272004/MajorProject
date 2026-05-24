function mapUrl(center) {
  const delta = 0.06;
  const bbox = [
    center.longitude - delta,
    center.latitude - delta,
    center.longitude + delta,
    center.latitude + delta
  ].join("%2C");
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${center.latitude}%2C${center.longitude}`;
}

export default function NearbyStationsMap({ center, stations }) {
  if (!center) {
    return <div className="empty-state map-empty">Share GPS or search a city to focus the nearby map.</div>;
  }

  return (
    <section className="detail-panel station-map-panel">
      <div className="section-heading">
        <h2>Nearby map</h2>
        <span className="badge">{stations.length} results</span>
      </div>
      <iframe
        className="osm-frame"
        title="Nearby ChargeUp stations"
        src={mapUrl(center)}
        loading="lazy"
      />
      <div className="map-result-strip">
        {stations.slice(0, 4).map((station) => (
          <a
            key={station.id}
            href={`https://www.openstreetmap.org/directions?to=${station.latitude}%2C${station.longitude}`}
            rel="noreferrer"
            target="_blank"
          >
            <strong>{station.name}</strong>
            <span>{station.distanceKm == null ? station.city : `${station.distanceKm} km`}</span>
          </a>
        ))}
      </div>
    </section>
  );
}
