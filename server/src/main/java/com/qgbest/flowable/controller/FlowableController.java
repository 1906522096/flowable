package com.qgbest.flowable.controller;

import cn.hutool.core.util.StrUtil;
import com.qgbest.flowable.common.TaskDTO;
import com.qgbest.flowable.config.enumeration.ConfigEnum;
import com.qgbest.flowable.config.exception.FlowableException;
import com.qgbest.flowable.service.FlowableService;
import com.qgbest.microservice.pojo.OperInfo;
import com.qgbest.tools.microservice.exception.SysRunException;
import com.qgbest.tools.microservice.pojo.ReturnInfo;
import com.qgbest.tools.microservice.util.ReturnUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


/**
 * flowable接口
 *
 * @author hjt
 * @date 2019/10/30
 * @description flowable接口
 */
@RestController
@Component
@RequestMapping
@Slf4j
public class FlowableController {

    @Autowired
    TaskService taskService;

    @Autowired
    FlowableService flowableService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    RuntimeService runtimeService;

    Logger logger = LoggerFactory.getLogger(FlowableController.class);

    /**
     * 资源部署
     *
     * @param modelName 模型名称
     * @param text      xml文本
     * @return 返回值
     * @throws IOException 异常
     */
    @PostMapping(value = "/deployMent")
    @ResponseBody
    public ReturnInfo deploy(@RequestParam("modelName") String modelName, @RequestParam("text") String text) {
        try {
            String processName = modelName + ".bpmn20.xml";

            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name(modelName);        //部署名称
            Deployment deployment = deploymentBuilder.addString(processName, text).deploy();    //完成部署
            return ReturnUtil.successWithData(deployment.getId());
        } catch (Exception e) {
            throw new SysRunException("流程文件部署失败");
        }
    }

    /**
     * 资源部署
     *
     * @param file 流程资源文件
     * @return 返回值
     * @throws IOException 异常
     */
    @PostMapping(value = "/deploy")
    @ResponseBody
    public String deploy(@RequestPart("file") MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            repositoryService.createDeployment().addInputStream(file.getOriginalFilename(), inputStream).deploy();

            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().processDefinitionResourceName(file.getOriginalFilename()).latestVersion().singleResult();

            return definition.getKey();
        } catch (Exception e) {
            throw new SysRunException("流程文件上传失败");
        }
    }

    /**
     * 启动流程
     *
     * @param processKey 流程定义的key,map类型为 Map<String, Object>
     * @return processInstanceId 流程实例id
     */
    @PostMapping(value = "/start")
    @ResponseBody
    public Object startProcessInstance(@RequestParam("processKey") String processKey, @RequestParam("userId") String userId) {
        if (StringUtils.isEmpty(processKey)) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }

        Authentication.setAuthenticatedUserId(userId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey);
        Authentication.setAuthenticatedUserId(null);
        List<Task> handingTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setProcessInstanceId(processInstance.getId());
        if (handingTasks != null && !handingTasks.isEmpty()) {
            taskDTO.setTaskId(handingTasks.get(0).getId());
        }
        return taskDTO;
    }

    /**
     * 停止流程接口
     *
     * @param instanceId 流程实例id
     * @return ReturnInfo
     */
    @PostMapping(value = "/stop")
    @ResponseBody
    public ReturnInfo stopProcessInstance(@RequestParam("processInstanceId") String instanceId) {
        if (StrUtil.isBlank(instanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        return stopProcessInstanceById(instanceId);
    }

    public ReturnInfo stopProcessInstanceById(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (null == processInstance) {
            log.info("不存在运行的流程实例processInstanceId:{},请确认!", processInstanceId);
            throw new FlowableException("不存在运行的流程实例processInstanceId:" + processInstanceId + ",请确认!");
        }
        //1、获取终止节点
        List<EndEvent> endNodes = findEndFlowElement(processInstance.getProcessDefinitionId());
        String endId = endNodes.get(0).getId();
        //2、执行终止
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstanceId).list();
        List<String> executionIds = new ArrayList<>();
        executions.forEach(execution -> executionIds.add(execution.getId()));
        runtimeService.createChangeActivityStateBuilder().moveExecutionsToSingleActivityId(executionIds, endId).changeState();
        log.info("终止processInstanceId:{}成功", processInstanceId);
        return ReturnUtil.successWithMessage("终止processInstanceId:" + processInstanceId + "成功");
    }

    public List findEndFlowElement(String processDefId) {
        Process mainProcess = repositoryService.getBpmnModel(processDefId).getMainProcess();
        Collection<FlowElement> list = mainProcess.getFlowElements();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.EMPTY_LIST;
        }
        return list.stream().filter(f -> f instanceof EndEvent).collect(Collectors.toList());
    }


    /**
     * 完成任务操作
     *
     * @param map 参数 ,必须包含 taskId 任务id，map类型为 Map<String, Object>,包括排他网关参数
     * @return
     */
    @PostMapping(value = "/complete")
    @ResponseBody
    @Transactional
    public void complete(@RequestBody Map map) {
        if (StringUtils.isEmpty(map.get(ConfigEnum.TASKID.getCode()))) {
            throw new FlowableException(ConfigEnum.TASKID.getName());
        }
        try {
            String taskId = (String) map.get(ConfigEnum.TASKID.getCode());
            String userId = (String) map.get(ConfigEnum.USERID.getCode());
            taskService.claim(taskId, userId);
            taskService.complete(taskId, map);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new SysRunException(e.getMessage());
        }
    }


    /**
     * 查询待处理任务列表方法
     *
     * @return 任务列表
     */
    @PostMapping(value = "/getPendingTaskList")
    @ResponseBody
    public Object getPendingTaskList(@RequestParam("processKey") String processKey, @RequestParam("userId") String userId, @RequestParam("roleCode") String roleCode, @RequestParam(value = "startDate", required = false) String startDate, @RequestParam(value = "endDate", required = false) String endDate, OperInfo operInfo

    ) {
        if (StringUtils.isEmpty(processKey)) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }
        List<String> keys = new ArrayList<>();
        keys.add(processKey);
        List list = flowableService.getPendingTaskList(keys, roleCode, userId, startDate, endDate, operInfo);
        return list;
    }

    /**
     * 查询待处理任务列表方法
     *
     * @param processKeys
     */
    @PostMapping(value = "/getPendingTaskListByKes")
    @ResponseBody
    public Object getPendingTaskListByKes(@RequestBody List<String> processKeys, @RequestParam("userId") String userId, @RequestParam("roleCode") String roleCode, @RequestParam(value = "startDate", required = false) String startDate, @RequestParam(value = "endDate", required = false) String endDate, OperInfo operInfo) {
        if (processKeys == null || processKeys.isEmpty()) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }
        return flowableService.getPendingTaskList(processKeys, userId, roleCode, startDate, endDate, operInfo);
    }


    /**
     * 查询已处理列表
     *
     * @param map 参数 必须包含 processKey 业务key，userIds 要查询的用户数据
     * @return
     */
    @PostMapping(value = "/getHisTaskList")
    @ResponseBody
    public Object getHisTaskList(@RequestBody Map map, OperInfo operInfo) {
        String userId = (String) map.get(ConfigEnum.USERID.getCode());
        String roleCode = (String) map.get(ConfigEnum.ROLECODE.getCode());
        return flowableService.getHisTaskListByKey(map, userId, roleCode, operInfo);
    }

    /**
     * 查询已处理任务列表方法
     *
     * @param map processKey 业务流程的keys, userIds 要查询的用户数据
     * @return 结果
     */
    @PostMapping(value = "/getHisTaskListByKeys")
    @ResponseBody
    public Object getHisTaskListByKeys(@RequestBody Map map, OperInfo operInfo) {
        String userId = (String) map.get(ConfigEnum.USERID.getCode());
        String roleCode = (String) map.get(ConfigEnum.ROLECODE.getCode());
        return flowableService.getHisTaskListByKeys(map, userId, roleCode, operInfo);
    }

    /**
     * 查询任务列表方法
     *
     * @param map processKey 业务流程的keys, userIds 要查询的用户数据
     * @return 结果
     */
    @PostMapping(value = "/getTaskListByKeys")
    @ResponseBody
    public Object getTaskListByKeys(@RequestBody Map map) {
        return flowableService.getTaskListByKeys(map);
    }

    /**
     * 撤回流程
     *
     * @param processInstanceId 流程实例id
     */
    @PostMapping(value = "/recall")
    public Object recall(@RequestParam("processInstanceId") String processInstanceId) {
        try {
            flowableService.recall(processInstanceId);
        } catch (Exception e) {
            return ReturnUtil.failedWithData(e.getMessage());
        }
        return ReturnUtil.success();
    }

    /**
     * 流程定义挂起
     *
     * @param processKey 流程定义的key
     */
    @PostMapping(value = "/suspendProcessDefinition")
    @ResponseBody
    public void suspendProcessDefinition(@RequestParam("processKey") String processKey) {
        if (StringUtils.isEmpty(processKey)) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }
        repositoryService.suspendProcessDefinitionByKey(processKey);
    }

    /**
     * 流程定义激活
     *
     * @param processKey 流程定义的key
     */
    @PostMapping(value = "/activateProcessDefinition")
    @ResponseBody
    public void activateProcessDefinition(@RequestParam("processKey") String processKey) {
        if (StringUtils.isEmpty(processKey)) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }
        repositoryService.activateProcessDefinitionByKey(processKey);
    }

    /**
     * 流程实例挂起
     *
     * @param processInstanceId 流程实例的id
     */
    @PostMapping(value = "/suspendProcessInstance")
    @ResponseBody
    public ReturnInfo suspendProcessInstance(@RequestParam("processInstanceId") String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        runtimeService.suspendProcessInstanceById(processInstanceId);
        return ReturnUtil.success();

    }


    /**
     * 删除流程实例
     *
     * @param processInstanceId 流程实例的id
     * @param deleteReason      删除原因
     */
    @DeleteMapping(value = "/deleteProcessInstance")
    @ResponseBody
    public Object deleteProcessInstance(@RequestParam("processInstanceId") String processInstanceId, @RequestParam("deleteReason") String deleteReason) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
        return ReturnUtil.success();
    }


    /**
     * 流程实例激活
     *
     * @param processInstanceId 流程实例的id
     */
    @PostMapping(value = "/activateProcessInstance")
    @ResponseBody
    public ReturnInfo activateProcessInstance(@RequestParam("processInstanceId") String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        runtimeService.activateProcessInstanceById(processInstanceId);
        return ReturnUtil.success();

    }

    /**
     * 获取实例参数(从历史表)
     *
     * @param processInstanceId 流程实例的id
     */
    @PostMapping(value = "/getVariables")
    @ResponseBody
    public Object getVariables(@RequestParam("processInstanceId") String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        return flowableService.getVariables(processInstanceId);
    }

    /**
     * 设置任务描述
     *
     * @param taskId       任务id
     * @param rejectOpnion 理由/建议
     */
    @PostMapping(value = "/setTaskDescription")
    @ResponseBody
    public void setTaskDescription(@RequestParam("taskId") String taskId, @RequestParam(value = "rejectOpnion", required = false) String rejectOpnion) {
        if (StringUtils.isEmpty(taskId)) {
            throw new FlowableException("任务id");
        }
        flowableService.setTaskDescription(taskId, rejectOpnion);
    }

    /**
     * 查询认定单流程信息
     *
     * @param processInstanceId 流程实例id
     * @return ReturnInfo
     */
    @PostMapping("/getBpList")
    public ReturnInfo getBpList(@RequestParam("processInstanceId") String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        return ReturnUtil.successWithData(flowableService.getBpList(processInstanceId));
    }


    /**
     * 查询任务id
     *
     * @param processInstanceId 流程实例id
     * @return String
     */
    @PostMapping("/getTaskIdByProInsId")
    public Object getTaskIdByProInsId(@RequestParam("processInstanceId") String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        return flowableService.getTaskIdByProInsId(processInstanceId);
    }

    /**
     * 根据当前人和processInstanceId查询待处理task
     *
     * @param processInstanceId 流程id
     * @return null /taskDTO
     */
    @PostMapping("/getTaskByProInsIdAndOperInfo")
    public Object getTaskByProInsIdAndOperInfo(@RequestParam("processInstanceId") String processInstanceId, @RequestParam("userId") String userId, @RequestParam("roleCode") String roleCode) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        return flowableService.getTaskByProInsIdAndOperInfo(processInstanceId, userId, roleCode);
    }

    @PostMapping("/setAssignee")
    public Object setAssignee(@RequestParam("taskId") String taskId, @RequestParam("userId") String userId) {
        try {
            flowableService.setAssignee(taskId, userId);
        } catch (Exception e) {
            ReturnUtil.failedWithData(e.getMessage());
        }

        return ReturnUtil.success();
    }


    /**
     * 获取待处理人/角色
     *
     * @param processInstanceId 流程实例id
     */
    @PostMapping("/getPendingUserOrRole")
    public Object getPendingUserOrRole(@RequestParam("processInstanceId") String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        return flowableService.getPendingUserByProInsId(processInstanceId);
    }

    @GetMapping("/getDealUsers")
    public Object getDealUsers(@RequestParam("processInstanceId") String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            throw new FlowableException(ConfigEnum.PROCESSINSTANCEID.getName());
        }
        return flowableService.getDealUsers(processInstanceId);
    }

    @GetMapping("/getFirstTaskName")
    public ReturnInfo getFirstTaskName(@RequestParam("processInstanceId") String processInstanceId) {
        return ReturnUtil.successWithData(flowableService.getFirstTaskName(processInstanceId));
    }

}


