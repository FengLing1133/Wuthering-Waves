// 主应用模块

document.addEventListener('DOMContentLoaded', async () => {
    // 检查登录状态
    if (!API.getToken()) {
        window.location.href = '/login.html';
        return;
    }

    // 初始化 CDN 配置
    initCDN();

    await initApp();
});

// 初始化 CDN 配置
function initCDN() {
    // ============================================
    // 部署时请修改这里的 baseUrl 为你的 COS 域名
    // 格式：https://<BucketName-APPID>.cos.<Region>.myqcloud.com
    // ============================================
    const COS_BASE_URL = 'https://wuthering-waves-gacha-1437401585.cos.ap-guangzhou.myqcloud.com';

    if (COS_BASE_URL) {
        CDN.init({
            baseUrl: COS_BASE_URL,
            enabled: true
        });
        console.log('CDN 配置已启用，资源将从 COS 加载');
    } else {
        console.log('CDN 未配置，使用本地资源');
    }
}

async function initApp() {
    await loadUserInfo();
    await Gacha.init();
    document.getElementById('logoutBtn').addEventListener('click', logout);

    // 移动端首次交互时尝试全屏
    if (/Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent)) {
        setupMobileFullscreen();
    }
}

// 移动端全屏：首次点击抽卡按钮时触发
function setupMobileFullscreen() {
    let fullscreenRequested = false;
    const pullBtns = document.querySelectorAll('.pull-btn');
    const requestFS = () => {
        if (fullscreenRequested) return;
        fullscreenRequested = true;
        const el = document.documentElement;
        const request = el.requestFullscreen || el.webkitRequestFullscreen || el.mozRequestFullScreen;
        if (request) {
            request.call(el).catch(() => {});
        }
    };
    pullBtns.forEach(btn => btn.addEventListener('click', requestFS, { once: false }));
}

async function loadUserInfo() {
    try {
        const result = await API.getUserInfo();
        if (result.success) {
            const user = result.user;
            API.setUser(user);

            document.getElementById('astraliteCount').textContent = user.starlight || 0;

        }
    } catch (error) {
        console.error('获取用户信息失败:', error);
    }
}

function logout() {
    API.clearToken();
    window.location.href = '/login.html';
}
