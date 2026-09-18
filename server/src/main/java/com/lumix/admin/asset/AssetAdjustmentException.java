package com.lumix.admin.asset;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;

/** 將受控 accounting rejection 映射為不洩漏內部實作的穩定 API code。 */
final class AssetAdjustmentException extends ApiException {
    AssetAdjustmentException(ApiErrorCode code) { super(code); }
}
