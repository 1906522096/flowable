package com.qgbest.flowable.config.enumeration;

import lombok.Getter;

/**配置枚举
 * @author hjt
 * @date 2019/11/13
 * @description
 */
@Getter
public enum ConfigEnum {
    /**
     * 流程key
     */
    PERMISSIONERR("permissionErr","抱歉，您没有操作权限"),
    DURATION("duration","耗时"),
    ENDTIME("endTime","结束时间"),
    OPNION("opnion","操作意见"),
    ACTNAME("actName","操作名称"),
    PROCESSKEY("processKey","流程的key"),
    TASKID("taskId","任务id"),
    USERID("userId","用户id"),
    ROLECODE("roleCode","角色编码"),
    PROCESSINSTANCEID("processInstanceId","流程实例Id"),
    USERIDS("userIds","用户ids"),
    ASSIGNEE("assignee","处理人"),
    ;

    /**
     * 编码
     */
    private String code;
    /**
     * 名称
     */
    private String name;

    ConfigEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
