from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "mcdemo" / "textures"


def add_outline(sprite: Image.Image, color: tuple[int, int, int, int]) -> Image.Image:
    alpha = sprite.getchannel("A")
    expanded = alpha.filter(ImageFilter.MaxFilter(3))
    outline_alpha = Image.eval(expanded, lambda value: 255 if value else 0)
    outline_alpha.paste(0, mask=alpha)
    outlined = Image.new("RGBA", sprite.size, color)
    outlined.putalpha(outline_alpha)
    return Image.alpha_composite(outlined, sprite)


def create_thunder_wand() -> Image.Image:
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # Brown and gold wrapped handle.
    draw.line([(4, 28), (12, 20)], fill="#7a3f0b", width=5)
    draw.line([(4, 27), (6, 25)], fill="#f5c542", width=2)
    draw.line([(7, 25), (9, 23)], fill="#d98c16", width=2)
    draw.line([(10, 22), (12, 20)], fill="#ffd95a", width=2)
    draw.rectangle((2, 27, 5, 30), fill="#8a4a0d")
    draw.rectangle((3, 27, 4, 29), fill="#ffe36b")

    # Short segmented silver shaft.
    draw.line([(11, 21), (18, 14)], fill="#9ca3aa", width=5)
    draw.line([(12, 20), (18, 14)], fill="#d9dde1", width=2)
    draw.rectangle((12, 19, 14, 21), fill="#737a82")
    draw.rectangle((15, 16, 17, 18), fill="#bfc5ca")

    # Ornate golden branches around the crystal.
    dark_gold = "#a8610d"
    gold = "#e8a619"
    bright_gold = "#ffd957"
    draw.line([(18, 14), (26, 11)], fill=dark_gold, width=4)
    draw.line([(18, 14), (18, 6)], fill=dark_gold, width=4)
    draw.line([(18, 8), (14, 6)], fill=dark_gold, width=3)
    draw.line([(19, 7), (23, 3)], fill=dark_gold, width=3)
    draw.line([(24, 12), (28, 9)], fill=dark_gold, width=3)
    draw.line([(26, 12), (28, 15)], fill=dark_gold, width=3)
    draw.line([(19, 13), (25, 11)], fill=gold, width=2)
    draw.line([(18, 13), (18, 7)], fill=gold, width=2)
    draw.rectangle((13, 5, 15, 7), fill=bright_gold)
    draw.rectangle((22, 2, 24, 4), fill=bright_gold)
    draw.rectangle((27, 8, 29, 10), fill=bright_gold)
    draw.rectangle((27, 15, 29, 17), fill="#b96d10")

    # Blue crystal inset, matching the reference image.
    draw.polygon([(20, 5), (26, 3), (29, 6), (27, 10), (22, 11), (20, 8)], fill="#2855d9")
    draw.polygon([(21, 5), (25, 4), (26, 7), (22, 8)], fill="#77a9ff")
    draw.polygon([(22, 8), (27, 7), (27, 10), (23, 10)], fill="#3d70ee")
    draw.rectangle((27, 5, 29, 8), fill="#9bc0ff")

    return add_outline(image, (55, 31, 12, 255))


def create_mystic_crystal() -> Image.Image:
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    draw.polygon(
        [(17, 2), (23, 9), (22, 17), (18, 28), (13, 30), (8, 23), (10, 11)],
        fill="#d99112",
    )
    draw.polygon([(17, 3), (21, 10), (18, 18), (15, 15), (12, 10)], fill="#ffd13d")
    draw.polygon([(10, 11), (15, 15), (13, 27), (9, 22)], fill="#a85a0b")
    draw.polygon([(15, 15), (21, 10), (21, 17), (18, 27), (13, 27)], fill="#e8a319")
    draw.polygon([(16, 4), (19, 9), (16, 13), (13, 10)], fill="#fff0a0")
    draw.polygon([(18, 18), (21, 17), (18, 26), (16, 26)], fill="#ffd95a")

    return add_outline(image, (83, 43, 7, 255))


def create_flame_wand() -> Image.Image:
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # A diagonal dark-red handle with hot metal bands.
    draw.line([(4, 28), (17, 15)], fill="#691315", width=5)
    draw.line([(5, 27), (16, 16)], fill="#b32621", width=2)
    draw.rectangle((2, 27, 5, 30), fill="#4a0b0d")
    draw.rectangle((3, 27, 4, 29), fill="#ff5a2a")
    draw.line([(7, 25), (9, 23)], fill="#f04a22", width=2)
    draw.line([(11, 21), (13, 19)], fill="#ff7b2f", width=2)

    # Forked crimson head surrounding a living flame crystal.
    draw.line([(17, 15), (24, 12)], fill="#7d1115", width=5)
    draw.line([(18, 15), (18, 7)], fill="#7d1115", width=4)
    draw.line([(23, 12), (27, 7)], fill="#7d1115", width=4)
    draw.line([(18, 8), (14, 6)], fill="#7d1115", width=3)
    draw.line([(19, 14), (24, 11)], fill="#d82d20", width=2)
    draw.line([(18, 13), (18, 8)], fill="#e33a22", width=2)
    draw.rectangle((13, 5, 15, 7), fill="#ff6b2b")
    draw.rectangle((26, 6, 28, 8), fill="#ff5426")

    draw.polygon([(20, 9), (22, 3), (26, 7), (25, 11), (22, 13), (19, 11)], fill="#e31d1a")
    draw.polygon([(22, 9), (23, 5), (25, 8), (24, 11)], fill="#ff762f")
    draw.polygon([(21, 10), (22, 7), (23, 10), (22, 12)], fill="#ffd45c")
    draw.rectangle((23, 3, 24, 5), fill="#ffb23d")

    return add_outline(image, (47, 5, 8, 255))


def create_ice_wand() -> Image.Image:
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # Deep-blue diagonal handle with frozen metal bands.
    draw.line([(4, 28), (17, 15)], fill="#123b73", width=5)
    draw.line([(5, 27), (16, 16)], fill="#2878bd", width=2)
    draw.rectangle((2, 27, 5, 30), fill="#09284f")
    draw.rectangle((3, 27, 4, 29), fill="#7fd8ff")
    draw.line([(7, 25), (9, 23)], fill="#42aee8", width=2)
    draw.line([(11, 21), (13, 19)], fill="#b2ecff", width=2)

    # Forked icy crown surrounding a bright frozen core.
    draw.line([(17, 15), (24, 12)], fill="#164f8c", width=5)
    draw.line([(18, 15), (18, 7)], fill="#164f8c", width=4)
    draw.line([(23, 12), (27, 7)], fill="#164f8c", width=4)
    draw.line([(18, 8), (14, 6)], fill="#164f8c", width=3)
    draw.line([(19, 14), (24, 11)], fill="#338fd1", width=2)
    draw.line([(18, 13), (18, 8)], fill="#4fb9ec", width=2)
    draw.rectangle((13, 5, 15, 7), fill="#8cddff")
    draw.rectangle((26, 6, 28, 8), fill="#65c8f3")

    draw.polygon([(20, 9), (22, 3), (26, 7), (25, 11), (22, 13), (19, 11)], fill="#258bd2")
    draw.polygon([(22, 9), (23, 5), (25, 8), (24, 11)], fill="#79d7ff")
    draw.polygon([(21, 10), (22, 7), (23, 10), (22, 12)], fill="#e4faff")
    draw.rectangle((23, 3, 24, 5), fill="#bcefff")

    return add_outline(image, (5, 29, 61, 255))


def create_excalibur() -> Image.Image:
    """Reference-inspired silver greatsword with a blue grip and golden fittings."""
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # Mostly parallel silver blade with only the final few pixels tapering.
    draw.polygon([(1, 30), (3, 25), (20, 8), (23, 11), (6, 28)], fill="#707985")
    draw.polygon([(1, 30), (3, 25), (20, 8), (21, 9), (5, 26)], fill="#eef1f2")
    draw.polygon([(1, 30), (5, 26), (21, 10), (23, 11), (6, 28)], fill="#b9c0c7")
    draw.line([(3, 25), (20, 8)], fill="#ffffff", width=1)

    # Blue-and-gold crest at the base of the blade.
    draw.polygon([(19, 10), (22, 7), (25, 10), (22, 13)], fill="#b58d31")
    draw.polygon([(20, 10), (22, 8), (24, 10), (22, 12)], fill="#173b8f")
    draw.point((22, 9), fill="#6d91e3")

    # Wide curved golden crossguard, perpendicular to the blade.
    draw.line([(17, 5), (27, 15)], fill="#665322", width=4)
    draw.line([(18, 5), (27, 14)], fill="#d7bd66", width=2)
    draw.rectangle((16, 4, 18, 7), fill="#bda350")
    draw.rectangle((26, 13, 28, 16), fill="#a98636")
    draw.point((17, 5), fill="#fff1a6")
    draw.point((27, 14), fill="#e9cf73")

    # Deep royal-blue grip with gold collar and pommel.
    draw.line([(23, 9), (28, 4)], fill="#0c1d51", width=4)
    draw.line([(23, 8), (28, 3)], fill="#224ba5", width=2)
    draw.line([(24, 7), (28, 3)], fill="#416fc8", width=1)
    draw.rectangle((21, 8, 24, 11), fill="#b99b45")
    draw.rectangle((22, 8, 24, 9), fill="#f0da83")
    draw.rectangle((27, 2, 29, 4), fill="#8c7130")
    draw.rectangle((27, 2, 28, 3), fill="#f4df88")
    draw.point((28, 2), fill="#fff4b3")

    # Minecraft handheld textures point from the lower-left grip toward the
    # upper-right blade, opposite to the orientation of the reference image.
    return add_outline(image, (30, 27, 24, 255)).rotate(180)


def create_excalibur_charge_variant(base: Image.Image, strength: float) -> Image.Image:
    result = base.copy()
    pixels = result.load()
    blade_mask = Image.new("L", base.size, 0)
    mask_pixels = blade_mask.load()

    for y in range(base.height):
        for x in range(base.width):
            red, green, blue, alpha = pixels[x, y]
            is_silver = alpha > 0 and red > 65 and max(red, green, blue) - min(red, green, blue) < 32
            if not is_silver:
                continue
            mask_pixels[x, y] = alpha
            pixels[x, y] = (
                int(red + (255 - red) * strength),
                int(green + (255 - green) * strength),
                int(blue + (244 - blue) * strength),
                alpha,
            )

    expanded = blade_mask.filter(ImageFilter.MaxFilter(3))
    aura_alpha = Image.new("L", base.size, 0)
    aura_pixels = aura_alpha.load()
    expanded_pixels = expanded.load()
    for y in range(base.height):
        for x in range(base.width):
            aura_pixels[x, y] = max(0, expanded_pixels[x, y] - mask_pixels[x, y])
    aura_alpha = aura_alpha.point(lambda value: int(value * (0.22 + strength * 0.38)))
    aura = Image.new("RGBA", base.size, (255, 249, 202, 0))
    aura.putalpha(aura_alpha)
    return Image.alpha_composite(aura, result)


def create_mystic_block() -> Image.Image:
    image = Image.new("RGBA", (16, 16), "#c27612")
    draw = ImageDraw.Draw(image)

    # Seam-safe gold crystal mosaic with bright mineral facets.
    draw.polygon([(0, 0), (6, 0), (4, 5), (0, 7)], fill="#e8a319")
    draw.polygon([(6, 0), (12, 0), (10, 5), (4, 5)], fill="#ffd95a")
    draw.polygon([(12, 0), (15, 0), (15, 6), (10, 5)], fill="#a85a0b")
    draw.polygon([(0, 7), (4, 5), (8, 9), (4, 13), (0, 12)], fill="#8c4808")
    draw.polygon([(4, 5), (10, 5), (12, 10), (8, 9)], fill="#f4bd2c")
    draw.polygon([(10, 5), (15, 6), (15, 12), (12, 10)], fill="#d98c16")
    draw.polygon([(0, 12), (4, 13), (6, 15), (0, 15)], fill="#d98c16")
    draw.polygon([(4, 13), (8, 9), (12, 10), (10, 15), (6, 15)], fill="#ffd95a")
    draw.polygon([(12, 10), (15, 12), (15, 15), (10, 15)], fill="#9d5209")
    draw.line([(1, 1), (4, 1), (3, 3)], fill="#fff0a0", width=1)
    draw.line([(6, 6), (9, 6)], fill="#fff3ad", width=1)
    draw.line([(6, 13), (9, 12)], fill="#fff0a0", width=1)
    draw.point((13, 8), fill="#ffe778")
    draw.point((2, 10), fill="#f5b928")

    return image


def create_golden_sword_wave_particle() -> Image.Image:
    image = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon([(3, 0), (5, 0), (5, 2), (7, 3), (7, 5), (5, 5),
                  (5, 7), (3, 7), (3, 5), (0, 5), (0, 3), (3, 3)],
                 fill="#d98b00")
    draw.polygon([(3, 1), (5, 1), (5, 3), (6, 3), (6, 5), (5, 5),
                  (5, 6), (3, 6), (3, 5), (1, 5), (1, 3), (3, 3)],
                 fill="#ffc72f")
    draw.rectangle((3, 3, 4, 4), fill="#fff6bd")
    return image


def create_golden_sword_trail_particle() -> Image.Image:
    image = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon([(0, 3), (5, 2), (7, 3), (7, 4), (5, 5), (0, 4)],
                 fill=(216, 132, 0, 120))
    draw.polygon([(1, 3), (6, 3), (7, 4), (1, 4)], fill=(255, 193, 42, 210))
    draw.line([(3, 3), (6, 3)], fill=(255, 244, 166, 240), width=1)
    return image


def create_excalibur_charge_particle() -> Image.Image:
    image = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon([(3, 0), (5, 0), (5, 3), (7, 3), (7, 5), (5, 5),
                  (5, 7), (3, 7), (3, 5), (0, 5), (0, 3), (3, 3)],
                 fill=(220, 230, 255, 150))
    draw.rectangle((2, 2, 5, 5), fill=(245, 248, 255, 220))
    draw.rectangle((3, 3, 4, 4), fill=(255, 255, 255, 255))
    return image


def main() -> None:
    item_dir = TEXTURES / "item"
    block_dir = TEXTURES / "block"
    particle_dir = TEXTURES / "particle"
    item_dir.mkdir(parents=True, exist_ok=True)
    block_dir.mkdir(parents=True, exist_ok=True)
    particle_dir.mkdir(parents=True, exist_ok=True)

    create_thunder_wand().save(item_dir / "thunder_wand.png")
    create_flame_wand().save(item_dir / "flame_wand.png")
    create_ice_wand().save(item_dir / "ice_wand.png")
    excalibur = create_excalibur()
    excalibur.save(item_dir / "excalibur.png")
    create_excalibur_charge_variant(excalibur, 0.28).save(item_dir / "excalibur_charge_1.png")
    create_excalibur_charge_variant(excalibur, 0.58).save(item_dir / "excalibur_charge_2.png")
    create_excalibur_charge_variant(excalibur, 0.9).save(item_dir / "excalibur_charge_3.png")
    create_mystic_crystal().save(item_dir / "mystic_crystal.png")
    create_mystic_block().save(block_dir / "mystic_block.png")
    create_golden_sword_wave_particle().save(particle_dir / "golden_sword_wave.png")
    create_golden_sword_trail_particle().save(particle_dir / "golden_sword_trail.png")
    create_excalibur_charge_particle().save(particle_dir / "excalibur_charge.png")


if __name__ == "__main__":
    main()
