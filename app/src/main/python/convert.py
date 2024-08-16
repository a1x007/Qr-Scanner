from PIL import Image, ImageFile, ImageEnhance, ImageSequence, ImageDraw
import sys
import os
ImageFile.LOAD_TRUNCATED_IMAGES = True


def convert_to_halftone(image, colorized=True, dot_size=5, resolution_factor=2):
    # Increase resolution for more detail
    width, height = image.size
    image = image.resize((width * resolution_factor, height * resolution_factor), Image.ANTIALIAS)

    dot_size = max(dot_size, 1)  # Ensure dot_size is at least 1
    halftone_image = Image.new('RGBA', image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(halftone_image)

    def get_grayscale_color(color):
        r, g, b, _ = color
        gray = int((r + g + b) / 3)
        return (gray, gray, gray)

    if colorized:
        # Apply colorized halftone effect
        for y in range(0, height * resolution_factor, dot_size):
            for x in range(0, width * resolution_factor, dot_size):
                color = image.getpixel((x, y))
                if isinstance(color, int):
                    color = (color, color, color)  # Handle grayscale color cases
                gray = int((color[0] + color[1] + color[2]) / 3)
                radius = dot_size * (1 - gray / 255.0) / 2  # Refine radius calculation
                draw.ellipse((x + dot_size // 2 - radius, y + dot_size // 2 - radius,
                              x + dot_size // 2 + radius, y + dot_size // 2 + radius),
                             fill=color, outline=color)
    else:
        # Apply grayscale halftone effect
        grayscale_image = image.convert('L')  # Convert to grayscale
        grayscale_image = grayscale_image.convert('RGBA')
        for y in range(0, height * resolution_factor, dot_size):
            for x in range(0, width * resolution_factor, dot_size):
                gray = grayscale_image.getpixel((x, y))[0]  # Grayscale value
                radius = dot_size * (1 - gray / 255.0) / 2  # Refine radius calculation
                draw.ellipse((x + dot_size // 2 - radius, y + dot_size // 2 - radius,
                              x + dot_size // 2 + radius, y + dot_size // 2 + radius),
                             fill=(0, 0, 0, 255), outline=(0, 0, 0, 255))

    # Downsample back to the original size to maintain clarity
    halftone_image = halftone_image.resize((width, height), Image.ANTIALIAS)

    return halftone_image


def enhance_frame(frame, contrast, brightness, colorized, dot_size, halftone_resolutaion):
    # Enhance contrast of the frame
    enhancer_contrast = ImageEnhance.Contrast(frame)
    enhanced_frame = enhancer_contrast.enhance(contrast)

    # Enhance brightness of the frame
    enhancer_brightness = ImageEnhance.Brightness(enhanced_frame)
    enhanced_frame = enhancer_brightness.enhance(brightness)

    if not colorized:
        # Convert the enhanced frame to binary (black & white)
        binary_frame = enhanced_frame.convert("L").point(lambda p: 255 if p > 128 else 0)  # Apply threshold
        # Create a new image to hold the binary result
        binary_frame = binary_frame.convert("RGBA")  # Convert binary to RGBA for overlaying
        return binary_frame

    # Apply halftone effect if colorized
    halftone_frame = convert_to_halftone(enhanced_frame, colorized=True, dot_size=dot_size, resolution_factor=halftone_resolutaion)
    return halftone_frame


def resize_frame(frame, quality):
    width, height = frame.size

    if quality == "L":
        new_size = (width // 4, height // 4)
    elif quality == "M":
        new_size = (width // 2, height // 2)
    elif quality == "H":
        new_size = (int(width * 0.75), int(height * 0.75))
    else:  # ultra
        new_size = (width, height)

    return frame.resize(new_size, Image.ANTIALIAS)

def overlay_qr_on_image(input_image, qr_image):
    """
    Overlay QR image on top of the input image, ensuring QR pixels are preserved.

    Args:
        input_image (PIL.Image): The base image.
        qr_image (PIL.Image): The QR code image to overlay.

    Returns:
        PIL.Image: The image with QR code overlayed.
    """
    # Ensure QR image is in RGBA mode
    qr_image = qr_image.convert("RGBA")

    # Prepare a new image for the output
    output_image = Image.new("RGBA", input_image.size)

    # Iterate through pixels
    for y in range(qr_image.height):
        for x in range(qr_image.width):
            qr_pixel = qr_image.getpixel((x, y))
            bg_pixel = input_image.getpixel((x, y))

            # Check if the QR pixel is black (or near black)
            if qr_pixel[0] < 50 and qr_pixel[1] < 50 and qr_pixel[2] < 50:  # Adjust the threshold as needed
                # Keep the QR pixel
                output_image.putpixel((x, y), qr_pixel)
            else:
                # Use the background pixel
                output_image.putpixel((x, y), bg_pixel)

    return output_image

""" def overlay_qr_on_image0(background_image, qr_image):
   
    Overlay QR image on top of the background image, without blending.

    Args:
        background_image (PIL.Image): The base image.
        qr_image (PIL.Image): The QR code image to overlay.

    Returns:
        PIL.Image: The image with QR code overlayed.
   
    # Ensure QR image is in RGBA mode
    qr_image = qr_image.convert("RGBA")
    qr_data = qr_image.load()

    # Resize background image to match QR image size
    qr_size = qr_image.size
    background_image = background_image.resize(qr_size, Image.ANTIALIAS)

    # Load the background image pixels
    bg_data = background_image.load()

    for y in range(qr_size[1]):
        for x in range(qr_size[0]):
            # Get the alpha value from the QR code
            qr_pixel = qr_data[x, y]
            qr_alpha = qr_pixel[3]

            # If QR code pixel is not fully transparent, overlay it
            if qr_alpha > 0:
                # Replace the background pixel with QR pixel if QR pixel is not transparent
                bg_data[x, y] = qr_pixel

    return background_image """

def process_image(input_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion, qr_path=None):
    image = Image.open(input_path)
    frame_rgb = resize_frame(image.convert("RGBA"), quality)
    enhanced_frame = enhance_frame(frame_rgb, contrast, brightness, colorized, dot_size, halftone_resolutaion)

    if qr_path:
        qr_image = Image.open(qr_path).convert("RGBA")  # Ensure QR image is in RGBA mode
        # Overlay QR code without blending
        enhanced_frame = overlay_qr_on_image(enhanced_frame, qr_image)

    # Save the enhanced frame
    enhanced_frame.save(output_path)
    return output_path

def process_gif(input_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion, qr_path=None):
    image = Image.open(input_path)
    frames = []

    # Sequential processing
    for frame in ImageSequence.Iterator(image):
        frame = resize_frame(frame.convert("RGBA"), quality)
        enhanced_frame = enhance_frame(frame, contrast, brightness, colorized, dot_size, halftone_resolutaion)

        if qr_path:
            qr_image = Image.open(qr_path).convert("RGBA")  # Ensure QR image is in RGBA mode
            # Overlay QR code without blending
            enhanced_frame = overlay_qr_on_image(enhanced_frame, qr_image)

        frames.append(enhanced_frame)

    # Save the processed frames as a new GIF
    if frames:
        frames[0].save(
            output_path,
            save_all=True,
            append_images=frames[1:],
            loop=0,
            duration=image.info.get('duration', 100),
            disposal=2
        )
        return output_path
    return None


def convert_to_binary(qr_path=None, bg_path=None, output_path=None, colorized=True, contrast=1.0, brightness=1.0, isGif=True, quality="U", dot_size=2, halftone_resolutaion=2):
    if isGif:
        return process_gif(bg_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion, qr_path)
    else:
        return process_image(bg_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion, qr_path)


if __name__ == "__main__":
    if len(sys.argv) < 10:
        print("Usage: python convert_image.py <bg_path> <output_path> <colorized> <contrast> <brightness> <isGif> <quality> <qr_path> <qr_transparency>")
        sys.exit(1)

    bg_path = sys.argv[1]
    output_path = sys.argv[2]
    colorized = sys.argv[3].lower() in ['true', '1']
    contrast = float(sys.argv[4])
    brightness = float(sys.argv[5])
    isGif = sys.argv[6].lower() in ['true', '1']
    quality = sys.argv[7].lower()
    qr_path = sys.argv[8] if sys.argv[8].lower() != 'none' else None
    qr_transparency = int(sys.argv[9])

    result = convert_to_binary(qr_path=qr_path, bg_path=bg_path, output_path=output_path, colorized=colorized, contrast=contrast, brightness=brightness, isGif=isGif, quality=quality, qr_transparency=qr_transparency)
    print(result if result else "Error during conversion.")
