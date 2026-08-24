package com.lumix.admin.control;
/** 管理端操作意圖；本 phase 僅允許 read 與受控 action 的資料邊界。 */
public enum AdminOperation { READ_OPERATIONAL_VIEW, REQUEST_CONTROLLED_ACTION, REVIEW_CONTROLLED_ACTION }
