// 认证模块
document.addEventListener('DOMContentLoaded', () => {
    // 检查是否已登录
    if (API.getToken()) {
        window.location.href = '/index.html';
        return;
    }

    // 初始化标签页切换
    initTabs();

    // 初始化表单
    initLoginForm();
    initRegisterForm();
});

// 初始化标签页切换
function initTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            // 移除所有 active 类
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // 切换表单显示
            const tab = btn.dataset.tab;
            if (tab === 'login') {
                loginForm.classList.remove('hidden');
                registerForm.classList.add('hidden');
            } else {
                loginForm.classList.add('hidden');
                registerForm.classList.remove('hidden');
            }

            // 清除消息
            clearMessage();
        });
    });
}

// 初始化登录表单
function initLoginForm() {
    const loginForm = document.getElementById('loginForm');

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        if (!username || !password) {
            showMessage('请填写用户名和密码', 'error');
            return;
        }

        try {
            const result = await API.login(username, password);

            if (result.success) {
                API.setToken(result.token);
                API.setUser(result.user);
                showMessage('登录成功，正在跳转...', 'success');
                setTimeout(() => {
                    window.location.href = '/index.html';
                }, 1000);
            } else {
                showMessage(result.message || '登录失败', 'error');
            }
        } catch (error) {
            showMessage('网络错误，请稍后重试', 'error');
        }
    });
}

// 初始化注册表单
function initRegisterForm() {
    const registerForm = document.getElementById('registerForm');

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = document.getElementById('regUsername').value.trim();
        const password = document.getElementById('regPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (!username || !password || !confirmPassword) {
            showMessage('请填写所有字段', 'error');
            return;
        }

        if (username.length < 3 || username.length > 20) {
            showMessage('用户名长度需在3-20之间', 'error');
            return;
        }

        if (password.length < 6) {
            showMessage('密码长度不能少于6位', 'error');
            return;
        }

        if (password !== confirmPassword) {
            showMessage('两次输入的密码不一致', 'error');
            return;
        }

        try {
            const result = await API.register(username, password);

            if (result.success) {
                API.setToken(result.token);
                API.setUser(result.user);
                showMessage('注册成功，正在跳转...', 'success');
                setTimeout(() => {
                    window.location.href = '/index.html';
                }, 1000);
            } else {
                showMessage(result.message || '注册失败', 'error');
            }
        } catch (error) {
            showMessage('网络错误，请稍后重试', 'error');
        }
    });
}

// 显示消息
function showMessage(text, type) {
    const messageEl = document.getElementById('message');
    messageEl.textContent = text;
    messageEl.className = `message ${type}`;
}

// 清除消息
function clearMessage() {
    const messageEl = document.getElementById('message');
    messageEl.textContent = '';
    messageEl.className = 'message';
}
