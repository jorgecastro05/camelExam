package com.example.fuseExample.transform;

import org.apache.camel.Exchange;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.springframework.stereotype.Component;

@Component
public class NamesAggregationStrategy implements AggregationStrategy{

	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		XPathBuilder xpath = XPathBuilder.xpath("/hello/message");
		XPathBuilder xpathTag = XPathBuilder.xpath("/hello/tag");
		if(oldExchange == null) {
			String evaluate = xpath.evaluate(newExchange.getContext(), newExchange.getIn().getBody());
			String tag = xpathTag.evaluate(newExchange.getContext(), newExchange.getIn().getBody());
			newExchange.getIn().setBody(evaluate);
			newExchange.getIn().setHeader("tag", tag);
			return newExchange;
		}else {
			String oldName = oldExchange.getIn().getBody(String.class);
			String evaluate = xpath.evaluate(newExchange.getContext(), newExchange.getIn().getBody());
			oldExchange.getIn().setBody(oldName +" - "+ evaluate);
			return oldExchange;
		}
		
	}

}
