import React from 'react';
import {
  AbsoluteFill,
  Img,
  Interactive,
  staticFile,
  useCurrentFrame,
  type InteractivitySchema,
} from 'remotion';

type MapStickerProps = {
  readonly art: string;
  readonly x: number;
  readonly y: number;
  readonly size: number;
  readonly tilt: number;
  readonly phase: number;
  readonly style?: React.CSSProperties;
};

const MapStickerInner: React.FC<MapStickerProps> = ({art, x, y, size, tilt, phase, style}) => {
  const frame = useCurrentFrame();
  const drift = Math.sin(frame * Math.PI / 90 + phase);
  return (
    <Interactive.Div
      name="Game map sticker"
      style={{
        position: 'absolute',
        left: x,
        top: y,
        width: size,
        height: size,
        translate: `0 ${drift * 8}px`,
        rotate: `${tilt + drift * 1.6}deg`,
        ...style,
      }}
    >
      <Img
        src={staticFile(`map-icons/onboarding_marker_${art}.webp`)}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'contain',
          filter: 'drop-shadow(0 10px 12px rgba(0, 9, 35, 0.58))',
        }}
      />
    </Interactive.Div>
  );
};

const stickerSchema = {
  x: {type: 'number', default: 0, hiddenFromList: false, description: 'Horizontal position'},
  y: {type: 'number', default: 0, hiddenFromList: false, description: 'Vertical position'},
  size: {type: 'number', default: 200, min: 50, max: 400, hiddenFromList: false, description: 'Sticker size'},
  tilt: {type: 'number', default: 0, min: -40, max: 40, hiddenFromList: false, description: 'Resting angle'},
  phase: {type: 'number', default: 0, hiddenFromList: false, description: 'Float timing'},
} as const satisfies InteractivitySchema;

export const MapSticker = Interactive.withSchema({
  Component: MapStickerInner,
  componentName: '<MapSticker>',
  schema: stickerSchema,
  wrapInSequence: true,
});

/** World-specific map symbols for the second Android onboarding page. */
export const IconCollageScene: React.FC = () => (
  <AbsoluteFill style={{overflow: 'hidden', background: 'linear-gradient(#3266c6 0%, #2454ae 33%, #061d50 69%, #02040e 100%)'}}>
    <MapSticker art="food" x={34} y={45} size={170} tilt={-11} phase={0} />
    <MapSticker art="pub" x={270} y={28} size={170} tilt={9} phase={1.7} />
    <MapSticker art="sights" x={512} y={52} size={170} tilt={13} phase={2.6} />
    <MapSticker art="stay" x={32} y={255} size={170} tilt={-15} phase={1.1} />
    <MapSticker art="parking" x={270} y={245} size={170} tilt={-6} phase={3.6} />
    <MapSticker art="cafe" x={510} y={262} size={170} tilt={12} phase={4.1} />
    <MapSticker art="place" x={32} y={465} size={170} tilt={-12} phase={2.3} />
    <MapSticker art="shop" x={270} y={452} size={170} tilt={8} phase={5.1} />
    <MapSticker art="nature" x={510} y={470} size={170} tilt={14} phase={0.8} />
    <MapSticker art="health" x={32} y={675} size={170} tilt={-9} phase={4.4} />
    <MapSticker art="fuel" x={270} y={666} size={170} tilt={8} phase={1.9} />
    <MapSticker art="cash" x={510} y={678} size={170} tilt={-7} phase={3.1} />
  </AbsoluteFill>
);
