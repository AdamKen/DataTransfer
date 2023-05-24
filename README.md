# DatabaseTransfer
一个简单的数据库同步程序。
- 支持通过配置的方式把A数据库的某个表的某些字段，同步到B数据库的某个表的某些字段上。  
- 允许配置多条规则。
- 允许定时执行。
## 使用

### 一、配置文件
src/main/resources/application.yml  
```yaml
#数据库配置，可配置多个
dataSources:
  your-db:
      type: mysql #mysql oracle oceanbase
      url: 127.0.0.1:5001
      scheme: your_database
      user: xxx
      password:  ENC(xxxxxxxxxx) #jasypt加密串 或 使用明文（不建议）
      config: 'charset=utf8mb4&parseTime=True&loc=Local'
  your-db-bak:
    type: oracle
    url: 127.0.0.1:5001
    scheme: your_database
    user: xxx
    password:  ENC(xxxxxxxxxx)
    config: 'charset=utf8mb4&parseTime=True&loc=Local'
      
#任务配置
tasks:
  - id: just-a-test-task-a
    timer: >- #corn表达式
      0/30 * * * * ? 
    db: your-db-bak
    proc: #任务子序列，循序执行，任何子任务异常将导致任务终止
      - db: your-db
        type: transfer #transfer 允许配置查询语句，使用自动转导到目标表
        sql: >- #在db执行查询语句， 并自动执行 INSERT INTO your-db-bak.user (pId, pName) VALUES (...)
          SELECT id pId, Name pName FROM user' 
        targetTable: user
      - db: your-db
        type: any #any 允许使用任何sql语句
        sql: >-
          select id tID, name tName from chain
  - id: just-a-test-task-b
    timer: >-
      0/30 * * * * ? 
    db: your-db-bak
    proc:
      - db: your-db
        type: any 
        sql: >-
          select id tID, name tName from chain
```