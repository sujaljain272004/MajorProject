function stationMapUrl(latitude, longitude) {
  const delta = 0.012;
  const bbox = [longitude - delta, latitude - delta, longitude + delta, latitude + delta].join("%2C");
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${latitude}%2C${longitude}`;
}

export default function StationPinPreview({ latitude, longitude }) {
  const hasPin = Number.isFinite(Number(latitude)) && Number.isFinite(Number(longitude));

  if (!hasPin) {
    return <div className="empty-state map-empty">Enter or capture coordinates to preview the exact public station pin.</div>;
  }

  return (
    <iframe
      className="osm-frame pin-preview"
      title="Pinned station location"
      src={stationMapUrl(Number(latitude), Number(longitude))}
      loading="lazy"
    />
  );
}
