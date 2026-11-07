"""Original vector pictograms for Waypoint's live OpenStreetMap categories."""

from __future__ import annotations

import math


def draw_category_glyph(image, kind: str, ink, accent, family: str) -> None:
    """Draw one semantic place glyph into a 128px Waypoint atlas cell."""
    stroke = 5 if family in {"pixel", "terminal", "pda", "machine"} else 4
    thin = max(2, stroke - 2)

    def line(*points, width=stroke):
        image.line(points, ink, width)

    def rect(x0, y0, x1, y1, fill=accent, width=thin):
        image.rect(x0, y0, x1, y1, ink, fill, width)

    def ring(x, y, rx, ry=None, fill=None, width=thin):
        image.ellipse(x, y, rx, rx if ry is None else ry, ink, fill, width)

    def star(x=64, y=65, outer=19, inner=8):
        points = []
        for i in range(10):
            angle = -math.pi / 2 + i * math.pi / 5
            radius = outer if i % 2 == 0 else inner
            points.append((x + math.cos(angle) * radius, y + math.sin(angle) * radius))
        image.polygon(points, ink, accent, thin)

    if kind == "place":
        ring(64, 62, 19, fill=None, width=stroke)
        ring(64, 62, 6, fill=accent, width=thin)
        line((64, 80), (64, 92), width=thin)
        line((50, 40), (50, 32), (56, 26), (64, 24), (72, 26), (78, 32), (78, 40), width=thin)
    elif kind == "restaurant":
        line((45, 45), (45, 86)); line((38, 45), (38, 59), (52, 59), (52, 45))
        line((81, 45), (81, 86)); line((74, 45), (74, 56), (81, 56))
    elif kind == "fast_food":
        image.polygon(((43, 58), (47, 49), (55, 44), (73, 44), (81, 49), (85, 58)), ink, accent, thin)
        line((43, 62), (85, 62), width=thin); rect(45, 66, 83, 70, width=1)
        line((47, 74), (81, 74), width=stroke)
        image.polygon(((49, 80), (79, 80), (75, 85), (53, 85)), ink, accent, thin)
    elif kind == "ice_cream":
        ring(64, 52, 13, fill=accent); ring(53, 58, 9, fill=accent); ring(75, 58, 9, fill=accent)
        image.polygon(((49, 65), (79, 65), (64, 91)), ink, accent, thin)
        line((57, 73), (70, 84), width=thin)
    elif kind == "cafe":
        rect(45, 57, 76, 80); ring(78, 64, 9, 9, fill=None)
        line((42, 84), (79, 84), width=thin); line((53, 51), (53, 45), (59, 41))
        line((68, 51), (68, 45), (72, 42), width=thin)
    elif kind == "pub":
        image.polygon(((46, 48), (78, 48), (74, 78), (50, 78)), ink, accent, thin)
        rect(46, 55, 78, 61, fill=ink, width=1)
        line((78, 54), (86, 54), (84, 70), (74, 70), width=thin)
        line((52, 85), (75, 85), width=thin)
    elif kind == "bar":
        line((43, 45), (85, 45), (66, 64), (66, 79), (57, 85), (75, 85), width=thin)
        line((55, 56), (77, 56), width=thin); ring(82, 48, 3, fill=accent, width=1)
    elif kind == "lodging":
        rect(42, 58, 86, 79, fill=None, width=stroke)
        rect(46, 48, 59, 59); line((42, 78), (42, 88)); line((86, 78), (86, 88))
        line((38, 87), (90, 87), width=thin); rect(61, 64, 82, 77, width=1)
    elif kind == "fuel":
        rect(46, 43, 70, 82, fill=None, width=stroke); rect(51, 49, 65, 61, width=2)
        line((70, 55), (81, 55), (81, 73), (88, 73), (88, 49), (81, 43))
        line((43, 87), (73, 87), width=thin)
    elif kind == "ev_charging":
        rect(49, 43, 78, 84, fill=None, width=stroke); rect(56, 37, 71, 43, width=2)
        image.polygon(((67, 48), (57, 64), (65, 64), (59, 78), (72, 60), (65, 60)), ink, accent, thin)
    elif kind == "parking":
        ring(64, 64, 25, fill=accent, width=thin)
        line((57, 81), (57, 47), (68, 47), (78, 52), (78, 61), (68, 66), (57, 66), width=6)
    elif kind == "grocery":
        line((43, 55), (49, 82), (79, 82), (86, 55), width=stroke); line((48, 65), (81, 65), width=thin)
        for x in (54, 64, 74): line((x, 67), (x, 79), width=thin)
        line((52, 51), (58, 44), (70, 44), (78, 51), width=thin)
    elif kind == "convenience":
        rect(45, 56, 83, 84, fill=None, width=thin)
        image.polygon(((42, 56), (48, 43), (80, 43), (86, 56)), ink, accent, thin)
        line((49, 57), (51, 64), (57, 64), (59, 57), (65, 64), (71, 64), (73, 57), (79, 64), (84, 62), width=thin)
        rect(61, 68, 69, 84, width=1)
    elif kind == "bakery":
        image.polygon(((43, 77), (46, 61), (55, 52), (74, 51), (82, 61), (86, 77), (80, 84), (48, 84)), ink, accent, thin)
        for x in (55, 65, 75): line((x, 59), (x - 3, 68), width=thin)
        line((43, 87), (85, 87), width=thin)
    elif kind == "butcher":
        image.polygon(((50, 44), (81, 44), (81, 68), (73, 77), (50, 69)), ink, accent, thin)
        line((54, 70), (67, 84), (64, 88), (49, 73), width=stroke); ring(78, 48, 2, fill=ink, width=1)
    elif kind == "shop":
        image.polygon(((48, 52), (55, 43), (73, 43), (80, 52), (77, 85), (51, 85)), ink, accent, thin)
        line((55, 52), (57, 44), (71, 44), (73, 52), width=thin); rect(57, 63, 72, 80, fill=None, width=thin)
    elif kind == "clothing":
        ring(64, 43, 5, fill=None); line((64, 48), (49, 57), (44, 63), (64, 84), (84, 63), (79, 57), (64, 48))
        line((52, 70), (76, 70), width=thin)
    elif kind == "mall":
        rect(42, 53, 86, 85, fill=None, width=stroke); rect(48, 59, 58, 70, width=1)
        rect(67, 59, 80, 70, width=1); rect(60, 70, 69, 85, width=1)
        line((38, 52), (64, 42), (90, 52), width=stroke)
    elif kind == "pharmacy":
        ring(64, 70, 22, 12, fill=None, width=stroke); line((48, 65), (58, 55), (71, 48), (78, 47), width=stroke)
        rect(59, 44, 69, 54); rect(62, 39, 67, 58, fill=ink, width=1); rect(56, 45, 73, 51, fill=ink, width=1)
    elif kind == "hospital":
        rect(42, 51, 86, 84, fill=None, width=stroke); rect(50, 44, 78, 52)
        rect(60, 61, 68, 84, width=1); rect(48, 60, 56, 67, width=1); rect(72, 60, 80, 67, width=1)
        rect(59, 44, 69, 56, fill=ink, width=1); rect(54, 48, 74, 52, fill=ink, width=1)
    elif kind == "clinic":
        ring(57, 46, 7, fill=None); line((57, 52), (57, 67), (64, 74), (72, 67), (72, 57))
        line((72, 57), (82, 57), (82, 75), (76, 81), (68, 81), (64, 77), width=thin)
        ring(64, 76, 4, fill=accent)
    elif kind == "bank":
        image.polygon(((39, 56), (64, 42), (89, 56)), ink, accent, thin); line((42, 59), (86, 59), width=thin)
        for x in (48, 58, 68, 78): line((x, 61), (x, 81))
        line((40, 84), (88, 84)); line((37, 90), (91, 90), width=thin)
    elif kind == "atm":
        rect(47, 43, 81, 85, fill=None, width=stroke); rect(53, 49, 75, 61)
        rect(55, 66, 73, 78, fill=None, width=thin); line((59, 72), (69, 72), width=thin); line((64, 82), (64, 88), width=thin)
    elif kind == "school":
        image.polygon(((42, 56), (64, 42), (86, 56)), ink, accent, thin); rect(45, 57, 83, 84, fill=None, width=stroke)
        rect(59, 69, 69, 84, width=1); rect(51, 61, 57, 68, width=1); rect(72, 61, 78, 68, width=1)
        ring(64, 53, 3, fill=ink, width=1)
    elif kind == "university":
        image.polygon(((39, 54), (64, 42), (89, 54), (64, 65)), ink, accent, thin)
        line((50, 60), (50, 75), (64, 83), (78, 75), (78, 60), width=thin)
        line((86, 55), (86, 71), (90, 75), width=thin); ring(90, 76, 2, fill=accent, width=1)
    elif kind == "library":
        image.polygon(((42, 48), (62, 52), (64, 85), (44, 79)), ink, accent, thin)
        image.polygon(((86, 48), (66, 52), (64, 85), (84, 79)), ink, accent, thin)
        line((49, 59), (59, 61), width=thin); line((49, 66), (59, 68), width=thin)
        line((69, 61), (79, 59), width=thin); line((69, 68), (79, 66), width=thin)
    elif kind == "museum":
        image.polygon(((40, 54), (64, 42), (88, 54)), ink, accent, thin); line((42, 58), (86, 58), width=thin)
        for x in (48, 58, 68, 78): line((x, 60), (x, 81))
        line((40, 84), (88, 84)); line((37, 90), (91, 90), width=thin)
    elif kind == "attraction":
        star(); ring(64, 64, 28, fill=None, width=1)
    elif kind == "castle":
        rect(43, 57, 85, 84, fill=None, width=stroke); rect(42, 44, 54, 61); rect(61, 38, 68, 60); rect(74, 44, 86, 61)
        rect(59, 70, 69, 84, width=1)
    elif kind == "monument":
        image.polygon(((64, 42), (76, 77), (71, 84), (57, 84), (52, 77)), ink, accent, thin)
        line((47, 86), (81, 86)); line((50, 91), (78, 91), width=thin); line((59, 67), (69, 67), width=thin)
    elif kind == "cinema":
        rect(43, 45, 85, 83, fill=None, width=stroke)
        for x in (48, 58, 68, 78): rect(x, 50, x + 4, 56, width=1)
        image.polygon(((59, 59), (59, 75), (74, 67)), ink, accent, thin); line((48, 88), (80, 88), width=thin)
    elif kind == "theatre":
        image.polygon(((43, 48), (59, 48), (63, 57), (61, 70), (52, 76), (43, 69)), ink, accent, thin)
        image.polygon(((65, 57), (69, 48), (85, 48), (85, 69), (76, 76), (67, 70)), ink, None, thin)
        line((48, 58), (53, 58), width=thin); line((73, 58), (78, 58), width=thin)
        line((47, 68), (57, 68)); line((71, 68), (81, 68), width=thin)
    elif kind == "park":
        ring(64, 53, 15, 14, fill=accent); ring(52, 63, 12, 11, fill=accent); ring(76, 63, 12, 11, fill=accent)
        ring(64, 69, 14, 12, fill=accent); line((64, 77), (64, 90)); line((52, 90), (76, 90), width=thin)
    elif kind == "playground":
        line((44, 43), (84, 43), (80, 48)); line((49, 44), (49, 84)); line((79, 44), (79, 84))
        line((58, 48), (58, 68), width=thin); line((71, 48), (71, 68), width=thin)
        line((55, 68), (74, 68), (71, 75), (58, 75), (55, 68), width=thin)
    elif kind == "garden":
        for angle in range(0, 360, 60):
            rad = math.radians(angle); ring(64 + math.cos(rad) * 11, 61 + math.sin(rad) * 11, 8, fill=accent)
        ring(64, 61, 6, fill=ink, width=thin); line((64, 69), (64, 88)); line((64, 79), (53, 74), (50, 69), width=thin)
    elif kind == "toilets":
        ring(53, 50, 6, fill=accent); ring(75, 50, 6, fill=accent)
        line((53, 57), (53, 79), (45, 87)); line((53, 65), (64, 65), (64, 83), width=thin)
        line((75, 57), (75, 79), (83, 87)); line((75, 65), (64, 65), width=thin)
        line((44, 88), (56, 88)); line((71, 88), (84, 88), width=thin)
    elif kind == "drinking_water":
        line((43, 51), (43, 44), (65, 44), (65, 53)); line((56, 54), (74, 54), (74, 64))
        line((69, 64), (80, 64), (80, 58), width=thin)
        image.polygon(((74, 66), (66, 77), (67, 84), (73, 88), (80, 84), (81, 77)), ink, accent, thin)
    elif kind == "information":
        ring(64, 64, 24, fill=accent, width=thin); ring(64, 51, 3, fill=ink, width=1)
        line((64, 59), (64, 79), width=6); line((59, 84), (69, 84), width=thin)
    elif kind == "police":
        image.polygon(((43, 46), (85, 46), (83, 67), (76, 80), (64, 89), (52, 80), (45, 67)), ink, accent, stroke)
        star(64, 65, 12, 5)
    elif kind == "fire_station":
        image.polygon(((64, 40), (75, 55), (81, 65), (78, 79), (70, 88), (57, 88), (48, 79), (47, 68), (54, 57), (57, 67), (62, 58)), ink, accent, thin)
        image.polygon(((65, 62), (72, 73), (68, 82), (60, 82), (57, 76)), ink, None, thin)
    elif kind == "post_office":
        rect(42, 49, 86, 81, fill=None, width=stroke); line((45, 53), (64, 69), (83, 53))
        line((45, 77), (57, 67), width=thin); line((83, 77), (71, 67), width=thin); ring(64, 67, 2, fill=accent, width=1)
    elif kind == "place_of_worship":
        rect(43, 61, 85, 85, fill=None, width=stroke)
        image.polygon(((40, 61), (44, 50), (64, 41), (84, 50), (88, 61)), ink, accent, thin)
        ring(64, 51, 7, 9, fill=None); rect(61, 72, 67, 85, width=1)
    elif kind == "stadium":
        ring(64, 64, 27, 19, fill=None, width=stroke); ring(64, 64, 16, 9, fill=accent, width=thin)
        for x in (46, 55, 73, 82): line((x, 48), (x + (64 - x) / 4, 55), width=thin)
        line((47, 80), (81, 80), width=thin)
    elif kind == "sports":
        ring(64, 64, 23, fill=accent, width=thin)
        line((47, 50), (58, 59), (57, 72), (48, 79), width=thin); line((81, 50), (70, 59), (71, 72), (80, 79), width=thin)
        line((58, 59), (70, 59), width=thin); line((57, 72), (71, 72), width=thin)
    elif kind == "train_station":
        rect(44, 43, 84, 78, fill=None, width=stroke); rect(51, 51, 60, 63); rect(68, 51, 77, 63)
        line((49, 70), (79, 70), width=thin); ring(52, 80, 4, fill=accent); ring(76, 80, 4, fill=accent)
        line((49, 88), (79, 88), width=thin)
    elif kind == "bus_stop":
        rect(46, 49, 82, 80, fill=None, width=stroke); rect(51, 54, 60, 65); rect(68, 54, 77, 65)
        ring(54, 81, 4, fill=accent); ring(74, 81, 4, fill=accent); line((44, 88), (84, 88), width=thin)
        line((86, 43), (86, 55), width=thin); rect(81, 38, 91, 48, fill=accent, width=thin)
    elif kind == "ferry_terminal":
        image.polygon(((44, 62), (84, 62), (78, 79), (72, 86), (54, 86), (47, 79)), ink, accent, thin)
        rect(54, 48, 74, 62, fill=None); rect(60, 51, 67, 57)
        line((40, 91), (50, 87), (60, 91), (70, 87), (80, 91), (88, 87), width=thin)
    elif kind == "car_repair":
        ring(53, 57, 9, fill=None, width=stroke); ring(53, 57, 3, fill=accent, width=thin); line((59, 63), (81, 85))
        image.polygon(((73, 46), (82, 50), (85, 57), (79, 63), (72, 59), (68, 51)), ink, accent, thin)
        line((44, 82), (61, 65), width=stroke)
    elif kind == "office":
        rect(43, 54, 85, 84, fill=None, width=stroke); rect(51, 49, 77, 58)
        line((58, 49), (58, 45), (70, 45), (70, 49), width=thin); line((43, 66), (85, 66), width=thin)
        ring(64, 69, 2, fill=accent, width=1)
    else:
        raise ValueError(f"No authored map glyph for category {kind!r}")
