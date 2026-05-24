export default function MockMap({ station }) {
  const mapUrl = `https://maps.google.com/maps?q=${station.latitude},${station.longitude}&z=15&output=embed`;

  return (
    <div className="mock-map" style={{ padding: 0, overflow: 'hidden' }}>
      <iframe
        title={`Map for ${station.name}`}
        width="100%"
        height="100%"
        style={{ border: 0, minHeight: '250px' }}
        src={mapUrl}
        allowFullScreen
        loading="lazy"
        referrerPolicy="no-referrer-when-downgrade"
      ></iframe>
    </div>
  );
}
