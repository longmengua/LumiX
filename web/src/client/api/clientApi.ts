export const CLIENT_API_BASE_PATH = '/api/v1';

export type ClientApiConfig = {
  basePath?: string;
};

export function createClientApi(config: ClientApiConfig = {}) {
  const basePath = config.basePath ?? CLIENT_API_BASE_PATH;

  // 目前只集中管理 basePath；等正式串接時，再在這裡補齊 request / auth / retry boundary。
  return {
    basePath,
  };
}
