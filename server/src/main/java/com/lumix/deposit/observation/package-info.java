/**
 * P22-T02 的 provider-neutral 鏈上觀測契約。
 *
 * <p>此套件只將外部鏈上事實轉成可重放的 immutable 值物件，絕不連線 RPC、保存 provider secret、寫入資料庫，
 * 亦不會把觀測轉為資產 credit。</p>
 */
package com.lumix.deposit.observation;
