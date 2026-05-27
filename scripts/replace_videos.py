import os

# 视频目录
VIDEO_DIRS = [
    '../backend/src/main/resources/static/videos',
    '../backend/uploads/videos'
]

def main():
    print("=== 替换视频文件 ===\n")

    count = 0
    for dir_path in VIDEO_DIRS:
        if not os.path.exists(dir_path):
            continue

        for root, dirs, files in os.walk(dir_path):
            for filename in files:
                if '_small' not in filename:
                    continue

                # 找到压缩文件
                small_path = os.path.join(root, filename)

                # 生成原文件名
                original_name = filename.replace('_small', '')
                original_path = os.path.join(root, original_name)

                # 删除原文件，重命名压缩文件
                if os.path.exists(original_path):
                    os.remove(original_path)
                    os.rename(small_path, original_path)
                    print(f"替换: {original_name}")
                    count += 1
                else:
                    # 原文件不存在，直接重命名
                    os.rename(small_path, original_path)
                    print(f"重命名: {filename} -> {original_name}")
                    count += 1

    print(f"\n完成！共替换 {count} 个文件")

if __name__ == '__main__':
    main()
