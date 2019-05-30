package com.example.fuseExample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TransformationBean {

    @Autowired
    @Qualifier("countries")
    private Map map2;


    public void buildCountries() {
        List<Map> results = new ArrayList<>();
        Map entry1 = new HashMap();
        entry1.put("COUNTRY", "Colombia");
        entry1.put("CODE", "CO");
        entry1.put("DEPTO", "CND");
        results.add(entry1);

        Map entry2 = new HashMap();
        entry2.put("COUNTRY", "Costa Rica");
        entry2.put("CODE", "CR");
        results.add(entry2);
        Map entry3 = new HashMap();
        entry3.put("COUNTRY", "Mexico");
        entry3.put("CODE", "MX");
        results.add(entry3);

        // convert list<Map> into Map
        results.forEach(map1 -> map2.put(map1.get("COUNTRY"), Arrays.asList(map1.get("CODE"),map1.get("DEPTO"))));

    }

}
