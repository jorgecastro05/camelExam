package com.example.fuseExample;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import java.util.HashMap;
import java.util.Map;

@Component
public class MySpringBootRouter extends RouteBuilder {


    @Bean
    public Map countries() {
        return new HashMap();
    }

    @Bean
    public ActiveMQComponent activemq() {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        ActiveMQComponent activemq = new ActiveMQComponent();
        activemq.setConnectionFactory(connectionFactory);
        return activemq;
    }

    @Bean
    public NoopHostnameVerifier hostnameVerifier() {
        return new NoopHostnameVerifier();
    }

    @Override
    public void configure() {
        AggregationStrategy aggregation = new NamesAggregationStrategy();
        AggregationStrategy csvAggregation = new CvsAggregationStrategy();


        CsvDataFormat csv = new CsvDataFormat("|");

        onException(IllegalArgumentException.class)
                .continued(true) //continues the route
                .log("Rollback local Transaction and continuing with the route")
                .markRollbackOnlyLast() // mark only the last transaction in the route
                .end();

        // testing aggregations with xpath
        from("file:dataIn?noop=true&antInclude=*.xml").routeId("route-XML-files").autoStartup(false)
                .aggregate(aggregation).xpath("/hello/tag").completionTimeout(10)
                .log("Messages Aggregated XML files: tag: ${headers.tag} = ${body}");

        //testing aggregations with csv dataformat
        from("file:dataIn?noop=true&antInclude=*.csv").routeId("route-CSV-files").autoStartup(false)
                .unmarshal(csv) //this return a list of list
                .split(body()) //this iterate for each element of list
                .aggregate(csvAggregation).simple("${body.get(0)}").completionTimeout(10) //aggregate equal tags
                .log("Messages Aggregated tag CSV files: ${headers.tag} = ${body}")
                .end();

        //testing transactions with database h2 and sql component, creating local boundaries of transactions
        from("timer:hello?repeatCount=1").autoStartup(false)
                .to("direct:beginTransaction")
                .to("sql:select * from billionaires")
                .log("The result is: ${body}");


        from("direct:beginTransaction").autoStartup(false)
                .transacted() //this init a transaction
                .to("sql:update billionaires set career = null")
                .log("Rows updated: ${headers.CamelSqlUpdateCount}")
                .throwException(IllegalArgumentException.class, "error generated controlled") //throw exception
                .end();

        //testing JPA

        from("timer:hello?delay=1000").routeId("myRouteLoadData").autoStartup(false)
                .log("Init countries route")
                .choice()
                .when(method("countries", "isEmpty"))
                .bean("transformationBean", "buildCountries")
                .log("The new data is loaded")
                .otherwise()
                .log("The data is was charged")
                .end()
                .log("The countries are ${bean:countries?method=values}")
                .end();


        //testing JPA and JMS
        from("jpa:com.example.fuseExample.Billionaries?consumeDelete=false&delay=5000").routeId("routeJpa")
                .marshal().jaxb() //convert results from pojos to xml
                .log(LoggingLevel.DEBUG, "the xml of results is ${body}")
                .log("Sending Message")
                .to("activemq:billionariesXml")
                .end();

        //testing jms
        from("activemq:billionariesXml")
                .log("Received Amq message from temporary broker")
                .log("${body}")
                .end();


    }
}
