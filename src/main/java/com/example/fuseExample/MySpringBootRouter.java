package com.example.fuseExample;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MySpringBootRouter extends RouteBuilder {


    @Bean
    public Map countries() {
        return new HashMap();
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
                .continued(true)
                .log("Rollback local Transaction and continuing with the route")
                .markRollbackOnlyLast()
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

        //testing transactions with database h2 and sql component
        from("timer:hello?repeatCount=1").autoStartup(false)
                .to("direct:beginTransaction")
                .to("sql:select * from billionaires")
                .log("The result is: ${body}");


        from("direct:beginTransaction").autoStartup(false)
                .transacted()
                .to("sql:update billionaires set career = null")
                .log("Rows updated: ${headers.CamelSqlUpdateCount}")
                .throwException(IllegalArgumentException.class, "error generated controlled")
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


        from("timer:hello?repeatCount=1").routeId("routeSsl").autoStartup(false)
                .log("Consuming ws")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/xml"))
                .setHeader("host", constant("vhacapodjp01.hec.avianca.com:50001"))
                .setHeader(Exchange.HTTP_URI, constant("https://vhacapodjp01.hec.avianca.com:50001/RESTAdapter/AMOS/SAP/Projects"))
                .to("https4:consumews?sslContextParameters=#contextParameters")
                .log("body: ${body}")
                .end();


        //testing JPA
        from("jpa:com.example.fuseExample.Billionaries?consumeDelete=false&delay=5000").routeId("routeJpa")
                //.marshal().json(JsonLibrary.Jackson)
                .marshal().jaxb()
                .log("the jpa body is ${body}")
                .end();

    }
}
