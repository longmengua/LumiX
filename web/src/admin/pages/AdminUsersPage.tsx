// 使用者檢視已有真實唯讀 API，不能再把頁面別名指向包含 mock 資料的整個 AdminConsole。
// 保留既有 page entry，讓舊 import 也會導向已拆分的使用者管理功能，而不會回到舊版 console 內嵌頁面。
export { AdminUsersPage } from '../features/users/AdminUsersPage';
