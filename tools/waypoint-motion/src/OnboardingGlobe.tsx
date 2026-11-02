import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame} from 'remotion';

/** Silent original Waypoint loop for the first onboarding hero. */
export const OnboardingGlobe: React.FC = () => {
  const frame = useCurrentFrame();
  const phase = (frame / 180) * Math.PI * 2;
  const float = Math.sin(phase) * 11;
  const roll = Math.sin(phase) * 2.6;

  const sticker = (
    artwork: string,
    left: number,
    top: number,
    size: number,
    tilt: number,
    shift: number,
  ) => (
    <Img src={staticFile(artwork)} style={{
      position: 'absolute', left, top: top + Math.sin(phase + shift) * 13,
      width: size, height: size, objectFit: 'contain',
      filter: 'drop-shadow(0 14px 12px #00143caa)',
      rotate: `${tilt + Math.sin(phase + shift) * 3}deg`,
    }} />
  );

  return <AbsoluteFill style={{overflow: 'hidden', background: 'linear-gradient(#3266c6 0%, #2254ac 44%, #09265c 82%, #061540 100%)'}}>
    <div style={{position: 'absolute', width: 770, height: 770, left: -25,
      top: 180 + float, borderRadius: '50%', overflow: 'hidden',
      boxShadow: '0 25px 80px #001039cc, inset 0 0 35px #8bfcff',
      rotate: `${roll}deg`, background: '#1976b4'}}>
      <Img src={staticFile('onboarding_globe.jpg')} style={{width: '100%', height: '100%', objectFit: 'cover', scale: 1.07}} />
      <div style={{position: 'absolute', inset: 0, borderRadius: '50%',
        background: 'radial-gradient(circle at 38% 28%, #b5fbff55 0%, transparent 33%), radial-gradient(circle at 74% 75%, transparent 18%, #06297599 80%)'}} />
    </div>
    <div style={{position: 'absolute', top: 516 + float, left: 48, width: 624,
      textAlign: 'center', color: 'white', fontFamily: 'Arial Rounded MT Bold, Arial, sans-serif',
      fontSize: 98, fontWeight: 900, letterSpacing: -3, textShadow: '0 6px 10px #082153cc, 0 1px 1px #fff'}}>
      WAYPOINT
    </div>
    {sticker('map-icons/onboarding_marker_place.webp', 292, 34, 118, -9, .3)}
    {sticker('map-icons/onboarding_marker_food.webp', 15, 228, 122, -14, 1.8)}
    {sticker('map-icons/onboarding_marker_sights.webp', 570, 218, 118, 16, 2.5)}
    {sticker('map-icons/onboarding_marker_cash.webp', 22, 700, 122, -12, 3.8)}
    {sticker('map-icons/onboarding_marker_nature.webp', 560, 720, 120, 12, 4.9)}
  </AbsoluteFill>;
};
