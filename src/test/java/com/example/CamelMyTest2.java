package com.example;

import static org.junit.Assert.assertEquals;

import org.apache.activemq.filter.ConstantExpression;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.language.Constant;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.fuseExample.MySpringBootApplication;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = MySpringBootApplication.class)
@UseAdviceWith
@SpringBootApplication
public class CamelMyTest2 {
	
	private static Logger logger = LoggerFactory.getLogger("myLog");
	

    @Autowired
    private CamelContext context;
    @Autowired
    private ProducerTemplate template;
    
    @Test
    public void testAdviceWith() throws Exception {
    	context.start();
    	RouteDefinition routeJpa = context.getRouteDefinition("routeJpa");
    	routeJpa.adviceWith(context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
			  weaveById("activemqEndpoint").replace().to("mock:endJpaRoute");
			}
		});
    	MockEndpoint mockJpa = context.getEndpoint("mock:endJpaRoute", MockEndpoint.class);
    	context.startRoute("routeJpa");
    	mockJpa.expectedMinimumMessageCount(3);
    	mockJpa.assertIsSatisfied(2000);
    	logger.info("The final result is: " + mockJpa.getExchanges());
    }
    
    @Test
    public void testFilter() throws Exception {
    	context.start();
    	RouteDefinition routeTestingFilter = context.getRouteDefinition("routeTestingFilter");
    	routeTestingFilter.adviceWith(context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
			  weaveById("finalog").after().to("mock:endRoute");
			}
		});
    	context.startRoute("routeTestingFilter");
    	MockEndpoint mockEnd = context.getEndpoint("mock:endRoute", MockEndpoint.class);
    	mockEnd.reset(); // reset the counters
    	mockEnd.expectedMessageCount(1);
    	mockEnd.message(0).body().isEqualTo("I'm jorge");   
    	mockEnd.assertIsSatisfied();
    }
    
    //testing interceptors
    @Test
    public void testInterceptor() throws Exception {
    	context.start();
    	RouteDefinition fooRoute = context.getRouteDefinition("fooRoute");
    	fooRoute.adviceWith(context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				interceptFrom().transform(constant("route Intercepted !!!"));
				weaveAddLast()
				.log("the body is: ${body}")
				.to("mock:endRoute");
			}
		});
    	context.startRoute("fooRoute");
    	template.sendBody("direct:foo", new ConstantExpression("Hello from camel !!"));
    	MockEndpoint mockEnd = context.getEndpoint("mock:endRoute", MockEndpoint.class);
    	mockEnd.expectedMessageCount(1);
    	logger.info(mockEnd.getExchanges().get(0).getIn().getBody(String.class));
    	mockEnd.message(0).body().isEqualTo("route Intercepted !!!");
    	mockEnd.assertIsSatisfied();
    }
    
    
    //testing notify builders
    @Test
    public void testNotifyBuilder() {
    	NotifyBuilder builder = new NotifyBuilder(context);
    	builder.fromRoute("barRoute").whenDone(1).create();
    	
    }
    
    
    

}
