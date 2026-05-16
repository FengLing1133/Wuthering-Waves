// API 封装模块
const API = {
    baseURL: 'http://localhost:8080/api',

    // 获取存储的 token
    getToken() {
        return localStorage.getItem('token');
    },

    // 设置 token
    setToken(token) {
        localStorage.setItem('token', token);
    },

    // 清除 token
    clearToken() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },

    // 获取用户信息
    getUser() {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },

    // 设置用户信息
    setUser(user) {
        localStorage.setItem('user', JSON.stringify(user));
    },

    // 通用请求方法
    async request(url, options = {}) {
        const token = this.getToken();
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(`${this.baseURL}${url}`, {
                ...options,
                headers
            });

            const data = await response.json();

            if (response.status === 401) {
                this.clearToken();
                window.location.href = '/login.html';
                return data;
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    },

    // 注册
    async register(username, password) {
        return this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
    },

    // 登录
    async login(username, password) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
    },

    // 获取用户信息
    async getUserInfo() {
        return this.request('/auth/user');
    },

    // 抽卡
    async pull(poolType, count) {
        return this.request('/gacha/pull', {
            method: 'POST',
            body: JSON.stringify({ poolType, count })
        });
    },

    // 获取抽卡历史
    async getHistory(poolType = 'character', page = 1, size = 20) {
        return this.request(`/gacha/history?poolType=${poolType}&page=${page}&size=${size}`);
    },

    // 获取统计数据
    async getStats(poolType = 'character') {
        return this.request(`/gacha/stats?poolType=${poolType}`);
    }
};
