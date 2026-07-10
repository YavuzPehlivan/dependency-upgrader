package com.yavuz.model;

import java.util.Map;

public class DependencyItem {

    // "parent", "dependencyManagement", "dependency" veya "plugin"
    public String type;

    public String groupId;
    public String artifactId;
    public String version;

    public Map< String , String > versions;
}