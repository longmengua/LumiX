export const ADMIN_API_BASE_PATH = '/api/admin/v1';

export type AdminApiConfig = {
  basePath?: string;
};

export function createAdminApi(config: AdminApiConfig = {}) {
  const basePath = config.basePath ?? ADMIN_API_BASE_PATH;

  return {
    basePath,
  };
}
