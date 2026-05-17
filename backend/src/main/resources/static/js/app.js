// 主应用模块
let currentHistoryPage = 1;
const HISTORY_PAGE_SIZE = 20;

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
    Gacha.init().catch(err => console.error('初始化失败:', err));
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
    currentHistoryPage = 1;
    document.getElementById('historyView').classList.remove('hidden');
    loadHistory(1);
}

function hideHistory() {
    document.getElementById('historyView').classList.add('hidden');
}

function initHistory() {
    document.getElementById('historyPoolFilter').addEventListener('change', () => {
        currentHistoryPage = 1;
        loadHistory(1);
    });
    document.getElementById('closeHistoryBtn').addEventListener('click', hideHistory);
}

async function loadHistory(page = 1) {
    currentHistoryPage = page;
    const poolType = document.getElementById('historyPoolFilter').value;

    try {
        const [historyResult, statsResult] = await Promise.all([
            API.getHistory(poolType, page, HISTORY_PAGE_SIZE),
            API.getStats(poolType)
        ]);

        if (historyResult.success) {
            renderHistoryTable(historyResult.records);
            renderPagination(historyResult.total || 0, page);
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
        'limited-character': '限定角色池',
        'limited-weapon': '限定武器池',
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
        { value: stats.fiveStarRate || '0.00%', label: '五星概率', cls: 'five-star' },
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

function renderPagination(total, currentPage) {
    const container = document.getElementById('historyPagination');
    container.innerHTML = '';

    const totalPages = Math.ceil(total / HISTORY_PAGE_SIZE);
    if (totalPages <= 1) return;

    // 上一页按钮
    const prevBtn = document.createElement('button');
    prevBtn.className = 'page-btn';
    prevBtn.textContent = '‹';
    prevBtn.disabled = currentPage <= 1;
    prevBtn.addEventListener('click', () => loadHistory(currentPage - 1));
    container.appendChild(prevBtn);

    // 页码按钮
    const maxVisible = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let endPage = Math.min(totalPages, startPage + maxVisible - 1);
    if (endPage - startPage < maxVisible - 1) {
        startPage = Math.max(1, endPage - maxVisible + 1);
    }

    if (startPage > 1) {
        const firstBtn = document.createElement('button');
        firstBtn.className = 'page-btn';
        firstBtn.textContent = '1';
        firstBtn.addEventListener('click', () => loadHistory(1));
        container.appendChild(firstBtn);
        if (startPage > 2) {
            const dots = document.createElement('span');
            dots.className = 'page-dots';
            dots.textContent = '...';
            container.appendChild(dots);
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.className = `page-btn ${i === currentPage ? 'active' : ''}`;
        pageBtn.textContent = i;
        pageBtn.addEventListener('click', () => loadHistory(i));
        container.appendChild(pageBtn);
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            const dots = document.createElement('span');
            dots.className = 'page-dots';
            dots.textContent = '...';
            container.appendChild(dots);
        }
        const lastBtn = document.createElement('button');
        lastBtn.className = 'page-btn';
        lastBtn.textContent = totalPages;
        lastBtn.addEventListener('click', () => loadHistory(totalPages));
        container.appendChild(lastBtn);
    }

    // 下一页按钮
    const nextBtn = document.createElement('button');
    nextBtn.className = 'page-btn';
    nextBtn.textContent = '›';
    nextBtn.disabled = currentPage >= totalPages;
    nextBtn.addEventListener('click', () => loadHistory(currentPage + 1));
    container.appendChild(nextBtn);

    // 页码信息
    const pageInfo = document.createElement('span');
    pageInfo.className = 'page-info';
    pageInfo.textContent = `共 ${total} 条`;
    container.appendChild(pageInfo);
}

function logout() {
    API.clearToken();
    window.location.href = '/login.html';
}
