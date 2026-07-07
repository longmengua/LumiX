/**
 * LumiX backend 的最上層 package。
 *
 * 這層只保留應用程式入口與最小共用設定，不承載交易、帳本或錢包邏輯。
 * 後續 bounded context 應往各自的子 package 演進，避免把邊界全部塞進 root package。
 */
package com.lumix;
