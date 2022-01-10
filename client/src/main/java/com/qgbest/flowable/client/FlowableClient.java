package com.qgbest.flowable.client;

import com.qgbest.tools.microservice.pojo.ReturnInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * flowable对外接口
 *
 * @author hjt
 * @date 2019/10/30
 * @description flowable对外接口
 */
@Component
@FeignClient(name = "flowable")
public interface FlowableClient {

    /**
     * 设置任务描述
     *
     * @param modelName 模型名称
     * @param text      xml文本
     */
    @PostMapping(value = "/deployMent")
    ReturnInfo deploy(@RequestParam("modelName") String modelName, @RequestParam(value = "text") String text);


    /**
     * 启动流程
     *
     * @param processKey 流程定义的key
     */
    @PostMapping("/start")
    Object startProcessInstance(@RequestParam("processKey") String processKey, @RequestParam("userId") String userId);

    /**
     * 完成任务操作
     *
     * @param map 参数 ,必须包含 taskId 任务id，map类型为 Map<String, Object>,包括排他网关参数
     */
    @PostMapping("/complete")
    Object complete(@RequestBody Map map);


    /**
     * 撤回流程
     *
     * @param processInstanceId 流程实例id
     */
    @PostMapping(value = "/recall")
    Object recall(@RequestParam("processInstanceId") String processInstanceId);

    @PostMapping(value = "/setAssignee")
    Object setAssignee(@RequestParam("taskId") String taskId,
                       @RequestParam("userId") String userId);

    /**
     * 流程定义挂起
     *
     * @param processKey 流程定义的key
     */
    @PostMapping(value = "/suspendProcessDefinition")
    Object suspendProcessDefinition(@RequestParam("processKey") String processKey);

    /**
     * 流程定义激活
     *
     * @param processKey 流程定义的key
     */
    @PostMapping(value = "/activateProcessDefinition")
    Object activateProcessDefinition(@RequestParam("processKey") String processKey);

    /**
     * 流程实例挂起
     *
     * @param processInstanceId 流程实例的id
     */
    @PostMapping(value = "/suspendProcessInstance")
    Object suspendProcessInstance(@RequestParam("processInstanceId") String processInstanceId);

    /**
     * 删除流程实例
     *
     * @param processInstanceId 流程实例的id
     * @param deleteReason      删除原因
     */
    @DeleteMapping(value = "/deleteProcessInstance")
    Object deleteProcessInstance(@RequestParam("processInstanceId") String processInstanceId, @RequestParam("deleteReason") String deleteReason);

    /**
     * 流程实例激活
     *
     * @param processInstanceId 流程实例的id
     */
    @PostMapping(value = "/activateProcessInstance")
    Object activateProcessInstance(@RequestParam("processInstanceId") String processInstanceId);

    /**
     * 获取实例参数(从历史表)
     *
     * @param processInstanceId 流程实例的id
     */
    @PostMapping(value = "/getVariables")
    Object getVariables(@RequestParam("processInstanceId") String processInstanceId);

    /**
     * 设置任务描述
     *
     * @param taskId       任务id
     * @param rejectOpnion 理由/建议
     */
    @PostMapping(value = "/setTaskDescription")
    Object setTaskDescription(@RequestParam("taskId") String taskId, @RequestParam(value = "rejectOpnion", required = false) String rejectOpnion);

    /**
     * 查询待处理任务列表方法
     *
     * @return 任务列表
     */
    @PostMapping(value = "/getPendingTaskList")
    Object getPendingTaskList(@RequestParam("processKey") String processKey,
                              @RequestParam("userId") String userId,
                              @RequestParam("roleCode") String roleCode,
                              @RequestParam(value = "startDate", required = false) String startDate,
                              @RequestParam(value = "endDate", required = false) String endDate);


    /**
     * 查询待处理任务列表方法
     *
     * @return 任务列表
     */
    @PostMapping(value = "/getPendingTaskListByKes")
    Object getPendingTaskListByKes(@RequestBody List<String> processKeys,
                                   @RequestParam("userId") String userId,
                                   @RequestParam("roleCode") String roleCode,
                                   @RequestParam(value = "startDate", required = false) String startDate,
                                   @RequestParam(value = "endDate", required = false) String endDate);


    /**
     * 查询已处理任务列表方法
     *
     * @return 任务列表
     */
    @PostMapping(value = "/getHisTaskListByKeys")
    Object getHisTaskListByKeys(@RequestBody Map map);

    /**
     * 查询任务列表方法（已处理和待处理）
     *
     * @return 任务列表
     */
    @PostMapping(value = "/getTaskListByKeys")
    Object getTaskListByKeys(@RequestBody Map map);

    /**
     * 查询已处理列表
     *
     * @param map 参数 必须包含 processKey 业务key，userIds 要查询的用户数据
     * @return
     */
    @PostMapping(value = "/getHisTaskList")
    Object getHisTaskList(@RequestBody Map map);


    /**
     * 查询任务id
     *
     * @param processInstanceId 流程实例id
     * @return String
     */
    @PostMapping("/getTaskIdByProInsId")
    Object getTaskIdByProInsId(@RequestParam("processInstanceId") String processInstanceId);

    /**
     * 根据当前人和processInstanceId查询待处理task
     *
     * @param processInstanceId 流程id
     * @return null /taskDTO
     */
    @PostMapping("/getTaskByProInsIdAndOperInfo")
    Object getTaskByProInsIdAndOperInfo(@RequestParam("processInstanceId") String processInstanceId,
                                        @RequestParam("userId") String userId,
                                        @RequestParam("roleCode") String roleCode);

    /**
     * 查询认定单流程信息
     *
     * @param processInstanceId 流程实例id
     * @return Object
     */
    @PostMapping("/getBpList")
    Object getBpList(@RequestParam("processInstanceId") String processInstanceId,
                     @RequestParam("userId") String userId,
                     @RequestParam("roleCode") String roleCode);

    /**
     * 获取待处理人/角色
     *
     * @param processInstanceId 流程实例id
     */
    @PostMapping("/getPendingUserOrRole")
    Object getPendingUserOrRole(@RequestParam("processInstanceId") String processInstanceId);
}
