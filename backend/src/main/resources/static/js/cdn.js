/**
 * CDN 配置模块
 * 用于从腾讯云 COS 加载静态资源，减轻服务器带宽压力
 */
const CDN = {
    // COS 存储桶域名（部署时替换为你的实际域名）
    // 格式：https://<BucketName-APPID>.cos.<Region>.myqcloud.com
    baseUrl: '',

    // 本地 fallback 基础路径
    localBase: '',

    // 是否启用 CDN
    enabled: false,

    /**
     * 初始化 CDN 配置
     * @param {Object} config - 配置对象
     * @param {string} config.baseUrl - COS 域名
     * @param {boolean} config.enabled - 是否启用
     */
    init(config = {}) {
        if (config.baseUrl) {
            this.baseUrl = config.baseUrl.replace(/\/$/, '');
            this.enabled = config.enabled !== false;
        }

        // 自动检测：如果配置了 baseUrl 就启用
        if (this.baseUrl) {
            this.enabled = true;
            console.log('CDN 已启用:', this.baseUrl);
        }
    },

    /**
     * 获取资源 URL
     * @param {string} path - 资源路径（如 '/videos/3star.mp4'）
     * @param {boolean} fallbackToLocal - 是否 fallback 到本地
     * @returns {string} 完整 URL
     */
    getUrl(path, fallbackToLocal = true) {
        // 确保路径以 / 开头
        if (!path.startsWith('/')) {
            path = '/' + path;
        }

        // 如果 CDN 启用，优先使用 CDN
        if (this.enabled && this.baseUrl) {
            return `${this.baseUrl}/gacha${path}`;
        }

        // 否则使用本地路径
        return path;
    },

    /**
     * 获取视频 URL
     * @param {string} videoName - 视频文件名（如 '3star.mp4'）
     * @returns {string} 视频 URL
     */
    getVideoUrl(videoName) {
        return this.getUrl(`/videos/${videoName}`);
    },

    /**
     * 获取图片 URL
     * @param {string} imagePath - 图片路径（相对于 images 目录）
     * @returns {string} 图片 URL
     */
    getImageUrl(imagePath) {
        return this.getUrl(`/images/${imagePath}`);
    },

    /**
     * 获取角色视频 URL
     * @param {string} characterName - 角色名（如 'daniya'）
     * @param {number} videoIndex - 视频序号（1 或 2）
     * @returns {string} 视频 URL
     */
    getCharacterVideoUrl(characterName, videoIndex) {
        return this.getUrl(`/videos/characters/${characterName}${videoIndex}.mp4`);
    },

    /**
     * 获取上传文件 URL
     * @param {string} filePath - 文件路径（相对于 uploads 目录）
     * @returns {string} 文件 URL
     */
    getUploadUrl(filePath) {
        // 确保路径不以 / 开头
        if (filePath.startsWith('/')) {
            filePath = filePath.substring(1);
        }
        return this.getUrl(`/uploads/${filePath}`);
    },

    /**
     * 解析 URL（自动检测 /uploads/ 路径并转换为 CDN）
     * @param {string} url - 原始 URL
     * @returns {string} 解析后的 URL
     */
    resolveUrl(url) {
        if (!url) return url;

        // 如果 CDN 未启用，直接返回
        if (!this.enabled || !this.baseUrl) {
            return url;
        }

        // 如果以 /uploads/ 开头，转换为 CDN 路径
        if (url.startsWith('/uploads/')) {
            return this.getUploadUrl(url.replace('/uploads/', ''));
        }

        // 其他路径直接返回
        return url;
    },

    /**
     * 预加载资源（可选）
     * @param {string[]} urls - 要预加载的 URL 列表
     */
    preload(urls) {
        urls.forEach(url => {
            const link = document.createElement('link');
            link.rel = 'preload';
            link.href = url;

            // 根据文件类型设置 as 属性
            if (url.endsWith('.mp4')) {
                link.as = 'video';
            } else if (url.match(/\.(png|jpg|jpeg|gif|webp)$/)) {
                link.as = 'image';
            } else if (url.endsWith('.css')) {
                link.as = 'style';
            } else if (url.endsWith('.js')) {
                link.as = 'script';
            }

            document.head.appendChild(link);
        });
    }
};

// 初始化示例（在 index.html 中配置）
// CDN.init({
//     baseUrl: 'https://wuthering-waves-gacha-1250000000.cos.ap-guangzhou.myqcloud.com',
//     enabled: true
// });

// 导出供其他模块使用
window.CDN = CDN;
