package com.valette.DataTransfer.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class TaskModel {
    String id;
    String timer;

    String db;

    ArrayList<Proc> proc;


}

