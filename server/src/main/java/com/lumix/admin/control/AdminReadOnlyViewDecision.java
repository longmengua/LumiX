package com.lumix.admin.control;
/** UI/transport 前的 pure view decision；資料不健康時不允許冒充 operational view。 */
public enum AdminReadOnlyViewDecision { VIEW_AVAILABLE, SOURCE_UNHEALTHY_REJECTED }
