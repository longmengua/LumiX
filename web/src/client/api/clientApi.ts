export const CLIENT_API_BASE_PATH = '/api/v1';

export type ClientApiConfig = {
  basePath?: string;
};

export function createClientApi(config: ClientApiConfig = {}) {
  const basePath = config.basePath ?? CLIENT_API_BASE_PATH;

  return {
    basePath,
  };
}
