type Translate = (key: string, fallback?: string, values?: Record<string, string | number>) => string;

const ERROR_MESSAGE_KEYS: Record<string, string> = {
  'Please enter an email or phone number.': 'auth.error.identifierRequired',
  'Please enter your password.': 'auth.error.passwordRequired',
  'Use a valid email address or phone number.': 'auth.register.errorInvalidIdentifier',
  'Verification code is required.': 'auth.register.errorVerificationCodeRequired',
  'Password must be between 8 and 32 characters.': 'auth.register.errorPasswordLength',
  'Display name must be at least 2 characters.': 'auth.register.errorDisplayNameTooShort',
  'Passwords do not match.': 'auth.register.errorPasswordsMismatch',
  'You must accept the terms.': 'auth.register.errorTermsRequired',
  EMAIL_ALREADY_REGISTERED: 'auth.register.errorEmailAlreadyRegistered',
  CURRENT_PASSWORD_INCORRECT: 'auth.changePassword.errorCurrentPasswordIncorrect',
  NEW_PASSWORD_SAME_AS_CURRENT: 'auth.changePassword.errorNewPasswordSameAsCurrent',
  CAPTCHA_REQUIRED: 'auth.captcha.required',
  CAPTCHA_INVALID: 'auth.captcha.invalid',
  'Please complete the slider verification.': 'auth.captcha.required',
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
