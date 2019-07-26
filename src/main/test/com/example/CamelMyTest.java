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

import java.awt.color.ICC_Profile;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = MySpringBootApplication.class)
@UseAdviceWith
@SpringBootApplication
public class CamelMyTest {

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;

    @EndpointInject(uri = "mock:endRouteXmlFiles")
    MockEndpoint endRouteXmlFiles;

    @EndpointInject(uri = "mock:endTransacionalRoute")
    MockEndpoint endTransacionalRoute;


    @EndpointInject(uri = "mock:endWiretap")
    MockEndpoint endWiretap;

    @EndpointInject(uri = "mock:endRoute")
    MockEndpoint endRoute;


    @EndpointInject(uri = "mock:endError")
    MockEndpoint endError;

    @Test
    public void testXmlConsumer() throws Exception {
        context.start();
        context.startRoute("route-XML-files");
        endRouteXmlFiles.expectedBodiesReceived("I'm jorge", "I'm camilo - I'm Richard - I'm Sandy");
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

    @Test
    public void testFTPRoute() throws Exception {
        String username = System.getenv("LOGNAME");
        String password = System.getenv("mypass");
        if (username == null || password == null) {
            fail("username (LOGNAME) or password (mypass) variables " +
                    "not found in enviroment variables, please add to test the stp component");
        }
        context.start();
        context.startRoute("routeFtp");
        // validate user and password on enviroment variables
        MockEndpoint endRouteFtp = context.getEndpoint("mock:endFtpRoute", MockEndpoint.class);
        endRouteFtp.expectedMinimumMessageCount(3);
        endRouteFtp.assertIsSatisfied();
    }


    @Test
    public void testRestRoute() throws Exception {
        context.start();
        context.startRoute("routeRest");
        template.sendBody("restlet:http://localhost:8090/receiveMessages?restletMethod=POST",
                "Hello world from testing !!!!");
    }


    @Test
    public void testRecipientListRoute() throws Exception {
        context.start();
        context.startRoute("recipientListRoute");
        context.startRoute("fooRoute");
        context.startRoute("barRoute");
        template.sendBody("direct:recipientListRoute",
                "{\"endpoints\":[\"direct:foo\",\"direct:bar\"]}");
    }

    @Test
    public void testWiretapRoute() throws Exception {
        context.start();
        context.startRoute("createPojoRoute");
        context.startRoute("wiretapRoute");
        template.sendBody("direct:createPojo", null);
        endWiretap.expectedMinimumMessageCount(1);
        endWiretap.assertIsSatisfied();
    }

    @Test
    public void testCBRRoute() throws Exception {
        context.start();
        context.startRoute("contentBasedRoute");
        endRoute.expectedMinimumMessageCount(5);
        endRoute.assertIsSatisfied();
    }

    @Test
    public void testErrorHandling1() throws Exception {
        context.start();
        context.startRoute("errorHandling1");
        template.sendBody("direct:errorHandllingRoute1", null);
        endRoute.expectedMinimumMessageCount(1);
        endRoute.assertIsSatisfied();
    }


    @Test
    public void testErrorHandling2() throws Exception {
        context.start();
        context.startRoute("routeLetterChannel");
        context.startRoute("routeErrorHandler");
        template.sendBody("direct:letterChannel", null);
        endError.expectedMinimumMessageCount(1);
        assertEquals("An unhandled error occurred", endError.getExchanges().get(0).getIn().getBody() );
        endError.assertIsSatisfied();
    }

    @Test
    public void testAttachments() throws Exception {
        context.start();
        context.startRoute("routeWithAttachments");
        template.sendBody("direct:attachments", null);
        List<Exchange> exchanges = endRoute.getExchanges();
        assertNotNull("There is not a message in attachments", exchanges.get(0).getIn().getAttachment("persons"));
        endRoute.assertIsSatisfied();
    }

    @Test
    public void testRouteXml() throws Exception {
        context.start();
        context.startRoute("routeHelloWorldXml");
        endRoute.expectedMinimumMessageCount(5);
        endRoute.assertIsSatisfied();
    }


}