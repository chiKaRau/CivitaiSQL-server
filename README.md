
```
server
├─ .mvn
│  └─ wrapper
│     ├─ maven-wrapper.jar
│     └─ maven-wrapper.properties
├─ mvnw
├─ mvnw.cmd
├─ pom.xml
└─ src
   ├─ main
   │  ├─ java
   │  │  └─ com
   │  │     └─ civitai
   │  │        └─ server
   │  │           ├─ config
   │  │           ├─ controllers
   │  │           │  ├─ CivitaiSQL_Controller.java
   │  │           │  ├─ Civitai_Controller.java
   │  │           │  └─ File_Controller.java
   │  │           ├─ exception
   │  │           │  ├─ CustomDatabaseException.java
   │  │           │  ├─ CustomException.java
   │  │           │  ├─ ErrorResponse.java
   │  │           │  └─ GlobalExceptionHandler.java
   │  │           ├─ mappers
   │  │           ├─ models
   │  │           │  ├─ dto
   │  │           │  │  ├─ Models_DTO.java
   │  │           │  │  └─ Tables_DTO.java
   │  │           │  └─ entities
   │  │           │     └─ civitaiSQL
   │  │           │        ├─ Models_Descriptions_Table_Entity.java
   │  │           │        ├─ Models_Details_Table_Entity.java
   │  │           │        ├─ Models_Images_Table_Entity.java
   │  │           │        ├─ Models_Table_Entity.java
   │  │           │        └─ Models_Urls_Table_Entity.java
   │  │           ├─ repositories
   │  │           │  └─ civitaiSQL
   │  │           │     ├─ CustomModelsTableRepository.java
   │  │           │     ├─ impl
   │  │           │     │  └─ CustomModelsTableRepositoryImpl.java
   │  │           │     ├─ Models_Descriptions_Table_Repository.java
   │  │           │     ├─ Models_Details_Table_Repository.java
   │  │           │     ├─ Models_Images_Table_Repository.java
   │  │           │     ├─ Models_Table_Repository.java
   │  │           │     ├─ Models_Table_Repository_Specification.java
   │  │           │     └─ Models_Urls_Table_Repository.java
   │  │           ├─ ServerApplication.java
   │  │           ├─ services
   │  │           │  ├─ CivitaiSQL_Service.java
   │  │           │  ├─ Civitai_Service.java
   │  │           │  ├─ File_Service.java
   │  │           │  └─ impl
   │  │           │     ├─ CivitaiSQL_Service_Impl.java
   │  │           │     ├─ Civitai_Service_Impl.java
   │  │           │     └─ File_Service_Impl.java
   │  │           ├─ specification
   │  │           │  └─ civitaiSQL
   │  │           │     └─ Models_Table_Specification.java
   │  │           └─ utils
   │  │              ├─ ConfigUtils.java
   │  │              ├─ CustomResponse.java
   │  │              ├─ FileUtils.java
   │  │              ├─ JsonUtils.java
   │  │              └─ ProgressBarUtils.java
   │  └─ resources
   └─ test
      └─ java
         └─ com
            └─ civitai
               └─ server
                  ├─ CivitaiSQL_Controller_Test.java
                  ├─ CivitaiSQL_Service_Test.java
                  └─ ServerApplicationTests.java

```