/**
 * outbox bounded context 的入口 package。
 *
 * 這裡用來放事件外送與重送所需的邊界型別，先建立 package marker，
 * 不在本 task 內加入實際 outbox dispatch。
 */
package com.lumix.outbox;
