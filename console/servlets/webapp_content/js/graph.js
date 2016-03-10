opletColor = {"quarks.metrics.oplets.CounterOp": "#c7c7c7", "quarks.metrics.oplets.RateMeter": "#aec7e8", "quarks.oplet.core.FanIn": "#ff7f0e", 
		"quarks.oplet.core.FanOut": "#ffbb78", "quarks.oplet.core.Peek": "#2ca02c", "quarks.oplet.core.PeriodicSource": "#98df8a", 
		"quarks.oplet.core.Pipe": "#d62728", "quarks.oplet.core.PipeWindow": "#ff9896", "quarks.oplet.core.ProcessSource": "#9467bd", 
		"quarks.oplet.core.Sink": "#c5b0d5", "quarks.oplet.core.Source": "#8c564b", "quarks.oplet.core.Split": "#c49c94", "quarks.oplet.core.Union" : "#1f77b4",
		"quarks.oplet.functional.ConsumerEventSource": "#e377c2", "quarks.oplet.functional.ConsumerPeek": "#f7b6d2", "quarks.oplet.functional.ConsumerSink": "#7f7f7f", 
		"quarks.oplet.functional.Filter": "#7F7F7F", "quarks.oplet.functional.FlatMapper": "#bcbd22", "quarks.oplet.functional.Isolate": "#dbdb8d", 
		"quarks.oplet.functional.Map": "#17becf", "quarks.oplet.functional.SupplierPeriodicSource": "#9edae5", "quarks.oplet.functional.SupplierSource": "#b5cf6b", 
		"quarks.oplet.plumbing.PressureReliever": "#e7cb94", "quarks.oplet.plumbing.TextFileReader": "#ad494a", "quarks.oplet.plumbing.UnorderedIsolate": "#de9ed6"};
colorMap = {};

addValuesToEdges = function(graph, counterMetrics) {
	var edges = graph.edges;
	var vertices = graph.vertices;
	var max = d3.max(counterMetrics, function(cm){
		return parseInt(cm.value, 10);
	});
	var quartile1 = parseInt(max * 0.25, 10);

	// assign the counter metric value to the edge that has the oplet id as a source or target
	counterMetrics.forEach(function(cm){
		edges.forEach(function(edge){
			if (edge.sourceId === cm.opId || edge.targetId === cm.opId) {
				// add a value to this edge from the metric
				edge.value = cm.value;	
			} 
		});
	});
	
	// if there is no counter metric, assign it a mean value, along with a flag that says it is a derived value
	edges.forEach(function(edge){
		if (!edge.value) {
			edge.value = quartile1;
			edge.derived = true;
		} else if (edge.value === "0") {
			edge.value = 0.45;
			edge.realValue = 0;
		} 
			
	});

	return graph;
};

getVertexFillColor = function(layer, data) {
	if (layer === "opletColor" || layer === "static") {
		return opletColor[data.invocation.kind];
	} else if (layer === "flow") {
		// return a muted color if it is a counter op
		if (data.invocation.kind.toUpperCase().endsWith("COUNTEROP")) {
			return "#c7c7c7";
		} else {
			return d3.rgb("rgb(31, 119, 180)");
		}
	} else {
		return colorMap[data.id.toString()];
	}
};

getFormattedTagLegend = function(tArray) {
	var items = [];
	tArray.forEach(function(t){
		var obj = {};
		obj.name = t;
		if (t === MULTIPLE_TAGS_TEXT) {
			obj.fill = MULTIPLE_TAGS_COLOR;
			obj.stroke = MULTIPLE_TAGS_COLOR;
		} else {
			obj.fill = color20(t);
			obj.stroke = color20(t);
		}
		items.push(obj);
	});
	return items;
};

getFormattedTupleLegend = function(metricBuckets, scale) {
	var items = [];
	var buckets = metricBuckets.buckets;
	buckets.forEach(function(b){
		var obj = {};
		obj.name = b.name;
		obj.fill = scale(b.id);
		obj.stroke = scale(b.id);
		obj.idx = b.id;
		items.push(obj);
	});
	
	var sortFunction = function(a, b) {
		 if (a.idx < b.idx)  {
 	    	return -1;
 	     } else if (a.idx > b.idx) {
 	    	return 1;
 	     } else {
 	    	return 0;
 	    }
	};
	return items.sort(sortFunction);
};

getLegendText = function(layer, data) {
	if (layer === "opletColor" || layer === "static") {
		return parseOpletKind(data.invocation.kind);
	} else {
		return "";
	}
};

parseOpletKind = function(kind) {
	var returnName = kind;
	var newNames = kind.split(".");
	if (newNames.length > 1) {
		returnName = newNames[newNames.length - 1];
		returnName += " (";
		for (var i = 0; i < newNames.length -1; i++) {
			returnName += newNames[i] + ".";
		}
		returnName = returnName.substring(0, returnName.length -1);
		returnName += ")";
	}
	return returnName;
};

getLegendColor = function(layer, d) {
	return getVertexFillColor(layer, d);
};

getEdgeColor = function(layer) {
	
};

setVertexColorByFlowRate = function() {
	
};

makeStaticFlowValues = function(numValues) {
	var littleVal = 0.001;
	var data = d3.range(numValues).map(function() {
		return littleVal;
		});
	return data;
};

makeRandomFlowValues = function(numValues) {
	var random = d3.random.normal(5000, 2000);
	var data = d3.range(numValues).map(random);
	return data;
};

hideElement = function(elementId){
	var id = "#" + elementId;
	d3.select(id).style("display", "none");
};

showElement = function(elementId) {
	var id = "#" + elementId;
	d3.select(id).style("display", "block");
};

