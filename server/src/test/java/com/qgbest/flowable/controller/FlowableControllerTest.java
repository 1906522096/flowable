//package com.qgbest.flowable.controller;
//
//import com.qgbest.microservice.util.JsonUtil;
//import org.junit.Before;
//import org.junit.FixMethodOrder;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.MethodSorters;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.RequestBuilder;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//
//import javax.persistence.EntityNotFoundException;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//
///**flowable测试用例
// * @author hjt
// * @date 2019/11/15
// * @description flowable测试用例
// */
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//public class FlowableControllerTest {
//
//    MockMvc mockMvc;
//
//    @Autowired
//    protected WebApplicationContext wac;
//
//
//    String token = "";
//
//    @Before
//    public void setupMockMvc() throws EntityNotFoundException {
//        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();  //初始化MockMvc对象
//    }
//
//
//    @Test
//    public void deploy() throws Exception {
//    }
//
//    @Test
//    public void startProcessInstance() throws Exception {
////        RequestBuilder request = post("/start")
////                .header("token",token)
////                .param("processKey", "leave");
////        mockMvc.perform(request)
////                .andExpect(status().isOk())
////                .andDo(print())
////                .andExpect(jsonPath("code").value("200"));
//    }
//
//    @Test
//    public void complete() throws Exception {
//    }
//
//
//    @Test
//    public void getPendingTaskList() throws Exception {
////        RequestBuilder request = post("/getPendingTaskList")
////                .contentType(MediaType.APPLICATION_JSON)
////                .param("processKey","leave");
////        mockMvc.perform(request)
////                .andExpect(status().isOk())
////                .andDo(print())
////                .andExpect(jsonPath("code").value("200"));
//    }
//
//    @Test
//    public void getHisTaskList() throws Exception {
////        RequestBuilder request = post("/getHisTaskList")
////                .contentType(MediaType.APPLICATION_JSON)
////                .param("processKey","leave");
////        mockMvc.perform(request)
////                .andExpect(status().isOk())
////                .andDo(print())
////                .andExpect(jsonPath("code").value("200"));
//    }
//
//    @Test
//    public void recall() throws Exception {
//    }
//
//    @Test
//    public void suspendProcessDefinition() throws Exception {
//    }
//
//    @Test
//    public void activateProcessDefinition() throws Exception {
//    }
//
//    @Test
//    public void suspendProcessInstance() throws Exception {
//    }
//
//    @Test
//    public void activateProcessInstance() throws Exception {
//    }
//
//    @Test
//    public void getVariables() throws Exception {
////        RequestBuilder request = post("/getVariables")
////                .param("processInstanceId", "c16a7c82-0779-11ea-8fe3-00ff33b785b1");
////        mockMvc.perform(request)
////                .andExpect(status().isOk())
////                .andDo(print())
////                .andExpect(jsonPath("code").value("200"));
//    }
//
//    @Test
//    public void setTaskDescription() throws Exception {
////        RequestBuilder request = post("/setTaskDescription")
////                .param("taskId", "28f4eaec-06b8-11ea-a609-123456789012");
////        mockMvc.perform(request)
////                .andExpect(status().isOk())
////                .andDo(print())
////                .andExpect(jsonPath("code").value("200"));
//    }
//
//    @Test
//    public void getBpList() throws Exception {
////        RequestBuilder request = post("/getBpList")
////                .param("processInstanceId", "c16a7c82-0779-11ea-8fe3-00ff33b785b1");
////        mockMvc.perform(request)
////                .andExpect(status().isOk())
////                .andDo(print())
////                .andExpect(jsonPath("code").value("200"));
//    }
//
//    public void getPendingUserOrRole() throws Exception {
////        RequestBuilder request = post("/getPendingUserOrRole")
////                .param("processInstanceId", "e46df3da-17d7-11ea-9af9-00ff33b785b1");
////        mockMvc.perform(request)
////                .andExpect(status().isOk())
////                .andDo(print())
////                .andExpect(jsonPath("code").value("200"));
//    }
//
//    @Test
//    public void getPendingTaskListByKesAndName() throws Exception {
//        List list = new ArrayList();
//        list.add("leaveKey");
//        RequestBuilder request = post("/getPendingTaskListByKesAndName")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(JsonUtil.toJson(list))
//                .param("userId","admin")
//                .param("taskName","提交")
//                .param("roleCode","admin");
//
//    }
//
//}