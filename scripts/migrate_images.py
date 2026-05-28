import base64
import os
import pymysql

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'Yxw20060614.',  # 改成你的密码
    'database': 'wuthering_waves_gacha'
}

# 基础上传目录（与后端 app.upload.dir 一致）
BASE_UPLOAD_DIR = '../backend/uploads'

def migrate():
    conn = pymysql.connect(**DB_CONFIG)

    try:
        with conn.cursor() as cursor:
            migrate_pool_images(cursor)
            migrate_item_images(cursor)
            migrate_item_videos(cursor)

        conn.commit()
        print("\n=== 全部迁移完成！===")

    finally:
        conn.close()


def migrate_pool_images(cursor):
    """迁移 gacha_pool 的 bg_image_url 和 thumbnail_url → /uploads/pools/"""
    upload_dir = os.path.join(BASE_UPLOAD_DIR, 'pools')
    os.makedirs(upload_dir, exist_ok=True)

    cursor.execute("SELECT id, name, bg_image_url, thumbnail_url FROM gacha_pool")
    pools = cursor.fetchall()
    print(f"[卡池图片] 找到 {len(pools)} 个卡池")

    for pool_id, name, bg_data, thumb_data in pools:
        if bg_data and bg_data.startswith('data:'):
            filename = save_base64(bg_data, upload_dir, f'pool_{pool_id}_bg')
            cursor.execute("UPDATE gacha_pool SET bg_image_url = %s WHERE id = %s",
                           (f'/uploads/pools/{filename}', pool_id))
            print(f"  卡池 {pool_id} 背景图: {filename}")

        if thumb_data and thumb_data.startswith('data:'):
            filename = save_base64(thumb_data, upload_dir, f'pool_{pool_id}_thumb')
            cursor.execute("UPDATE gacha_pool SET thumbnail_url = %s WHERE id = %s",
                           (f'/uploads/pools/{filename}', pool_id))
            print(f"  卡池 {pool_id} 缩略图: {filename}")


def migrate_item_images(cursor):
    """迁移 gacha_items 的 image_url → /uploads/images/"""
    upload_dir = os.path.join(BASE_UPLOAD_DIR, 'images')
    os.makedirs(upload_dir, exist_ok=True)

    cursor.execute("SELECT id, name, image_url FROM gacha_items WHERE image_url LIKE 'data:%'")
    items = cursor.fetchall()
    print(f"\n[物品图片] 找到 {len(items)} 个 base64 图片")

    for item_id, name, image_data in items:
        safe_name = name.replace(' ', '_').replace('/', '_')
        filename = save_base64(image_data, upload_dir, f'item_{item_id}_{safe_name}')
        cursor.execute("UPDATE gacha_items SET image_url = %s WHERE id = %s",
                       (f'/uploads/images/{filename}', item_id))
        print(f"  物品 {item_id} ({name}): {filename}")


def migrate_item_videos(cursor):
    """迁移 gacha_items 的 video_url 和 loop_video_url → /uploads/videos/"""
    upload_dir = os.path.join(BASE_UPLOAD_DIR, 'videos')
    os.makedirs(upload_dir, exist_ok=True)

    for col, label in [('video_url', '唤取视频'), ('loop_video_url', '循环视频')]:
        cursor.execute(f"SELECT id, name, {col} FROM gacha_items WHERE {col} LIKE 'data:%'")
        items = cursor.fetchall()
        print(f"\n[物品{label}] 找到 {len(items)} 个 base64 视频")

        for item_id, name, video_data in items:
            safe_name = name.replace(' ', '_').replace('/', '_')
            filename = save_base64(video_data, upload_dir, f'item_{item_id}_{safe_name}_{col}')
            cursor.execute(f"UPDATE gacha_items SET {col} = %s WHERE id = %s",
                           (f'/uploads/videos/{filename}', item_id))
            print(f"  物品 {item_id} ({name}): {filename}")


def save_base64(data_url, upload_dir, base_name):
    """解析 data URL 并保存为文件，返回文件名"""
    # data:image/png;base64,... 或 data:video/mp4;base64,...
    header, b64data = data_url.split(',', 1)

    # 从 header 提取扩展名
    mime = header.split(';')[0].split(':')[1]  # e.g. image/png
    ext_map = {
        'image/png': 'png', 'image/jpeg': 'jpg', 'image/gif': 'gif', 'image/webp': 'webp',
        'video/mp4': 'mp4', 'video/webm': 'webm',
    }
    ext = ext_map.get(mime, 'bin')

    filename = f'{base_name}.{ext}'
    filepath = os.path.join(upload_dir, filename)
    with open(filepath, 'wb') as f:
        f.write(base64.b64decode(b64data))

    return filename


if __name__ == '__main__':
    migrate()
