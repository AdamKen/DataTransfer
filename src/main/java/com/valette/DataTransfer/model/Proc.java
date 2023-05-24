package com.valette.DataTransfer.model;

import lombok.Data;

@Data
public class Proc {
    String db;
    String type;
    String sql;
    String targetTable;
}
