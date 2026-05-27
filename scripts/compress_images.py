from PIL import Image
import os

# 所有需要压缩的图片目录
IMAGE_DIRS = [
    '../backend/uploads/pools',
    '../backend/src/main/resources/static/assets',
    '../backend/src/main/resources/static/images/background',
    '../backend/src/main/resources/static/images/background/characters',
    '../backend/src/main/resources/static/images/background/weapons',
    '../backend/src/main/resources/static/images/wuzhizhuansheng/background',
    '../backend/src/main/resources/static/images/wuzhizhuansheng/banners',
]

def compress_image(input_path, output_path, quality=85, max_width=1920):
    """压缩图片"""
    with Image.open(input_path) as img:
        # 如果图片宽度超过最大值，按比例缩放
        if img.width > max_width:
            ratio = max_width / img.width
            new_height = int(img.height * ratio)
            img = img.resize((max_width, new_height), Image.Resampling.LANCZOS)

        # 转换为 RGB（去掉 alpha 通道）
        if img.mode == 'RGBA':
            img = img.convert('RGB')

        # 保存为 JPEG
        img.save(output_path, 'JPEG', quality=quality, optimize=True)

        # 获取压缩后的大小
        new_size = os.path.getsize(output_path)
        return new_size

def main():
    print("=== 开始压缩所有图片 ===\n")

    total_old = 0
    total_new = 0

    for dir_path in IMAGE_DIRS:
        if not os.path.exists(dir_path):
            print(f"目录不存在，跳过: {dir_path}\n")
            continue

        print(f"处理目录: {dir_path}")

        for filename in os.listdir(dir_path):
            if not filename.endswith(('.png', '.jpg', '.jpeg')):
                continue

            input_path = os.path.join(dir_path, filename)
            old_size = os.path.getsize(input_path)

            # 只压缩较大的图片（> 300KB）
            if old_size < 300 * 1024:
                continue

            # 生成输出文件名（改为 .jpg）
            output_filename = os.path.splitext(filename)[0] + '.jpg'
            output_path = os.path.join(dir_path, output_filename)

            # 压缩
            new_size = compress_image(input_path, output_path)

            # 删除原文件（如果是不同格式）
            if input_path != output_path:
                os.remove(input_path)

            total_old += old_size
            total_new += new_size

            ratio = (1 - new_size / old_size) * 100
            print(f"  {filename}: {old_size / 1024:.0f} KB -> {new_size / 1024:.0f} KB (-{ratio:.1f}%)")

        print()

    # 汇总
    if total_old > 0:
        total_ratio = (1 - total_new / total_old) * 100
        print(f"=== 压缩完成 ===")
        print(f"总计: {total_old / 1024 / 1024:.1f} MB -> {total_new / 1024 / 1024:.1f} MB (-{total_ratio:.1f}%)")
    else:
        print("没有需要压缩的图片")

if __name__ == '__main__':
    main()
