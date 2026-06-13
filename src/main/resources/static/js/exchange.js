/* File purpose: Browser behavior for the prod-facing exchange trading console. */
// DOM helper keeps the static MVP page dependency-free while the app is still backend-first.
const $ = (id) => document.getElementById(id);
const I18N_STORAGE_KEY = 'exchangeLanguage';
const PENDING_REGISTRATION_EMAIL_KEY = 'exchangePendingRegistrationEmail';
const translations = {
    en: {
        'page.title': 'Exchange Console',
        'page.subtitle': 'Trade perpetual markets with live order book depth, order entry, open orders, and account risk.',
        'language.label': 'Language',
        'tab.trade': 'Trade',
        'profile.open': 'Open Profile',
        'profile.close': 'Close Profile',
        'profile.title': 'Profile',
        'profile.funds': 'Personal Funds',
        'profile.frozen': 'Frozen Funds',
        'profile.orders': 'Open Orders',
        'profile.held': 'Held Positions',
        'profile.history': 'Position History',
        'profile.orderHold': 'Open Order Hold',
        'market.helper': 'Select an available market and trade against live book liquidity.',
        'field.market': 'Market',
        'field.email': 'Email',
        'field.password': 'Password',
        'field.humanVerification': 'Human Verification',
        'field.humanVerificationPlaceholder': 'Verification token',
        'field.emailVerificationCode': 'Email Code',
        'field.emailVerificationCodePlaceholder': '6-digit code',
        'field.leverage': 'Leverage',
        'field.margin': 'Margin',
        'field.clientOrderId': 'Client Order ID',
        'action.reloadOrders': 'Reload Orders',
        'action.register': 'Register',
        'action.login': 'Login',
        'action.logout': 'Logout',
        'action.verifyRegistration': 'Verify Registration',
        'action.resendVerification': 'Resend Code',
        'action.placeBuy': 'Place Buy',
        'action.placeSell': 'Place Sell',
        'metric.session': 'Session',
        'metric.balance': 'Total Balance',
        'metric.available': 'Available to Trade',
        'metric.frozen': 'Frozen',
        'metric.positionMargin': 'Used Margin',
        'account.createAccount': 'Register',
        'account.assetsTitle': 'Assets',
        'book.title': 'Order Book',
        'book.helper': 'Runtime matching depth for the selected market.',
        'book.asks': 'Asks',
        'book.bids': 'Bids',
        'book.spread': 'Spread',
        'book.depth': 'Ticks / side',
        'order.title': 'Place Order',
        'orders.title': 'Open Orders',
        'orders.helper': "Current user's live orders for the selected market.",
        'table.orderId': 'Order ID',
        'table.side': 'Side',
        'table.type': 'Type',
        'table.price': 'Price',
        'table.qty': 'Qty',
        'table.notional': 'Order Value',
        'table.filled': 'Filled',
        'table.status': 'Status',
        'empty.authState': 'Not logged in.',
        'empty.accountRaw': 'No account loaded.',
        'empty.orderResult': 'No order submitted.',
        'empty.loginForOrders': 'Login and refresh to load open orders',
        'empty.book': 'Runtime order book is empty',
        'empty.noOpenOrders': 'No open orders',
        'empty.noHeldPositions': 'No held positions loaded.',
        'empty.noPositionHistory': 'No position history loaded.',
        'status.authenticated': 'Authenticated',
        'status.notLoggedIn': 'Not logged in',
        'status.sessionUnavailable': 'Session unavailable. Please login again.',
        'status.accountUnavailable': 'Account unavailable.',
        'status.registrationCreated': 'Account created. You can log in now.',
        'status.registrationPending': 'Enter the email verification code to finish registration.',
        'status.verificationResent': 'A new verification code has been sent.',
        'status.emailVerified': 'Email verified. You can log in now.',
        'status.registrationVerified': 'Registration verified. You can log in now.',
        'error.emailRequired': 'Email is required.',
        'error.emailInvalid': 'Enter a valid email address.',
        'error.passwordRequired': 'Password is required.',
        'error.passwordTooShort': 'Password must be at least 8 characters.',
        'error.verificationCodeRequired': 'Enter the email verification code.',
        'error.invalidCredentials': 'Account not found or password is incorrect.',
        'error.registrationPending': 'Registration verification is already in progress. Enter the email code or try again later.',
        'notice.bookRecovered': 'Open orders are persisted, but the in-memory order book is empty. This usually means the app restarted before order-book recovery/replay restored those resting orders.',
        'error.loginBeforeOrder': 'Please login before placing orders.'
    },
    'zh-TW': {
        'page.title': '交易所前台',
        'page.subtitle': '交易永續合約市場，查看即時訂單簿深度、下單、開放訂單與帳戶風險。',
        'language.label': '語言',
        'tab.trade': '交易',
        'profile.open': '開啟個人資料',
        'profile.close': '關閉個人資料',
        'profile.title': '個人資料',
        'profile.funds': '個人資金',
        'profile.frozen': '凍結資金',
        'profile.orders': '委託',
        'profile.held': '持有倉位',
        'profile.history': '歷史開關倉位',
        'profile.orderHold': '委託凍結',
        'market.helper': '選擇可用市場，並直接與即時訂單簿流動性交易。',
        'field.market': '市場',
        'field.email': '電子信箱',
        'field.password': '密碼',
        'field.humanVerification': '真人驗證',
        'field.humanVerificationPlaceholder': '驗證 token',
        'field.emailVerificationCode': '信箱驗證碼',
        'field.emailVerificationCodePlaceholder': '6 位數驗證碼',
        'field.leverage': '槓桿',
        'field.margin': '保證金',
        'field.clientOrderId': '客戶訂單 ID',
        'action.reloadOrders': '重新載入訂單',
        'action.register': '註冊',
        'action.login': '登入',
        'action.logout': '登出',
        'action.verifyRegistration': '完成註冊驗證',
        'action.resendVerification': '重新寄送驗證碼',
        'action.placeBuy': '下買單',
        'action.placeSell': '下賣單',
        'metric.session': '登入狀態',
        'metric.balance': '總餘額',
        'metric.available': '可下單餘額',
        'metric.frozen': '凍結',
        'metric.positionMargin': '已用保證金',
        'account.createAccount': '註冊',
        'account.assetsTitle': '資產',
        'book.title': '訂單簿',
        'book.helper': '所選市場的即時撮合深度。',
        'book.asks': '賣盤',
        'book.bids': '買盤',
        'book.spread': '價差',
        'book.depth': '每邊 ticks',
        'order.title': '下單',
        'orders.title': '開放訂單',
        'orders.helper': '目前使用者在所選市場的即時訂單。',
        'table.orderId': '訂單 ID',
        'table.side': '方向',
        'table.type': '類型',
        'table.price': '價格',
        'table.qty': '數量',
        'table.notional': '訂單價值',
        'table.filled': '已成交',
        'table.status': '狀態',
        'empty.authState': '尚未登入。',
        'empty.accountRaw': '尚未載入帳戶。',
        'empty.orderResult': '尚未送出訂單。',
        'empty.loginForOrders': '登入並重新整理以載入開放訂單',
        'empty.book': '即時訂單簿為空',
        'empty.noOpenOrders': '沒有開放訂單',
        'empty.noHeldPositions': '尚未載入持有倉位。',
        'empty.noPositionHistory': '尚未載入歷史開關倉位。',
        'status.authenticated': '已驗證',
        'status.notLoggedIn': '尚未登入',
        'status.sessionUnavailable': 'Session 不可用，請重新登入。',
        'status.accountUnavailable': '帳戶不可用。',
        'status.registrationCreated': '帳號已建立，現在可以登入。',
        'status.registrationPending': '請輸入信箱驗證碼完成註冊。',
        'status.verificationResent': '新的驗證碼已寄出。',
        'status.emailVerified': 'Email 已驗證，現在可以登入。',
        'status.registrationVerified': '註冊已完成驗證，現在可以登入。',
        'error.emailRequired': '請輸入電子信箱。',
        'error.emailInvalid': '請輸入有效的電子信箱。',
        'error.passwordRequired': '請輸入密碼。',
        'error.passwordTooShort': '密碼至少需要 8 個字元。',
        'error.verificationCodeRequired': '請輸入信箱驗證碼。',
        'error.invalidCredentials': '查無此帳號或密碼錯誤。',
        'error.registrationPending': '此帳號已有註冊驗證進行中，請輸入信箱驗證碼或稍後再試。',
        'notice.bookRecovered': '開放訂單已持久化，但記憶體訂單簿為空。這通常表示 app 重啟後尚未透過 recovery/replay 還原掛單。',
        'error.loginBeforeOrder': '下單前請先登入。'
    },
    ms: {
        'page.title': 'Konsol Bursa',
        'page.subtitle': 'Dagangkan pasaran perpetual dengan kedalaman buku langsung, kemasukan pesanan, pesanan terbuka dan risiko akaun.',
        'language.label': 'Bahasa',
        'tab.trade': 'Dagangan',
        'profile.open': 'Buka Profil',
        'profile.close': 'Tutup Profil',
        'profile.title': 'Profil',
        'profile.funds': 'Dana Peribadi',
        'profile.frozen': 'Dana Dibekukan',
        'profile.orders': 'Pesanan Terbuka',
        'profile.held': 'Posisi Dipegang',
        'profile.history': 'Sejarah Posisi',
        'profile.orderHold': 'Pegangan Pesanan Terbuka',
        'market.helper': 'Pilih pasaran tersedia dan berdagang dengan kecairan buku langsung.',
        'field.market': 'Pasaran',
        'field.email': 'E-mel',
        'field.password': 'Kata Laluan',
        'field.humanVerification': 'Pengesahan Manusia',
        'field.humanVerificationPlaceholder': 'Token pengesahan',
        'field.emailVerificationCode': 'Kod E-mel',
        'field.emailVerificationCodePlaceholder': 'Kod 6 digit',
        'field.leverage': 'Leveraj',
        'field.margin': 'Margin',
        'field.clientOrderId': 'ID Pesanan Klien',
        'action.reloadOrders': 'Muat Semula Pesanan',
        'action.register': 'Daftar',
        'action.login': 'Log Masuk',
        'action.logout': 'Log Keluar',
        'action.verifyRegistration': 'Sahkan Pendaftaran',
        'action.resendVerification': 'Hantar Semula Kod',
        'action.placeBuy': 'Buat Belian',
        'action.placeSell': 'Buat Jualan',
        'metric.session': 'Sesi',
        'metric.balance': 'Jumlah Baki',
        'metric.available': 'Tersedia untuk Dagangan',
        'metric.frozen': 'Dibekukan',
        'metric.positionMargin': 'Margin Digunakan',
        'account.createAccount': 'Daftar',
        'account.assetsTitle': 'Aset',
        'book.title': 'Buku Pesanan',
        'book.helper': 'Kedalaman padanan runtime untuk pasaran dipilih.',
        'book.asks': 'Ask',
        'book.bids': 'Bid',
        'book.spread': 'Spread',
        'book.depth': 'Ticks / sisi',
        'order.title': 'Buat Pesanan',
        'orders.title': 'Pesanan Terbuka',
        'orders.helper': 'Pesanan langsung pengguna semasa untuk pasaran dipilih.',
        'table.orderId': 'ID Pesanan',
        'table.side': 'Arah',
        'table.type': 'Jenis',
        'table.price': 'Harga',
        'table.qty': 'Kuantiti',
        'table.notional': 'Nilai Pesanan',
        'table.filled': 'Diisi',
        'table.status': 'Status',
        'empty.authState': 'Belum log masuk.',
        'empty.accountRaw': 'Tiada akaun dimuatkan.',
        'empty.orderResult': 'Tiada pesanan dihantar.',
        'empty.loginForOrders': 'Log masuk dan muat semula untuk memuat pesanan terbuka',
        'empty.book': 'Buku pesanan runtime kosong',
        'empty.noOpenOrders': 'Tiada pesanan terbuka',
        'empty.noHeldPositions': 'Tiada posisi dipegang dimuatkan.',
        'empty.noPositionHistory': 'Tiada sejarah posisi dimuatkan.',
        'status.authenticated': 'Disahkan',
        'status.notLoggedIn': 'Belum log masuk',
        'status.sessionUnavailable': 'Sesi tidak tersedia. Sila log masuk semula.',
        'status.accountUnavailable': 'Akaun tidak tersedia.',
        'status.registrationCreated': 'Akaun dibuat. Anda boleh log masuk sekarang.',
        'status.registrationPending': 'Masukkan kod pengesahan e-mel untuk melengkapkan pendaftaran.',
        'status.verificationResent': 'Kod pengesahan baharu telah dihantar.',
        'status.emailVerified': 'E-mel disahkan. Anda boleh log masuk sekarang.',
        'status.registrationVerified': 'Pendaftaran disahkan. Anda boleh log masuk sekarang.',
        'error.emailRequired': 'E-mel diperlukan.',
        'error.emailInvalid': 'Masukkan alamat e-mel yang sah.',
        'error.passwordRequired': 'Kata laluan diperlukan.',
        'error.passwordTooShort': 'Kata laluan mesti sekurang-kurangnya 8 aksara.',
        'error.verificationCodeRequired': 'Masukkan kod pengesahan e-mel.',
        'error.invalidCredentials': 'Akaun tidak ditemui atau kata laluan salah.',
        'error.registrationPending': 'Pengesahan pendaftaran sedang berjalan. Masukkan kod e-mel atau cuba lagi kemudian.',
        'notice.bookRecovered': 'Pesanan terbuka telah disimpan, tetapi buku pesanan memori kosong. Biasanya app dimulakan semula sebelum recovery/replay memulihkan pesanan.',
        'error.loginBeforeOrder': 'Sila log masuk sebelum membuat pesanan.'
    },
    ko: {
        'page.title': '거래소 콘솔',
        'page.subtitle': '실시간 호가창 깊이, 주문 입력, 미체결 주문, 계정 리스크로 무기한 시장을 거래하세요.',
        'language.label': '언어',
        'tab.trade': '거래',
        'profile.open': '프로필 열기',
        'profile.close': '프로필 닫기',
        'profile.title': '프로필',
        'profile.funds': '개인 자금',
        'profile.frozen': '동결 자금',
        'profile.orders': '미체결 주문',
        'profile.held': '보유 포지션',
        'profile.history': '포지션 내역',
        'profile.orderHold': '미체결 주문 동결',
        'market.helper': '사용 가능한 시장을 선택하고 실시간 호가창 유동성과 거래하세요.',
        'field.market': '시장',
        'field.email': '이메일',
        'field.password': '비밀번호',
        'field.humanVerification': '사람 인증',
        'field.humanVerificationPlaceholder': '인증 토큰',
        'field.emailVerificationCode': '이메일 코드',
        'field.emailVerificationCodePlaceholder': '6자리 코드',
        'field.leverage': '레버리지',
        'field.margin': '마진',
        'field.clientOrderId': '클라이언트 주문 ID',
        'action.reloadOrders': '주문 새로고침',
        'action.register': '가입',
        'action.login': '로그인',
        'action.logout': '로그아웃',
        'action.verifyRegistration': '가입 인증',
        'action.resendVerification': '코드 다시 보내기',
        'action.placeBuy': '매수 주문',
        'action.placeSell': '매도 주문',
        'metric.session': '세션',
        'metric.balance': '총 잔고',
        'metric.available': '거래 가능 잔고',
        'metric.frozen': '동결',
        'metric.positionMargin': '사용 중인 증거금',
        'account.createAccount': '가입',
        'account.assetsTitle': '자산',
        'book.title': '호가창',
        'book.helper': '선택한 시장의 런타임 매칭 깊이입니다.',
        'book.asks': '매도',
        'book.bids': '매수',
        'book.spread': '스프레드',
        'book.depth': '한쪽 ticks',
        'order.title': '주문 입력',
        'orders.title': '미체결 주문',
        'orders.helper': '선택한 시장에서 현재 사용자의 실시간 주문입니다.',
        'table.orderId': '주문 ID',
        'table.side': '방향',
        'table.type': '유형',
        'table.price': '가격',
        'table.qty': '수량',
        'table.notional': '주문 금액',
        'table.filled': '체결',
        'table.status': '상태',
        'empty.authState': '로그인하지 않았습니다.',
        'empty.accountRaw': '계정이 로드되지 않았습니다.',
        'empty.orderResult': '아직 주문을 제출하지 않았습니다.',
        'empty.loginForOrders': '로그인 후 새로고침하여 미체결 주문을 불러오세요',
        'empty.book': '런타임 호가창이 비어 있습니다',
        'empty.noOpenOrders': '미체결 주문 없음',
        'empty.noHeldPositions': '로드된 보유 포지션이 없습니다.',
        'empty.noPositionHistory': '로드된 포지션 내역이 없습니다.',
        'status.authenticated': '인증됨',
        'status.notLoggedIn': '로그인하지 않음',
        'status.sessionUnavailable': '세션을 사용할 수 없습니다. 다시 로그인하세요.',
        'status.accountUnavailable': '계정을 사용할 수 없습니다.',
        'status.registrationCreated': '계정이 생성되었습니다. 이제 로그인할 수 있습니다.',
        'status.registrationPending': '가입을 완료하려면 이메일 인증 코드를 입력하세요.',
        'status.verificationResent': '새 인증 코드가 전송되었습니다.',
        'status.emailVerified': '이메일이 인증되었습니다. 이제 로그인할 수 있습니다.',
        'status.registrationVerified': '가입 인증이 완료되었습니다. 이제 로그인할 수 있습니다.',
        'error.emailRequired': '이메일을 입력하세요.',
        'error.emailInvalid': '올바른 이메일 주소를 입력하세요.',
        'error.passwordRequired': '비밀번호를 입력하세요.',
        'error.passwordTooShort': '비밀번호는 최소 8자여야 합니다.',
        'error.verificationCodeRequired': '이메일 인증 코드를 입력하세요.',
        'error.invalidCredentials': '계정을 찾을 수 없거나 비밀번호가 올바르지 않습니다.',
        'error.registrationPending': '가입 인증이 이미 진행 중입니다. 이메일 코드를 입력하거나 나중에 다시 시도하세요.',
        'notice.bookRecovered': '미체결 주문은 저장되어 있지만 메모리 호가창이 비어 있습니다. 앱 재시작 후 복구/replay가 아직 완료되지 않았을 수 있습니다.',
        'error.loginBeforeOrder': '주문 전에 로그인하세요.'
    }
};
let currentLanguage = localStorage.getItem(I18N_STORAGE_KEY) || 'en';

function normalizeLanguage(language) {
    return translations[language] ? language : 'en';
}

function t(key) {
    return translations[currentLanguage]?.[key] || translations.en[key] || key;
}

function setCurrentLanguage(language, { persist = true, render = true } = {}) {
    currentLanguage = normalizeLanguage(language);
    $('language').value = currentLanguage;
    if (persist) {
        localStorage.setItem(I18N_STORAGE_KEY, currentLanguage);
    }
    if (render) {
        applyTranslations();
    }
}

function applyTranslations() {
    document.documentElement.lang = currentLanguage;
    document.title = t('page.title');
    document.querySelectorAll('[data-i18n]').forEach((el) => {
        el.textContent = t(el.dataset.i18n);
    });
    document.querySelectorAll('[data-i18n-aria-label]').forEach((el) => {
        el.setAttribute('aria-label', t(el.dataset.i18nAriaLabel));
    });
    document.querySelectorAll('[data-i18n-title]').forEach((el) => {
        el.setAttribute('title', t(el.dataset.i18nTitle));
    });
    document.querySelectorAll('[data-i18n-placeholder]').forEach((el) => {
        el.setAttribute('placeholder', t(el.dataset.i18nPlaceholder));
    });
}

// Form fields mirror the current exchange REST API request shape.
const fields = {
    symbol: $('symbol'),
    uid: $('uid'),
    humanVerificationToken: $('humanVerificationToken'),
    emailVerificationCode: $('emailVerificationCode'),
    side: $('side'),
    type: $('type'),
    price: $('price'),
    qty: $('qty'),
    leverage: $('leverage'),
    marginMode: $('marginMode'),
    clientOrderId: $('clientOrderId')
};

// Tokens are stored only for local MVP browser testing; production should move to hardened session storage.
const auth = {
    accessToken: localStorage.getItem('exchangeAccessToken') || '',
    refreshToken: localStorage.getItem('exchangeRefreshToken') || ''
};
const authConfig = {
    humanVerificationEnabled: false,
    humanVerificationProvider: 'turnstile',
    humanVerificationSiteKey: '',
    emailVerificationEnabled: false
};

// Page state compares runtime order book depth with persisted open orders for clearer MVP diagnostics.
const marketState = {
    depthEmpty: true,
    openOrderCount: 0,
    refreshTimer: null,
    refreshInFlight: false,
    exchangeSocket: null,
    exchangeSocketReconnectTimer: null,
    exchangeSocketConnected: false,
    exchangeMarketSymbol: '',
    exchangeUserUid: '',
    exchangeUserSubscribed: false,
    exchangeLastConnectionId: '',
    cancelOnDisconnect: false,
    latestOrders: []
};
const DEFAULT_VISIBLE_TICKS_PER_SIDE = 5;

// Display helpers keep empty numeric fields readable when the matching book has no resting liquidity.
function text(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value);
}

function money(value) {
    if (value === null || value === undefined || value === '') return '-';
    const num = Number(value);
    return Number.isFinite(num) ? num.toLocaleString(undefined, { maximumFractionDigits: 8 }) : String(value);
}

function notional(level) {
    const price = Number(level.price);
    const qty = Number(level.qty);
    return Number.isFinite(price) && Number.isFinite(qty) ? money(price * qty) : '-';
}

function numeric(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : 0;
}

function selectedSymbol() {
    return fields.symbol.value.trim().toUpperCase();
}

function syncSymbolTitle() {
    document.title = `${t('page.title')} - ${selectedSymbol() || 'Market'}`;
}

function setUserIdentity(user) {
    const uid = user?.uid || '';
    fields.uid.value = uid;
    $('uidDisplay').textContent = text(uid);
    $('sessionDisplay').textContent = user?.email || (auth.accessToken ? t('status.authenticated') : t('status.notLoggedIn'));
    syncAuthenticatedUi();
}

function setActiveTab(tabName) {
    // Tabs keep the trading workflow compact while preserving the existing account-management controls.
    document.querySelectorAll('[data-tab]').forEach(button => {
        const active = button.dataset.tab === tabName;
        button.setAttribute('aria-selected', String(active));
    });
    document.querySelectorAll('[data-tab-panel]').forEach(panel => {
        panel.hidden = panel.dataset.tabPanel !== tabName;
    });
}

function toggleProfilePanel(forceOpen) {
    const panel = $('profilePanel');
    const open = forceOpen === undefined ? panel.hidden : forceOpen;
    panel.hidden = !open;
    $('profileToggle').setAttribute('aria-expanded', String(open));
    $('profileToggle').setAttribute('aria-label', open ? t('profile.close') : t('profile.open'));
    $('profileToggle').setAttribute('title', open ? t('profile.close') : t('profile.open'));
    syncAuthenticatedUi();
}

function promptLogin() {
    // Unauthenticated trade actions open the existing profile auth drawer instead of leaving users at an error.
    auth.accessToken = '';
    auth.refreshToken = '';
    localStorage.removeItem('exchangeAccessToken');
    localStorage.removeItem('exchangeRefreshToken');
    setUserIdentity(null);
    syncAuthenticatedUi();
    toggleProfilePanel(true);
    $('profilePanel').scrollTo({top: 0, behavior: 'smooth'});
    window.requestAnimationFrame(() => $('authEmail').focus({preventScroll: true}));
}

function syncProfileAuthState() {
    const loggedIn = Boolean(auth.accessToken && fields.uid.value.trim());
    $('profileContent').hidden = !loggedIn;
}

function syncAccountAuthState() {
    const loggedIn = Boolean(auth.accessToken && fields.uid.value.trim());
    $('authCard').hidden = loggedIn;
    $('accountSummary').hidden = !loggedIn;
}

function syncAuthenticatedUi() {
    syncAccountAuthState();
    syncProfileAuthState();
}

function renderMarketOptions(markets) {
    const current = selectedSymbol() || 'BTCUSDT';
    const configuredSymbols = (markets || [])
        .filter(market => market && market.symbol && market.tradingEnabled !== false)
        .map(market => market.symbol.toUpperCase());
    const symbols = [...new Set(configuredSymbols.length > 0 ? configuredSymbols : [current])];
    if (!symbols.includes(current)) {
        symbols.unshift(current);
    }
    fields.symbol.replaceChildren(...symbols.map(symbol => {
        const option = document.createElement('option');
        option.value = symbol;
        option.textContent = symbol;
        return option;
    }));
    fields.symbol.value = current;
    syncSymbolTitle();
}

function showError(el, error) {
    el.textContent = error.message || String(error);
    el.hidden = false;
}

function clearError(el) {
    el.textContent = '';
    el.hidden = true;
}

function showNotice(message) {
    $('authNotice').textContent = message;
    $('authNotice').hidden = false;
}

function clearNotice() {
    $('authNotice').textContent = '';
    $('authNotice').hidden = true;
}

function setRegistrationVerificationMode(active) {
    // The verification-code step is a distinct registration state; showing login/register beside it creates duplicate CTAs.
    $('emailVerificationStep').hidden = !active;
    $('authActions').hidden = active;
}

function rememberPendingRegistration(email) {
    const normalized = (email || '').trim();
    if (normalized) {
        localStorage.setItem(PENDING_REGISTRATION_EMAIL_KEY, normalized);
    }
}

function clearPendingRegistration() {
    localStorage.removeItem(PENDING_REGISTRATION_EMAIL_KEY);
}

function continuePendingRegistration(email) {
    // Pending registration is recoverable: users can return after refresh/offline interruption and type the email code.
    rememberPendingRegistration(email);
    $('authEmail').value = (email || $('authEmail').value).trim();
    fields.emailVerificationCode.value = '';
    setRegistrationVerificationMode(true);
    showNotice(t('status.registrationPending'));
    fields.emailVerificationCode.focus();
}

// Session-bound data must disappear on logout so the next person at the browser cannot see stale account state.
function clearSessionUi() {
    $('authState').textContent = t('empty.authState');
    $('accountRaw').textContent = t('empty.accountRaw');
    $('sessionDisplay').textContent = t('status.notLoggedIn');
    setRegistrationVerificationMode(false);
    renderAccountSnapshot(null);
    marketState.latestOrders = [];
    renderProfileOrders([]);
    $('orders').innerHTML = `<tr><td colspan="8"><div class="empty">${t('empty.loginForOrders')}</div></td></tr>`;
    $('orderResult').textContent = t('empty.orderResult');
    unsubscribeCurrentUser();
    setUserIdentity(null);
    updateFallbackPolling();
    marketState.openOrderCount = 0;
    updateMarketStateNotice();
    clearError($('accountError'));
    clearError($('orderError'));
    syncAuthenticatedUi();
}

// API wrapper attaches the first-party JWT when present; auth is optional in dev but required in prod.
async function api(path, options = {}) {
    const headers = {'Accept': 'application/json', 'Content-Type': 'application/json'};
    if (auth.accessToken) {
        headers.Authorization = `Bearer ${auth.accessToken}`;
    }
    const response = await fetch(path, {
        headers,
        ...options
    });
    const body = await response.json();
    if (!response.ok || !body.ok) {
        const error = new Error(body.error || body.message || `HTTP ${response.status}`);
        error.status = response.status;
        error.code = body.code;
        error.serverMessage = body.message || body.error || '';
        throw error;
    }
    return body.data;
}

// Registration starts verification, while login is the only path that stores access and refresh tokens.
async function authenticate(mode) {
    clearError($('authError'));
    clearNotice();
    if (!validateCredentials(mode)) {
        return;
    }
    try {
        if (mode === 'register') {
            const result = await api('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({
                    email: $('authEmail').value.trim(),
                    password: $('authPassword').value,
                    humanVerificationToken: fields.humanVerificationToken.value.trim(),
                    preferredLanguage: currentLanguage,
                    timeZone: browserTimeZone()
                })
            });
            fields.humanVerificationToken.value = '';
            fields.emailVerificationCode.value = '';
            if (result.emailVerificationRequired) {
                continuePendingRegistration(result.email || $('authEmail').value);
                return;
            }
            clearPendingRegistration();
            setRegistrationVerificationMode(false);
            showNotice(result.emailVerificationRequired ? t('status.registrationPending') : t('status.registrationCreated'));
            return;
        }
        const result = await api(`/api/auth/${mode}`, {
            method: 'POST',
            body: JSON.stringify({
                email: $('authEmail').value.trim(),
                password: $('authPassword').value
            })
        });
        applyAuthResult(result);
        await refreshAll();
        subscribeCurrentUser();
    } catch (error) {
        if (mode === 'register' && error.code === 'AUTH_REGISTRATION_PENDING') {
            continuePendingRegistration($('authEmail').value);
            return;
        }
        showError($('authError'), authDisplayError(mode, error));
    }
}

function authDisplayError(mode, error) {
    // Login failures intentionally collapse unknown email and wrong password into one customer-facing message.
    if (mode === 'login' && (error.code === 'AUTH_INVALID_CREDENTIAL' || [400, 401, 403].includes(error.status))) {
        return new Error(t('error.invalidCredentials'));
    }
    if (mode === 'register' && error.code === 'AUTH_REGISTRATION_PENDING') {
        return new Error(t('error.registrationPending'));
    }
    return error;
}

function validateCredentials(mode) {
    const email = $('authEmail').value.trim();
    const password = $('authPassword').value;
    if (!email) {
        showError($('authError'), new Error(t('error.emailRequired')));
        $('authEmail').focus();
        return false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showError($('authError'), new Error(t('error.emailInvalid')));
        $('authEmail').focus();
        return false;
    }
    if (!password) {
        showError($('authError'), new Error(t('error.passwordRequired')));
        $('authPassword').focus();
        return false;
    }
    if (mode === 'register' && password.length < 8) {
        showError($('authError'), new Error(t('error.passwordTooShort')));
        $('authPassword').focus();
        return false;
    }
    return true;
}

function browserTimeZone() {
    // IANA time zones let the backend render email expiry in the customer's local time.
    return Intl.DateTimeFormat().resolvedOptions().timeZone || '';
}

function applyAuthResult(result) {
    // Registration verification and password login both establish the same browser session shape.
    auth.accessToken = result.accessToken;
    auth.refreshToken = result.refreshToken;
    localStorage.setItem('exchangeAccessToken', auth.accessToken);
    localStorage.setItem('exchangeRefreshToken', auth.refreshToken);
    applyUserLanguage(result.user);
    setUserIdentity(result.user);
    renderAuth(result.user);
}

async function verifyRegistrationCode() {
    clearError($('authError'));
    clearNotice();
    if (!validateEmailCode()) {
        return;
    }
    try {
        const result = await api('/api/auth/verify-email', {
            method: 'POST',
            body: JSON.stringify({
                email: $('authEmail').value.trim(),
                code: fields.emailVerificationCode.value.trim()
            })
        });
        fields.emailVerificationCode.value = '';
        clearPendingRegistration();
        setRegistrationVerificationMode(false);
        applyAuthResult(result);
        showNotice(t('status.registrationVerified'));
        await refreshAll();
        subscribeCurrentUser();
    } catch (error) {
        showError($('authError'), error);
    }
}

async function resendRegistrationCode() {
    clearError($('authError'));
    clearNotice();
    const email = $('authEmail').value.trim() || localStorage.getItem(PENDING_REGISTRATION_EMAIL_KEY) || '';
    if (!email) {
        showError($('authError'), new Error(t('error.emailRequired')));
        $('authEmail').focus();
        return;
    }
    try {
        const result = await api('/api/auth/resend-verification', {
            method: 'POST',
            body: JSON.stringify({
                email,
                preferredLanguage: currentLanguage,
                timeZone: browserTimeZone()
            })
        });
        continuePendingRegistration(result?.email || email);
        showNotice(t('status.verificationResent'));
    } catch (error) {
        // Resend may fail when offline; keep the recoverable code-entry state visible for retry.
        setRegistrationVerificationMode(true);
        showError($('authError'), error);
    }
}

function validateEmailCode() {
    const email = $('authEmail').value.trim();
    const code = fields.emailVerificationCode.value.trim();
    if (!email) {
        showError($('authError'), new Error(t('error.emailRequired')));
        $('authEmail').focus();
        return false;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showError($('authError'), new Error(t('error.emailInvalid')));
        $('authEmail').focus();
        return false;
    }
    if (!code) {
        showError($('authError'), new Error(t('error.verificationCodeRequired')));
        fields.emailVerificationCode.focus();
        return false;
    }
    return true;
}

async function loadAuthConfig() {
    try {
        const config = await api('/api/auth/config');
        Object.assign(authConfig, config || {});
        $('humanVerification').hidden = !authConfig.humanVerificationEnabled;
        if (authConfig.humanVerificationEnabled && authConfig.humanVerificationProvider === 'turnstile' && authConfig.humanVerificationSiteKey) {
            loadTurnstile();
        }
    } catch (error) {
        $('humanVerification').hidden = true;
    }
}

function loadTurnstile() {
    if (window.turnstile) {
        renderTurnstile();
        return;
    }
    const script = document.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    script.async = true;
    script.defer = true;
    script.onload = renderTurnstile;
    document.head.appendChild(script);
}

function renderTurnstile() {
    if (!window.turnstile || $('turnstileWidget').dataset.rendered === 'true') {
        return;
    }
    window.turnstile.render('#turnstileWidget', {
        sitekey: authConfig.humanVerificationSiteKey,
        callback: token => fields.humanVerificationToken.value = token,
        'expired-callback': () => fields.humanVerificationToken.value = ''
    });
    $('turnstileWidget').dataset.rendered = 'true';
}

async function verifyEmailFromUrl() {
    const token = new URLSearchParams(window.location.search).get('verifyEmailToken');
    if (!token) {
        return;
    }
    clearError($('authError'));
    try {
        const result = await api('/api/auth/verify-email', {
            method: 'POST',
            body: JSON.stringify({ token })
        });
        clearPendingRegistration();
        setRegistrationVerificationMode(false);
        applyAuthResult(result);
        showNotice(t('status.emailVerified'));
        await refreshAll();
        subscribeCurrentUser();
        window.history.replaceState({}, document.title, window.location.pathname);
    } catch (error) {
        showError($('authError'), error);
    }
}

// Logout revokes the refresh session server-side and clears local browser state regardless of API outcome.
async function logout() {
    clearError($('authError'));
    try {
        await api('/api/auth/logout', {
            method: 'POST',
            body: JSON.stringify({ refreshToken: auth.refreshToken })
        });
    } catch (error) {
        showError($('authError'), error);
    } finally {
        auth.accessToken = '';
        auth.refreshToken = '';
        localStorage.removeItem('exchangeAccessToken');
        localStorage.removeItem('exchangeRefreshToken');
        unsubscribeCurrentUser();
        clearSessionUi();
    }
}

// On reload, /me hydrates the UI from the access token without asking the user to login again.
async function loadCurrentUser() {
    if (!auth.accessToken) {
        clearSessionUi();
        return;
    }
    try {
        const user = await api('/api/auth/me');
        if (user) {
            applyUserLanguage(user);
            setUserIdentity(user);
            renderAuth(user);
            subscribeCurrentUser();
        } else {
            clearSessionUi();
            $('authState').textContent = t('status.sessionUnavailable');
        }
    } catch (error) {
        clearSessionUi();
        showError($('authError'), error);
    }
}

// Auth state intentionally shows roles/scopes so MVP testers can inspect authorization claims.
function renderAuth(user) {
    $('authState').textContent = JSON.stringify({
        uid: user.uid,
        email: user.email,
        roles: user.roles,
        scopes: user.scopes,
        preferredLanguage: user.preferredLanguage
    }, null, 2);
}

function applyUserLanguage(user) {
    if (user?.preferredLanguage && user.preferredLanguage !== currentLanguage) {
        // Server preference wins after login/session restore so returning customers see their saved locale.
        setCurrentLanguage(user.preferredLanguage);
    }
}

async function syncPreferredLanguage() {
    if (!auth.accessToken) {
        return;
    }
    try {
        const user = await api('/api/auth/language', {
            method: 'POST',
            body: JSON.stringify({ preferredLanguage: currentLanguage })
        });
        if (user) {
            setUserIdentity(user);
            renderAuth(user);
        }
    } catch (error) {
        showError($('authError'), error);
    }
}

function renderAccountSnapshot(account) {
    const balance = money(account?.balance);
    const available = money(account?.available);
    const frozen = money(account?.frozen);
    const orderHold = money(account?.orderHold);
    const positionMargin = money(account?.positionMargin);
    $('balance').textContent = balance;
    $('available').textContent = available;
    $('frozen').textContent = frozen;
    $('positionMargin').textContent = positionMargin;
    $('profileBalance').textContent = balance;
    $('profileAvailable').textContent = available;
    $('profileFrozen').textContent = frozen;
    $('profileOrderHold').textContent = orderHold;
    $('profilePositionMargin').textContent = positionMargin;
}

// Depth is read-only market data and may legitimately be empty in a fresh local environment.
async function loadDepth() {
    const symbol = selectedSymbol();
    syncSymbolTitle();
    const depth = await api(`/api/depth/${encodeURIComponent(symbol)}?depth=${visibleTicksPerSide()}`);
    $('spreadMid').textContent = `${t('book.spread')}: ${money(Math.max(0, numeric(depth.bestAsk) - numeric(depth.bestBid)))}`;
    renderOrderBook(depth);
    marketState.depthEmpty = (depth.bids || []).length === 0 && (depth.asks || []).length === 0;
    updateMarketStateNotice();
}

function updateMarketStateNotice() {
    const notice = $('marketStateNotice');
    if (marketState.depthEmpty && marketState.openOrderCount > 0) {
        notice.textContent = t('notice.bookRecovered');
        notice.hidden = false;
        return;
    }
    notice.textContent = '';
    notice.hidden = true;
}

// The visible book is capped independently per side, matching common exchange depth controls.
function renderOrderBook(depth) {
    renderLevels($('orderBookRows'), depth.asks || [], depth.bids || []);
}

function visibleTicksPerSide() {
    const value = Number($('bookDepth')?.value || DEFAULT_VISIBLE_TICKS_PER_SIDE);
    return [5, 10, 20, 50].includes(value) ? value : DEFAULT_VISIBLE_TICKS_PER_SIDE;
}

// Ask and bid ticks are rendered separately so a 5-tick view means 5 asks plus 5 bids.
function renderLevels(target, asks, bids) {
    const visibleAsks = sortedDepthLevels(asks.map(level => ({...level, side: 'ask'}))).slice(0, visibleTicksPerSide());
    const visibleBids = sortedDepthLevels(bids.map(level => ({...level, side: 'bid'}))).slice(0, visibleTicksPerSide());
    const sortedLevels = [...visibleAsks, ...visibleBids];
    if (visibleAsks.length === 0 && visibleBids.length === 0) {
        target.innerHTML = `<tr><td colspan="3"><div class="empty">${t('empty.book')}</div></td></tr>`;
        return;
    }
    const maxQty = sortedLevels.reduce((max, level) => Math.max(max, numeric(level.qty)), 0) || 1;
    target.innerHTML = [
        ...visibleAsks.map(level => depthRow(level, maxQty)),
        ...visibleBids.map(level => depthRow(level, maxQty))
    ].join('');
}

function depthRow(level, maxQty) {
    return `
        <tr class="depth-row ${level.side}" style="--depth-pct: ${Math.max(4, Math.min(100, (numeric(level.qty) / maxQty) * 100)).toFixed(2)}%;">
            <td class="${level.side === 'bid' ? 'buy-text' : 'sell-text'}"><span class="depth-fill" aria-hidden="true"></span>${money(level.price)}</td>
            <td>${money(level.qty)}</td>
            <td>${notional(level)}</td>
        </tr>
    `;
}

function sortedDepthLevels(levels) {
    return [...levels].sort((left, right) => {
        const priceDiff = numeric(right.price) - numeric(left.price);
        if (Math.abs(priceDiff) > 0.00000001) {
            return priceDiff;
        }
        // Same-price aggregate rows stay deterministic when replayed from snapshots or WebSocket deltas.
        return numeric(right.qty) - numeric(left.qty);
    });
}

// Open orders are scoped by uid and symbol; future auth hardening should verify uid ownership server-side.
async function loadOrders() {
    if (!auth.accessToken || !fields.uid.value.trim()) {
        $('orders').innerHTML = `<tr><td colspan="8"><div class="empty">${t('empty.loginForOrders')}</div></td></tr>`;
        marketState.latestOrders = [];
        renderProfileOrders([]);
        marketState.openOrderCount = 0;
        updateMarketStateNotice();
        return;
    }
    const uid = encodeURIComponent(fields.uid.value.trim());
    const symbol = encodeURIComponent(selectedSymbol());
    const orders = await api(`/api/order/open?uid=${uid}&symbol=${symbol}`);
    marketState.latestOrders = orders || [];
    renderProfileOrders(marketState.latestOrders);
    marketState.openOrderCount = orders ? orders.length : 0;
    updateMarketStateNotice();
    if (!orders || orders.length === 0) {
        $('orders').innerHTML = `<tr><td colspan="8"><div class="empty">${t('empty.noOpenOrders')}</div></td></tr>`;
        return;
    }
    $('orders').innerHTML = orders.map(order => `
        <tr>
            <td>${text(order.orderId).slice(0, 12)}</td>
            <td>${text(order.symbol)}</td>
            <td class="${order.side === 'BUY' ? 'buy-text' : 'sell-text'}">${text(order.side)}</td>
            <td>${text(order.type)}</td>
            <td>${money(order.price)}</td>
            <td>${money(order.qty)}</td>
            <td>${money(order.executedQty)}</td>
            <td><span class="status">${text(order.status)}</span></td>
        </tr>
    `).join('');
}

function renderProfileOrders(orders) {
    const target = $('profileOrders');
    if (!auth.accessToken || !fields.uid.value.trim()) {
        target.className = 'empty';
        target.textContent = t('empty.loginForOrders');
        return;
    }
    if (!orders || orders.length === 0) {
        target.className = 'empty';
        target.textContent = t('empty.noOpenOrders');
        return;
    }
    target.className = 'table-wrap';
    target.innerHTML = `
        <table>
            <thead>
            <tr>
                <th>${t('table.orderId')}</th>
                <th>${t('field.market')}</th>
                <th>${t('table.side')}</th>
                <th>${t('table.price')}</th>
                <th>${t('table.qty')}</th>
                <th>${t('table.status')}</th>
            </tr>
            </thead>
            <tbody>
            ${orders.map(order => `
                <tr>
                    <td>${text(order.orderId).slice(0, 12)}</td>
                    <td>${text(order.symbol)}</td>
                    <td class="${order.side === 'BUY' ? 'buy-text' : 'sell-text'}">${text(order.side)}</td>
                    <td>${money(order.price)}</td>
                    <td>${money(order.qty)}</td>
                    <td><span class="status">${text(order.status)}</span></td>
                </tr>
            `).join('')}
            </tbody>
        </table>
    `;
}

// Account lookup is read-only here; deposit/withdraw remain separate explicit workflows.
async function loadAccount() {
    clearError($('accountError'));
    if (!auth.accessToken || !fields.uid.value.trim()) {
        $('accountRaw').textContent = t('empty.accountRaw');
        renderAccountSnapshot(null);
        return;
    }
    try {
        const account = await api(`/api/margin/account?uid=${encodeURIComponent(fields.uid.value.trim())}`);
        $('accountRaw').textContent = JSON.stringify(account, null, 2);
        renderAccountSnapshot(account);
    } catch (error) {
        $('accountRaw').textContent = t('status.accountUnavailable');
        renderAccountSnapshot(null);
        showError($('accountError'), error);
    }
}

// Market selection is controlled by admin market config; the client cannot type arbitrary symbols.
async function loadMarketOptions() {
    try {
        const config = await api('/api/markets');
        renderMarketOptions(config?.markets || []);
    } catch (error) {
        renderMarketOptions([{ symbol: selectedSymbol() || 'BTCUSDT', tradingEnabled: true }]);
    }
}

// Refresh runs independent panes concurrently so one failing API does not blank the entire console.
async function refreshAll() {
    await loadMarketOptions();
    await Promise.allSettled([loadDepth(), loadOrders(), loadAccount()]);
}

function startMarketRefresh() {
    startExchangeStream();
    updateFallbackPolling();
}

function startFallbackPolling() {
    if (marketState.refreshTimer) {
        return;
    }
    // Polling is the reconnect fallback when the private user WebSocket is unavailable.
    marketState.refreshTimer = window.setInterval(refreshLivePanes, 1000);
}

function stopFallbackPolling() {
    if (marketState.refreshTimer) {
        window.clearInterval(marketState.refreshTimer);
        marketState.refreshTimer = null;
    }
}

function updateFallbackPolling() {
    const userStreamRequired = Boolean(auth.accessToken && fields.uid.value.trim());
    const marketReady = marketState.exchangeSocketConnected && marketState.exchangeMarketSymbol === selectedSymbol();
    const userReady = !userStreamRequired
        || (marketState.exchangeUserSubscribed && marketState.exchangeUserUid === fields.uid.value.trim());
    const websocketReady = marketReady && userReady;
    if (websocketReady) {
        stopFallbackPolling();
    } else {
        startFallbackPolling();
    }
}

async function refreshLivePanes() {
    if (marketState.refreshInFlight) {
        return;
    }
    marketState.refreshInFlight = true;
    try {
        await Promise.allSettled([loadDepth(), loadOrders()]);
    } finally {
        marketState.refreshInFlight = false;
    }
}

function startExchangeStream() {
    if (marketState.exchangeSocket || marketState.exchangeSocketConnected) {
        return;
    }
    window.clearTimeout(marketState.exchangeSocketReconnectTimer);
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = `${protocol}//${window.location.host}/ws/exchange`;
    try {
        const socket = new WebSocket(url);
        marketState.exchangeSocket = socket;
        socket.onopen = () => {
            marketState.exchangeSocketConnected = true;
            subscribeCurrentMarket();
            subscribeCurrentUser();
            refreshLivePanes();
        };
        socket.onmessage = event => handleExchangeStreamMessage(event.data);
        socket.onerror = () => reconnectExchangeStream();
        socket.onclose = () => reconnectExchangeStream();
    } catch (error) {
        reconnectExchangeStream();
    }
}

function handleExchangeStreamMessage(raw) {
    try {
        const message = JSON.parse(raw);
        if (message.event === 'subscribed.market') {
            marketState.exchangeMarketSymbol = text(message.data?.symbol);
            updateFallbackPolling();
            return;
        }
        if (message.event === 'subscribed.user') {
            marketState.exchangeUserUid = text(message.data?.uid);
            marketState.exchangeUserSubscribed = true;
            marketState.exchangeLastConnectionId = text(message.data?.connectionId);
            updateFallbackPolling();
            return;
        }
        if (message.event === 'unsubscribed.user') {
            marketState.exchangeUserUid = '';
            marketState.exchangeUserSubscribed = false;
            updateFallbackPolling();
            return;
        }
        if (message.event === 'error') {
            updateFallbackPolling();
            return;
        }
        if (message.event === 'market-maker.quote') {
            // Market-maker identity is an operator concern; clients refresh only the public book impact.
            Promise.allSettled([loadDepth(), loadOrders()]);
            return;
        }
        if (['depth-delta', 'ticker', 'trade', 'order.lifecycle', 'gateway.heartbeat'].includes(message.event)) {
            refreshLivePanes();
        }
    } catch (error) {
        refreshLivePanes();
    }
}

function reconnectExchangeStream() {
    marketState.exchangeSocketConnected = false;
    marketState.exchangeSocket = null;
    marketState.exchangeMarketSymbol = '';
    marketState.exchangeUserUid = '';
    marketState.exchangeUserSubscribed = false;
    updateFallbackPolling();
    window.clearTimeout(marketState.exchangeSocketReconnectTimer);
    marketState.exchangeSocketReconnectTimer = window.setTimeout(startExchangeStream, 1500);
}

function sendExchangeCommand(command) {
    if (!marketState.exchangeSocket || marketState.exchangeSocket.readyState !== 1) {
        updateFallbackPolling();
        return false;
    }
    marketState.exchangeSocket.send(JSON.stringify(command));
    return true;
}

function subscribeCurrentMarket() {
    const symbol = selectedSymbol();
    if (!symbol) {
        updateFallbackPolling();
        return;
    }
    if (marketState.exchangeMarketSymbol && marketState.exchangeMarketSymbol !== symbol) {
        sendExchangeCommand({type: 'unsubscribe.market', symbol: marketState.exchangeMarketSymbol});
    }
    sendExchangeCommand({type: 'subscribe.market', symbol});
}

function subscribeCurrentUser() {
    const uid = fields.uid.value.trim();
    if (!auth.accessToken || !uid) {
        updateFallbackPolling();
        return;
    }
    if (marketState.exchangeUserUid && marketState.exchangeUserUid !== uid) {
        sendExchangeCommand({type: 'unsubscribe.user', uid: Number(marketState.exchangeUserUid)});
        marketState.exchangeUserSubscribed = false;
    }
    sendExchangeCommand({
        type: 'subscribe.user',
        uid: Number(uid),
        token: auth.accessToken,
        symbol: selectedSymbol(),
        cancelOnDisconnect: marketState.cancelOnDisconnect,
        resumeConnectionId: marketState.cancelOnDisconnect ? marketState.exchangeLastConnectionId : ''
    });
}

function unsubscribeCurrentUser() {
    const uid = fields.uid.value.trim() || marketState.exchangeUserUid;
    if (uid) {
        sendExchangeCommand({type: 'unsubscribe.user', uid: Number(uid)});
    }
    marketState.exchangeUserUid = '';
    marketState.exchangeUserSubscribed = false;
    marketState.exchangeLastConnectionId = '';
    updateFallbackPolling();
}

// Order entry sends internal exchange orders only; backend venue routing decides whether Polymarket is used.
async function placeOrder(side) {
    clearError($('orderError'));
    if (!auth.accessToken || !fields.uid.value.trim()) {
        const error = new Error(t('error.loginBeforeOrder'));
        $('orderResult').textContent = t('empty.orderResult');
        showError($('orderError'), error);
        promptLogin();
        return;
    }
    fields.side.value = side;
    const payload = {
        uid: Number(fields.uid.value),
        symbol: selectedSymbol(),
        side,
        type: fields.type.value,
        price: fields.type.value === 'MARKET' ? null : fields.price.value,
        qty: fields.qty.value,
        leverage: Number(fields.leverage.value),
        marginMode: fields.marginMode.value,
        clientOrderId: fields.clientOrderId.value.trim() || `web-${Date.now()}`,
        timeInForce: 'GTC',
        reduceOnly: false,
        postOnly: false
    };
    try {
        const result = await api('/api/order/place', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        $('orderResult').textContent = JSON.stringify({ request: payload, result }, null, 2);
        await refreshAll();
    } catch (error) {
        $('orderResult').textContent = JSON.stringify(payload, null, 2);
        showError($('orderError'), error);
        if (error.status === 401 || error.status === 403) {
            promptLogin();
        }
    }
}

$('register').addEventListener('click', () => authenticate('register'));
$('login').addEventListener('click', () => authenticate('login'));
$('verifyEmailCode').addEventListener('click', verifyRegistrationCode);
$('resendEmailCode').addEventListener('click', resendRegistrationCode);
$('logout').addEventListener('click', logout);
$('reloadOrders').addEventListener('click', loadOrders);
$('placeBuy').addEventListener('click', () => placeOrder('BUY'));
$('placeSell').addEventListener('click', () => placeOrder('SELL'));
document.querySelectorAll('[data-tab]').forEach(button => {
    button.addEventListener('click', () => setActiveTab(button.dataset.tab));
});
$('profileToggle').addEventListener('click', () => toggleProfilePanel());
$('profileClose').addEventListener('click', () => toggleProfilePanel(false));
setCurrentLanguage(currentLanguage, { render: false });
$('language').addEventListener('change', () => {
    setCurrentLanguage($('language').value);
    toggleProfilePanel(!$('profilePanel').hidden);
    renderProfileOrders(marketState.latestOrders);
    setUserIdentity(fields.uid.value ? { uid: fields.uid.value, email: $('sessionDisplay').textContent } : null);
    syncPreferredLanguage();
    Promise.allSettled([loadDepth(), loadOrders(), loadAccount()]);
});
fields.symbol.addEventListener('change', () => {
    syncSymbolTitle();
    subscribeCurrentMarket();
    Promise.allSettled([loadDepth(), loadOrders()]);
});
$('bookDepth').addEventListener('change', () => Promise.allSettled([loadDepth()]));
fields.uid.addEventListener('change', () => Promise.allSettled([loadOrders(), loadAccount()]));

applyTranslations();
syncSymbolTitle();
setActiveTab('trade');
renderProfileOrders([]);
setUserIdentity(null);
syncAuthenticatedUi();
loadAuthConfig();
verifyEmailFromUrl();
loadCurrentUser().finally(() => refreshAll().finally(startMarketRefresh));
