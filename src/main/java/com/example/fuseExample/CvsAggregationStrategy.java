package com.example.fuseExample;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;


public class CvsAggregationStrategy implements AggregationStrategy {

	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		if (oldExchange == null) {
			List<String> data = newExchange.getIn().getBody(List.class);
			newExchange.getIn().setBody(data.get(1));
			newExchange.getIn().setHeader("tag", data.get(0));
			return newExchange;
		} else {
			String data1 = oldExchange.getIn().getBody(String.class);
			List<String> data = newExchange.getIn().getBody(List.class);
			oldExchange.getIn().setBody(data1 +" - " + data.get(1));
			return oldExchange;
		}
	}

}
