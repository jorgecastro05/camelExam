package com.example.fuseExample.routes;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ErrorHandlingRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        //try catch testing
        from("direct:errorHandllingRoute1").routeId("errorHandling1")
                .log("Testing try catch error handling")
                .doTry()
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        throw new IllegalArgumentException("This is a generated Error");
                    }
                })
                .doCatch(IllegalArgumentException.class)
                .log("Exception catch: ${exception.message}")
                .to("mock:endRoute")
                .end();


        //testing using a dead letter inside the route to apply only on this route
        from("direct:letterChannel").routeId("routeLetterChannel")
                .errorHandler(deadLetterChannel("direct:errorHandler")
                        .onPrepareFailure(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getIn().setBody("An unhandled error occurred");
                            }
                        }).logStackTrace(true))
                .log("Generating an exception to be trown by the dead letter queue")
                .throwException(IllegalArgumentException.class, "The credentials are invalid")
                .end();

        from("direct:errorHandler").routeId("routeErrorHandler")
                .log("The error is: ${body}")
                .to("mock:endError")
                .end();
        
        
        from("direct:routeOnException").routeId("routeOnException")
        	.onException(IllegalArgumentException.class)
        	.handled(true)
        	.log("catched an Illegal ArgumentException: ${exception.message}")
        	.end() //end onException
        	.log("received Message: ${body}")
        	.throwException(IllegalArgumentException.class, "the messsage must be null")
        .end();

    }
}
