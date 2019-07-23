package com.example;

import com.example.fuseExample.MySpringBootApplication;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = MySpringBootApplication.class)
@UseAdviceWith
@SpringBootApplication
public class CamelMyTest {

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    @EndpointInject(uri="mock:endRouteXmlFiles")
    MockEndpoint endRouteXmlFiles;

    @EndpointInject(uri="mock:endTransacionalRoute")
    MockEndpoint endTransacionalRoute;


    @Test
    public void testXmlConsumer() throws Exception {
        context.start();
        context.startRoute("route-XML-files");
        endRouteXmlFiles.expectedBodiesReceived("I'm jorge","I'm camilo - I'm Richard - I'm Sandy");
        endRouteXmlFiles.assertIsSatisfied();
    }


    @Test
    public void testCsvConsumer() throws Exception {
        // in this test we are mocking the endpoint with AdviceWithRouteBuilder
        RouteDefinition csvRouteDefinition = context.getRouteDefinition("route-CSV-files");
        csvRouteDefinition.adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:endRouteCsvFiles");
            }
        });
        context.start();
        context.startRoute("route-CSV-files");
        MockEndpoint mockEndRoute = context.getEndpoint("mock:endRouteCsvFiles", MockEndpoint.class);
        mockEndRoute.expectedMessageCount(2);
        mockEndRoute.assertIsSatisfied();
    }


    @Test
    public void testTransactionalRoute() throws Exception {
        context.start();
        context.startRoute("routeInvokerTransaction");
        context.startRoute("routeDoTransaction");
        endTransacionalRoute.expectedBodiesReceived("[{ID=1, FIRST_NAME=Aliko, LAST_NAME=Dangote, CAREER=Billionaire Industrialist}, {ID=2, FIRST_NAME=Bill, LAST_NAME=Gates, CAREER=Billionaire Tech Entrepreneur}, {ID=3, FIRST_NAME=Folrunsho, LAST_NAME=Alakija, CAREER=Billionaire Oil Magnate}]");
        endTransacionalRoute.assertIsSatisfied();
    }


    @Test
    public void testJpaRoute() throws Exception {
        context.start();
        context.startRoute("routeJpa");
        Thread.sleep(30000);
    }

}