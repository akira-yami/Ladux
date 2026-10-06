import laduxLogo from '../../assets/ladux-logo-sky-v2.png';

export default function BrandMark() {
    return (
        <svg
            role="img"
            aria-label="LADUX"
            viewBox="256 145 1660 434"
            width={1660}
            height={434}
            className="block h-auto w-32 shrink-0 overflow-hidden sm:w-44"
        >
            <image href={laduxLogo} width={2172} height={724} />
        </svg>
    );
}
