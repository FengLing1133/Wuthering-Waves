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

            document.getElementById('usernameDisplay').textContent = user.username;
            document.getElementById('starlightCount').textContent = user.starlight;
            document.getElementById('starshardsCount').textContent = user.starshards;
        }
    } catch (error) {
        console.error('获取用户信息失败:', error);
        document.getElementById('usernameDisplay').textContent = '加载失败';
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
        const tbody = document.getElementById('historyTableBody');
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-secondary);">加载失败，请稍后重试</td></tr>';
    }
}

// 渲染历史记录表格
function renderHistoryTable(records) {
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '';

    if (!records || records.length === 0) {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 6;
        td.style.textAlign = 'center';
        td.style.color = 'var(--text-secondary)';
        td.textContent = '暂无记录';
        tr.appendChild(td);
        tbody.appendChild(tr);
        return;
    }

    const poolNames = { 'character': '角色池', 'weapon': '武器池', 'limited': '限定池' };

    records.forEach(record => {
        const tr = document.createElement('tr');

        const date = new Date(record.createdAt);
        const timeStr = date.toLocaleString('zh-CN');
        const poolName = poolNames[record.poolType] || record.poolType;

        let rarityStars = '';
        for (let i = 0; i < record.itemRarity; i++) {
            rarityStars += '★';
        }

        const typeText = record.itemType === 'character' ? '角色' : '武器';
        const rarityClass = record.itemRarity === 5 ? 'five-star' : record.itemRarity === 4 ? 'four-star' : 'three-star';

        const fields = [timeStr, poolName, record.itemName, rarityStars, typeText, record.pityCount || '-'];
        fields.forEach((value, idx) => {
            const td = document.createElement('td');
            td.textContent = value;
            if (idx === 3) td.className = rarityClass;
            tr.appendChild(td);
        });

        tbody.appendChild(tr);
    });
}

// 渲染统计数据
function renderHistoryStats(stats) {
    const statsContainer = document.getElementById('historyStats');
    statsContainer.innerHTML = '';

    const statItems = [
        { value: stats.totalPulls || 0, label: '总抽取次数', cls: '' },
        { value: stats.fiveStarCount || 0, label: '五星数量', cls: 'five-star' },
        { value: stats.fourStarCount || 0, label: '四星数量', cls: 'four-star' },
        { value: stats.currentPity || 0, label: '当前保底计数', cls: '' }
    ];

    statItems.forEach(item => {
        const card = document.createElement('div');
        card.className = `stat-card ${item.cls}`.trim();

        const valueDiv = document.createElement('div');
        valueDiv.className = 'stat-value';
        valueDiv.textContent = item.value;

        const labelDiv = document.createElement('div');
        labelDiv.className = 'stat-label';
        labelDiv.textContent = item.label;

        card.appendChild(valueDiv);
        card.appendChild(labelDiv);
        statsContainer.appendChild(card);
    });
}

// 退出登录
function logout() {
    API.clearToken();
    window.location.href = '/login.html';
}
