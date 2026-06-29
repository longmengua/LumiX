type Translate = (key: string, fallback?: string, values?: Record<string, string | number>) => string;

const ERROR_MESSAGE_KEYS: Record<string, string> = {
  'Please enter an email or phone number.': 'auth.error.identifierRequired',
  'Please enter your password.': 'auth.error.passwordRequired',
  'Use a valid email address or phone number.': 'auth.register.errorInvalidIdentifier',
  'Verification code is required.': 'auth.register.errorVerificationCodeRequired',
  'Password must be at least 8 characters.': 'auth.register.errorPasswordTooShort',
  'Passwords do not match.': 'auth.register.errorPasswordsMismatch',
  'You must accept the terms.': 'auth.register.errorTermsRequired',
  'Verification failed. Try 123456 in the demo flow.': 'auth.twoFactor.errorInvalidCode',
  'Invalid verification code. Try 123456 for the demo flow.': 'auth.twoFactor.errorInvalidCode',
};

export function translateAuthError(error: unknown, t: Translate, fallbackKey: string) {
  if (!(error instanceof Error)) {
    return t(fallbackKey);
  }

  const key = ERROR_MESSAGE_KEYS[error.message];
  if (key) {
    return t(key);
  }

  return t(fallbackKey, error.message);
}

export function translateMaskedMessage(
  message: string,
  t: Translate,
  prefixKey: string,
  fallbackKey: string,
) {
  const maskMatch = message.match(/^(.*?)(?:\s+to\s+)(.+)\.$/i);
  if (maskMatch) {
    return t(prefixKey, undefined, { target: maskMatch[2] });
  }

  return t(fallbackKey, message);
}
