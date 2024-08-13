from PIL import Image, ImageEnhance
import os

def combine(qr_path, bg_path, output_path, colorized, contrast, brightness, ver=2):
    qr = Image.open(qr_path)
    qr = qr.convert('RGBA') if colorized else qr

    bg0 = Image.open(bg_path).convert('RGBA')
    bg0 = ImageEnhance.Contrast(bg0).enhance(contrast)
    bg0 = ImageEnhance.Brightness(bg0).enhance(brightness)

    if bg0.size[0] < bg0.size[1]:
        bg0 = bg0.resize((qr.size[0] - 24, int((qr.size[0] - 24) * bg0.size[1] / bg0.size[0])))
    else:
        bg0 = bg0.resize((int((qr.size[1] - 24) * bg0.size[0] / bg0.size[1]), qr.size[1] - 24))

    bg = bg0 if colorized else bg0.convert('1')

    # Handle alignment patterns if version is provided
    if ver is not None:
        from constant import alig_location

        aligs = []
        if ver > 1:
            aloc = alig_location[ver - 2]
            for a in range(len(aloc)):
                for b in range(len(aloc)):
                    if not ((a == b == 0) or (a == len(aloc) - 1 and b == 0) or (a == 0 and b == len(aloc) - 1)):
                        for i in range(3 * (aloc[a] - 2), 3 * (aloc[a] + 3)):
                            for j in range(3 * (aloc[b] - 2), 3 * (aloc[b] + 3)):
                                aligs.append((i, j))

    # Combine the QR and background images
    for i in range(qr.size[0] - 24):
        for j in range(qr.size[1] - 24):
            if not ((i in (18, 19, 20)) or (j in (18, 19, 20)) or (i < 24 and j < 24) or
                    (i < 24 and j > qr.size[1] - 49) or (i > qr.size[0] - 49 and j < 24) or
                    (i % 3 == 1 and j % 3 == 1) or (bg0.getpixel((i, j))[3] == 0) or
                    (ver is not None and (i, j) in aligs)):
                qr.putpixel((i + 12, j + 12), bg.getpixel((i, j)))

    # Save the output image
    if not output_path:
        output_path = os.path.join(os.path.dirname(bg_path), 'combined_qrcode.png')

    qr.resize((qr.size[0] * 3, qr.size[1] * 3)).save(output_path)
    return output_path
