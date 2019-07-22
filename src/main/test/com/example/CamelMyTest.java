package com.example;

import com.example.fuseExample.MySpringBootApplication;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = MySpringBootApplication.class)
@UseAdviceWith
public class CamelMyTest {

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    @Test
    public void testMoveFile() throws Exception {


        RouteDefinition route = context.getRouteDefinition("route-XML-files");
        route.adviceWith(context, new AdviceWithRouteBuilder() {
            public void configure() throws Exception {
                mockEndpoints();
            }
        });
        context.start();
    }
}