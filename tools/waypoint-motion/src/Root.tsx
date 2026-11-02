import {Composition, Folder} from 'remotion';
import {WaypointIntroVideo} from './Composition';
import {GameArtworkScene} from './GameArtworkScene';
import {OnboardingGlobe} from './OnboardingGlobe';
import {IconCollageScene, MapSticker} from './IconCollageScene';

export const RemotionRoot: React.FC = () => {
  return (
    <Folder name="Waypoint">
      <Composition
        id="OnboardingGlobe"
        component={OnboardingGlobe}
        durationInFrames={180}
        fps={30}
        width={720}
        height={1170}
        defaultProps={{}}
      />
      <Composition
        id="IconCollageScene"
        component={IconCollageScene}
        durationInFrames={180}
        fps={30}
        width={720}
        height={1200}
        defaultProps={{}}
      />
      <Composition
        id="WaypointIntro"
        component={WaypointIntroVideo}
        durationInFrames={654}
        fps={30}
        width={720}
        height={1280}
        defaultProps={{}}
      />
      <Folder name="Editable-scene">
        <Composition
          id="MapSticker"
          component={MapSticker}
          durationInFrames={180}
          fps={30}
          width={720}
          height={1200}
          defaultProps={{art: 'food', x: 170, y: 200, size: 260, tilt: -12, phase: 0}}
        />
        <Composition
          id="GameArtworkScene"
          component={GameArtworkScene}
          durationInFrames={150}
          fps={30}
          width={720}
          height={1280}
          defaultProps={{
            title: 'Los Santos',
            game: 'Grand Theft Auto V',
            artwork: 'game-art/art_los_santos.jpg',
            accentColor: '#B6DB91',
            routeColor: '#B786ED',
            routeStyle: 'urban',
          }}
        />
      </Folder>
    </Folder>
  );
};
