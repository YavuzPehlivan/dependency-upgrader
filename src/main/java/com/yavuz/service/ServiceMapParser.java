package com.yavuz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ServiceMapParser {

    /**
     * services.yaml dosyasını okur ve servis adlarını servis tiplerine eşler.
     * Doküman Madde 7.2 formatına uygundur.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, String>> parseServiceMap(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // YAML ağacını generic Map olarak oku
        Map<String, Object> root = mapper.readValue(new File(filePath), Map.class);

        if (root != null && root.containsKey("services")) {
            return (Map<String, Map<String, String>>) root.get("services");
        }
        return null;
    }
}