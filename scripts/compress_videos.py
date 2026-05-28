import subprocess
import os

# ffmpeg 路径（Windows）
FFMPEG = r'D:\Demo\ffmpeg-master-latest-win64-gpl\bin\ffmpeg.exe'

# 视频目录
VIDEO_DIRS = [
    '../backend/src/main/resources/static/videos',
    '../backend/uploads/videos',
]

# 压缩参数
CRF = 28          # 越大压缩越狠，画质越低（推荐 25-32）
MAX_WIDTH = 1280  # 最大宽度

def compress_video(input_path, output_path):
    """压缩视频"""
    cmd = [
        FFMPEG,
        '-i', input_path,
        '-vcodec', 'libx264',
        '-crf', str(CRF),
        '-preset', 'fast',
        '-vf', f'scale={MAX_WIDTH}:-2',
        '-y',  # 覆盖输出文件
        output_path
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)
    return result.returncode == 0

def main():
    print("=== 开始压缩视频 ===\n")

    # 检查 ffmpeg
    if not os.path.exists(FFMPEG):
        print(f"ffmpeg 不存在: {FFMPEG}")
        print("请先下载 ffmpeg")
        return

    total_old = 0
    total_new = 0

    for dir_path in VIDEO_DIRS:
        if not os.path.exists(dir_path):
            print(f"目录不存在，跳过: {dir_path}\n")
            continue

        print(f"处理目录: {dir_path}")

        # 处理子目录
        for root, dirs, files in os.walk(dir_path):
            for filename in files:
                if not filename.endswith(('.mp4', '.webm', '.mov')):
                    continue

                # 跳过已经压缩过的
                if '_small' in filename:
                    continue

                input_path = os.path.join(root, filename)
                old_size = os.path.getsize(input_path)

                # 跳过小文件（< 1MB）
                if old_size < 1024 * 1024:
                    continue

                # 输出文件名
                name, ext = os.path.splitext(filename)
                output_filename = f"{name}_small{ext}"
                output_path = os.path.join(root, output_filename)

                print(f"  压缩中: {filename} ({old_size / 1024 / 1024:.1f} MB)...", end='', flush=True)

                if compress_video(input_path, output_path):
                    new_size = os.path.getsize(output_path)
                    total_old += old_size
                    total_new += new_size

                    ratio = (1 - new_size / old_size) * 100
                    print(f" -> {new_size / 1024 / 1024:.1f} MB (-{ratio:.1f}%)")
                else:
                    print(" 失败!")

        print()

    # 汇总
    if total_old > 0:
        total_ratio = (1 - total_new / total_old) * 100
        print(f"=== 压缩完成 ===")
        print(f"总计: {total_old / 1024 / 1024:.1f} MB -> {total_new / 1024 / 1024:.1f} MB (-{total_ratio:.1f}%)")
        print(f"\n输出文件带有 '_small' 后缀，确认效果后可删除原文件")
    else:
        print("没有需要压缩的视频")

if __name__ == '__main__':
    main()
