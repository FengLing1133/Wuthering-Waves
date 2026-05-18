// 主应用模块

document.addEventListener('DOMContentLoaded', async () => {
    // 检查登录状态
    if (!API.getToken()) {
        window.location.href = '/login.html';
        return;
    }

    await initApp();
});

async function initApp() {
    await loadUserInfo();
    await Gacha.init();
    document.getElementById('logoutBtn').addEventListener('click', logout);
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
