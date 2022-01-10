FROM java:8-alpine
  
ADD test.txt commit.txt

ADD target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT [ "java","-Xms256m","-Xmx1024m","-jar","/app.jar","--spring.profiles.active=prod" ]

ENV TZ=Asia/Shanghai

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
