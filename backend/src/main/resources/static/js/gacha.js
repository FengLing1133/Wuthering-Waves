// 抽卡模块
const Gacha = {
    currentPool: null,
    currentPoolData: null,
    isPulling: false,
    poolList: [],
    _skipRequested: false,
    _videoCache: {},
    // 历史记录状态
    historyCurrentPage: 1,
    historyPageSize: 5,
    historyTotalPages: 1,
    historyPoolType: 'limited-character',
    // 抽卡分析状态
    analysisData: null,
    currentAnalysisPool: 'limited-character',

    // 初始化
    async init() {
        this.bindEvents();
        this.createSnowflakes();
        this.preloadVideos();
        await this.loadPools();
    },

    // 预加载视频为 Blob URL 缓存
    preloadVideos() {
        [3, 4, 5].forEach(rarity => {
            fetch(`/videos/${rarity}star.mp4`)
                .then(res => res.blob())
                .then(blob => {
                    this._videoCache[rarity] = URL.createObjectURL(blob);
                })
                .catch(err => console.warn(`视频 ${rarity}star.mp4 预加载失败:`, err));
        });
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
            let tag = '活动';
            if (pool.poolType && pool.poolType.startsWith('standard-')) {
                tag = '常驻';
            } else if (pool.poolType.startsWith('special-')) {
                tag = '特殊';
            }
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

        // 副标题区分四种池类型
        let subtitle = '';
        switch (pool.poolType) {
            case 'standard-weapon': subtitle = '武器常驻唤取'; break;
            case 'standard-character': subtitle = '角色常驻唤取'; break;
            case 'limited-weapon': subtitle = '武器活动唤取'; break;
            case 'special-character': subtitle = '特殊角色唤取'; break;
            case 'special-weapon': subtitle = '特殊武器唤取'; break;
            default: subtitle = '角色活动唤取';
        }
        document.getElementById('bannerSubtitle').textContent = subtitle;
        document.getElementById('bannerTitle').textContent = pool.name || '';
        document.getElementById('bannerTimer').textContent = this.formatTimer(pool);
        document.getElementById('upCharacterName').textContent = this.getUpCharacterName(pool);
        document.getElementById('upElement').querySelector('.element-icon').textContent = this.getUpElement(pool);
        document.getElementById('maxPityDisplay').textContent = pool.maxPity || 90;

        // 常驻角色池隐藏UP信息，其他池显示
        const upSection = document.getElementById('upCharacter');
        if (upSection) {
            upSection.style.display = pool.poolType === 'standard-character' ? 'none' : '';
        }

        // 更换按钮：仅常驻武器池显示
        const changeUpBtn = document.getElementById('changeUpBtn');
        if (changeUpBtn) {
            changeUpBtn.classList.toggle('hidden', pool.poolType !== 'standard-weapon');
        }

        // 更新背景图
        const bgEl = document.getElementById('bannerBg');
        if (bgEl) {
            const bgUrl = pool.bgImageUrl;
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
                    <div class="rate-up-avatar" style="background-image: url('${avatar.imageUrl}'); background-size: cover; background-position: center top;"></div>
                </div>
            `).join('');
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
        if (pool.upItems && pool.upItems.length > 0) {
            return pool.upItems[0].name || 'UP';
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

        // 背包按钮（抽卡分析）
        document.querySelector('.top-icon-btn[title="背包"]').addEventListener('click', () => {
            this.showAnalysis();
        });

        // 更换UP武器按钮
        document.getElementById('changeUpBtn').addEventListener('click', () => {
            this.showWeaponSelectModal();
        });

        // 关闭武器选择弹窗
        document.getElementById('closeWeaponSelectBtn').addEventListener('click', () => {
            document.getElementById('weaponSelectModal').classList.add('hidden');
        });

        // 关闭历史记录弹窗
        document.getElementById('closeHistoryBtn').addEventListener('click', () => {
            this.hideHistory();
        });

        // 点击遮罩关闭历史记录弹窗
        document.getElementById('historyModalOverlay').addEventListener('click', () => {
            this.hideHistory();
        });

        // 历史记录池类型筛选
        document.getElementById('historyPoolFilter').addEventListener('change', (e) => {
            this.historyPoolType = e.target.value;
            this.historyCurrentPage = 1;
            this.loadHistory();
        });

        // 关闭抽卡分析弹窗
        document.getElementById('closeAnalysisBtn').addEventListener('click', () => {
            this.hideAnalysis();
        });

        // 点击遮罩关闭抽卡分析弹窗
        document.getElementById('analysisModalOverlay').addEventListener('click', () => {
            this.hideAnalysis();
        });

        // 抽卡分析卡池类型切换
        document.querySelectorAll('.analysis-pool-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                document.querySelectorAll('.analysis-pool-tab').forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                this.currentAnalysisPool = tab.dataset.pool;
                this.renderAnalysisPoolRecords();
            });
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
        this._skipRequested = false;

        try {
            const poolType = this.getCurrentPoolType();
            const result = await API.pull(poolType, count, this.currentPool);

            if (result.success) {
                this.updateCurrency(result.starlight);
                this.updatePityCount();

                const results = result.results;
                // 确定播放视频的星级（单抽按物品星级，十连按最高星级）
                const maxRarity = Math.max(...results.map(r => r.rarity));
                await this.playGachaVideo(maxRarity);

                if (count === 1) {
                    // 单抽：显示物品展示页
                    await this.showItemShowcase(results[0]);
                } else {
                    // 十连：逐个展示物品，最后汇总
                    await this.showTenPullAnimation(results);
                }
            } else {
                alert(result.message || '抽卡失败');
            }
        } catch (error) {
            this.hideVideoOverlay();
            this.hideSkipBtn();
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

    // 播放抽卡视频
    playGachaVideo(rarity) {
        return new Promise((resolve) => {
            const video = document.getElementById('gachaVideo');
            const animation = document.getElementById('pullAnimation');

            // 设置视频源（优先使用缓存的 Blob URL）
            const cachedUrl = this._videoCache[rarity];
            if (cachedUrl) {
                video.src = cachedUrl;
            } else {
                video.src = `/videos/${rarity}star.mp4`;
            }

            // 显示动画层和视频
            animation.classList.remove('hidden');
            video.classList.remove('hidden');
            this.showSkipBtn();

            // 统一的清理函数
            const cleanup = () => {
                document.removeEventListener('keydown', onKeyDown);
                this.hideVideoOverlay();
                resolve();
            };

            // 跳过按钮：暂停视频，标记跳过
            const onSkip = () => {
                video.pause();
                this._skipRequested = true;
                cleanup();
            };
            document.getElementById('skipBtn').onclick = onSkip;

            // ESC 键跳过
            const onKeyDown = (e) => {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    onSkip();
                }
            };
            document.addEventListener('keydown', onKeyDown);

            // 视频播放结束
            video.onended = cleanup;

            // 视频加载失败时直接跳过
            video.onerror = cleanup;

            // 开始播放（取消静音，因为抽卡按钮点击已是用户交互）
            video.muted = false;
            video.play().catch(() => {
                this.hideVideoOverlay();
                resolve();
            });
        });
    },

    // 隐藏视频遮罩
    hideVideoOverlay() {
        const video = document.getElementById('gachaVideo');
        const animation = document.getElementById('pullAnimation');
        video.pause();
        video.currentTime = 0;
        video.classList.add('hidden');
        animation.classList.add('hidden');
        this.hideSkipBtn();
    },

    // 显示跳过按钮
    showSkipBtn() {
        document.getElementById('skipBtn').classList.remove('hidden');
    },

    // 隐藏跳过按钮
    hideSkipBtn() {
        document.getElementById('skipBtn').classList.add('hidden');
    },

    // 创建流星效果
    createMeteors() {
        const content = document.querySelector('.animation-content');
        // 清理上一次未移除的流星元素
        content.querySelectorAll('.meteor').forEach(m => m.remove());
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

    // 显示单个物品展示页面
    showItemShowcase(item) {
        return new Promise((resolve) => {
            const showcase = document.getElementById('itemShowcase');
            const icon = document.getElementById('showcaseIcon');
            const name = document.getElementById('showcaseName');
            const stars = document.getElementById('showcaseStars');
            const bg = showcase.querySelector('.showcase-bg');

            // 设置背景颜色主题
            bg.className = `showcase-bg rarity-${item.rarity}`;

            // 设置物品图标
            if (item.imageUrl) {
                icon.style.backgroundImage = `url('${item.imageUrl}')`;
            } else {
                // 没有图片时显示默认图标
                icon.style.backgroundImage = 'none';
                icon.innerHTML = `<div style="font-size: 60px; color: var(--text-dim);">${item.type === 'character' ? '👤' : '⚔️'}</div>`;
            }

            // 设置物品名称
            name.textContent = item.name;

            // 设置星级
            let starsText = '';
            for (let i = 0; i < item.rarity; i++) {
                starsText += '★';
            }
            stars.textContent = starsText;
            stars.className = `showcase-item-stars rarity-${item.rarity}`;

            // 显示展示页面
            showcase.classList.remove('hidden');

            const skipBtn = showcase.querySelector('.showcase-skip-btn');

            const cleanup = () => {
                showcase.classList.add('hidden');
                showcase.removeEventListener('click', clickHandler);
                skipBtn.removeEventListener('click', skipHandler);
                resolve();
            };

            // 跳过按钮（阻止事件冒泡到 showcase）
            const skipHandler = (e) => {
                e.stopPropagation();
                this._skipRequested = true;
                cleanup();
            };
            skipBtn.addEventListener('click', skipHandler);

            // 点击任意位置继续
            const clickHandler = () => {
                cleanup();
            };
            showcase.addEventListener('click', clickHandler);
        });
    },

    // 显示十连抽动画
    async showTenPullAnimation(results) {
        // 依次展示每个物品，按实际抽取顺序，跳过时直接退出循环
        for (const item of results) {
            if (this._skipRequested) break;
            await this.showItemShowcase(item);
            if (!this._skipRequested) {
                await this.sleep(300); // 物品之间的间隔
            }
        }

        // 显示汇总页面
        await this.showTenPullSummary(results);
    },

    // 显示十连抽汇总页面
    showTenPullSummary(results) {
        return new Promise((resolve) => {
            const summary = document.getElementById('tenPullSummary');
            const cardsContainer = document.getElementById('summaryCards');
            const closeBtn = document.getElementById('closeSummaryBtn');

            cardsContainer.innerHTML = '';

            // 按实际抽取顺序展示
            results.forEach(item => {
                const card = document.createElement('div');
                card.className = `summary-card rarity-${item.rarity}`;

                let starsText = '';
                for (let i = 0; i < item.rarity; i++) {
                    starsText += '★';
                }

                const iconStyle = item.imageUrl
                    ? `background-image: url('${item.imageUrl}');`
                    : 'background: var(--bg-card);';

                card.innerHTML = `
                    <div class="summary-card-icon" style="${iconStyle}"></div>
                    <div class="summary-card-info">
                        <div class="summary-card-name">${item.name}</div>
                        <div class="summary-card-stars rarity-${item.rarity}">${starsText}</div>
                    </div>
                `;

                cardsContainer.appendChild(card);
            });

            // 显示汇总页面和跳过按钮
            summary.classList.remove('hidden');
            this.showSkipBtn();

            const cleanup = () => {
                summary.classList.add('hidden');
                this.hideSkipBtn();
                resolve();
            };

            // 跳过按钮
            document.getElementById('skipBtn').onclick = cleanup;

            // 确认按钮
            closeBtn.onclick = cleanup;
        });
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

    // 显示历史记录弹窗
    showHistory() {
        this.historyCurrentPage = 1;
        this.loadHistory();
        document.getElementById('historyModal').classList.remove('hidden');
    },

    // 隐藏历史记录弹窗
    hideHistory() {
        document.getElementById('historyModal').classList.add('hidden');
    },

    // 加载历史记录
    async loadHistory() {
        try {
            const result = await API.getHistory(this.historyPoolType, this.historyCurrentPage, this.historyPageSize);
            if (result.success) {
                this.renderHistoryTable(result.records);
                this.renderHistoryPagination(result.total);
            }
        } catch (error) {
            console.error('加载历史记录失败:', error);
        }
    },

    // 渲染历史记录表格
    renderHistoryTable(records) {
        const tbody = document.getElementById('historyTableBody');
        if (!records || records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="history-empty-tip">暂无唤取记录</td></tr>';
            return;
        }

        tbody.innerHTML = records.map(record => {
            const typeText = record.itemType === 'character' ? '角色' : '武器';
            const typeClass = record.itemType === 'character' ? 'character' : 'weapon';
            const rarityClass = record.itemRarity === 5 ? 'five-star' : record.itemRarity === 4 ? 'four-star' : '';
            const time = this.formatHistoryTime(record.createdAt);

            return `<tr>
                <td><span class="item-type-badge ${typeClass}">${typeText}</span></td>
                <td><span class="item-name ${rarityClass}">${record.itemName}</span></td>
                <td>1</td>
                <td class="time-cell">${time}</td>
            </tr>`;
        }).join('');
    },

    // 格式化历史记录时间
    formatHistoryTime(timeStr) {
        if (!timeStr) return '--';
        const date = new Date(timeStr);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    },

    // 渲染分页
    renderHistoryPagination(total) {
        const container = document.getElementById('historyPagination');
        this.historyTotalPages = Math.ceil(total / this.historyPageSize) || 1;

        if (this.historyTotalPages <= 1) {
            container.innerHTML = `<span class="page-info">1 / 1</span>`;
            return;
        }

        let html = '';

        // 上一页按钮
        html += `<button class="page-btn nav-btn" ${this.historyCurrentPage <= 1 ? 'disabled' : ''} data-page="${this.historyCurrentPage - 1}">«</button>`;

        // 页码按钮
        const maxVisible = 5;
        let startPage = Math.max(1, this.historyCurrentPage - Math.floor(maxVisible / 2));
        let endPage = Math.min(this.historyTotalPages, startPage + maxVisible - 1);

        if (endPage - startPage + 1 < maxVisible) {
            startPage = Math.max(1, endPage - maxVisible + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            html += `<button class="page-btn ${i === this.historyCurrentPage ? 'active' : ''}" data-page="${i}">${i}</button>`;
        }

        // 下一页按钮
        html += `<button class="page-btn nav-btn" ${this.historyCurrentPage >= this.historyTotalPages ? 'disabled' : ''} data-page="${this.historyCurrentPage + 1}">»</button>`;

        // 页码信息
        html += `<span class="page-info">${this.historyCurrentPage} / ${this.historyTotalPages}</span>`;
        html += `<input type="number" class="page-jump-input" min="1" max="${this.historyTotalPages}" placeholder="页码" data-action="jump">`;

        container.innerHTML = html;

        // 绑定分页点击事件
        container.querySelectorAll('.page-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const page = parseInt(btn.dataset.page);
                if (page >= 1 && page <= this.historyTotalPages) {
                    this.historyCurrentPage = page;
                    this.loadHistory();
                }
            });
        });

        // 绑定页码跳转
        const jumpInput = container.querySelector('.page-jump-input');
        if (jumpInput) {
            jumpInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    const page = Math.max(1, Math.min(this.historyTotalPages, parseInt(jumpInput.value) || 1));
                    this.historyCurrentPage = page;
                    this.loadHistory();
                }
            });
        }
    },

    // 更新货币显示
    updateCurrency(starlight) {
        document.getElementById('astraliteCount').textContent = starlight;
    },

    // 工具函数：延时
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    },

    // 显示武器选择弹窗
    async showWeaponSelectModal() {
        const modal = document.getElementById('weaponSelectModal');
        const grid = document.getElementById('weaponSelectGrid');
        if (!modal || !grid) return;

        try {
            const result = await API.getStandardWeaponUpOptions();
            if (!result.success) {
                alert(result.message || '获取武器列表失败');
                return;
            }

            const { weapons, selectedId } = result;

            grid.innerHTML = weapons.map(weapon => {
                const stars = '★'.repeat(weapon.rarity);
                const isSelected = weapon.id === selectedId;
                const imgStyle = weapon.imageUrl
                    ? `background-image: url('${weapon.imageUrl}'); background-size: cover; background-position: center;`
                    : 'background: linear-gradient(135deg, #2a3a5a, #1a2a4a);';
                return `<div class="weapon-card ${isSelected ? 'selected' : ''}" data-weapon-id="${weapon.id}">
                    <div class="weapon-card-img" style="${imgStyle}"></div>
                    <div class="weapon-card-name">${weapon.name}</div>
                    <div class="weapon-card-stars">${stars}</div>
                    ${isSelected ? '<div class="weapon-card-badge">当前UP</div>' : ''}
                </div>`;
            }).join('');

            // 绑定点击事件
            grid.querySelectorAll('.weapon-card').forEach(card => {
                card.addEventListener('click', async () => {
                    const weaponId = parseInt(card.dataset.weaponId);
                    try {
                        const res = await API.setStandardWeaponUp(weaponId);
                        if (res.success) {
                            modal.classList.add('hidden');
                            // 刷新当前卡池信息
                            if (this.currentPool) {
                                this.switchPool(this.currentPool);
                            }
                        } else {
                            alert(res.message || '设置失败');
                        }
                    } catch (e) {
                        alert('网络错误');
                    }
                });
            });

            modal.classList.remove('hidden');
        } catch (error) {
            console.error('获取武器列表失败:', error);
            alert('获取武器列表失败');
        }
    },

    // ========== 抽卡分析 ==========

    // 显示抽卡分析弹窗
    async showAnalysis() {
        document.getElementById('analysisModal').classList.remove('hidden');
        await this.loadAnalysisData();
    },

    // 隐藏抽卡分析弹窗
    hideAnalysis() {
        document.getElementById('analysisModal').classList.add('hidden');
    },

    // 加载抽卡分析数据
    async loadAnalysisData() {
        try {
            const result = await API.getAnalysis();
            if (result.success) {
                this.analysisData = result.analysis;
                this.renderAnalysis();
            }
        } catch (error) {
            console.error('加载抽卡分析数据失败:', error);
        }
    },

    // 渲染抽卡分析
    renderAnalysis() {
        const data = this.analysisData;
        if (!data) return;

        // 渲染称号
        document.getElementById('analysisTitle').textContent = data.title || '初入江湖';
        document.getElementById('analysisTitleDesc').textContent = data.titleDesc || '';

        // 渲染基础统计
        document.getElementById('analysisTotalPulls').textContent = data.totalPulls || 0;
        document.getElementById('analysisAvgPity').textContent = (data.avgFiveStarPity || 0) + '抽';

        // 渲染详细统计
        document.getElementById('analysisNotLostRate').textContent = data.notLostRate || '0%';
        document.getElementById('analysisTotalFiveStar').textContent = data.totalFiveStar || 0;

        const poolStats = data.poolStats || {};
        document.getElementById('analysisAvgCharacterPity').textContent = (poolStats.avgCharacterPity || 0) + '抽';
        document.getElementById('analysisAvgWeaponPity').textContent = (poolStats.avgWeaponPity || 0) + '抽';

        // 渲染五星物品总结
        document.getElementById('analysisLimitedCount').textContent = data.limitedFiveStar || 0;
        document.getElementById('analysisStandardCount').textContent = data.standardFiveStar || 0;
        this.renderFiveStarItems(data.fiveStarItems || []);

        // 渲染卡池记录
        this.renderAnalysisPoolRecords();
    },

    // 渲染五星物品图标列表
    renderFiveStarItems(items) {
        const container = document.getElementById('analysisFiveStarList');
        container.innerHTML = items.map(item => {
            const imageUrl = item.imageUrl;
            const count = item.count || 1;
            const showCount = count > 1;

            if (imageUrl) {
                return `<div class="analysis-five-star-item">
                    <img src="${imageUrl}" alt="${item.name}">
                    ${showCount ? `<div class="count-badge">${count}</div>` : ''}
                </div>`;
            } else {
                return `<div class="analysis-five-star-item no-image">
                    <span>${item.name.substring(0, 2)}</span>
                    ${showCount ? `<div class="count-badge">${count}</div>` : ''}
                </div>`;
            }
        }).join('');
    },

    // 渲染卡池五星记录
    renderAnalysisPoolRecords() {
        const data = this.analysisData;
        if (!data || !data.poolGroupedItems) return;

        const poolType = this.currentAnalysisPool;
        const poolItems = data.poolGroupedItems[poolType] || [];
        const container = document.getElementById('analysisPoolRecords');

        // 获取当前卡池的垫抽数
        const currentPity = (data.poolCurrentPity && data.poolCurrentPity[poolType]) || 0;

        // 构建当前垫抽数提示
        let pityHintHtml = '';
        if (currentPity > 0) {
            pityHintHtml = `<div class="analysis-pool-pity-hint">
                当前已垫 <span class="highlight">${currentPity}</span> 抽
            </div>`;
        }

        if (poolItems.length === 0) {
            container.innerHTML = pityHintHtml +
                '<div class="analysis-pool-empty">暂无五星记录</div>';
            return;
        }

        // 按时间倒序排列（最新的在上面）
        const sortedItems = [...poolItems].sort((a, b) => {
            if (a.createdAt && b.createdAt) {
                return new Date(b.createdAt) - new Date(a.createdAt);
            }
            return 0;
        });

        const recordsHtml = sortedItems.map(item => {
            const imageUrl = item.imageUrl;
            const pityCount = item.pityCount || 0;
            const isLimited = item.isLimited;
            const barWidth = Math.min((pityCount / 80) * 100, 100);
            const barColor = pityCount < 60 ? 'green' : 'yellow';

            let iconHtml;
            if (imageUrl) {
                iconHtml = `<div class="analysis-pool-record-icon">
                    <img src="${imageUrl}" alt="${item.name}">
                </div>`;
            } else {
                iconHtml = `<div class="analysis-pool-record-icon no-image">
                    <span>${item.name.substring(0, 2)}</span>
                </div>`;
            }

            let lostHtml = '';
            if (item.poolType && (item.poolType.startsWith('limited-') || item.poolType.startsWith('special-')) && !isLimited) {
                lostHtml = '<span class="analysis-pool-record-lost">歪</span>';
            }

            return `<div class="analysis-pool-record-item">
                ${iconHtml}
                <div class="analysis-pool-record-bar-container">
                    <div class="analysis-pool-record-bar ${barColor}" style="width: ${barWidth}%">
                        ${pityCount}抽
                    </div>
                </div>
                ${lostHtml}
            </div>`;
        }).join('');

        container.innerHTML = pityHintHtml + recordsHtml;
    }
};
