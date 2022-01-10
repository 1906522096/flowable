package com.qgbest.flowable.common;

import lombok.Data;

/** 任务DTO
 * @author hjt
 * @date 2019/10/30
 * @description 任务DTO
 */
@Data
public class TaskDTO {
    /**
     * 任务id
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 流程图中定义的规则 document
     */
    private String docRule;

    /**
     *流程实例id
     */
    private String processInstanceId;

}
