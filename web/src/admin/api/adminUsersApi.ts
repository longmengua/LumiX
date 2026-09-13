import { ADMIN_API_BASE_PATH } from './adminApi';
export type AdminUser={userId:string;email:string;displayName:string;status:string;createdAt:string;lastLoginAt:string|null;fundTransferRestrictedUntil:string|null}; export type AdminUserDetail={user:AdminUser;devices:Array<{platform:string;label:string;lastSeenAt:string}>};
async function read<T>(path:string):Promise<T>{const r=await fetch(`${ADMIN_API_BASE_PATH}${path}`,{credentials:'same-origin'});if(!r.ok)throw new Error((await r.json().catch(()=>null) as {code?:string}|null)?.code??'ADMIN_USER_QUERY_FAILED');return r.json() as Promise<T>;}
export const findAdminUsers=(q:string)=>read<AdminUser[]>(`/users?q=${encodeURIComponent(q)}`); export const getAdminUser=(id:string)=>read<AdminUserDetail>(`/users/${encodeURIComponent(id)}`);
