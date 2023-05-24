package com.valette.DataTransfer.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;

/**
 将配置文件中配置的每一个属性的值,映射在这个组件中
 ConfigurationProperties:告诉spring boot将本类中的所有属性和配置文件中的相关配置进行绑定:
 prefix = "person "配置文件中哪个下面的所有属性进行--映射
 只有这个组件是容器中的组件，才能使用容器提供的ConfigurationProperties功能
 注意:默认读取全局配置文件
 */
@Component
@ConfigurationProperties(prefix = "")
@Data
public class DataTransferConfig {
    private ArrayList<TaskModel> tasks;

    private HashMap<String, DataSourceModel> dataSources;
}
