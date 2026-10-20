export default function BrandMark({ compact = false }: { compact?: boolean }) {
    return <span className="brand-mark" role="img" aria-label="LADUX">
        <svg viewBox="0 0 40 40" fill="none" aria-hidden="true" className="brand-symbol">
            <rect width="40" height="40" rx="12" fill="#ed651f" />
            <path d="M12 10H19L15.5 25H29L27.5 31H7L12 10Z" fill="white" />
            <path d="M24 10H31L28.5 21H21.5L24 10Z" fill="#ffd2ae" />
        </svg>
        {!compact && <span className="brand-wordmark">ladux<span>.</span></span>}
    </span>;
}
