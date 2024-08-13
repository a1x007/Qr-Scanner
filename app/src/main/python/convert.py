from PIL import Image, ImageEnhance, ImageSequence, ImageDraw
import sys
import os



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


def enhance_frame(frame, contrast, brightness, colorized, dotSize, resolutaion):
    # Enhance contrast of the frame
    enhancer_contrast = ImageEnhance.Contrast(frame)
    enhanced_frame = enhancer_contrast.enhance(contrast)

    # Enhance brightness of the frame
    enhancer_brightness = ImageEnhance.Brightness(enhanced_frame)
    enhanced_frame = enhancer_brightness.enhance(brightness)

    if not colorized:
        # Convert the enhanced frame to binary (black & white)
        binary_frame = enhanced_frame.convert("1").convert("P")
        return binary_frame

    # Apply halftone effect if colorized

    halftone_frame = convert_to_halftone(enhanced_frame, colorized=True, dot_size=dotSize, resolution_factor=resolutaion)

    # Create a binary frame (black & white)
    binary_frame = enhanced_frame.convert("L")  # Convert to grayscale
    binary_frame = binary_frame.point(lambda p: 255 if p > 128 else 0)  # Apply threshold

    # Create a new image to hold the colorized result
    colorized_frame = Image.new("RGBA", binary_frame.size)  # Using "RGBA" instead of "P"

    # Get pixel data
    binary_data = binary_frame.getdata()
    original_data = enhanced_frame.getdata()

    # Loop through the binary data and map colors
    for i in range(len(binary_data)):
        bin_value = binary_data[i]
        if bin_value == 0:  # Black pixel
            colorized_frame.putpixel((i % binary_frame.width, i // binary_frame.width), (0, 0, 0, 255))  # Set to black
        else:  # White pixel
            # Get the color from the original frame based on the same pixel position
            original_color = original_data[i]
            colorized_frame.putpixel((i % binary_frame.width, i // binary_frame.width), original_color)

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

def process_gif(input_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion):
    image = Image.open(input_path)
    frames = []

    # Sequential processing
    for frame in ImageSequence.Iterator(image):
        frame = resize_frame(frame.convert("RGBA"), quality)
        enhanced_frame = enhance_frame(frame, contrast, brightness, colorized, dot_size, halftone_resolutaion)
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

def process_image(input_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion):
    image = Image.open(input_path)
    frame_rgb = resize_frame(image.convert("RGBA"), quality)
    enhanced_frame = enhance_frame(frame_rgb, contrast, brightness, colorized, dot_size, halftone_resolutaion)

    # Save the enhanced frame
    enhanced_frame.save(output_path)
    return output_path

def overlay_qr_on_image(input_image, qr_image, position=(0, 0), transparency=0):
    """
    Overlay QR image on top of the input image.

    Args:
        input_image (PIL.Image): The base image.
        qr_image (PIL.Image): The QR code image to overlay.
        position (tuple): The (x, y) position to place the QR code on the input image.
        transparency (int): Transparency level of the QR code (0: fully transparent, 255: fully opaque).

    Returns:
        PIL.Image: The image with QR code overlayed.
    """
    # Ensure QR image is in RGBA mode
    qr_image = qr_image.convert("RGBA")

    # Adjust transparency
    qr_image_with_transparency = Image.new("RGBA", qr_image.size)
    for x in range(qr_image.width):
        for y in range(qr_image.height):
            pixel = qr_image.getpixel((x, y))
            qr_image_with_transparency.putpixel((x, y), (pixel[0], pixel[1], pixel[2], transparency))

    # Paste QR image on the input image with transparency
    input_image.paste(qr_image_with_transparency, position, qr_image_with_transparency)

    return input_image


def convert_to_binary(input_path, output_path, colorized=True, contrast=1.0, brightness=1.0, isGif=True, quality="U", dot_size = 2, halftone_resolutaion = 2):
    if isGif:
        return process_gif(input_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion)
    else:
        return process_image(input_path, output_path, contrast, brightness, colorized, quality, dot_size, halftone_resolutaion)

if __name__ == "__main__":
    if len(sys.argv) < 7:
        print("Usage: python convert_image.py <input_path> <output_path> <colorized> <contrast> <brightness> <isGif> <quality>")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]
    colorized = sys.argv[3].lower() in ['true', '1']
    contrast = float(sys.argv[4])
    brightness = float(sys.argv[5])
    isGif = sys.argv[6].lower() in ['true', '1']
    quality = sys.argv[7].lower()

    result = convert_to_binary(input_path, output_path, colorized, contrast, brightness, isGif, quality)
    print(result if result else "Error during conversion.")