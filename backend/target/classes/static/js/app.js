// 主应用模块
document.addEventListener('DOMContentLoaded', async () => {
    // 检查登录状态
    if (!API.getToken()) {
        window.location.href = '/login.html';
        return;
    }

    // 初始化应用
    await initApp();
});

// 初始化应用
async function initApp() {
    // 获取用户信息
    await loadUserInfo();

    // 初始化抽卡模块
    Gacha.init();

    // 初始化历史记录
    initHistory();

    // 绑定导航事件
    bindNavigation();

    // 绑定退出按钮
    document.getElementById('logoutBtn').addEventListener('click', logout);
}

// 加载用户信息
async function loadUserInfo() {
    try {
        const result = await API.getUserInfo();
        if (result.success) {
            const user = result.user;
            API.setUser(user);

            // 更新界面显示
            document.getElementById('usernameDisplay').textContent = user.username;
            document.getElementById('starlightCount').textContent = user.starlight;
            document.getElementById('starshardsCount').textContent = user.starshards;
        }
    } catch (error) {
        console.error('获取用户信息失败:', error);
    }
}

// 绑定导航事件
function bindNavigation() {
    // 历史记录按钮
    document.querySelector('.nav-btn[data-page="history"]').addEventListener('click', () => {
        showHistory();
    });
}

// 显示历史记录
function showHistory() {
    // 切换视图
    document.getElementById('gachaView').classList.add('hidden');
    document.getElementById('historyView').classList.remove('hidden');

    // 更新导航状态
    document.querySelectorAll('.nav-btn[data-pool]').forEach(btn => {
        btn.classList.remove('active');
    });
    document.querySelector('.nav-btn[data-page="history"]').classList.add('active');

    // 加载历史数据
    loadHistory();
}

// 初始化历史记录
function initHistory() {
    // 池子筛选
    document.getElementById('historyPoolFilter').addEventListener('change', () => {
        loadHistory();
    });
}

// 加载历史记录
async function loadHistory(page = 1) {
    const poolType = document.getElementById('historyPoolFilter').value;

    try {
        // 并行获取历史记录和统计数据
        const [historyResult, statsResult] = await Promise.all([
            API.getHistory(poolType, page, 20),
            API.getStats(poolType)
        ]);

        if (historyResult.success) {
            renderHistoryTable(historyResult.records);
        }

        if (statsResult.success) {
            renderHistoryStats(statsResult.stats);
        }
    } catch (error) {
        console.error('加载历史记录失败:', error);
    }
}

// 渲染历史记录表格
function renderHistoryTable(records) {
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '';

    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-secondary);">暂无记录</td></tr>';
        return;
    }

    records.forEach(record => {
        const tr = document.createElement('tr');

        // 格式化时间
        const date = new Date(record.createdAt);
        const timeStr = date.toLocaleString('zh-CN');

        // 池子名称
        const poolNames = {
            'character': '角色池',
            'weapon': '武器池',
            'limited': '限定池'
        };
        const poolName = poolNames[record.poolType] || record.poolType;

        // 稀有度星星
        let rarityStars = '';
        for (let i = 0; i < record.itemRarity; i++) {
            rarityStars += '★';
        }

        // 类型
        const typeText = record.itemType === 'character' ? '角色' : '武器';

        tr.innerHTML = `
            <td>${timeStr}</td>
            <td>${poolName}</td>
            <td>${record.itemName}</td>
            <td class="${record.itemRarity === 5 ? 'five-star' : record.itemRarity === 4 ? 'four-star' : 'three-star'}">${rarityStars}</td>
            <td>${typeText}</td>
            <td>${record.pityCount || '-'}</td>
        `;

        tbody.appendChild(tr);
    });
}

// 渲染统计数据
function renderHistoryStats(stats) {
    const statsContainer = document.getElementById('historyStats');

    statsContainer.innerHTML = `
        <div class="stat-card">
            <div class="stat-value">${stats.totalPulls || 0}</div>
            <div class="stat-label">总抽取次数</div>
        </div>
        <div class="stat-card five-star">
            <div class="stat-value">${stats.fiveStarCount || 0}</div>
            <div class="stat-label">五星数量</div>
        </div>
        <div class="stat-card four-star">
            <div class="stat-value">${stats.fourStarCount || 0}</div>
            <div class="stat-label">四星数量</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">${stats.currentPity || 0}</div>
            <div class="stat-label">当前保底计数</div>
        </div>
    `;
}

// 退出登录
function logout() {
    API.clearToken();
    window.location.href = '/login.html';
}
