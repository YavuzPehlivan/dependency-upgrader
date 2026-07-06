package com.yavuz.model;

public class MicroService {
    public String name; // Servisin klasör adı (Örn: dnext-cost-management)
    public String path; // pom.xml tam disk yolu
    public String serviceType;  // "ca", "non-ca" veya "unknown"

    public MicroService(String name , String path){
        this.name = name;
        this.path = path;
        this.serviceType = "unknown";
    }
}

