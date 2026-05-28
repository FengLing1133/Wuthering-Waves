// API 封装模块
const API = {
    baseURL: '/api',

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

        headers['ngrok-skip-browser-warning'] = 'true';

        try {
            const response = await fetch(`${this.baseURL}${url}`, {
                ...options,
                headers
            });

            if (response.status === 401) {
                this.clearToken();
                window.location.href = '/login.html';
                throw new Error('Unauthorized');
            }

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            return await response.json();
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
    async pull(poolType, count, poolId) {
        return this.request('/gacha/pull', {
            method: 'POST',
            body: JSON.stringify({ poolType, count, poolId })
        });
    },

    // 获取抽卡历史
    async getHistory(poolType = 'character', page = 1, size = 20) {
        return this.request(`/gacha/history?poolType=${poolType}&page=${page}&size=${size}`);
    },

    // 获取统计数据
    async getStats(poolType = 'character') {
        return this.request(`/gacha/stats?poolType=${poolType}`);
    },

    // 获取活跃卡池列表（公开，无需认证）
    async getActivePools() {
        return this.request('/gacha/pools');
    },

    // 获取卡池详情（公开，无需认证）
    async getPoolDetail(poolId) {
        return this.request(`/gacha/pools/${poolId}`);
    },

    // 获取常驻武器池可选UP列表
    async getStandardWeaponUpOptions() {
        return this.request('/gacha/standard-weapon-up');
    },

    // 设置常驻武器池UP
    async setStandardWeaponUp(weaponId) {
        return this.request('/gacha/standard-weapon-up', {
            method: 'PUT',
            body: JSON.stringify({ weaponId })
        });
    },

    // 获取抽卡分析数据
    async getAnalysis() {
        return this.request('/gacha/analysis');
    },

    // ========== 管理员接口 ==========

    // 删除卡池
    async deletePool(id) {
        return this.request(`/admin/pools/${id}`, {
            method: 'DELETE'
        });
    },

    // 将文件转为 base64 data URL
    fileToBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });
    }
};
