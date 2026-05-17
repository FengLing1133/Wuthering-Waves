// 抽卡模块
const Gacha = {
    currentPool: null,
    currentPoolData: null,
    isPulling: false,
    poolList: [],

    // 初始化
    async init() {
        this.bindEvents();
        this.createSnowflakes();
        await this.loadPools();
    },

    // 从API加载卡池列表
    async loadPools() {
        try {
            const result = await API.getActivePools();
            if (result.success && result.pools && result.pools.length > 0) {
                this.poolList = result.pools;
                this.renderSidebar();
                // 默认选中第一个
                this.switchPool(this.poolList[0].id);
            } else {
                document.getElementById('bannerSlots').innerHTML =
                    '<div style="text-align:center;padding:20px;color:var(--text-dim);">暂无活动卡池</div>';
            }
        } catch (error) {
            console.error('加载卡池列表失败:', error);
            document.getElementById('bannerSlots').innerHTML =
                '<div style="text-align:center;padding:20px;color:var(--text-dim);">加载失败</div>';
        }
    },

    // 动态渲染侧栏
    renderSidebar() {
        const container = document.getElementById('bannerSlots');
        container.innerHTML = this.poolList.map((pool, index) => {
            const thumbStyle = pool.thumbnailUrl
                ? `background-image: url('${pool.thumbnailUrl}'); background-size: cover; background-position: center;`
                : 'background: linear-gradient(135deg, #1a2a4a, #0d1b2e);';
            const isActive = index === 0 ? 'active' : '';
            const tag = pool.poolType && pool.poolType.startsWith('standard-') ? '常驻' : '活动';
            return `<div class="banner-slot ${isActive}" data-pool-id="${pool.id}">
                <div class="banner-thumb">
                    <div class="banner-thumb-img" style="${thumbStyle}"></div>
                </div>
                <span class="banner-tag">${tag}</span>
            </div>`;
        }).join('');

        // 绑定点击事件
        container.querySelectorAll('.banner-slot').forEach(slot => {
            slot.addEventListener('click', () => {
                const poolId = parseInt(slot.dataset.poolId);
                this.switchPool(poolId);
            });
        });
    },

    // 切换卡池
    async switchPool(poolId) {
        this.currentPool = poolId;

        // 更新侧边栏选中状态
        document.querySelectorAll('.banner-slot').forEach(slot => {
            slot.classList.toggle('active', parseInt(slot.dataset.poolId) === poolId);
        });

        // 隐藏历史记录，显示主界面
        document.getElementById('historyView').classList.add('hidden');

        // 获取卡池详情
        try {
            const result = await API.getPoolDetail(poolId);
            if (result.success) {
                this.currentPoolData = result.pool;
                this.updatePoolInfo(this.currentPoolData);
                this.updatePityCount();
            }
        } catch (error) {
            console.error('获取卡池详情失败:', error);
        }
    },

    // 更新卡池信息
    updatePoolInfo(pool) {
        if (!pool) return;

        document.getElementById('bannerSubtitle').textContent = pool.poolType && pool.poolType.startsWith('standard-') ? '常驻唤取' : '角色活动唤取';
        document.getElementById('bannerTitle').textContent = pool.name || '';
        document.getElementById('bannerTimer').textContent = this.formatTimer(pool);
        document.getElementById('upCharacterName').textContent = this.getUpCharacterName(pool);
        document.getElementById('upElement').querySelector('.element-icon').textContent = this.getUpElement(pool);
        document.getElementById('maxPityDisplay').textContent = pool.maxPity || 90;

        // 常驻池隐藏UP角色信息
        const upSection = document.getElementById('upCharacter');
        if (upSection) {
            upSection.style.display = pool.poolType && pool.poolType.startsWith('standard-') ? 'none' : '';
        }

        // 更新背景图
        const bgEl = document.getElementById('bannerBg');
        if (bgEl) {
            const bgUrl = pool.bgImageUrl || pool.imageUrl;
            if (bgUrl) {
                bgEl.style.backgroundImage = `url('${bgUrl}')`;
            }
        }

        // 更新四星概率提升头像
        this.updateRateUpAvatars(pool);
    },

    // 更新四星概率提升头像显示
    updateRateUpAvatars(pool) {
        const rateUpList = document.getElementById('rateUpList');
        const rateUpSection = document.getElementById('rateUpSection');
        if (!rateUpList || !rateUpSection) return;

        if (pool.fourStarAvatars && pool.fourStarAvatars.length > 0) {
            rateUpSection.style.display = '';
            rateUpList.innerHTML = pool.fourStarAvatars.map(avatar => `
                <div class="rate-up-item">
                    <div class="rate-up-avatar" style="background-image: url('${avatar.avatarUrl}'); background-size: cover; background-position: center top;"></div>
                </div>
            `).join('');
        } else {
            rateUpSection.style.display = 'none';
        }
    },

    // 格式化倒计时
    formatTimer(pool) {
        if (pool.endTime) {
            const end = new Date(pool.endTime);
            const now = new Date();
            const diff = end - now;
            if (diff <= 0) return '已结束';
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            return `${days}天${hours}小时`;
        }
        if (pool.poolType && pool.poolType.startsWith('standard-')) return '永久开放';
        return '永久开放';
    },

    // 获取UP角色名
    getUpCharacterName(pool) {
        if (pool.upItems) {
            try {
                let items = pool.upItems.trim();
                if (items.startsWith('[') && items.endsWith(']')) {
                    items = items.substring(1, items.length - 1);
                    const names = items.split(',').map(s => s.trim().replace(/"/g, '').replace(/'/g, ''));
                    return names[0] || 'UP角色';
                }
            } catch (e) {}
        }
        return pool.poolType && pool.poolType.includes('weapon') ? 'UP武器' : 'UP角色';
    },

    // 获取元素图标
    getUpElement(pool) {
        if (pool.poolType && pool.poolType.includes('weapon')) return '⚔';
        return '✦';
    },

    // 绑定事件
    bindEvents() {
        // 单抽按钮
        document.getElementById('singlePullBtn').addEventListener('click', () => {
            this.pull(1);
        });

        // 十连按钮
        document.getElementById('tenPullBtn').addEventListener('click', () => {
            this.pull(10);
        });

        // 关闭结果
        document.getElementById('closeResultBtn').addEventListener('click', () => {
            this.closeResult();
        });

        document.getElementById('confirmResultBtn').addEventListener('click', () => {
            this.closeResult();
        });

        // 历史记录按钮
        document.getElementById('historyBtn').addEventListener('click', () => {
            this.showHistory();
        });
    },

    // 获取当前卡池的poolType（用于抽卡API调用）
    getCurrentPoolType() {
        if (this.currentPoolData) {
            return this.currentPoolData.poolType;
        }
        return 'limited-character';
    },

    // 更新保底计数
    async updatePityCount() {
        try {
            const poolType = this.getCurrentPoolType();
            const result = await API.getStats(poolType);
            if (result.success && result.stats) {
                this.updatePityDisplay(result.stats.currentPity || 0);
            }
        } catch (error) {
            console.error('获取保底信息失败:', error);
        }
    },

    // 更新保底显示UI
    updatePityDisplay(currentPity) {
        const maxPity = this.currentPoolData ? (this.currentPoolData.maxPity || 90) : 90;
        const pityNumber = document.getElementById('pityNumber');
        const pityRingProgress = document.getElementById('pityRingProgress');

        if (pityNumber) {
            pityNumber.textContent = currentPity;
        }

        if (pityRingProgress) {
            const circumference = 2 * Math.PI * 16;
            const progress = currentPity / maxPity;
            const offset = circumference * (1 - progress);
            pityRingProgress.style.strokeDasharray = circumference;
            pityRingProgress.style.strokeDashoffset = offset;

            if (currentPity >= maxPity - 10) {
                pityRingProgress.style.stroke = '#ff4a6a';
                pityNumber.style.color = '#ff4a6a';
            } else if (currentPity >= maxPity - 20) {
                pityRingProgress.style.stroke = '#f0d060';
                pityNumber.style.color = '#f0d060';
            } else {
                pityRingProgress.style.stroke = 'var(--accent-blue)';
                pityNumber.style.color = 'var(--accent-blue)';
            }
        }
    },

    // 创建雪花效果
    createSnowflakes() {
        const container = document.getElementById('snowParticles');
        if (!container) return;

        const count = 30;
        for (let i = 0; i < count; i++) {
            const flake = document.createElement('div');
            flake.className = 'snowflake';
            const size = Math.random() * 3 + 1;
            flake.style.width = size + 'px';
            flake.style.height = size + 'px';
            flake.style.left = Math.random() * 100 + '%';
            flake.style.animationDuration = (Math.random() * 8 + 6) + 's';
            flake.style.animationDelay = (Math.random() * 10) + 's';
            flake.style.opacity = Math.random() * 0.5 + 0.2;
            container.appendChild(flake);
        }
    },

    // 执行抽卡
    async pull(count) {
        if (this.isPulling) return;

        this.isPulling = true;
        this.showPullAnimation();

        try {
            const poolType = this.getCurrentPoolType();
            const result = await API.pull(poolType, count);

            await this.sleep(2000);
            this.hidePullAnimation();

            if (result.success) {
                this.updateCurrency(result.starlight);
                this.showResult(result.results);
                this.updatePityCount();
            } else {
                alert(result.message || '抽卡失败');
            }
        } catch (error) {
            this.hidePullAnimation();
            alert('网络错误，请稍后重试');
        } finally {
            this.isPulling = false;
        }
    },

    // 显示抽卡动画
    showPullAnimation() {
        const animation = document.getElementById('pullAnimation');
        animation.classList.remove('hidden');
        this.createMeteors();
    },

    // 隐藏抽卡动画
    hidePullAnimation() {
        const animation = document.getElementById('pullAnimation');
        animation.classList.add('hidden');
    },

    // 创建流星效果
    createMeteors() {
        const content = document.querySelector('.animation-content');
        for (let i = 0; i < 5; i++) {
            const meteor = document.createElement('div');
            meteor.className = 'meteor';
            meteor.style.top = `${Math.random() * 100}%`;
            meteor.style.left = `${Math.random() * 100}%`;
            meteor.style.animationDelay = `${Math.random() * 0.5}s`;
            content.appendChild(meteor);
            setTimeout(() => meteor.remove(), 1500);
        }
    },

    // 显示抽卡结果
    showResult(results) {
        const resultView = document.getElementById('resultView');
        const resultCards = document.getElementById('resultCards');

        resultCards.innerHTML = '';

        results.forEach(item => {
            const card = document.createElement('div');
            card.className = `result-card rarity-${item.rarity}`;

            let rarityStars = '';
            for (let i = 0; i < item.rarity; i++) {
                rarityStars += '★';
            }

            const typeText = item.type === 'character' ? '角色' : '武器';
            const rarityClass = item.rarity === 5 ? 'five-star' : item.rarity === 4 ? 'four-star' : 'three-star';

            card.innerHTML = `
                <div class="item-name">${item.name}</div>
                <div class="item-rarity ${rarityClass}">${rarityStars}</div>
                <div class="item-type">${typeText}</div>
                ${item.isLimited ? '<span class="limited-badge">限定</span>' : ''}
            `;

            resultCards.appendChild(card);
        });

        resultView.classList.remove('hidden');
    },

    // 关闭结果
    closeResult() {
        document.getElementById('resultView').classList.add('hidden');
    },

    // 显示历史记录
    showHistory() {
        document.getElementById('historyView').classList.remove('hidden');
    },

    // 更新货币显示
    updateCurrency(starlight) {
        document.getElementById('astraliteCount').textContent = starlight;
    },

    // 工具函数：延时
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
};
