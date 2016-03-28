package com.cm.android.winecellar.db;

import java.io.Serializable;


public class Note implements Serializable{
    public long id;
    public String wine="";
    public String rating="0.0";
    public String textExtract="";
    public String notes="";
    public String share="";
    public String picture="";
    public long created;
    public long updated;
}
