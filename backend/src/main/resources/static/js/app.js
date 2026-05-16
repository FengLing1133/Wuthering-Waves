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
    Gacha.init();
    initHistory();
    bindNavigation();
    document.getElementById('logoutBtn').addEventListener('click', logout);
}

async function loadUserInfo() {
    try {
        const result = await API.getUserInfo();
        if (result.success) {
            const user = result.user;
            API.setUser(user);

            document.getElementById('astraliteCount').textContent = user.starlight || 0;
            document.getElementById('lustrumCount').textContent = user.starshards || 0;
            document.getElementById('coralsCount').textContent = user.corals || 0;
        }
    } catch (error) {
        console.error('获取用户信息失败:', error);
    }
}

function bindNavigation() {
    document.getElementById('historyBtn').addEventListener('click', () => {
        showHistory();
    });
}

function showHistory() {
    document.getElementById('historyView').classList.remove('hidden');
    loadHistory();
}

function initHistory() {
    document.getElementById('historyPoolFilter').addEventListener('change', () => {
        loadHistory();
    });
}

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

    const poolNames = {
        'character-1': '角色池1',
        'character-2': '角色池2',
        'character-3': '角色池3',
        'weapon-1': '武器池1',
        'weapon-2': '武器池2',
        'weapon-3': '武器池3',
        'standard-character': '常驻角色',
        'standard-weapon': '常驻武器'
    };

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

function logout() {
    API.clearToken();
    window.location.href = '/login.html';
}
