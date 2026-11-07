"""Regression checks for POI crop drift and severed badges."""
from __future__ import annotations

import unittest

from PIL import Image, ImageDraw

from import_imagegen_poi_atlases import transparent_cell
from validate_map_assets import distant_fragment, horizontal_split


class PoiAtlasRegressionTests(unittest.TestCase):
    def test_horizontal_cut_through_badge_is_rejected(self) -> None:
        sprite = Image.new("RGBA", (128, 128))
        drawer = ImageDraw.Draw(sprite)
        drawer.rounded_rectangle((20, 15, 108, 113), radius=22, fill="#f1cf8a")
        drawer.rectangle((20, 59, 108, 66), fill=(0, 0, 0, 0))
        self.assertTrue(horizontal_split(sprite))

    def test_hollow_badge_is_not_a_horizontal_cut(self) -> None:
        sprite = Image.new("RGBA", (128, 128))
        drawer = ImageDraw.Draw(sprite)
        drawer.rounded_rectangle((20, 15, 108, 113), radius=22, fill="#f1cf8a")
        drawer.ellipse((44, 40, 84, 80), fill=(0, 0, 0, 0))
        self.assertFalse(horizontal_split(sprite))

    def test_detached_neighbor_fragment_is_rejected(self) -> None:
        sprite = Image.new("RGBA", (128, 128))
        drawer = ImageDraw.Draw(sprite)
        drawer.rounded_rectangle((20, 15, 100, 113), radius=18, fill="#f1cf8a")
        drawer.rectangle((115, 60, 120, 70), fill="#f1cf8a")
        self.assertTrue(distant_fragment(sprite))
        drawer.rectangle((115, 60, 120, 70), fill=(0, 0, 0, 0))
        self.assertFalse(distant_fragment(sprite))

    def test_reviewed_bounds_keep_drifted_source_icon_whole(self) -> None:
        sheet = Image.new("RGB", (1280, 2560), "#ff00ff")
        drawer = ImageDraw.Draw(sheet)
        # This source icon crosses the nominal 256 px row and column lines.
        drawer.rounded_rectangle((210, 245, 350, 380), radius=20, fill="#fff0cb")
        columns = [0, 200, 450, 700, 950, 1280]
        rows = [0, 240, 500, 760, 1020, 1280, 1540, 1800, 2060, 2320, 2560]
        reviewed = transparent_cell(sheet, 6, columns, rows).getchannel("A").getbbox()
        nominal = transparent_cell(sheet, 6, [i * 256 for i in range(6)], [i * 256 for i in range(11)]).getchannel("A").getbbox()
        self.assertIsNotNone(reviewed)
        self.assertIsNotNone(nominal)
        reviewed_ratio = (reviewed[2] - reviewed[0]) / (reviewed[3] - reviewed[1])
        nominal_ratio = (nominal[2] - nominal[0]) / (nominal[3] - nominal[1])
        self.assertGreater(reviewed_ratio, 0.95)
        self.assertLess(nominal_ratio, 0.85)


if __name__ == "__main__":
    unittest.main()
