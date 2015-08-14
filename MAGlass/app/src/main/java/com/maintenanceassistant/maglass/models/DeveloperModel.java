package com.maintenanceassistant.maglass.models;

import java.io.Serializable;

/**
 * Created by Jake on 2015-07-10.
 */
public class DeveloperModel implements Serializable {
    private String name;
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name=name;
    }
    private String platform;
    public String getPlatform(){
        return platform;
    }
    public void setPlatform(String platform){
        this.platform=platform;
    }
    private String image;
    public String getImage(){
        return image;
    }
    public void setImage(String image){
        this.image=image;
    }
}
