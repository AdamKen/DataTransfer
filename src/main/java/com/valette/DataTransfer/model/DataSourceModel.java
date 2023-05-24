package com.valette.DataTransfer.model;

import lombok.Data;

@Data
public class DataSourceModel {
    private String type;
    private String url;
    private String scheme;
    private String user;
    private String password;
    private String config;

}
