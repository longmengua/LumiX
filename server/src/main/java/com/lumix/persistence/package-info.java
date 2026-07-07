/**
 * persistence boundary package。
 *
 * 這一層只放 repository contract、persistence exception 與資料存取邊界的共用型別。
 * 不把 database client 直接暴露給 API 層，也不在這裡實作 runtime 資金流程。
 */
package com.lumix.persistence;
