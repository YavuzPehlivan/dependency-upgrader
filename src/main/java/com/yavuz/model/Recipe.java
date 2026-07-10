package com.yavuz.model;

import java.util.List;

public class Recipe {

    public String recipeVersion;

    // Eskiden parentVersions / dependencyManagement / dependencies / plugins
    // şeklinde 4 ayrı listeydi; artık tek liste + item.type alanı ile ayrım yapılıyor.
    public List<DependencyItem> updates;
}