const API_BASE = '/api';

const state = {
    tempToken: null,
    username: null,
    password: null
};

// ==================== 工具 ====================

function showStep(id) {
    document.querySelectorAll('.step').forEach(el => el.classList.remove('active'));
    document.getElementById(id).classList.add('active');
}

function showError(id, msg) {
    const el = document.getElementById(id);
    if (msg) {
        el.textContent = msg;
        el.classList.add('show');
    } else {
        el.textContent = '';
        el.classList.remove('show');
    }
}

async function apiCall(method, path, body, token) {
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (token) options.headers['Authorization'] = 'Bearer ' + token;
    if (body !== undefined) options.body = JSON.stringify(body);

    const res = await fetch(API_BASE + path, options);
    const json = await res.json();
    if (json.code !== 0) {
        throw new Error(json.msg || '请求失败');
    }
    return json.data;
}

function saveLogin(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user || {}));
    location.href = '/';
}

// ==================== Step 1: 登录 ====================

async function doLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    if (!username || !password) {
        showError('login-error', '请输入用户名和密码');
        return;
    }
    showError('login-error', '');

    state.username = username;
    state.password = password;

    try {
        const data = await apiCall('POST', '/auth/login', { username, password });
        state.tempToken = data.tempToken;

        // 需要修改初始密码
        if (data.requirePasswordChange) {
            document.getElementById('old-password').value = state.password;
            showStep('step-password');
            document.getElementById('new-password').focus();
            return;
        }
        // 登录成功
        if (data.token) {
            saveLogin(data);
            return;
        }
        // 需要输入 TOTP
        if (data.requireTotp) {
            showStep('step-totp');
            document.getElementById('totp-code').focus();
            return;
        }
        // 需要绑定 TOTP
        if (data.requireBinding) {
            await loadTotpSetup();
            showStep('step-bind');
            document.getElementById('bind-code').focus();
            return;
        }
        showError('login-error', '未知响应');
    } catch (e) {
        showError('login-error', e.message);
    }
}

// ==================== Step 修改密码 ====================

async function doChangePassword() {
    const oldPassword = document.getElementById('old-password').value;
    const newPassword = document.getElementById('new-password').value;
    const confirmPassword = document.getElementById('confirm-password').value;

    if (!oldPassword || !newPassword || !confirmPassword) {
        showError('password-error', '请填写所有字段');
        return;
    }
    if (newPassword.length < 6) {
        showError('password-error', '新密码长度至少 6 位');
        return;
    }
    if (newPassword !== confirmPassword) {
        showError('password-error', '两次输入的新密码不一致');
        return;
    }
    showError('password-error', '');

    try {
        const data = await apiCall('POST', '/auth/change-password', {
            oldPassword,
            newPassword
        }, state.tempToken);

        state.tempToken = data.tempToken;

        if (data.token) {
            saveLogin(data);
            return;
        }
        if (data.requireBinding) {
            await loadTotpSetup();
            showStep('step-bind');
            document.getElementById('bind-code').focus();
            return;
        }
        if (data.requireTotp) {
            showStep('step-totp');
            document.getElementById('totp-code').focus();
            return;
        }
        showError('password-error', '未知响应');
    } catch (e) {
        showError('password-error', e.message);
    }
}

// ==================== Step 2: 验证 TOTP ====================

async function doVerifyTotp() {
    const code = document.getElementById('totp-code').value.trim();
    if (!code) {
        showError('totp-error', '请输入验证码');
        return;
    }
    showError('totp-error', '');

    try {
        const data = await apiCall('POST', '/auth/login', {
            username: state.username,
            password: state.password,
            totpCode: code
        });
        if (data.token) {
            saveLogin(data);
        } else {
            showError('totp-error', '验证码错误');
        }
    } catch (e) {
        showError('totp-error', e.message);
    }
}

// ==================== Step 3: 绑定 TOTP ====================

async function loadTotpSetup() {
    try {
        const data = await apiCall('POST', '/auth/totp/setup', {}, state.tempToken);
        document.getElementById('qr-code').src = 'data:image/png;base64,' + data.qrCode;
        document.getElementById('totp-secret').textContent = data.secret;
    } catch (e) {
        showError('bind-error', e.message);
    }
}

async function doBind() {
    const code = document.getElementById('bind-code').value.trim();
    if (!code) {
        showError('bind-error', '请输入验证码');
        return;
    }
    showError('bind-error', '');

    try {
        const data = await apiCall('POST', '/auth/totp/bind', { totpCode: code }, state.tempToken);
        saveLogin(data);
    } catch (e) {
        showError('bind-error', e.message);
    }
}

async function copySecret() {
    const text = document.getElementById('totp-secret').textContent;
    try {
        await navigator.clipboard.writeText(text);
        alert('密钥已复制到剪贴板');
    } catch (e) {
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        alert('密钥已复制到剪贴板');
    }
}

// ==================== 事件绑定 ====================

document.getElementById('btn-login').onclick = doLogin;
document.getElementById('btn-totp').onclick = doVerifyTotp;
document.getElementById('btn-bind').onclick = doBind;
document.getElementById('btn-copy-secret').onclick = copySecret;

document.getElementById('btn-back-totp').onclick = () => {
    state.tempToken = null;
    state.username = null;
    state.password = null;
    document.getElementById('totp-code').value = '';
    showError('totp-error', '');
    showStep('step-login');
};

document.getElementById('btn-change-password').onclick = doChangePassword;
['old-password', 'new-password', 'confirm-password'].forEach(id => {
    document.getElementById(id).addEventListener('keypress', e => {
        if (e.key === 'Enter') doChangePassword();
    });
});

['username', 'password'].forEach(id => {
    document.getElementById(id).addEventListener('keypress', e => {
        if (e.key === 'Enter') doLogin();
    });
});
document.getElementById('totp-code').addEventListener('keypress', e => {
    if (e.key === 'Enter') doVerifyTotp();
});
document.getElementById('bind-code').addEventListener('keypress', e => {
    if (e.key === 'Enter') doBind();
});

// 如果已登录，直接跳转
if (localStorage.getItem('token')) {
    location.href = '/';
}