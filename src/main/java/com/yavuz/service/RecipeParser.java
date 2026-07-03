package com.yavuz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.yavuz.model.Recipe;

import java.io.File;
import java.io.IOException;

public class RecipeParser {

    public static Recipe parseRecipe(String filePath) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(filePath), Recipe.class);
    }
}
