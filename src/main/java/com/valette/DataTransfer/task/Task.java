package com.valette.DataTransfer.task;

import com.valette.DataTransfer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


@Configuration
@EnableScheduling
public class Task implements SchedulingConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(Task.class);
    @Autowired
    Tasks tasks;

    @Autowired
    DataSources dataSources;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        for (int i = 0; i < tasks.getTasks().size(); i++) {
            TaskModel task = tasks.getTasks().get(i);
            taskRegistrar.addCronTask(
                    ()->execTask(task),
                    task.getTimer());
        }
    }


    void execTask(TaskModel t) {
       logger.info("开始执行任务 "+ t.getId());
        try {
            for (int i =0; i < t.getProc().size(); i++) {
                Proc p = t.getProc().get(i);
                logger.info(String.format("正在处理序列 %d/%d", i+1, t.getProc().size()) );
                switch (p.getType()){
                    case "transfer":
                        execTransfer(t.getDb(),p);
                        break;
                    case "any":
                        execAny(t.getDb(),p);
                        break;
                    default:
                        throw new SQLException("不支持的序列任务类型: "+p.getType());
                }
            }
        } catch (Exception e){
            logger.error(String.format("任务执行失败 %s\n\n", t.getId()) );
            return;
        }

        logger.info(String.format("完成任务 %s\n\n", t.getId()) );
    }

    private void execTransfer(String targetDB,Proc p) throws SQLException {
        ArrayList<List> dataList  =new ArrayList<>();
        ArrayList<String> colList = new ArrayList<>();

        //1. 从源数据库中查询数据并保存
        Statement state = null;
        Connection srcConn = null;
        try {


            //1.获取数据库连接对象
            srcConn = getDBCByName(p.getDb());

            //2.定义sql语句
            String sql = p.getSql();

            //3.获取执行sql的对象 Statement
            state = srcConn.createStatement();

            //4.查询数据
            ResultSet rs =  state.executeQuery(sql);
            //5.保存查到的数据
            ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();
            while(rs.next()) {
                ArrayList<Object> rowData = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    if(colList.size() < colCount){
                       colList.add(md.getColumnLabel(i));
                    }
                    rowData.add(rs.getObject(i));
                }
                dataList.add(rowData);
            }


        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("查询数据失败");
            throw e;
        } finally {

            try {
                if (state != null) {
                    //8.释放资源
                    state.close();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            try {
                if (srcConn != null) {
                    srcConn.close();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        logger.info(String.format("查询到 %d条数据", dataList.size()));

        if(dataList.size()==0){
            return;
        }

        Connection targetConn = null;
        PreparedStatement ps = null;
        //2. 将数据插入到目标数据库中
        try {
            //1.获取数据库连接对象
            targetConn = getDBCByName(targetDB);

            //2.定义sql语句
            StringBuilder sqlBuffer = new StringBuilder("insert into ");
            sqlBuffer.append(p.getTargetTable());
            sqlBuffer.append("(");
            for (int i=0; i < colList.size(); i++) {
                if(i>0){
                    sqlBuffer.append(",");
                }
                sqlBuffer.append(colList.get(i));
            }
            sqlBuffer.append(") values (");
            for (int i=0; i < colList.size(); i++) {
                if(i>0){
                    sqlBuffer.append(",");
                }
                sqlBuffer.append("?");
            }
            sqlBuffer.append(")");

            String psSQL = sqlBuffer.toString();
            logger.info(String.format("使用SQL: %s", psSQL));
            ps = targetConn.prepareStatement(psSQL);

            for (int i = 0; i < dataList.size(); i++) {
                List data = dataList.get(i);
                for (int j=1; j <= data.size(); j++) {
                    ps.setObject(j, data.get(j-1));
                }
                ps.addBatch();
                if(i %500==0){
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
            ps.executeBatch();
            ps.clearBatch();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("数据插入失败");
            throw e;
        } finally {

            try {
                if (ps != null) {
                    //8.释放资源
                    ps.close();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            try {
                if (targetConn != null) {
                    targetConn.close();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    private void execAny(String targetDB,Proc p) throws SQLException {
        //1. 从源数据库中查询数据并保存
        Statement state = null;
        Connection srcConn = null;
        try {


            //1.获取数据库连接对象
            srcConn = getDBCByName(p.getDb());

            //2.定义sql语句
            String sql = p.getSql();

            //3.获取执行sql的对象 Statement
            state = srcConn.createStatement();

            //4.查询数据
            boolean rs =  state.execute(sql);
            //5.保存查到的数据
            logger.info("SQL执行结果: "+rs);


        } catch (SQLException e) {
            throw e;
        } finally {

            try {
                if (state != null) {
                    //8.释放资源
                    state.close();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            try {
                if (srcConn != null) {
                    srcConn.close();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

    }

    private Connection getDBCByName(String dbName) throws SQLTimeoutException,SQLException {
        DataSourceModel dbc = dataSources.getDataSources().get(dbName);
        if(dbc==null){
            logger.error("配置文件中没有该数据库配置: "+ dbName);
            throw new RuntimeException("can not find db config in file. db: "+dbName);
        }
        String url = "";
        switch (dbc.getType()){
            case "mysql":
                url = String.format("jdbc:mysql://%s/%s",dbc.getUrl(),dbc.getScheme());
                break;
            case "oracle":
                url = String.format("jdbc:oracle:thin:@//%s/%s",dbc.getUrl(),dbc.getScheme());
                break;
            case    "oceanbase":
                url = String.format("jdbc:oceanbase://%s/%s",dbc.getUrl(),dbc.getScheme());
                break;
            default:
                throw new SQLException("不支持该数据库类型: "+dbc.getType());
        }
        if(!"".equals(dbc.getConfig())){
            url = url + "?" + dbc.getConfig();
        }
        Connection c;
        try {
            c = DriverManager.getConnection(url, dbc.getUser(), dbc.getPassword());
        }catch (Exception e){
            e.printStackTrace();
            logger.error("数据库连接失败 "+dbc.getUrl());
            throw e;
        }
        return c;

    }


}
