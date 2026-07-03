package com.yavuz.model;

import java.util.List;

public class Recipe {

    public String recipeVersion;
    public List<DependencyItem> parentVersions;
    public List<DependencyItem> dependencyManagement;
    public List<DependencyItem> dependencies;
    public List<DependencyItem> plugins;
}
