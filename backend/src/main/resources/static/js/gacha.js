// 抽卡模块
const Gacha = {
    currentPool: 'character-1',
    isPulling: false,

    // 卡池配置
    poolConfig: {
        'character-1': {
            title: '雪色所映千般未来',
            subtitle: '角色活动唤取',
            description: '限定五星角色概率提升',
            maxPity: 90,
            fiveStarRate: '0.800%',
            fourStarRate: '6.000%',
            threeStarRate: '93.200%',
            upCharacter: '绯雪',
            upElement: '❄',
            upElementName: '冰',
            timer: '4天13小时',
            background: '/images/background/characters/beijing-feixue.png'
        },
        'character-2': {
            title: '暮光之誓',
            subtitle: '角色活动唤取',
            description: '限定五星角色概率提升',
            maxPity: 90,
            fiveStarRate: '0.800%',
            fourStarRate: '6.000%',
            threeStarRate: '93.200%',
            upCharacter: '柚诺',
            upElement: '🔥',
            upElementName: '火',
            timer: '6天8小时',
            background: '/images/background/characters/beijing-younuo.png'
        },
        'character-3': {
            title: '深渊之歌',
            subtitle: '角色活动唤取',
            description: '限定五星角色概率提升',
            maxPity: 90,
            fiveStarRate: '0.800%',
            fourStarRate: '6.000%',
            threeStarRate: '93.200%',
            upCharacter: '莫凝',
            upElement: '⚡',
            upElementName: '雷',
            timer: '2天20小时',
            background: '/images/background/characters/beijing-moning.png'
        },
        'weapon-1': {
            title: '霜锋映月',
            subtitle: '武器活动唤取',
            description: '限定五星武器概率提升',
            maxPity: 80,
            fiveStarRate: '0.700%',
            fourStarRate: '6.000%',
            threeStarRate: '93.300%',
            upCharacter: '绯雪专武',
            upElement: '⚔',
            upElementName: '武器',
            timer: '4天13小时',
            background: '/images/background/weapons/wuqi-feixue.png'
        },
        'weapon-2': {
            title: '星陨之刃',
            subtitle: '武器活动唤取',
            description: '限定五星武器概率提升',
            maxPity: 80,
            fiveStarRate: '0.700%',
            fourStarRate: '6.000%',
            threeStarRate: '93.300%',
            upCharacter: '柚诺专武',
            upElement: '⚔',
            upElementName: '武器',
            timer: '6天8小时',
            background: '/images/background/weapons/wuqi-younuo.png'
        },
        'weapon-3': {
            title: '破晓之弓',
            subtitle: '武器活动唤取',
            description: '限定五星武器概率提升',
            maxPity: 80,
            fiveStarRate: '0.700%',
            fourStarRate: '6.000%',
            threeStarRate: '93.300%',
            upCharacter: '莫凝专武',
            upElement: '⚔',
            upElementName: '武器',
            timer: '2天20小时',
            background: '/images/background/weapons/wuqi-moning.png'
        },
        'standard-character': {
            title: '寂都之忆',
            subtitle: '常驻角色唤取',
            description: '常驻五星角色概率提升',
            maxPity: 90,
            fiveStarRate: '0.800%',
            fourStarRate: '6.000%',
            threeStarRate: '93.200%',
            upCharacter: '常驻角色',
            upElement: '✦',
            upElementName: '常驻',
            timer: '永久开放',
            background: '/images/background/characters/beijing-changzhu.png'
        },
        'standard-weapon': {
            title: '铸城之锋',
            subtitle: '常驻武器唤取',
            description: '常驻五星武器概率提升',
            maxPity: 80,
            fiveStarRate: '0.700%',
            fourStarRate: '6.000%',
            threeStarRate: '93.300%',
            upCharacter: '常驻武器',
            upElement: '⚔',
            upElementName: '武器',
            timer: '永久开放',
            background: '/images/background/weapons/wuqi-changzhu.png'
        }
    },

    // 初始化
    init() {
        this.bindEvents();
        this.updatePoolInfo(this.currentPool);
        this.createSnowflakes();
    },

    // 绑定事件
    bindEvents() {
        // 卡池切换
        document.querySelectorAll('.banner-slot').forEach(slot => {
            slot.addEventListener('click', () => {
                this.switchPool(slot.dataset.pool);
            });
        });

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

    // 切换卡池
    switchPool(pool) {
        this.currentPool = pool;

        // 更新侧边栏选中状态
        document.querySelectorAll('.banner-slot').forEach(slot => {
            slot.classList.toggle('active', slot.dataset.pool === pool);
        });

        // 隐藏历史记录，显示主界面
        document.getElementById('historyView').classList.add('hidden');

        this.updatePoolInfo(pool);
        this.updatePityCount();
    },

    // 更新卡池信息
    updatePoolInfo(pool) {
        const config = this.poolConfig[pool];
        if (!config) return;

        document.getElementById('bannerSubtitle').textContent = config.subtitle;
        document.getElementById('bannerTitle').textContent = config.title;
        document.getElementById('bannerTimer').textContent = config.timer;
        document.getElementById('upCharacterName').textContent = config.upCharacter;
        document.getElementById('upElement').querySelector('.element-icon').textContent = config.upElement;
        document.getElementById('maxPityDisplay').textContent = config.maxPity;

        // 常驻池隐藏UP角色信息
        const upSection = document.getElementById('upCharacter');
        if (upSection) {
            upSection.style.display = pool.startsWith('standard-') ? 'none' : '';
        }

        // 更新背景图
        const bgEl = document.getElementById('bannerBg');
        if (bgEl && config.background) {
            bgEl.style.backgroundImage = `url('${config.background}')`;
        }
    },

    // 更新保底计数
    async updatePityCount() {
        try {
            const result = await API.getStats(this.currentPool);
            if (result.success && result.stats) {
                this.updatePityDisplay(result.stats.currentPity || 0);
            }
        } catch (error) {
            console.error('获取保底信息失败:', error);
        }
    },

    // 更新保底显示UI
    updatePityDisplay(currentPity) {
        const config = this.poolConfig[this.currentPool];
        const maxPity = config ? config.maxPity : 90;
        const pityNumber = document.getElementById('pityNumber');
        const pityRingProgress = document.getElementById('pityRingProgress');

        if (pityNumber) {
            pityNumber.textContent = currentPity;
        }

        // 更新环形进度条
        if (pityRingProgress) {
            const circumference = 2 * Math.PI * 16; // r=16
            const progress = currentPity / maxPity;
            const offset = circumference * (1 - progress);
            pityRingProgress.style.strokeDasharray = circumference;
            pityRingProgress.style.strokeDashoffset = offset;

            // 接近保底时变色
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

        // 显示动画
        this.showPullAnimation();

        try {
            const result = await API.pull(this.currentPool, count);

            // 等待动画播放
            await this.sleep(2000);

            // 隐藏动画
            this.hidePullAnimation();

            if (result.success) {
                // 更新货币显示
                this.updateCurrency(result.starlight, result.starshards);

                // 显示结果
                this.showResult(result.results);

                // 更新保底计数
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
    updateCurrency(starlight, starshards) {
        document.getElementById('astraliteCount').textContent = starlight;
        document.getElementById('lustrumCount').textContent = starshards;
    },

    // 工具函数：延时
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
};
