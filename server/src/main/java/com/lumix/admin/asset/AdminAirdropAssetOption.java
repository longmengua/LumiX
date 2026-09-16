package com.lumix.admin.asset;

/** 後端交易所內部資產設定投影；鏈上 token／網路名稱不得透過此契約傳遞。 */
public record AdminAirdropAssetOption(String assetSymbol, String internalName, int precisionScale) { }
