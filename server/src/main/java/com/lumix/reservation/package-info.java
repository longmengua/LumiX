/**
 * reservation bounded context 的入口 package。
 *
 * reservation 的 hold / release 規則屬於高風險區域，本 task 只建立邊界，
 * 不放任何資產凍結或解凍流程。
 */
package com.lumix.reservation;
