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

# 图片保存目录（项目静态资源目录）
UPLOAD_DIR = '../backend/src/main/resources/static/uploads/pools'

def migrate():
    os.makedirs(UPLOAD_DIR, exist_ok=True)

    conn = pymysql.connect(**DB_CONFIG)

    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT id, name, bg_image_url, thumbnail_url FROM gacha_pool")
            pools = cursor.fetchall()

            print(f"找到 {len(pools)} 个卡池")

            for pool_id, name, bg_data, thumb_data in pools:
                print(f"\n处理卡池 {pool_id}: {name}")

                # 处理背景图
                if bg_data and bg_data.startswith('data:image'):
                    filename = save_image(bg_data, pool_id, 'bg')
                    cursor.execute(
                        "UPDATE gacha_pool SET bg_image_url = %s WHERE id = %s",
                        (f'/uploads/pools/{filename}', pool_id)
                    )
                    print(f"  背景图已保存: {filename}")

                # 处理缩略图
                if thumb_data and thumb_data.startswith('data:image'):
                    filename = save_image(thumb_data, pool_id, 'thumb')
                    cursor.execute(
                        "UPDATE gacha_pool SET thumbnail_url = %s WHERE id = %s",
                        (f'/uploads/pools/{filename}', pool_id)
                    )
                    print(f"  缩略图已保存: {filename}")

            conn.commit()
            print("\n=== 迁移完成！===")

    finally:
        conn.close()

def save_image(base64_data, pool_id, img_type):
    # 解析 base64
    header, data = base64_data.split(',')

    # 获取扩展名
    ext = 'png' if 'png' in header else 'jpg'

    # 生成文件名
    filename = f'pool_{pool_id}_{img_type}.{ext}'

    # 保存文件
    filepath = os.path.join(UPLOAD_DIR, filename)
    with open(filepath, 'wb') as f:
        f.write(base64.b64decode(data))

    return filename

if __name__ == '__main__':
    migrate()
