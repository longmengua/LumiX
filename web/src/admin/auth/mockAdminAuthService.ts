export const ADMIN_SESSION_STORAGE_KEY = 'lumix.admin.session';

export type AdminSession = {
  operatorId: string;
  email: string;
  role: 'admin' | 'operator' | 'auditor';
  permissions: string[];
  issuedAt: string;
};

export type AdminLoginInput = {
  email: string;
  password: string;
};

export function readAdminSession(): AdminSession | null {
  try {
    const raw = window.localStorage.getItem(ADMIN_SESSION_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    return JSON.parse(raw) as AdminSession;
  } catch {
    return null;
  }
}

export function saveAdminSession(session: AdminSession) {
  window.localStorage.setItem(ADMIN_SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function clearAdminSession() {
  window.localStorage.removeItem(ADMIN_SESSION_STORAGE_KEY);
}

export async function signInAdminMock(input: AdminLoginInput): Promise<AdminSession> {
  await delay(180);
  if (!input.email.trim() || !input.password.trim()) {
    throw new Error('Missing admin credentials.');
  }

  return {
    operatorId: 'op-0001',
    email: input.email.trim(),
    role: 'operator',
    permissions: ['admin:read', 'admin:write', 'risk:read', 'risk:write'],
    issuedAt: new Date().toISOString(),
  };
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
