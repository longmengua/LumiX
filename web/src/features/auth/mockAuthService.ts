const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export type LoginInput = {
  identifier: string;
  password: string;
  remember: boolean;
};

export type LoginResult =
  | {
      status: 'success';
      displayName: string;
      nextPath: string;
    }
  | {
      status: 'two_factor';
      displayName: string;
      challengeLabel: string;
      nextPath: string;
    };

export type RegisterInput = {
  identifier: string;
  verificationCode: string;
  password: string;
  confirmPassword: string;
  referralCode?: string;
  acceptedTerms: boolean;
};

export type ResetPasswordInput = {
  identifier: string;
  newPassword: string;
  confirmPassword: string;
};

export async function signInMock(input: LoginInput): Promise<LoginResult> {
  await delay(500);

  if (!input.identifier.trim()) {
    throw new Error('Please enter an email or phone number.');
  }

  if (!input.password.trim()) {
    throw new Error('Please enter your password.');
  }

  const secureFlow = input.identifier.toLowerCase().includes('secure');

  if (secureFlow) {
    return {
      status: 'two_factor',
      displayName: 'Secure user',
      challengeLabel: `Sent to ${maskIdentifier(input.identifier)}`,
      nextPath: '/?demo=secure',
    };
  }

  return {
    status: 'success',
    displayName: input.identifier,
    nextPath: input.remember ? '/?session=remembered' : '/',
  };
}

export async function registerMock(input: RegisterInput): Promise<string> {
  await delay(550);

  if (!emailPattern.test(input.identifier) && !isPhoneNumber(input.identifier)) {
    throw new Error('Use a valid email address or phone number.');
  }

  if (!input.verificationCode.trim()) {
    throw new Error('Verification code is required.');
  }

  if (input.password.length < 8) {
    throw new Error('Password must be at least 8 characters.');
  }

  if (input.password !== input.confirmPassword) {
    throw new Error('Passwords do not match.');
  }

  if (!input.acceptedTerms) {
    throw new Error('You must accept the terms.');
  }

  return `Verification sent to ${maskIdentifier(input.identifier)}.`;
}

export async function requestPasswordResetMock(identifier: string): Promise<string> {
  await delay(400);

  if (!identifier.trim()) {
    throw new Error('Please enter an email or phone number.');
  }

  return `Reset instructions sent to ${maskIdentifier(identifier)}.`;
}

export async function resetPasswordMock(input: ResetPasswordInput): Promise<string> {
  await delay(450);

  if (!input.identifier.trim()) {
    throw new Error('Please enter an email or phone number.');
  }

  if (input.newPassword.length < 8) {
    throw new Error('Password must be at least 8 characters.');
  }

  if (input.newPassword !== input.confirmPassword) {
    throw new Error('Passwords do not match.');
  }

  return 'Password updated successfully.';
}

export async function verifyTwoFactorMock(code: string): Promise<void> {
  await delay(350);

  if (code.trim() !== '123456') {
    throw new Error('Invalid verification code. Try 123456 for the demo flow.');
  }
}

function maskIdentifier(identifier: string) {
  if (identifier.includes('@')) {
    const [local, domain] = identifier.split('@');
    if (!local || !domain) return identifier;
    return `${local.slice(0, 2)}***@${domain}`;
  }

  if (identifier.length <= 4) {
    return identifier;
  }

  return `${identifier.slice(0, 3)}****${identifier.slice(-2)}`;
}

function isPhoneNumber(value: string) {
  return /^[0-9+\s-]{8,}$/.test(value);
}
