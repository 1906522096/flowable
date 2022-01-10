package com.qgbest.flowable.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.reflect.TypeToken;
import com.qgbest.client.UserClient;
import com.qgbest.flowable.common.TaskDTO;
import com.qgbest.flowable.config.enumeration.ConfigEnum;
import com.qgbest.flowable.config.exception.FlowableException;
import com.qgbest.microservice.pojo.OperInfo;
import com.qgbest.tools.microservice.dao.CommonDao;
import com.qgbest.tools.microservice.exception.SysRunException;
import com.qgbest.tools.microservice.pojo.ReturnInfo;
import com.qgbest.tools.microservice.util.ReturnUtil;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.*;


/**
 * @author hjt
 * @date 2019/10/30
 * @description
 */
@Service
public class FlowableService {

    @Autowired
    TaskService taskService;

    @Autowired
    HistoryService historyService;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    CommonDao commonDao;

    @Autowired
    UserClient userClient;

    /**
     * 待处理任务列表查询
     *
     * @return 任务列表
     */
    public List getPendingTaskList(List<String> processKeys, String userId, String roleCode, String startDate, String endDate, OperInfo operInfo) {
        List<Task> handingGroupTasks = getPendingTaskListByKes(processKeys, userId, roleCode, startDate, endDate, operInfo);
        List list = new ArrayList();
        for (Task task : handingGroupTasks) {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setProcessInstanceId(task.getProcessInstanceId());
            taskDTO.setTaskId(task.getId());
            taskDTO.setTaskName(task.getName());
            taskDTO.setDocRule(task.getDescription());
            list.add(taskDTO);
        }
        return list;
    }

    public void setAssignee(String taskId, String userId) {
        taskService.setAssignee(taskId, userId);
    }

    /**
     * 任务列表查询
     *
     * @return 任务列表
     */
    public List getHisTaskListByKey(Map map, String userId, String roleCode, OperInfo operInfo) {
        String processKey = (String) map.get(ConfigEnum.PROCESSKEY.getCode());
        if (StringUtils.isEmpty(processKey)) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }
        List<String> processKeys = new ArrayList<>();
        processKeys.add(processKey);
        List<String> userIds = (List<String>) map.get(ConfigEnum.USERIDS.getCode());
        if (userIds == null || userIds.isEmpty()) {
            throw new FlowableException(ConfigEnum.USERIDS.getName());
        }
        return checkTasks(userIds, processKeys, userId, roleCode, operInfo);
    }

    /**
     * 过滤掉待处理数据
     */
    private List<TaskDTO> checkTasks(List<String> userIds, List<String> processKeys, String userId, String roleCode, OperInfo operInfo) {
        List<TaskDTO> hisList = getHisTaskListByKes(userIds, processKeys);
        List<TaskDTO> handingTasks = getPendingTaskList(processKeys, userId, roleCode, "", "", operInfo);
        List list = new ArrayList();
        for (TaskDTO taskDTO : hisList) {
            for (TaskDTO taskDTO1 : handingTasks) {
                if (taskDTO.getProcessInstanceId().equals(taskDTO1.getProcessInstanceId())) {
                    list.add(taskDTO);
                }
            }
        }
        hisList.removeAll(list);
        return hisList;
    }


    /**
     * 任务列表查询
     *
     * @return 任务列表
     */
    public List getHisTaskListByKeys(Map map, String userId, String roleCode, OperInfo operInfo) {
        List<String> processKeys = (List<String>) map.get(ConfigEnum.PROCESSKEY.getCode());
        if (processKeys == null || processKeys.isEmpty()) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }
        List<String> userIds = (List<String>) map.get(ConfigEnum.USERIDS.getCode());
        if (userIds == null || userIds.isEmpty()) {
            throw new FlowableException(ConfigEnum.USERIDS.getName());
        }
        return checkTasks(userIds, processKeys, userId, roleCode, operInfo);
    }


    /**
     * 任务列表查询
     *
     * @return 任务列表
     */
    public List getTaskListByKeys(Map map) {
        List<String> processKeys = (List<String>) map.get(ConfigEnum.PROCESSKEY.getCode());
        if (processKeys == null || processKeys.isEmpty()) {
            throw new FlowableException(ConfigEnum.PROCESSKEY.getName());
        }
        List<String> userIds = (List<String>) map.get(ConfigEnum.USERIDS.getCode());
        if (userIds == null || userIds.isEmpty()) {
            throw new FlowableException(ConfigEnum.USERIDS.getName());
        }
        return getTaskListByKes(userIds, processKeys);
    }


    /**
     * 撤回流程
     *
     * @param processInstanceId 业务流程key
     */
    public void recall(String processInstanceId) {
        //历史节点
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).finished().orderByHistoricActivityInstanceEndTime().desc().list();

        List<HistoricActivityInstance> activities1 = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().desc().list();
        List<HistoricActivityInstance> activitiesC = new ArrayList<>();
        activitiesC.addAll(activities);
        for (HistoricActivityInstance activitie : activitiesC) {
            if (activitie.getTaskId() == null) {
                activities.remove(activitie);
            }
        }
        List<HistoricActivityInstance> activities1C = new ArrayList<>();
        activities1C.addAll(activities1);
        for (HistoricActivityInstance activitie1 : activities1C) {
            if (activitie1.getTaskId() == null) {
                activities1.remove(activitie1);
            }
        }
        if (activities.size() == 1) {
            activities.add(activities.get(0));
        }

        runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId).moveActivityIdTo(activities1.get(0).getActivityId(), activities.get(0).getActivityId()).changeState();

        //删除历史流程走向记录
        historyService.deleteHistoricTaskInstance(activities1.get(0).getTaskId());
        historyService.deleteHistoricTaskInstance(activities.get(0).getTaskId());
    }

    /**
     * 设置任务描述
     *
     * @param taskId       任务id
     * @param rejectOpnion 理由/建议
     */
    public void setTaskDescription(String taskId, String rejectOpnion) {

        String sql = "";

        if (!StringUtils.isEmpty(rejectOpnion)) {
            sql = "update ACT_HI_TASKINST set DESCRIPTION_ = '" + rejectOpnion + "' where ID_ ='" + taskId + "'";
            commonDao.updateBySql(sql);
        }
    }

    /**
     * 根据实例id获取实例参数
     *
     * @param processInstanceId
     * @return
     */
    public Map getVariables(String processInstanceId) {
        List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery()//创建一个历史的流程变量查询
                .processInstanceId(processInstanceId).list();
        Map map = new HashMap();
        if (list != null && !list.isEmpty()) {
            for (HistoricVariableInstance variableInstance : list) {
                map.put(variableInstance.getVariableName(), variableInstance.getValue());
            }
        }
        return map;
    }

    /**
     * 获取流程信息
     *
     * @param processInstanceId 流程实例id
     * @return List
     */
    public List getBpList(String processInstanceId) {
        List newList = new ArrayList();
        String sql = "select hc.ID_,hc.ACT_TYPE_ \"actType\",hc.ACT_NAME_ \"actName\",hc.ASSIGNEE_ \"assignee\",hc.TASK_ID_,hc.START_TIME_ \"startTime\",hc.END_TIME_ \"endTime\"," + "ht.DESCRIPTION_ \"opnion\",hc.DURATION_ \"duration\" from ACT_HI_ACTINST hc\n" + "left join ACT_HI_TASKINST ht on hc.TASK_ID_ = ht.ID_\n" + "where (hc.ACT_NAME_ <>'' and hc.ACT_NAME_ is not NULL ) and hc.ACT_TYPE_<>'startEvent' and hc.PROC_INST_ID_ ='" + processInstanceId + "' " + "order by hc.START_TIME_ asc,hc.END_TIME_ is null,hc.END_TIME_ asc";
        List list;
        try {
            list = commonDao.getDataBySql(sql, new HashMap());
        } catch (Exception e) {
            throw new SysRunException("查询失败");
        }
        if (list == null || list.isEmpty()) {
            return newList;
        }

        String userIds = "";

        for (int i = 0; i < list.size(); i++) {
            Map map = (Map) list.get(i);
            setTimeInfo(map);
            Object endTimeV = map.get(ConfigEnum.ENDTIME.getCode());
            if (StringUtils.isEmpty(endTimeV)) {
                map.put(ConfigEnum.ENDTIME.getCode(), "等待" + map.get(ConfigEnum.ACTNAME.getCode()));
            }
            if ("sequenceFlow".equals(map.get("actType").toString())) {
                Map newMap = (Map) newList.get(newList.size() - 1);
                newMap.put(ConfigEnum.ACTNAME.getCode(), newMap.get(ConfigEnum.ACTNAME.getCode()).toString() + map.get(ConfigEnum.ACTNAME.getCode()).toString());
                newList.remove(newList.size() - 1);
                newList.add(newMap);
                continue;
            }
            newList.add(map);
            userIds += map.get("assignee") + ",";
        }
        return translate(newList, userIds);
    }


    /**
     * 翻译处理人
     *
     * @param list
     * @param userIds
     * @return
     */
    private List translate(List list, String userIds) {
        List newList = new ArrayList();
        //用户调用失败,清空处理人，不能直接显示id
        ReturnInfo returnInfo = userClient.getUsersbyIds(userIds);
        if (!ReturnUtil.isSuccess(returnInfo)) {
            for (int i = 0; i < list.size(); i++) {
                Map map = (Map) list.get(i);
                map.put(ConfigEnum.ASSIGNEE.getCode(), "");
                newList.add(map);
            }
            return newList;
        }

        //用户调用成功
        Type typeToken = (new TypeToken<List<Map>>() {
        }).getType();
        List users = ReturnUtil.deal(returnInfo, typeToken);
        for (int i = 0; i < list.size(); i++) {
            Map map = (Map) list.get(i);
            boolean getNameFlag = false;
            String assignee = (String) map.get(ConfigEnum.ASSIGNEE.getCode());
            if (!StringUtils.isEmpty(assignee)) {
                for (int j = 0; j < users.size(); j++) {
                    Map userMap = (Map) users.get(j);
                    if (assignee.equals(userMap.get("USER_ID"))) {
                        getNameFlag = true;
                        map.put(ConfigEnum.ASSIGNEE.getCode(), userMap.get("USERNME"));
                    }
                }
            }

            //没找到用户
            if (!getNameFlag) {
                map.put(ConfigEnum.ASSIGNEE.getCode(), "");
            }
            newList.add(map);
        }
        return newList;
    }

    /**
     * 设置耗时格式
     *
     * @param map 参数
     */
    private void setTimeInfo(Map map) {
        Object duration = map.get(ConfigEnum.DURATION.getCode());
        if (StringUtils.isEmpty(duration)) {
            return;
        }
        Long ztime = Long.parseLong(duration.toString());
        Long tian = ztime / (1000 * 60 * 60 * 24);
        Long shi = (ztime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        Long fen = (ztime % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60) / (1000 * 60);
        Long miao = (ztime % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60) % (1000 * 60) / 1000;
        map.put(ConfigEnum.DURATION.getCode(), tian + "天" + shi + "时" + fen + "分" + miao + "秒");
    }


    /**
     * 查询任务id
     *
     * @param processInstanceId 流程实例id
     * @return String
     */
    public TaskDTO getTaskIdByProInsId(String processInstanceId) {
        //一个流程下可能有多个待处理的任务（并行网关） todo 临时处理报错问题，后面解决并行网关问题
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        Task task = tasks.get(0);
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTaskId(task.getId());
        taskDTO.setTaskName(task.getName());
        taskDTO.setProcessInstanceId(task.getProcessInstanceId());
        return taskDTO;
    }

    /**
     * 查询任务
     *
     * @param processInstanceId 流程实例id
     * @return String
     */
    public TaskDTO getTaskByProInsIdAndOperInfo(String processInstanceId, String userId, String roleCode) {
        Task task = null;
        TaskDTO taskDTO = new TaskDTO();
        task = taskService.createTaskQuery().processInstanceId(processInstanceId).taskAssignee(userId).singleResult();

        if (task == null) {
            task = taskService.createTaskQuery().processInstanceId(processInstanceId).taskCandidateGroup(roleCode).singleResult();
        }

        if (task == null) {
            return null;
        }

        taskDTO.setTaskId(task.getId());
        taskDTO.setTaskName(task.getName());
        taskDTO.setProcessInstanceId(task.getProcessInstanceId());

        return taskDTO;
    }


    /**
     * 获取待处理人/角色
     *
     * @param processInstanceId 流程实例id
     */
    public Map getPendingUserByProInsId(String processInstanceId) {
        Map map = new HashMap();
        String returnVal = "";
        //一个流程下可能有多个待处理的任务（并行网关） todo 临时处理报错问题，后面解决并行网关问题
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }


        Task task = tasks.get(0);
        if (task == null) {
            return map;
        }

        List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(task.getId());
        HistoricIdentityLink link = null;
        if (links != null && !links.isEmpty()) {
            link = links.get(0);
        }

        if (link == null) {
            return map;
        }

        returnVal = link.getUserId();
        map.put("userId", returnVal);
        if (StringUtils.isEmpty(returnVal)) {
            returnVal = link.getGroupId();
            map.put("roleCode", returnVal);
        }
        return map;
    }

    /**
     * 获取待处理人/角色
     *
     * @param processInstanceId 流程实例id
     */
    public Map getDealUsers(String processInstanceId) {
        Set users = new HashSet();
        Set groups = new HashSet();
        Map res = new HashMap();
        //一个流程下可能有多个待处理的任务（并行网关） todo 临时处理报错问题，后面解决并行网关问题
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
        if (tasks == null || tasks.isEmpty()) {
            return res;
        }
        Task task = tasks.get(0);
        if (task == null) {
            return res;
        }
        List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(task.getId());
        if (links != null && !links.isEmpty()) {
            for (HistoricIdentityLink link : links) {
                String userId = link.getUserId();
                if (StrUtil.isNotBlank(userId)) {
                    users.add(userId);
                }
                String groupId = link.getGroupId();
                if (StrUtil.isNotBlank(groupId)) {
                    groups.add(groupId);
                }
            }
        }
        res.put("roleCodes", Arrays.asList(groups.toArray()));
        res.put("userIds", Arrays.asList(users.toArray()));

        return res;
    }


    /**
     * 查询待处理列表
     *
     * @param processKeys 业务key
     * @return List
     */
    private List getPendingTaskListByKes(List<String> processKeys, String userId, String roleCode, String startDate, String endDate, OperInfo operInfo) {
        //查询业务人员待处理任务
        TaskQuery taskQuery = taskService.createTaskQuery();
        taskQuery.taskCandidateGroup(roleCode).processDefinitionKeyIn(processKeys);
        if (StrUtil.isNotBlank(startDate)) {
            taskQuery.taskCreatedAfter(DateUtil.parseDate(startDate));
        }
        if (StrUtil.isNotBlank(endDate)) {
            taskQuery.taskCreatedBefore(DateUtil.parseDate(endDate));
        }
        List<Task> handingGroupTasks = taskQuery.list();
        if (processKeys.contains("mentorApprProcess")) {
            System.out.println(processKeys);
        }
        TaskQuery userTaskQuery = taskService.createTaskQuery();
        userTaskQuery.taskAssignee(userId).processDefinitionKeyIn(processKeys);
        if (StrUtil.isNotBlank(startDate)) {
            userTaskQuery.taskCreatedAfter(DateUtil.parseDate(startDate));
        }
        if (StrUtil.isNotBlank(endDate)) {
            userTaskQuery.taskCreatedBefore(DateUtil.parseDate(endDate));
        }
        List<Task> handingUserTasks = userTaskQuery.list();
        handingGroupTasks.removeAll(handingUserTasks);
        handingGroupTasks.addAll(handingUserTasks);
        return handingGroupTasks;
    }

    /**
     * 获取已处理列表
     *
     * @param userIds     用户ids
     * @param processKeys 流程keys
     * @return
     */
    private List getHisTaskListByKes(List<String> userIds, List<String> processKeys) {
        List<HistoricProcessInstance> allHisStarts = new ArrayList<>();
        //查询所有发起事件
        for (String userId : userIds) {
            List<HistoricProcessInstance> hisStarts = historyService.createHistoricProcessInstanceQuery().processDefinitionKeyIn(processKeys).startedBy(userId).finished().list();
            allHisStarts.addAll(hisStarts);
        }


        //查询业务人员已经处理过的任务
        List<HistoricTaskInstance> hisTasks = historyService.createHistoricTaskInstanceQuery().processDefinitionKeyIn(processKeys).taskAssigneeIds(userIds).finished().list();

        List<TaskDTO> list = new ArrayList();
        for (HistoricProcessInstance hisProIns : allHisStarts) {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setProcessInstanceId(hisProIns.getId());
            list.add(taskDTO);
        }

        for (HistoricTaskInstance hisTask : hisTasks) {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setProcessInstanceId(hisTask.getProcessInstanceId());
            list.add(taskDTO);
        }
        HashSet h = new HashSet(list);
        list.clear();
        list.addAll(h);
        return list;
    }


    /**
     * 获取已处理列表
     *
     * @param userIds     用户ids
     * @param processKeys 流程keys
     * @return
     */
    private List getTaskListByKes(List<String> userIds, List<String> processKeys) {
        List<HistoricProcessInstance> allHisStarts = new ArrayList<>();
        //查询所有发起事件
        for (String userId : userIds) {
            List<HistoricProcessInstance> hisStarts = historyService.createHistoricProcessInstanceQuery().processDefinitionKeyIn(processKeys).startedBy(userId).list();
            allHisStarts.addAll(hisStarts);
        }

        //查询业务人员已经处理过的任务
        List<HistoricTaskInstance> hisTasks = historyService.createHistoricTaskInstanceQuery().processDefinitionKeyIn(processKeys).taskAssigneeIds(userIds).list();

        List<TaskDTO> list = new ArrayList();
        List<TaskDTO> returnList = new ArrayList<>();
        for (HistoricProcessInstance hisProIns : allHisStarts) {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setProcessInstanceId(hisProIns.getId());
            list.add(taskDTO);
        }

        for (HistoricTaskInstance hisTask : hisTasks) {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setProcessInstanceId(hisTask.getProcessInstanceId());
            list.add(taskDTO);
        }
        HashSet h = new HashSet(list);
        list.clear();
        list.addAll(h);
        for (TaskDTO taskDTO : list) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(taskDTO.getProcessInstanceId()).orderByHistoricTaskInstanceStartTime().desc().list();

            if (historicTasks.size() > 0) {
                HistoricTaskInstance historicTaskInstance = historicTasks.get(historicTasks.size() - 1);
                if (StringUtils.isEmpty(historicTaskInstance.getEndTime())) {
                    taskDTO.setTaskId(historicTaskInstance.getId());
                    taskDTO.setTaskName(historicTaskInstance.getName());
                }
            }
            returnList.add(taskDTO);
        }
        return returnList;
    }

    /**
     * 查询待处理列表
     *
     * @param processKeys 业务key
     * @return List
     */
    public List getPendingTaskListByKesAndName(List<String> processKeys, String userId, String roleCode, String taskName) {
        //查询业务人员待处理任务
        List<Task> handingGroupTasks = taskService.createTaskQuery().taskCandidateGroup(roleCode).processDefinitionKeyIn(processKeys).taskName(taskName).list();

        List<Task> handingUserTasks = taskService.createTaskQuery().taskAssignee(userId).processDefinitionKeyIn(processKeys).taskName(taskName).list();

        handingGroupTasks.removeAll(handingUserTasks);
        handingGroupTasks.addAll(handingUserTasks);
        return handingGroupTasks;
    }


    @Autowired
    RepositoryService repositoryService;

    /**
     * 1. 首先拿到BpmnModel，所有流程定义信息都可以通过BpmnModel获取；若流程尚未发起，则用modelId查询最新部署的流程定义数据；
     * 若流程已经发起，可以通过流程实例的processDefinitionId查询流程定义的历史数据。
     */
    public String getFirstTaskName(String processInstanceId) {
        BpmnModel bpmnModel;
        FlowElement targetElementOfStartElement = null;
        if (StrUtil.isNotBlank(processInstanceId)) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            if (null == processInstance) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).orderByHistoricTaskInstanceStartTime().asc().list();
                if (historicTasks.isEmpty()) {
                    return null;
                }
                return historicTasks.get(0).getName();
            }
            bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
            Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
            Optional<FlowElement> startElementOpt = flowElements.stream().filter(flowElement -> flowElement instanceof StartEvent).findFirst();
            if (startElementOpt.isPresent()) {
                FlowElement startElement = startElementOpt.get();
                List<SequenceFlow> outgoingFlows = ((StartEvent) startElement).getOutgoingFlows();
                String targetRef = outgoingFlows.get(0).getTargetRef();
                // 根据ID找到FlowElement
                targetElementOfStartElement = getFlowElement(flowElements, targetRef);
            }
        }
        if (null != targetElementOfStartElement) {
            return targetElementOfStartElement.getName();
        } else {
            return null;
        }
    }


    private FlowElement getFlowElement(Collection<FlowElement> flowElements, String targetRef) {
        return flowElements.stream().filter(flowElement -> targetRef.equals(flowElement.getId())).findFirst().orElse(null);
    }


}
