/* ==================== 基础配置 ==================== */

const API_BASE = '/api/bot'

/* ==================== 登录校验 ==================== */

function getToken() {
    return localStorage.getItem('token');
}

function forceLogout(msg) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    if (msg) {
        sessionStorage.setItem('logoutMsg', msg);
    }
    location.href = '/login.html';
}

async function logout() {
    if (!confirm('确定退出登录？')) return;

    const token = getToken();
    if (token) {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + token }
            });
        } catch (e) {
            // 忽略网络错误
        }
    }
    forceLogout();
}

function getCurrentUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || '{}');
    } catch (e) {
        return {};
    }
}

/* ==================== 通用请求（带 token） ==================== */

async function api(method, path, body) {
    const token = getToken();
    if (!token) {
        forceLogout();
        return;
    }

    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        }
    };
    if (body !== undefined) {
        options.body = JSON.stringify(body);
    }

    try {
        const res = await fetch(API_BASE + path, options);
        if (res.status === 401) {
            forceLogout('登录已失效，请重新登录');
            return;
        }
        const json = await res.json();
        if (json.code !== 0) {
            throw new Error(json.msg || '请求失败');
        }
        return json.data;
    } catch (e) {
        throw new Error(e.message || '网络错误');
    }
}

const get  = (path) => api('GET', path);
const post = (path, body) => api('POST', path, body);
const put  = (path, body) => api('PUT', path, body);
const del  = (path) => api('DELETE', path);

/* ==================== Toast（居中 + 点击关闭） ==================== */

const Toast = {
    _current: null,

    show(msg, type = 'success') {
        if (this._current) {
            this._current.remove();
            this._current = null;
        }

        const container = document.getElementById('toast-container');
        container.innerHTML = '';

        const el = document.createElement('div');
        el.className = `toast ${type}`;
        el.innerHTML = `
            <div class="toast-msg">${this._escape(msg)}</div>
            <div class="toast-close-hint">点击任意位置关闭</div>
        `;

        const close = () => this.close();
        el.addEventListener('click', close);
        container.addEventListener('click', close);

        container.appendChild(el);
        container.classList.add('show');
        this._current = el;
    },

    close() {
        const container = document.getElementById('toast-container');
        container.classList.remove('show');
        container.innerHTML = '';
        this._current = null;
    },

    success(msg) { this.show(msg, 'success'); },
    error(msg)   { this.show(msg, 'error'); },
    warning(msg) { this.show(msg, 'warning'); },

    _escape(s) {
        if (s === null || s === undefined) return '';
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
};

/* ==================== 弹窗 ==================== */

const Modal = {
    _confirmHandler: null,

    open(title, bodyHtml, onConfirm, confirmText) {
        document.getElementById('modal-title').textContent = title;
        document.getElementById('modal-body').innerHTML = bodyHtml;
        this._confirmHandler = onConfirm;

        const confirmBtn = document.getElementById('modal-confirm');
        const cancelBtn = document.querySelector('.modal-footer .btn:not(.primary)');

        confirmBtn.textContent = confirmText || '确认';
        confirmBtn.style.display = '';
        confirmBtn.disabled = false;

        // 恢复"取消"按钮显示
        if (cancelBtn) cancelBtn.style.display = '';

        document.getElementById('modal-mask').classList.add('show');
        confirmBtn.onclick = async () => {
            if (this._confirmHandler) {
                await this._confirmHandler();
            }
        };
    },

    /**
     * 只读弹窗：隐藏「取消」和「确认」，只保留关闭 X
     */
    openReadonly(title, bodyHtml) {
        document.getElementById('modal-title').textContent = title;
        document.getElementById('modal-body').innerHTML = bodyHtml;
        this._confirmHandler = null;

        const confirmBtn = document.getElementById('modal-confirm');
        const cancelBtn = document.querySelector('.modal-footer .btn:not(.primary)');

        confirmBtn.style.display = 'none';
        if (cancelBtn) cancelBtn.style.display = 'none';

        document.getElementById('modal-mask').classList.add('show');
    },

    close() {
        document.getElementById('modal-mask').classList.remove('show');
        document.getElementById('modal-body').innerHTML = '';
        this._confirmHandler = null;

        // 恢复按钮显示
        const confirmBtn = document.getElementById('modal-confirm');
        const cancelBtn = document.querySelector('.modal-footer .btn:not(.primary)');
        confirmBtn.style.display = '';
        if (cancelBtn) cancelBtn.style.display = '';
    }
};

/* ==================== 工具函数 ==================== */

function shortAddr(addr) {
    if (!addr) return '-';
    return addr.substring(0, 8) + '...' + addr.substring(addr.length - 6);
}

function formatTime(t) {
    if (!t) return '-';
    return new Date(t).toLocaleString('zh-CN', { hour12: false });
}

function escapeHtml(s) {
    if (s === null || s === undefined) return '';
    return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function copyText(text, label) {
    const successMsg = label ? `${label}已复制` : '已复制到剪贴板';
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(
            () => Toast.success(successMsg),
            () => Toast.error('复制失败')
        );
    } else {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        try {
            document.execCommand('copy');
            Toast.success(successMsg);
        } catch (err) {
            Toast.error('复制失败');
        }
        document.body.removeChild(ta);
    }
}

/* ==================== 钱包管理 ==================== */

const WalletUI = {
    async reload() {
        const tbody = document.getElementById('wallet-tbody');
        tbody.innerHTML = '<tr><td colspan="9" class="empty">加载中...</td></tr>';
        try {
            const list = await get('/wallet/list');
            if (!list || list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="empty">暂无钱包</td></tr>';
                return;
            }
            tbody.innerHTML = list.map(w => {
                const tokenCount = w.tokenCount || 0;
                const tokenCell = tokenCount > 0
                    ? `<button class="btn" onclick="WalletUI.openAssets(${w.id})">${tokenCount} 种</button>`
                    : `<button class="btn" onclick="WalletUI.openAssets(${w.id})" style="color:#94a3b8;">查看</button>`;

                return `
                <tr>
                    <td>${w.id}</td>
                    <td><span class="addr" title="${escapeHtml(w.address)}">${shortAddr(w.address)}</span></td>
                    <td>${w.bnbBalance ?? 0} BNB</td>
                    <td>${w.usdtBalance ?? 0} USDT</td>
                    <td>${tokenCell}</td>
                    <td>${w.status === 1
                        ? '<span class="badge green">启用</span>'
                        : '<span class="badge gray">禁用</span>'}</td>
                    <td>${w.hasMnemonic
                        ? '<span class="badge blue">有</span>'
                        : '<span class="badge gray">无</span>'}</td>
                    <td>${formatTime(w.createdAt)}</td>
                    <td>
                        <div class="actions">
                            <button class="btn" onclick="copyText('${w.address}', '钱包地址')">复制地址</button>
                            <button class="btn" onclick="WalletUI.refreshBalance(${w.id})">刷新余额</button>
                            ${w.hasMnemonic
                                ? `<button class="btn" onclick="WalletUI.exportMnemonic(${w.id})">导出助记词</button>`
                                : ''}
                            ${w.status === 1
                                ? `<button class="btn danger" onclick="WalletUI.disable(${w.id})">禁用</button>`
                                : `<button class="btn" onclick="WalletUI.enable(${w.id})">启用</button>`}
                        </div>
                    </td>
                </tr>
            `;
            }).join('');
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="9" class="empty">加载失败: ${escapeHtml(e.message)}</td></tr>`;
        }
    },

    /**
     * 打开钱包资产详情弹窗
     */
    async openAssets(walletId) {
        Modal.openReadonly('钱包资产详情', `
            <div id="asset-loading" class="hint" style="text-align:center;background:#f1f5f9;border-left-color:#94a3b8;color:#475569;">
                加载中...
            </div>
            <div id="asset-body"></div>
        `);

        await this._loadAssets(walletId);
    },

    async _loadAssets(walletId) {
        const loading = document.getElementById('asset-loading');
        const body = document.getElementById('asset-body');
        if (!body) return;

        try {
            const list = await get(`/wallet/${walletId}/tokens`);
            if (loading) loading.style.display = 'none';

            if (!list || list.length === 0) {
                body.innerHTML = `
                    <div class="hint" style="text-align:center;background:#f1f5f9;border-left-color:#94a3b8;color:#64748b;">
                        该钱包暂无代币持仓<br>
                        <span style="font-size:12px;">点击下方按钮刷新持仓</span>
                    </div>
                    <div style="text-align:center;margin-top:12px;">
                        <button class="btn primary" onclick="WalletUI._refreshAssets(${walletId})">刷新持仓</button>
                    </div>`;
                return;
            }

            const rows = list.map(t => `
                <tr>
                    <td><strong>${escapeHtml(t.tokenName || '-')}</strong></td>
                    <td><span class="addr" title="${escapeHtml(t.tokenAddress)}">${shortAddr(t.tokenAddress)}</span></td>
                    <td style="text-align:right;"><strong>${t.balance ?? 0}</strong></td>
                </tr>
            `).join('');

            body.innerHTML = `
                <table class="data-table" style="font-size:12px;min-width:auto;margin-top:12px;">
                    <thead>
                    <tr>
                        <th>代币</th>
                        <th>合约地址</th>
                        <th style="text-align:right;">持仓数量</th>
                    </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
                <div style="text-align:center;margin-top:12px;">
                    <button class="btn primary" onclick="WalletUI._refreshAssets(${walletId})">刷新持仓</button>
                </div>`;
        } catch (e) {
            if (loading) loading.style.display = 'none';
            body.innerHTML = `
                <div class="hint" style="background:#fee2e2;border-left-color:#ef4444;color:#991b1b;text-align:center;">
                    加载失败: ${escapeHtml(e.message)}
                </div>`;
        }
    },

    async _refreshAssets(walletId) {
        const loading = document.getElementById('asset-loading');
        const body = document.getElementById('asset-body');
        if (loading) {
            loading.style.display = 'block';
            loading.textContent = '刷新中...';
        }
        if (body) body.innerHTML = '';

        try {
            await post(`/wallet/${walletId}/tokens/refresh`);
            await this._loadAssets(walletId);
            WalletUI.reload();  // 同步刷新主列表的 tokenCount
            Toast.success('持仓已刷新');
        } catch (e) {
            Toast.error(e.message);
            if (loading) loading.style.display = 'none';
        }
    },

    openCreate() {
        Modal.open('创建钱包', `
            <div class="hint">推荐使用"带助记词"创建，方便后续备份和恢复。</div>
            <div class="form-group">
                <label>
                    <input type="radio" name="create-type" value="mnemonic" checked> 带助记词（推荐）
                </label>
            </div>
            <div class="form-group">
                <label>
                    <input type="radio" name="create-type" value="private-key"> 仅私钥（不推荐）
                </label>
            </div>
        `, async () => {
            const type = document.querySelector('input[name="create-type"]:checked').value;
            try {
                if (type === 'mnemonic') {
                    const data = await post('/wallet/create-with-mnemonic');
                    Modal.close();
                    Toast.success(`创建成功: ${shortAddr(data.wallet.address)}`);
                    WalletUI.reload();
                    WalletUI.showMnemonicWarning(data.wallet.id);
                } else {
                    const data = await post('/wallet/create');
                    Modal.close();
                    Toast.success(`创建成功: ${shortAddr(data.address)}`);
                    WalletUI.reload();
                }
            } catch (e) {
                Toast.error(e.message);
            }
        }, '创建');
    },

    showMnemonicWarning(walletId) {
        Modal.open('⚠️ 请立即备份助记词', `
            <div class="hint">助记词是恢复钱包的唯一凭证，请立即离线备份。关闭弹窗后将不再提示。</div>
            <div class="form-group">
                <label>钱包ID</label>
                <input type="text" value="${walletId}" readonly>
            </div>
            <p style="font-size:13px;color:#64748b;">
                点击下方按钮导出助记词并妥善保存。
            </p>
        `, async () => {
            Modal.close();
            WalletUI.exportMnemonic(walletId);
        }, '导出助记词');
    },

    async exportMnemonic(id) {
        try {
            const data = await get(`/wallet/export-mnemonic/${id}`);
            Modal.open('助记词（请立即备份）', `
                <div class="hint">⚠️ 此助记词等同于钱包全部资产控制权，请勿泄露给任何人。</div>
                <div class="mnemonic-box">${escapeHtml(data.mnemonic)}</div>
                <button class="copy-btn" onclick="copyText('${escapeHtml(data.mnemonic)}', '助记词')">复制助记词</button>
            `, async () => { Modal.close(); }, '关闭');
        } catch (e) {
            Toast.error(e.message);
        }
    },

    openImport() {
        Modal.open('导入助记词', `
            <div class="form-group">
                <label>助记词（12 或 24 个单词，空格分隔）</label>
                <textarea id="import-mnemonic" placeholder="abandon ability able about..."></textarea>
            </div>
        `, async () => {
            const mnemonic = document.getElementById('import-mnemonic').value.trim();
            if (!mnemonic) { Toast.error('请输入助记词'); return; }
            try {
                const data = await post('/wallet/import', { mnemonic });
                Modal.close();
                Toast.success(`导入成功: ${shortAddr(data.address)}`);
                WalletUI.reload();
            } catch (e) {
                Toast.error(e.message);
            }
        }, '导入');
    },

    openDerive() {
        Modal.open('批量派生钱包', `
            <div class="hint">同一个助记词可以派生出无数个地址，只需备份一个助记词即可恢复全部。</div>
            <div class="form-group">
                <label>助记词</label>
                <textarea id="derive-mnemonic" placeholder="abandon ability able about..."></textarea>
            </div>
            <div class="form-group">
                <label>派生数量（1-100）</label>
                <input type="number" id="derive-count" value="5" min="1" max="100">
            </div>
        `, async () => {
            const mnemonic = document.getElementById('derive-mnemonic').value.trim();
            const count = parseInt(document.getElementById('derive-count').value);
            if (!mnemonic) { Toast.error('请输入助记词'); return; }
            if (!count || count < 1 || count > 100) { Toast.error('数量必须在 1-100 之间'); return; }
            try {
                const data = await post('/wallet/derive', { mnemonic, count });
                Modal.close();
                Toast.success(`派生成功 ${data.count} 个钱包`);
                WalletUI.reload();
            } catch (e) {
                Toast.error(e.message);
            }
        }, '派生');
    },

    openVerify() {
        Modal.open('验证助记词', `
            <div class="form-group">
                <label>助记词</label>
                <textarea id="verify-mnemonic" placeholder="abandon ability able about..."></textarea>
            </div>
            <div id="verify-result"></div>
        `, async () => {
            const mnemonic = document.getElementById('verify-mnemonic').value.trim();
            if (!mnemonic) { Toast.error('请输入助记词'); return; }
            try {
                const data = await post('/wallet/verify-mnemonic', { mnemonic });
                document.getElementById('verify-result').innerHTML = `
                    <div class="hint" style="background:#dcfce7;border-left-color:#22c55e;color:#166534;">
                        助记词合法，派生地址：<br>
                        <span class="addr">${escapeHtml(data.address)}</span>
                    </div>`;
            } catch (e) {
                document.getElementById('verify-result').innerHTML = `
                    <div class="hint" style="background:#fee2e2;border-left-color:#ef4444;color:#991b1b;">
                        ${escapeHtml(e.message)}
                    </div>`;
            }
        }, '验证');
    },

    async refreshBalance(id) {
        try {
            await post(`/wallet/refresh/${id}`);
            Toast.success('余额已刷新');
            WalletUI.reload();
        } catch (e) {
            Toast.error(e.message);
        }
    },

    async refreshAll() {
        try {
            await post('/wallet/refresh-all');
            Toast.success('已触发批量刷新');
            setTimeout(() => WalletUI.reload(), 1500);
        } catch (e) {
            Toast.error(e.message);
        }
    },

    async enable(id) {
        try {
            await put(`/wallet/${id}/enable`);
            Toast.success('已启用');
            WalletUI.reload();
        } catch (e) { Toast.error(e.message); }
    },

    async disable(id) {
        try {
            await put(`/wallet/${id}/disable`);
            Toast.success('已禁用');
            WalletUI.reload();
        } catch (e) { Toast.error(e.message); }
    }
};

/* ==================== 任务管理 ==================== */

const TaskUI = {
    async reload() {
        const tbody = document.getElementById('task-tbody');
        tbody.innerHTML = '<tr><td colspan="9" class="empty">加载中...</td></tr>';
        try {
            const list = await get('/task/list');
            if (!list || list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="empty">暂无任务</td></tr>';
                return;
            }
            tbody.innerHTML = list.map(t => {
                const market = (t.marketPair || 'BNB').toUpperCase();
                const amountUnit = market === 'USDT' ? 'USDT' : 'BNB';
                const tokenName = t.tokenName ? escapeHtml(t.tokenName) : '-';

                return `
                <tr>
                    <td>${t.id}</td>
                    <td><strong>${tokenName}</strong></td>
                    <td><span class="addr" title="${escapeHtml(t.tokenAddress)}">${shortAddr(t.tokenAddress)}</span></td>
                    <td>${t.minBuyAmount} ~ ${t.maxBuyAmount} ${amountUnit}</td>
                    <td>${t.minIntervalSec} ~ ${t.maxIntervalSec}</td>
                    <td>${t.buyWeight}%</td>
                    <td>${t.isRunning === 1
                        ? '<span class="badge green">运行中</span>'
                        : '<span class="badge gray">已停止</span>'}</td>
                    <td>${t.graduated === 1
                        ? '<span class="badge blue">已毕业</span>'
                        : '<span class="badge orange">未毕业</span>'}</td>
                    <td>
                        <div class="actions">
                            ${t.isRunning === 1
                                ? `<button class="btn danger" onclick="TaskUI.stop(${t.id})">停止</button>`
                                : `<button class="btn primary" onclick="TaskUI.start(${t.id})">启动</button>`}
                            <button class="btn danger" onclick="TaskUI.delete(${t.id})">删除</button>
                        </div>
                    </td>
                </tr>
            `;
            }).join('');
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="9" class="empty">加载失败: ${escapeHtml(e.message)}</td></tr>`;
        }
    },

    openCreate() {
        TaskCreateWizard.open();
    },

    async start(id) {
        try {
            await put(`/task/${id}/start`);
            Toast.success('任务已启动');
            TaskUI.reload();
        } catch (e) { Toast.error(e.message); }
    },

    async stop(id) {
        try {
            await put(`/task/${id}/stop`);
            Toast.success('任务已停止');
            TaskUI.reload();
        } catch (e) { Toast.error(e.message); }
    },

    async delete(id) {
        if (!confirm('确定删除这个任务吗？')) return;
        try {
            await del(`/task/${id}`);
            Toast.success('任务已删除');
            TaskUI.reload();
        } catch (e) { Toast.error(e.message); }
    }
};

/* ==================== 创建任务向导（两步） ==================== */

const TaskCreateWizard = {
    state: {
        tokenAddress: null,
        tokenName: null,
        marketPair: null
    },

    open() {
        this.state = {
            tokenAddress: null,
            tokenName: null,
            marketPair: null
        };
        this.renderStep1();
    },

    renderStep1(prefill) {
        Modal.open('创建任务 · 第 1 步', `
            <div class="hint">输入代币合约地址后，系统会自动识别代币简称和所属市场。</div>
            <div class="form-group">
                <label>代币合约地址 *</label>
                <input type="text" id="wiz-address" placeholder="0x..."
                       value="${prefill ? escapeHtml(prefill) : ''}">
            </div>
            <div id="wiz-error" class="error-msg"></div>
        `, async () => {
            await this.submitStep1();
        }, '下一步');
    },

    async submitStep1() {
        const address = document.getElementById('wiz-address').value.trim();
        const errEl = document.getElementById('wiz-error');

        const showErr = (msg) => {
            errEl.textContent = msg;
            errEl.style.display = 'block';
        };
        errEl.style.display = 'none';

        if (!address) {
            showErr('请输入代币合约地址');
            return;
        }
        if (!/^0x[a-fA-F0-9]{40}$/.test(address)) {
            showErr('地址格式不正确，应为 0x 开头的 42 位十六进制');
            return;
        }

        const confirmBtn = document.getElementById('modal-confirm');
        const oldText = confirmBtn.textContent;
        confirmBtn.disabled = true;
        confirmBtn.textContent = '识别中...';

        try {
            const meta = await get('/token/info?address=' + address);

            if (!meta) {
                throw new Error('无法获取代币信息');
            }
            if (!meta.marketPair) {
                throw new Error('无法自动识别该代币的所属市场（BNB 或 USDT），请确认该代币是否已在 Four.meme 上创建');
            }
            const tokenName = meta.tokenName;
            if (!tokenName) {
                throw new Error('无法获取代币简称');
            }

            this.state.tokenAddress = address;
            this.state.tokenName = tokenName;
            this.state.marketPair = meta.marketPair;

            this.renderStep2();
        } catch (e) {
            showErr(e.message || '识别失败');
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.textContent = oldText;
        }
    },

    renderStep2() {
        const market = (this.state.marketPair || 'BNB').toUpperCase();
        const unit = market === 'USDT' ? 'USDT' : 'BNB';

        let defMin, defMax, defThreshold;
        if (market === 'USDT') {
            defMin = '1';
            defMax = '10';
            defThreshold = '12000';
        } else {
            defMin = '0.001';
            defMax = '0.005';
            defThreshold = '18';
        }

        Modal.open('创建任务 · 第 2 步', `
            <div class="hint" style="background:#dcfce7;border-left-color:#22c55e;color:#166534;">
                <strong>代币简称：</strong>${escapeHtml(this.state.tokenName)}<br>
                <strong>所属市场：</strong>${escapeHtml(market)}<br>
                <strong>代币地址：</strong><span class="addr">${escapeHtml(this.state.tokenAddress)}</span>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>最小买入金额 (${unit})</label>
                    <input type="number" id="wiz-min" value="${defMin}" step="any">
                </div>
                <div class="form-group">
                    <label>最大买入金额 (${unit})</label>
                    <input type="number" id="wiz-max" value="${defMax}" step="any">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>最小间隔 (秒)</label>
                    <input type="number" id="wiz-min-int" value="15">
                </div>
                <div class="form-group">
                    <label>最大间隔 (秒)</label>
                    <input type="number" id="wiz-max-int" value="30">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>买入权重 (%)</label>
                    <input type="number" id="wiz-buy-weight" value="60" min="0" max="100">
                </div>
                <div class="form-group">
                    <label>每轮交易次数</label>
                    <input type="number" id="wiz-trades" value="1" min="1" max="10">
                </div>
            </div>
            <div class="form-group">
                <label>毕业阈值 (${unit})</label>
                <input type="number" id="wiz-threshold" value="${defThreshold}" step="any">
            </div>
            <div style="margin-top:12px;">
                <button type="button" class="btn" onclick="TaskCreateWizard.backToStep1()">← 返回修改地址</button>
            </div>
            <div id="wiz-error" class="error-msg"></div>
        `, async () => {
            await this.submitStep2();
        }, '创建任务');
    },

    backToStep1() {
        this.renderStep1(this.state.tokenAddress);
    },

    async submitStep2() {
        const errEl = document.getElementById('wiz-error');
        const showErr = (msg) => {
            errEl.textContent = msg;
            errEl.style.display = 'block';
        };
        errEl.style.display = 'none';

        const minBuyAmount = parseFloat(document.getElementById('wiz-min').value);
        const maxBuyAmount = parseFloat(document.getElementById('wiz-max').value);
        const minIntervalSec = parseInt(document.getElementById('wiz-min-int').value);
        const maxIntervalSec = parseInt(document.getElementById('wiz-max-int').value);
        const buyWeight = parseInt(document.getElementById('wiz-buy-weight').value);
        const maxTradesPerRound = parseInt(document.getElementById('wiz-trades').value);
        const graduationThresholdBnb = parseFloat(document.getElementById('wiz-threshold').value);

        if (isNaN(minBuyAmount) || minBuyAmount <= 0) {
            showErr('请输入有效的最小买入金额');
            return;
        }
        if (isNaN(maxBuyAmount) || maxBuyAmount <= 0) {
            showErr('请输入有效的最大买入金额');
            return;
        }
        if (minBuyAmount > maxBuyAmount) {
            showErr('最小金额不能大于最大金额');
            return;
        }
        if (isNaN(minIntervalSec) || minIntervalSec <= 0) {
            showErr('请输入有效的最小间隔');
            return;
        }
        if (isNaN(maxIntervalSec) || maxIntervalSec < minIntervalSec) {
            showErr('最大间隔不能小于最小间隔');
            return;
        }
        if (isNaN(buyWeight) || buyWeight < 0 || buyWeight > 100) {
            showErr('买入权重必须在 0-100 之间');
            return;
        }
        if (isNaN(maxTradesPerRound) || maxTradesPerRound < 1 || maxTradesPerRound > 10) {
            showErr('每轮交易次数必须在 1-10 之间');
            return;
        }
        if (isNaN(graduationThresholdBnb) || graduationThresholdBnb <= 0) {
            showErr('请输入有效的毕业阈值');
            return;
        }

        const body = {
            tokenAddress: this.state.tokenAddress,
            minBuyAmount,
            maxBuyAmount,
            minIntervalSec,
            maxIntervalSec,
            buyWeight,
            maxTradesPerRound,
            graduationThresholdBnb
        };

        const confirmBtn = document.getElementById('modal-confirm');
        const oldText = confirmBtn.textContent;
        confirmBtn.disabled = true;
        confirmBtn.textContent = '创建中...';

        try {
            await post('/task/create', body);
            Modal.close();
            Toast.success('任务创建成功');
            TaskUI.reload();
        } catch (e) {
            showErr(e.message || '创建失败');
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.textContent = oldText;
        }
    }
};

/* ==================== 交易记录 ==================== */

const TradeUI = {
    currentPage: 1,
    pageSize: 10,
    totalPages: 0,
    total: 0,

    async reload() {
        const tbody = document.getElementById('trade-tbody');
        tbody.innerHTML = '<tr><td colspan="9" class="empty">加载中...</td></tr>';

        const tokenName = document.getElementById('trade-token-filter').value.trim();
        const walletId = document.getElementById('trade-wallet-filter').value.trim();
        const params = new URLSearchParams();
        if (tokenName) params.append('tokenName', tokenName);
        if (walletId) params.append('walletId', walletId);
        params.append('pageNum', this.currentPage);
        params.append('pageSize', this.pageSize);

        try {
            const result = await get('/trade/list?' + params.toString());

            const list = (result && result.records) ? result.records : [];
            this.total = (result && result.total) ? result.total : 0;
            this.totalPages = (result && result.pages) ? result.pages : 0;
            this.currentPage = (result && result.current) ? result.current : 1;

            if (list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="empty">暂无交易记录</td></tr>';
                this.renderPagination();
                return;
            }

            tbody.innerHTML = list.map(t => {
                const statusBadge = t.status === 1
                    ? '<span class="badge green">成功</span>'
                    : t.status === 2
                        ? '<span class="badge red">失败</span>'
                        : '<span class="badge gray">待确认</span>';
                const typeBadge = t.tradeType === 'BUY'
                    ? '<span class="badge blue">买入</span>'
                    : '<span class="badge orange">卖出</span>';
                const stageBadge = t.stage === 'PANCAKE'
                    ? '<span class="badge blue">Pancake</span>'
                    : '<span class="badge gray">联合曲线</span>';

                const symbol = (t.tokenName || 'TOKEN') + '/' + (t.quoteName || 'BNB');
                const amount = t.tradeType === 'BUY'
                    ? `${t.quoteAmount} ${t.quoteName}`
                    : `${t.tokenAmount} ${t.tokenName}`;

                const hash = t.txHash
                    ? `<a class="addr-link" href="https://bscscan.com/tx/${t.txHash}" target="_blank">${shortAddr(t.txHash)}</a>`
                    : '-';
                const errorMsg = t.errorMsg ? ` title="${escapeHtml(t.errorMsg)}"` : '';
                return `
                    <tr${errorMsg}>
                        <td>${t.id}</td>
                        <td>${symbol}</td>
                        <td>${t.walletId}</td>
                        <td>${typeBadge}</td>
                        <td>${amount}</td>
                        <td>${stageBadge}</td>
                        <td>${statusBadge}</td>
                        <td>${hash}</td>
                        <td>${formatTime(t.createdAt)}</td>
                    </tr>
                `;
            }).join('');

            this.renderPagination();
        } catch (e) {
            tbody.innerHTML = `<tr><td colspan="9" class="empty">加载失败: ${escapeHtml(e.message)}</td></tr>`;
            this.renderPagination();
        }
    },

    renderPagination() {
        const el = document.getElementById('trade-pagination');
        if (!el) return;

        if (this.total === 0) {
            el.innerHTML = '';
            return;
        }

        const cur = this.currentPage;
        const total = this.totalPages;
        const hasPrev = cur > 1;
        const hasNext = cur < total;

        el.innerHTML = `
            <button class="btn" onclick="TradeUI.goPage(1)" ${!hasPrev ? 'disabled' : ''}>首页</button>
            <button class="btn" onclick="TradeUI.goPage(${cur - 1})" ${!hasPrev ? 'disabled' : ''}>上一页</button>
            <span class="page-info">第 ${cur} / ${total} 页 · 共 ${this.total} 条</span>
            <button class="btn" onclick="TradeUI.goPage(${cur + 1})" ${!hasNext ? 'disabled' : ''}>下一页</button>
            <button class="btn" onclick="TradeUI.goPage(${total})" ${!hasNext ? 'disabled' : ''}>末页</button>
        `;
    },

    goPage(page) {
        if (page < 1 || page > this.totalPages) return;
        if (page === this.currentPage) return;
        this.currentPage = page;
        this.reload();
    },

    search() {
        this.currentPage = 1;
        this.reload();
    },

    clearFilter() {
        document.getElementById('trade-token-filter').value = '';
        document.getElementById('trade-wallet-filter').value = '';
        this.currentPage = 1;
        this.reload();
    }
};

/* ==================== Tab 切换 ==================== */

document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
        tab.classList.add('active');
        const target = tab.dataset.tab;
        document.getElementById('tab-' + target).classList.add('active');
        if (target === 'wallets') WalletUI.reload();
        if (target === 'tasks')   TaskUI.reload();
        if (target === 'trades')  TradeUI.reload();
    });
});

/* ==================== API 健康检查 ==================== */

async function checkApi() {
    const dot = document.getElementById('api-status');
    const text = document.getElementById('api-status-text');
    try {
        const token = getToken();
        await fetch(API_BASE + '/wallet/list', {
            headers: token ? { 'Authorization': 'Bearer ' + token } : {}
        });
        dot.className = 'dot online';
        text.textContent = 'API 正常';
    } catch (e) {
        dot.className = 'dot offline';
        text.textContent = 'API 不可用';
    }
}

/* ==================== 修改密码 ==================== */

const PasswordUI = {
    open() {
        Modal.open('修改密码', `
            <div class="form-group">
                <label>原密码 *</label>
                <input type="password" id="pwd-old" placeholder="请输入当前密码" autocomplete="current-password">
            </div>
            <div class="form-group">
                <label>新密码 *（至少 6 位）</label>
                <input type="password" id="pwd-new" placeholder="请输入新密码" autocomplete="new-password">
            </div>
            <div class="form-group">
                <label>确认新密码 *</label>
                <input type="password" id="pwd-confirm" placeholder="再次输入新密码" autocomplete="new-password">
            </div>
            <div id="pwd-error" class="error-msg"></div>
        `, async () => {
            await PasswordUI.submit();
        }, '确认修改');
    },

    async submit() {
        const oldPassword = document.getElementById('pwd-old').value;
        const newPassword = document.getElementById('pwd-new').value;
        const confirmPassword = document.getElementById('pwd-confirm').value;
        const errorEl = document.getElementById('pwd-error');

        const showErr = (msg) => {
            errorEl.textContent = msg;
            errorEl.style.display = 'block';
        };
        errorEl.style.display = 'none';

        if (!oldPassword || !newPassword || !confirmPassword) {
            showErr('请填写所有字段');
            return;
        }
        if (newPassword.length < 6) {
            showErr('新密码长度至少 6 位');
            return;
        }
        if (newPassword !== confirmPassword) {
            showErr('两次输入的新密码不一致');
            return;
        }

        const confirmBtn = document.getElementById('modal-confirm');
        const oldText = confirmBtn.textContent;
        confirmBtn.disabled = true;
        confirmBtn.textContent = '提交中...';

        try {
            await post2('/auth/change-password-token', {
                oldPassword,
                newPassword
            });
            Modal.close();
            Toast.success('密码修改成功，请使用新密码重新登录');

            setTimeout(() => {
                forceLogout();
            }, 1500);
        } catch (e) {
            showErr(e.message);
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.textContent = oldText;
        }
    }
};

/* ==================== 用户管理（仅 admin） ==================== */

const UserAdmin = {
    async open() {
        try {
            const list = await get2('/auth/users');
            const rows = list.map(u => `
                <tr>
                    <td>${u.id}</td>
                    <td>${escapeHtml(u.username)}</td>
                    <td>${u.role === 'ADMIN'
                        ? '<span class="badge blue">管理员</span>'
                        : '<span class="badge gray">普通用户</span>'}</td>
                    <td>${u.totpBound
                        ? '<span class="badge green">已绑定</span>'
                        : '<span class="badge orange">未绑定</span>'}</td>
                    <td>${formatTime(u.lastLoginAt)}</td>
                </tr>
            `).join('');

            Modal.open('用户管理', `
                <div class="toolbar" style="margin-bottom:12px;">
                    <button class="btn primary" onclick="UserAdmin.openCreate()">添加用户</button>
                </div>
                <table class="data-table" style="font-size:12px;min-width:auto;">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>用户名</th>
                        <th>角色</th>
                        <th>TOTP</th>
                        <th>最后登录</th>
                    </tr>
                    </thead>
                    <tbody>${rows || '<tr><td colspan="5" class="empty">暂无用户</td></tr>'}</tbody>
                </table>
            `, async () => { Modal.close(); }, '关闭');
        } catch (e) {
            Toast.error(e.message);
        }
    },

    openCreate() {
        Modal.open('添加用户', `
            <div class="form-group">
                <label>用户名 *</label>
                <input type="text" id="new-user-name" placeholder="例如 user1">
            </div>
            <div class="form-group">
                <label>初始密码 *（至少6位）</label>
                <input type="password" id="new-user-pwd" placeholder="请输入密码">
            </div>
            <div class="form-group">
                <label>角色</label>
                <select id="new-user-role">
                    <option value="USER">普通用户</option>
                    <option value="ADMIN">管理员</option>
                </select>
            </div>
            <div id="create-user-error" class="error-msg"></div>
        `, async () => {
            const username = document.getElementById('new-user-name').value.trim();
            const password = document.getElementById('new-user-pwd').value;
            const role = document.getElementById('new-user-role').value;
            const errEl = document.getElementById('create-user-error');

            const showErr = (msg) => {
                errEl.textContent = msg;
                errEl.style.display = 'block';
            };
            errEl.style.display = 'none';

            if (!username) { showErr('请输入用户名'); return; }
            if (!password || password.length < 6) { showErr('密码长度至少 6 位'); return; }

            try {
                await post2('/auth/users', { username, password, role });
                Toast.success(`用户 ${username} 创建成功`);
                setTimeout(() => UserAdmin.open(), 800);
            } catch (e) {
                showErr(e.message);
            }
        }, '创建');
    }
};

/* ==================== 二级 API 调用（/api 前缀） ==================== */

async function get2(path) {
    const token = getToken();
    const res = await fetch('/api' + path, {
        headers: { 'Authorization': 'Bearer ' + token }
    });
    if (res.status === 401) { forceLogout('登录已失效，请重新登录'); return; }
    const json = await res.json();
    if (json.code !== 0) throw new Error(json.msg || '请求失败');
    return json.data;
}

async function post2(path, body) {
    const token = getToken();
    const res = await fetch('/api' + path, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(body)
    });
    if (res.status === 401) { forceLogout('登录已失效，请重新登录'); return; }
    const json = await res.json();
    if (json.code !== 0) throw new Error(json.msg || '请求失败');
    return json.data;
}

/* ==================== 初始化 ==================== */

document.addEventListener('DOMContentLoaded', () => {
    if (!getToken()) {
        forceLogout();
        return;
    }
    const user = getCurrentUser();
    const userEl = document.getElementById('current-user');
    if (userEl && user.username) userEl.textContent = user.username;

    if (user.role === 'ADMIN') {
        const btn = document.getElementById('btn-user-mgmt');
        if (btn) btn.style.display = '';
    }

    const btnChangePwd = document.getElementById('btn-change-pwd');
    if (btnChangePwd) {
        btnChangePwd.onclick = () => PasswordUI.open();
    }

    checkApi();
    WalletUI.reload();
    setInterval(checkApi, 30000);
});

/* ==================== bfcache 处理 ==================== */

window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
        checkApi();
        const active = document.querySelector('.tab.active');
        const target = active ? active.dataset.tab : 'wallets';
        if (target === 'wallets') WalletUI.reload();
        if (target === 'tasks')   TaskUI.reload();
        if (target === 'trades')  TradeUI.reload();
    }
});