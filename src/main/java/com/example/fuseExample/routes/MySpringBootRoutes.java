package com.example.fuseExample.routes;

import com.example.fuseExample.domain.Billionaries;
import com.example.fuseExample.domain.Greeting;
import com.example.fuseExample.transform.CvsAggregationStrategy;
import com.example.fuseExample.transform.NamesAggregationStrategy;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.jms.ConnectionFactory;

@Component
public class MySpringBootRoutes extends RouteBuilder {

    @Autowired
    CvsAggregationStrategy cvsAggregationStrategy;
    @Autowired
    NamesAggregationStrategy namesAggregationStrategy;

    //sftp credentials from enviroment variables
    private String username = System.getenv("LOGNAME");
    private String password = System.getenv("mypass");

    @Bean
    public CsvDataFormat csvDataFormat() {
        return new CsvDataFormat("|");
    }

    @Bean
    public ActiveMQComponent activemq() {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        ActiveMQComponent activemq = new ActiveMQComponent();
        activemq.setConnectionFactory(connectionFactory);
        return activemq;
    }


    @Override
    public void configure() {
        getContext().setAutoStartup(false);

        onException(IllegalArgumentException.class)
                .continued(true) //continues the route
                .log("Rollback local Transaction and continuing with the route")
                .markRollbackOnlyLast() // mark only the last transaction in the route
                .end();

        // testing aggregations with xpath
        from("file:dataIn?noop=true&antInclude=*.xml").routeId("route-XML-files")
                .aggregate(namesAggregationStrategy).xpath("/hello/tag").completionTimeout(10)
                .log("Messages Aggregated XML files: tag: ${headers.tag} = ${body}")
                .to("mock:endRouteXmlFiles");

        //testing split -  aggregations with csv dataformat
        from("file:dataIn?noop=true&antInclude=*.csv").routeId("route-CSV-files")
                .unmarshal("csvDataFormat") //this return a list of list
                .split(body()) //this iterate for each element of list
                .aggregate(cvsAggregationStrategy).simple("${body.get(0)}").completionTimeout(10) //aggregate equal tags
                .log("Messages Aggregated tag CSV files: ${headers.tag} = ${body}")
                .end();

        //testing transactions with database h2 and sql component, creating local boundaries of transactions
        from("timer:hello?repeatCount=1").routeId("routeInvokerTransaction").autoStartup(false)
                .to("direct:beginTransaction")
                .to("sql:select * from billionaires")
                .log("The result is: ${body}")
                .to("mock:endTransacionalRoute");


        from("direct:beginTransaction").routeId("routeDoTransaction")
                .transacted() //this init a transaction
                .to("sql:update billionaires set career = null")
                .log("Rows updated: ${headers.CamelSqlUpdateCount}")
                .throwException(IllegalArgumentException.class, "error generated by me") //throw exception
                .end();


        //testing JPA and JMS
        from("jpa:com.example.fuseExample.domain.Billionaries?consumeDelete=false&delay=5000").routeId("routeJpa")
                .marshal().jaxb() //convert results from pojos to xml
                .log(LoggingLevel.DEBUG, "the xml of results is ${body}")
                .log("Sending Message")
                .to("activemq:billionariesXml")
                .end();

        //testing JMS
        from("activemq:billionariesXml").routeId("routeJms")
                .log("Received Amq message from temporary broker")
                .log("${body}")
                .end();

        // testing ftp2 component (sftp)
        from("sftp:localhost:22?username=" + username + "&password=" + password + "&noop=true").routeId("routeFtp").autoStartup(false)
                .log("File received with name ${headers.CamelFileName}")
                .to("mock:endFtpRoute")
                .end();

        // testing rest endpoint as consumer
        from("restlet:http://localhost:8090/receiveMessages?restletMethod=POST").routeId("routeRest")
                .log("Message received from rest endpoint ${body}")
                .end();

        // testing recipient list and direct component from json list of endpoints
        from("direct:recipientListRoute").routeId("recipientListRoute")
                .log("Invoking recipient List for json message ${body}")
                .recipientList(jsonpath("$.endpoints.*"))
                .end();

        from("direct:foo").routeId("fooRoute")
                .log("Hello from foo route").end();

        from("direct:bar").routeId("barRoute")
                .log("Hello from bar route").end();


        // testing wiretap with shadow copy, this route creates a pojo and send it into wiretap that has a delay, meanwhile
        // we change the pojo and is reflected into the wiretap route as a shadow copy.
        from("direct:createPojo").routeId("createPojoRoute")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Billionaries rich = new Billionaries();
                        rich.setFirstName("Bruce");
                        rich.setLastName("Wayne");
                        rich.setCareer("Batman");
                        exchange.getIn().setBody(rich);
                    }
                })
                .log("The billionary is ${id} ${body.toString()}")
                .wireTap("direct:printAgainBillionary").copy()
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Billionaries rich = exchange.getIn().getBody(Billionaries.class);
                        rich.setCareer("CEO at wayne enterprises");
                    }
                })
                .end();

        from("direct:printAgainBillionary").routeId("wiretapRoute")
                .delay(1000)
                .log("The billionary in wiretap ${id} is: ${body.toString()}")
                .to("mock:endWiretap")
                .end();


        //Test for Content-based routing
        // we use the same csv files unmarshal with bindy and make a choice of type of greeting (bye|greeting)
        from("file:dataIn?noop=true&antInclude=*.csv").routeId("contentBasedRoute").autoStartup(false)
                .log("reading file ${headers.CamelFileName}")
                .unmarshal().bindy(BindyType.Csv, Greeting.class)
                .split(body())
                .choice()
                .when(simple("${body.message} == 'greeting'"))
                .log("╯°□°╯ Hello from ${body.person}")
                .when(simple("${body.message} == 'bye'"))
                .log("ಠ_ಠ Bye from ${body.person}")
                .to("mock:endRoute")
                .end();

        //test with attachments and pollencrich
        from("direct:attachments").routeId("routeWithAttachments")
                .log("Testing route with attachments")
                .pollEnrich("file:dataIn?noop=true&antInclude=*.xml")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message message = exchange.getIn();
                        DataHandler dataHandler = new DataHandler(
                                exchange.getIn().getBody(), "text/xml");
                        message.addAttachment("persons", dataHandler);
                    }
                })
                .to("mock:endRoute")
                .end();


    }
}
