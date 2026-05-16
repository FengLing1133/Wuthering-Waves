// 抽卡模块
const Gacha = {
    currentPool: 'character',
    isPulling: false,

    // 池子配置
    poolConfig: {
        character: {
            title: '角色活动唤取',
            description: '限定五星角色概率提升',
            maxPity: 90,
            fiveStarRate: '0.800%',
            fourStarRate: '6.000%',
            threeStarRate: '93.200%'
        },
        weapon: {
            title: '武器活动唤取',
            description: '限定五星武器概率提升',
            maxPity: 80,
            fiveStarRate: '0.700%',
            fourStarRate: '6.000%',
            threeStarRate: '93.300%'
        },
        limited: {
            title: '限定角色唤取',
            description: '当期限定五星角色概率大幅提升',
            maxPity: 90,
            fiveStarRate: '1.600%',
            fourStarRate: '6.000%',
            threeStarRate: '92.400%'
        }
    },

    // 初始化
    init() {
        this.bindEvents();
        this.updatePoolInfo();
    },

    // 绑定事件
    bindEvents() {
        // 池子切换
        document.querySelectorAll('.nav-btn[data-pool]').forEach(btn => {
            btn.addEventListener('click', () => {
                this.switchPool(btn.dataset.pool);
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
    },

    // 切换池子
    switchPool(pool) {
        this.currentPool = pool;

        // 更新按钮状态
        document.querySelectorAll('.nav-btn[data-pool]').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.pool === pool);
        });

        // 更新页面显示
        document.getElementById('gachaView').classList.remove('hidden');
        document.getElementById('historyView').classList.add('hidden');
        document.querySelectorAll('.nav-btn[data-page]').forEach(btn => {
            btn.classList.remove('active');
        });

        this.updatePoolInfo();
        this.updatePityCount();
    },

    // 更新池子信息
    updatePoolInfo() {
        const config = this.poolConfig[this.currentPool];
        document.getElementById('poolTitle').textContent = config.title;
        document.getElementById('poolDescription').textContent = config.description;

        // 更新概率显示
        const rateItems = document.querySelectorAll('.rate-value');
        if (rateItems.length >= 3) {
            rateItems[0].textContent = config.fiveStarRate;
            rateItems[1].textContent = config.fourStarRate;
            rateItems[2].textContent = config.threeStarRate;
        }
    },

    // 更新保底计数
    async updatePityCount() {
        try {
            const result = await API.getStats(this.currentPool);
            if (result.success) {
                document.getElementById('pityCount').textContent = result.stats.currentPity || 0;
            }
        } catch (error) {
            console.error('获取保底信息失败:', error);
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

        // 添加流星效果
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

            // 动画结束后移除
            setTimeout(() => {
                meteor.remove();
            }, 1500);
        }
    },

    // 显示抽卡结果
    showResult(results) {
        const resultView = document.getElementById('resultView');
        const resultCards = document.getElementById('resultCards');

        // 清空之前的结果
        resultCards.innerHTML = '';

        // 生成结果卡片
        results.forEach(item => {
            const card = document.createElement('div');
            card.className = `result-card rarity-${item.rarity}`;

            let rarityStars = '';
            for (let i = 0; i < item.rarity; i++) {
                rarityStars += '★';
            }

            const typeText = item.type === 'character' ? '角色' : '武器';

            card.innerHTML = `
                <div class="item-name">${item.name}</div>
                <div class="item-rarity ${item.rarity === 5 ? 'five-star' : item.rarity === 4 ? 'four-star' : 'three-star'}">${rarityStars}</div>
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

    // 更新货币显示
    updateCurrency(starlight, starshards) {
        document.getElementById('starlightCount').textContent = starlight;
        document.getElementById('starshardsCount').textContent = starshards;
    },

    // 工具函数：延时
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
};
