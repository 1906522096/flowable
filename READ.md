# flowable使用文档

## 画流程图

1. 用户权限登录地址  http://192.168.117.83:8080/flowable-idm/#/login ，用户名 admin 密码 test（本地，服务器上待部署）

    1.1 在这里可以创建用户（基本上不用创建，除非流程中需要指定某个人来处理，方便画流程图的时候选择某个用户）

   1.2 可以创建用户组（用户组对应的是jsbi系统的角色，如流程中需要配置角色则需要自行创建用户组）

2. 画流程图的地址 http://192.168.117.83:8080/flowable-modeler/#/processes 

   （如果未登录权限，会先让你登录，用户名密码同上）

   2.1 新建流程图 (单线圆圈)

   （也可以点击一个进行修改，修改的时候点击可视化编辑进入编辑状态），进入页面会有一个默认的开始

   2.2 添加人工任务(黑色的人头)

   点击开始的时候会展示出可以选择的任务 ，以人工任务为例（下图），直接点击黑色的人头就可以

   ![1572573919792](C:\Users\cew\AppData\Roaming\Typora\typora-user-images\1572573919792.png)

   （也可以从左边的activitives选择User task，直接拖到开始的后面然后用线连接起来）

    2.3 画连接线 (带箭头的线)

   在下图中选中带箭头的实线，按住鼠标从要连接的开始（连接的开始会出现红色的框）到连接的结束（连接的结束会出现绿色的框），放开鼠标就可以了，如果需要拐弯的线，可以点击上面的 添加节点，也可以点击 删除节点，来调整线的样式（如下图）

   ![1572573896901](C:\Users\cew\AppData\Roaming\Typora\typora-user-images\1572573896901.png)

   tips: 1.  模型名称和模型的key尽量避免用中文，最好用业务的缩写，

   ​        2.  一个流程必须要有一个开始和至少一个结束

   2.4 设置任务

   设置任务名称  双击某个任务可以设置任务的名称

   设置任务处理人  单击任务，下面会显示任务的属性，点击选择分配人会弹出选择框，默认身份存储，

   ​		分配给某个人执行，选择分配给单个用户，在下面输入用户就可以选择（目前未用到）

   ​		分配给某些人执行 ，选择候选用户，在下面输入多个用户，点击选择（目前未用到）

   ​		分配给某个角色的用户，选择后选择，在下面选择某个角色

   ​	选择以后在页面是可以看到的

   2.5 排他网关(菱形中间带个"X")

   和人工任务一样可以单击上一个任务，直接选择排他网关，也有可以在左侧选择（不建议左侧选择这个）

   排他网关必须要有两个带有条件的输出，点击排他网关的某个出线，下面选择Flow condiction弹出连线条件输入匡，可以输入 ${flag==false}，格式${参数==参数值}

   2.6 结束任务（黑色粗线的原圆圈）

   每个流程图至少有一个结束任务

   2.7 流程检查 （最上面按钮栏的第二个 蓝色框里的对号）

   如果流程有问题会有红色提示,需要作出相应的修改，直到没有问题 

   如果没有问题会提示 “NO errors detected.”

   2.8 保存流程（最上面按钮栏的第一个 保存符号）

   点击保存，会弹出提示点击保存或者保存并关闭都可以

   2.9 流程文件下载

   从流程列表页面点击流程进入，点击下载按钮（下图）

   ![1572576238330](C:\Users\cew\AppData\Roaming\Typora\typora-user-images\1572576238330.png)

   

   

## 流程部署

在JSBI系统的业务系统管理的业务流程配置管理，点击部署，上传2.9下载的流程文件，会自动部署

（目前上传功能未做，可以在postman用本地服务先上传 地址：192.168.117.83:9998/deploy）

![1572576418760](C:\Users\cew\AppData\Roaming\Typora\typora-user-images\1572576418760.png)

## 接口和依赖的使用

1. 在pom里添加flowable服务的依赖

   <dependency>
   			<groupId>com.qgbest.flowable</groupId>
   			<artifactId>flowable-client</artifactId>
   			<version>1.0.0-SNAPSHOT</version>
   </dependency>

   如果用到common里的TaskDTO

   <dependency>
   			<groupId>com.qgbest.flowable</groupId>

   ​            <artifactId>flowable-common</artifactId>

   ​            <version>1.0.0-SNAPSHOT</version>
   </dependency>

2. 接口

        /**
        * 启动流程
        * @param map 参数 必须包含 processKey 流程定义的key，也就是流程图的业务key
        * @return  TaskDTO 包括 待处理任务id，流程实例id
        */
         @PostMapping("/start")
         ReturnInfo startProcessInstance(@RequestBody Map map);
    启动流程的时候调用，然后把流程id存到自己的业务表
    
    

        /** 
        *完成任务操作 
        * @param map 参数 ,必须包含 taskId 任务id,包括排他网关参数（如果该操作有排他网关） 
        * */
        @PostMapping("/complete")ReturnInfo complete(@RequestBody Map map);

​      提交，审核等任务操作调用

     /** 
     * 查询任务列表方法 
     * @param map 参数 , 必须包含 processKey 流程定义的key，如果有部门也需要 userIds ，部门下的某个角色的所有用户的id
     * @return List<TaskDTO>
     */
     @PostMapping(value = "/getTaskList")ReturnInfo getTaskList(@RequestBody Map map);

在查询列表里使用，包括操作人创建的数据，操作人处理过的数据，操作人待处理的数据，

其中操作人待处理的数据包含taskId(任务id),其他数据只有 processInstanceId （流程实例的id）

前端展示数据的时候需要根据流程实例id来关联并取得任务id,然后传到前端

## 数据结构

某个业务模块数据表里要存一个实例的id(processInstanceId),这个是用来关联流程实例和业务实体的字段（一个业务实体（例如一条合同数据）对应一个流程实例）。

列表查询的时候，待处理任务的实体需要把任务id带到列表去，每一个流程任务的执行都需要一个任务id(taskId)