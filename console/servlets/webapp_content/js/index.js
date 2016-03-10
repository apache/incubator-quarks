var layerVal = "flow";
var refreshInt = 5000;
var metricChartType = 'barChart';

var stopTimer = false;
var startGraph = null;
var run = null;
var refreshedRowValues = [];
var stateTooltip = null;
var rowsTooltip = null;

var tagsColors = {};
var propWindow;

var resetAll = function(bNew) {
    clearInterval(run);
    clearTableGraphs();
	d3.select("#graphLoading").style("display", "none");
	var selectedJob = d3.select("#jobs").node().value;
	getCounterMetricsForJob(renderGraph, selectedJob, bNew);
	if (bNew) {
		startGraph(refreshInt);
	}
};

d3.select("#jobs")
.on("change", function() {
  tagsArray = [];
  streamsTags = {};

  resetAll(true);
});

d3.select("#tags")
.on("change", function(){
	resetAll(false);
});

d3.select("#layers")
.on("change", function() {
    layerVal = this.value;
	
    clearInterval(run);
    clearTableGraphs();

	d3.select("#graphLoading").style("display", "none");
	var selectedJob = d3.select("#jobs").node().value;
	getCounterMetricsForJob(renderGraph, selectedJob);
	startGraph(refreshInt);
});

d3.select("#metrics")
.on("change", function() {
	// determine if the just selected metric is associated with multiple oplets
	var theOption = d3.select(this)
    .selectAll("option")
    .filter(function (d, i) { 
        return this.selected; 
    });
	
	var chartType = d3.select("#mChartType");
	var multipleOps = theOption.attr("multipleops");
	
	var lineChartOption = chartType.selectAll("option")
	.filter(function (d, i){
		return this.value === "lineChart";
	});

	var chartValue = chartType.node().value;
	if (multipleOps === "false") {
		lineChartOption.property("disabled", false);
	} else {
		// disable it even if it is not selected
		lineChartOption.property("disabled", true);
		if (chartValue === "lineChart") {
			// if it is selected, deselect it and select barChart
			chartType.node().value = "barChart";
		}
	}
	
	
	if (chartValue === "barChart") {
		fetchMetrics();
	} else if (chartValue === "lineChart") {
		if (multipleOps === "false") {
			fetchLineChart();
		}
	}	
});

d3.select("#mChartType")
.on("change", function() {
	metricChartType = this.value;
	if (metricChartType === "barChart") {
		fetchMetrics();
	} else if (metricChartType === "lineChart") {
		fetchLineChart();
	}
});

d3.select("#refreshInterval")
.on("change", function() {
	var isValid = this.checkValidity();
	if (isValid) {
		clearInterval(run);
		refreshInt = this.value * 1000;
		startGraph(refreshInt);
	} else {
		alert("The refresh interval must be between 3 and 20 seconds");
		this.value = 5;
	}
});

d3.select("#toggleTimer")
.on("click", function() {
	if (stopTimer === false){
		stopTimer = true;
		d3.select(this).text("Resume graph");
		d3.select(this)
		.attr("class", "start");
	} else {
		stopTimer = false;
		d3.select(this).text("Pause graph");
		d3.select(this)
		.attr("class", "stop");
	}
});

var clearTableGraphs = function() {
	d3.select("#chart").selectAll("*").remove();
	d3.select("#graphLoading").
	style("display", "block");
};

var margin = {top: 30, right: 5, bottom: 6, left: 30},
	width = 860 - margin.left - margin.right,
    height = 600 - margin.top - margin.bottom;


var svgLegend = d3.select("#graphLegend")
	.append("svg")
	.attr("height", 600)
  	.append("g")
  	.attr("width", 300)
    .attr("height", 600)
  	.attr("id", "legendG")
  	.attr("transform", "translate(0," + 30 + ")");

var formatNumber = d3.format(",.0f"),
    format = function(d) { return formatNumber(d) + " tuples"; },
    makeRandomMetrics = function() {
    	var retObjs = [];
    	var num = 2;
    	var random = d3.random.normal(400, 100);
    	var data = d3.range(num).map(random);
    	var metNames = ["Tuples transmitted", "Tuples submitted"];
    	var i = 0;
    	data.forEach(function(d) {
    		retObjs.push({"name": metNames[i], "value": formatNumber(d)});
    		i++;
    	});
    	return retObjs;
    },
    formatMetric = function(retObjs) {
    	var retString = "";
    	retObjs.forEach(function(d) {
    		retString += "<div>" + d.name + ": " + d.value + "</div>";
    	});
    	return retString;
    },
    color20 = d3.scale.category20(),
    color10 = d3.scale.category10(),
    // colors of d3.scale.category10() to do - just call color10.range();
    tupleColorRange = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf" ];
    

	var	showTimeout = null,
     hideTimeout = null,
     showTime = 800,
     hideTime = 300;
    
    var clearHideTimeout = function(){
   		if (hideTimeout){
  			clearTimeout(hideTimeout);
  			hideTimeout = null;
  		}
   };

    var hideTooltip = function(d, i)  {
		clearHideTimeout();
    	hideTimeout = setTimeout(function(){  
    		hideTimeout = null;
    		if(showTimeout){
    			clearTimeout(showTimeout);
    		}
    		
    		tooltip.style("display", "none");
    		
    	}, hideTime);
	};

var svg = d3.select("#chart").append("svg")
    .attr("width", width + margin.left + margin.right + 5)
    .attr("height", height + margin.top + margin.bottom)
  	.append("g")
  	.attr("id", "parentG")
    .attr("transform", "translate(20,10)");

var sankey = d3.sankey()
    .nodeWidth(30)
    .nodePadding(10)
    .size([width, height]);

var path = d3.svg.diagonal()
.source(function(d) { 
	return {"x":d.sourceIdx.y + d.sourceIdx.dy/2, "y":d.sourceIdx.x + sankey.nodeWidth()/2}; 
 })            
.target(function(d) { 
	return {"x":d.targetIdx.y + d.targetIdx.dy/2, "y":d.targetIdx.x + sankey.nodeWidth()/2}; 
 })
.projection(function(d) { 
	return [d.y, d.x]; 
 });

var showAllLink = d3.select("#showAll")
	.on("click", function() {
		 displayRowsTooltip(true);
	});
	
var tooltip = d3.select("body")
	.append("div")
	.attr("class", "tooltip")
	.style("display", "none")
	.on('mouseover', function(d,i) {
		clearHideTimeout();
	})
	.on('mouseout', function(d,i) {
		hideTooltip(null,i);
	});
	

var showTooltip = function(content, d, i, event) {
	clearHideTimeout();

	if(showTimeout){
		clearTimeout(showTimeout);
	}
	
	var leftOffset = d.invocation.kind.toUpperCase().endsWith("COUNTEROP") ? 100 : 350;
    	
	showTimeout = setTimeout(function(){
		showTimeout = null;
			if(content){
    			tooltip.html(content);

				tooltip.style("padding-x", -22)
				.style("padding-y", 0)
				.style("left", (event.pageX - leftOffset) + "px")
				.style("top", event.pageY +"px")
				.style("display", "block");
			}
		
		}, showTime);
    	
    	
};

var displayRowsTooltip = function(newRequest) {
	var rows = makeRows();
	var headerStr = "<html><head><title>Oplet properties</title><link rel='stylesheet' type='text/css' href='resources/css/main.css'></head>" + 
	"<body><table style='width: 675px;margin: 10px;table-layout:fixed;word-wrap: break-word;'>";
	var content = "";
	var firstTime = true;
	var firstKey = true;
	for (var key in rows) {
		var row = rows[key];
		content += "<tr>";
		for (var newKey in row) {
			if (firstTime) {
				if (newKey === "Name") {
					headerStr += "<th style='width: 100px;'>" + newKey + "</th>";
				} else {
					headerStr += "<th style='width: 150px;'>" + newKey + "</th>";
				}
			}

			if (newKey === "Name"){
				content += "<td class='center100'>" + row[newKey] + "</td>";
			} else if (newKey === "Tuple count"){
				content += "<td class='right'>" + row[newKey] + "</td>";
			} else if (newKey === "Oplet kind"){
				content += "<td class='left'>" + row[newKey] + "</td>";
			} else {
				content += "<td class='center'>" + row[newKey] + "</td>";
			}
		}
		firstTime = false;
		if (firstKey) {
			headerStr += "</tr>";
			firstKey = false;
		}
		content += "</tr>";
	}
	var str = headerStr + content + "</table></body><html>";
	
	if (newRequest) {
		propWindow = window.open("", "Properties", "width=825,height=500,scrollbars=yes,dependent=yes");
		propWindow.document.body.innerHTML = "";
		propWindow.document.write(str);
		propWindow.onunload = function() {
			propWindow = null;
		};
		window.onunload = function() {
			if (propWindow) {
				propWindow.close();
			}
		};
	} else {
		if (typeof(propWindow) === "object") {
			propWindow.document.body.innerHTML = "";
			propWindow.document.write(str);
		}
	}
};


var showStateTooltip = function(event) {
	var jobId = d3.select("#jobs").node().value;
	var jobObj = jobMap[jobId];
	var content = "<div style='margin:10px'>";
	for (var key in jobObj) {
		var idx = key.indexOf("State");
		if ( idx !== -1) {
			var name = key.substring(0, idx) + " " + key.substring(idx, key.length).toLowerCase();
			var val = jobObj[key];
			var value = val.substring(0,1) + val.substring(1,val.length).toLowerCase();
			content += name + ": " + value + "<br/>";
		} else {
			content += key + ": " + jobObj[key] + "<br/>";
		}
	}
	content += "</div>";
	stateTooltip
	.html(content)
	.style("left", (event.pageX - 200) + "px")
	.style("top", event.pageY +"px")
	.style("padding-x", 22)
	.style("padding-y", 10)
	.style("display", "block");
};

var hideStateTooltip = function() {
	stateTooltip
	.style("display", "none");
};


var makeRows = function() {
	var nodes = refreshedRowValues !== null ? refreshedRowValues : sankey.nodes();
	var theRows = [];
	 nodes.forEach(function(n) {
		 var sources = [];
	   	 var sourceStreams = [];
		 n.targetLinks.forEach(function(trg){
			sources.push(trg.sourceIdx.idx);
 	  		if (trg.tags && trg.tags.length > 0) {
   	  			sourceStreams = trg.tags;
   	  		}
		 });
		 var targets = [];
		 var targetStreams = [];
		 n.sourceLinks.forEach(function(src){
			 targets.push(src.targetIdx.idx);
	   	  		if (src.tags && src.tags.length > 0) {
	   	  			targetStreams = src.tags;
	   	  		}
		 });
   	  	var kind = parseOpletKind(n.invocation.kind);

   	  	var value = "";
   	  	if (n.derived === true) {
   	  		value = "Not applicable - counter not present";
   	  	} else if (n.realValue === 0 && value === 0.45) {
   	  		value = 0;
   	  	} else {
   	  		value = formatNumber(n.value);
   	  	}
   	  	
   	  	var rowObj = {"Name": n.idx, "Oplet kind": kind, "Tuple count": formatNumber(n.value), 
   	  			"Sources": sources.toString(), "Targets": targets.toString(), 
   	  			"Source stream tags": sourceStreams.toString() === "" ? "None" : sourceStreams.toString(), 
   	  			"Target stream tags": targetStreams.toString() === "" ? "None" : targetStreams.toString()};
		theRows.push(rowObj);
	 });
	return theRows;
};

vertexMap = {};

var renderGraph = function(jobId, counterMetrics, bIsNewJob) {
	d3.select("#loading").remove();
	var qString = "jobs?jobgraph=true&jobId=" + jobId;
	d3.xhr(qString, function(error, jsonresp) {
		if (error) {
			console.log("error retrieving job with id of " + jobId);
		}
		if (!jsonresp.response || jsonresp.response === "") {
			return;
		}
		var layer = d3.select("#layers")
					.node().value;
		var graph = JSON.parse(jsonresp.response);
		
		if (counterMetrics && counterMetrics.length > 0) {
			graph = addValuesToEdges(graph, counterMetrics);
		} 
		
		// these are used if the topology has no metrics, and to display the static graph
		var generatedFlowValues = makeStaticFlowValues(graph.edges.length);
		
		d3.select("#chart").selectAll("*").remove();
		
		svg = d3.select("#chart").append("svg")
	   .attr("width", width + margin.left + margin.right + 5)
	   .attr("height", height + margin.top + margin.bottom)
	   .append("g")
	   .attr("id", "parentG")
	   .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


		var j = 0;
		graph.vertices.forEach(function(vertex){
			vertex.idx = j;
			if (!vertexMap[vertex.id]) {
				vertexMap[vertex.id] = vertex;
			}
			j++;
		});
		
		var i = 0;
		graph.edges.forEach(function(edge) {
			var value = "";
			if (layer === "static" || !edge.value) {
				value = generatedFlowValues[i];
			} else {
				value = edge.value;
			}
			edge.value = value;
			edge.sourceIdx = vertexMap[edge.sourceId].idx;
			edge.targetIdx = vertexMap[edge.targetId].idx;
			i++;
			if (edge.tags && edge.tags.length > 0) {
				setAvailableTags(edge.tags);
			}
		});
		var layers = d3.select("#layers");
		var selectedL = layers.node().value;

		showTagDiv(bIsNewJob);
		selectedTags = [];
		if (d3.select("#showTags").property("checked") === true) {
			// fetch the selected tags, and modify the graph
			selectedTags = getSelectedTags();
		}

		refreshedRowValues = graph.vertices;
		
		sankey
		.nodes(graph.vertices)
		.links(graph.edges)
		.layout(32);
  
  refreshedRowValues = sankey.nodes();
  var tupleMaxBucketsIdx = null;

  var link = svg.append("g").selectAll(".link")
  			.data(graph.edges)
  			.enter().append("path")
  			.attr("class", "link")
  			.style("stroke", function(d){
  				var matchedTags = [];
  				
  				if (d.tags && selectedTags.length > 0 && layer !== "flow") {
  					var tags = d.tags;
  					/*
  					 * if this stream has multiple tags on it
  					 * and if the number of selectedTags is greater
  					 * than zero, find the matches
  					 */
  					tags.sort();
  					
  					tags.forEach(function(t){
  						selectedTags.forEach(function(sTag) {
  							if (t === sTag) {
  								matchedTags.push(sTag);
  							}
  						});
  					});
 
  					if (matchedTags.length > 0) {
  						if (matchedTags.length === 1) {
  							return d.color = color20(streamsTags[matchedTags[0]]);
  						} else {
  							// more than one tag is on this stream
  							return d.color = MULTIPLE_TAGS_COLOR;	
  						}
  						
  					} else {
  						return d.color = "#d3d3d3";
  					}
  				} else if (layer ==="flow" && (counterMetrics && counterMetrics.length > 0)) {
  					var tupleValue = parseInt(d.value, 10);
  					var derived = d.derived ? true : false;
  					var isZero = d.realValue === 0 && d.value === 0.45 ? true : false;
  					tupleBucketsIdx = getTupleCountBucketsIndex(counterMetrics, tupleValue, derived, isZero);
  					if (tupleMaxBucketsIdx === null) {
  						tupleMaxBucketsIdx = tupleBucketsIdx;
  					} else if (tupleBucketsIdx.buckets.length >= tupleMaxBucketsIdx.buckets.length) {
  						tupleMaxBucketsIdx = tupleBucketsIdx;
  					}

  					var myScale = d3.scale.linear().domain([0,tupleBucketsIdx.buckets.length -1]).range(tupleColorRange);
  					return d.color = myScale(tupleBucketsIdx.bucketIdx); 
  				} else {
  					// layer is not flow, but no stream tags available
  					return d.color = "#d3d3d3";
  				}
  				
  			})
  			.style("stroke-opacity", function(d){
  				if (d.tags && selectedTags.length > 0) {
  					// if the link has this color it is not the selected tag, make it more transparent
  					if (d.color === "#d3d3d3") {
  						return 0.2;
  					}
  				}
  			})
  			.attr("d", path)
  			.style("stroke-width", function(d) { 
  				return Math.max(1, Math.sqrt(d.dy));
  			 })
  			.sort(function(a, b) { return b.dy - a.dy; });

  // this is the hover text for the links between the nodes
  link.append("title")
      .text(function(d) {
    	  var value = format(d.value);
    	  if (d.derived) {
    		  value = "No value - counter not present";
    	  } else if (d.isZero) {
    		  value = "0";
    	  }
    	  var sKind = parseOpletKind(d.sourceIdx.invocation.kind);
    	  var tKind = parseOpletKind(d.targetIdx.invocation.kind);
    	  var retString = "Oplet name: " + d.sourceIdx.idx + "\nOplet kind: " + sKind + " --> \n"
    	  + "Oplet name: " + d.targetIdx.idx + "\nOplet kind: " + tKind;
    	  
    	  if (layerVal === "flow") {
    		  retString += "\n" + value; 
    	  }
    	  if (d.tags && d.tags.length > 0) {
    		  retString += "\nStream tags: " + d.tags.toString();
    	  }
    	  
    	  return retString;
    	  });
  
  var node = svg.append("g").selectAll(".node")
      .data(graph.vertices)
      .enter().append("g")
      .attr("class", "node")
      .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; })
      .call(d3.behavior.drag() 
      .origin(function(d) { return d; })
      .on("dragstart", function() { this.parentNode.appendChild(this); })
      .on("drag", dragmove));
      
      node.append(function(d) {
    	  if (d.invocation.kind.toUpperCase().endsWith("COUNTEROP")) {
    			return document.createElementNS(d3.ns.prefix.svg, 'rect');
    		} else {
    			return document.createElementNS(d3.ns.prefix.svg, 'circle');
    		}
      });
      
  	node.selectAll("circle")
  	.attr("cx", sankey.nodeWidth()/2)
  	.attr("cy", function(d){
	  return d.dy/2;
  	})
  	.attr("r", function(d){
	  return Math.sqrt(d.dy);
  	})
  	.style("fill", function(d) {
  		if (!colorMap[d.id.toString()]) {
  			colorMap[d.id.toString()] = color20(d.id.toString());
  		}
  		if (!opletColor[d.invocation.kind]) {
  			opletColor[d.invocation.kind] = color20(d.invocation.kind);
  		}
  		return getVertexFillColor(layer, d);  		
  	})
  	.attr("data-legend", function(d) {
  		return getLegendText(layer, d);
  	 })
  	.style("stroke", function(d) {
  		return getLegendColor(layer, d);

  	});
  	
  	node.selectAll("rect")
    .attr("x", sankey.nodeWidth()/2 )
    .attr("y", function(d) {
    	return d.dy/2 - 3;
    })
    .attr("width", 5)
    .attr("height", 5)
  	.style("fill", function(d) {
  		if (!colorMap[d.id.toString()]) {
  			colorMap[d.id.toString()] = color20(d.id.toString());
  		}
  		if (!opletColor[d.invocation.kind]) {
  			opletColor[d.invocation.kind] = color20(d.invocation.kind);
  		}
  		return getVertexFillColor(layer, d);  		
  	})
  	.attr("data-legend", function(d) {
  		return getLegendText(layer, d);
  	 })
  	.style("stroke", function(d) {
  		return getLegendColor(layer, d);

  	});
  
  	svg.selectAll("circle")
	.on("mouseover", function(d, i) {
  	  	var kind = parseOpletKind(d.invocation.kind);
		var headStr =  "<div><table style='table-layout:fixed;word-wrap: break-word;'><tr><th class='smaller'>Name</th>" +
			"<th class='smaller'>Oplet kind</th><th class='smaller'>Tuple count</th><th class='smaller'>Sources</th>" +
			"<th class='smaller'>Targets</th><th class='smaller'>Source stream tags</th><th class='smaller'>Target stream tags</th></tr>";
		var valueStr = "<tr><td class='smallCenter'>" + d.idx.toString() + "</td><td class='smallLeft'>" + kind + "</td><td class='smallRight'>" 
			+ formatNumber(d.value) + "</td>";

		var sources = [];
		var sourceStreams = [];
		d.targetLinks.forEach(function(trg){
			sources.push(trg.sourceIdx.idx.toString());
 	  		if (trg.tags && trg.tags.length > 0) {
   	  			sourceStreams = trg.tags;
   	  		}
		});
		var targets = [];
		var targetStreams = [];

		d.sourceLinks.forEach(function(src){
			targets.push(src.targetIdx.idx);
	   	  		if (src.tags && src.tags.length > 0) {
	   	  			targetStreams = src.tags;
	   	  		}
		});

		valueStr += "<td class='smallCenter'>" + sources.toString() + "</td>";
		valueStr += "<td class='smallCenter'>" + targets.toString() + "</td>";
		var sStreamString = sourceStreams.toString() === "" ? "None" : sourceStreams.toString();
		valueStr += "<td class='smallCenter'>" + sStreamString + "</td>";
		var tStreamString = targetStreams.toString() === "" ? "None" : targetStreams.toString();
		valueStr += "<td class='smallCenter'>" + tStreamString + "</td>";

		valueStr += "</tr></table></div>";
		var str = headStr + valueStr;
		showTooltip(str, d, i, d3.event);
	})
	.on("mouseout", function(d, i){
		hideTooltip(d, i);
	});
  	
  	svg.selectAll("rect")
  	.on("mouseover", function(d, i){
  		var kind = parseOpletKind(d.invocation.kind);
  		var headStr = "<div><table style='table-layout:fixed;word-wrap: break-word;'><tr><th class='smaller'>Name</th>" +
		"<th class='smaller'>Oplet kind</th></tr>";
  		var valueStr = "<tr><td class='smallCenter'>" + d.idx.toString() + "</td><td class='smallLeft'>" + kind + "</td></tr><table></div>";
  		var str = headStr + valueStr;
		showTooltip(str, d, i, d3.event);
  	})
  	.on("mouseout", function(d, i){
		hideTooltip(d, i);
	})
  	
  	node.append("text")
    .attr("x", function (d) {
        return - 6 + sankey.nodeWidth() / 2 - Math.sqrt(d.dy);
    })
    .attr("y", function (d) {
        return d.dy / 2;
    })
    .attr("dy", ".35em")
    .attr("text-anchor", "end")
    .attr("text-shadow", "0 1px 0 #fff")
    .attr("transform", null)
    .text(function (d) {
        return d.idx;
    })
    .filter(function (d) {
        return d.x < width / 2;
    })
    .attr("x", function (d) {
        return 6 + sankey.nodeWidth() / 2 + Math.sqrt(d.dy);
    })
    .attr("text-anchor", "start");

  function dragmove(d) {
    d3.select(this).attr("transform", "translate(" + d.x + "," + (d.y = Math.max(0, Math.min(height - d.dy, d3.event.y))) + ")");
    sankey.relayout();
    link.attr("d", path);
  }

  d3.selectAll(".legend").remove();
  
  if (layer === "opletColor"){
		 svgLegend
		  .append("g")
		  .attr("class","legend")
		  .attr("transform","translate(10,10)")
		  .style("font-size","11px")
		  .call(d3.legend, svg, null, "Oplet kind"); 
 }
  
  var showTagsChecked = $("#showTags").prop("checked");
  // add a second legend for tags, even if opletColor has been chosen
  if (tagsArray.length > 0 && layer !== "flow" && showTagsChecked) {
	  var tItems = getFormattedTagLegend(tagsArray);
	  if (!svgLegend.select("g").empty()) {
		  // get the dimensions of the other legend and append this one after it
		  var otherLegend = svgLegend.select("g")[0][0];
		  var translateY = otherLegend.getBBox().height + 10 + 10;
		  svgLegend
		  .append("g")
		  .attr("class","legend")
		  .attr("transform","translate(10," + translateY + ")")
		  .style("font-size","11px")
		  .call(d3.legend, svg, tItems, "Stream tags");
	  } else {
		  svgLegend
		  .append("g")
		  .attr("class","legend")
		  .attr("transform","translate(10,10)")
		  .style("font-size","11px")
		  .call(d3.legend, svg, tItems, "Stream tags");
  	}
  } else if (layer === "flow" && counterMetrics.length > 0) {
	  var bucketScale = d3.scale.linear().domain([0,tupleMaxBucketsIdx.buckets.length - 1]).range(tupleColorRange);
	  var flowItems = getFormattedTupleLegend(tupleMaxBucketsIdx, bucketScale);
	  legend = svgLegend
	  .append("g")
	  .attr("class","legend")
	  .attr("transform","translate(10,10)")
	  .style("font-size","11px")
	  .call(d3.legend, svg, flowItems, "Tuple count");
  } 
  if (bIsNewJob !== undefined) {
	  fetchAvailableMetricsForJob(bIsNewJob);
  } else {
	  fetchAvailableMetricsForJob();
  }
});
};

// update the metrics drop down with the metrics that are available for the selected job
var fetchAvailableMetricsForJob = function(isNewJob) {
    var selectedJobId = d3.select("#jobs").node().value;
    var queryString = "metrics?job=" + selectedJobId + "&availableMetrics=all";
    if (isNewJob !== undefined) {
    	metricsAvailable(queryString, selectedJobId, isNewJob);
    } else {
    	metricsAvailable(queryString, selectedJobId);
    }
};

var fetchMetrics = function() {
    // this makes a "GET" to the metrics servlet for the currently selected job
    var selectedJobId = d3.select("#jobs").node().value;
    var metricSelected = d3.select("#metrics").node().value;
    var queryString = "metrics?job=" + selectedJobId + "&metric=" + metricSelected;
    if (metricSelected !== "") {
    	metricFunction(selectedJobId, metricSelected);
    }
};

var fetchLineChart = function() {
	// the question is anything new, if it's not, then just keep refreshing what I have
	var jobId = d3.select("#jobs").node().value;
	var metricSelected = d3.select("#metrics").node().value;
	plotMetricChartType(jobId, metricSelected);
	
};

var jobMap = {};

var fetchJobsInfo = function() {
	// this makes a "GET" to the context path http://localhost:<myport>/jobs
	d3.xhr("jobs?jobsInfo=true",
	        function(error, data) {
	                if (error) {
	                        console.log("error retrieving job output " + error);
	                }
	                if (data) {
	                        var jobObjs = [];
	                        jobObjs = JSON.parse(data.response);
	                        var jobSelect = d3.select("#jobs");
	                        
	                        if (jobObjs.length === 0) {
	                                //no jobs were found, put an entry in the select
	                                // To Do: if the graph is real, remove it ...
	                                jobSelect
	                                .append("option")
	                                .text("No jobs were found")
	                                .attr("value", "none");
	                        }
	                        
	                        jobObjs.forEach(function(job){
	                                var obj = {};
	                                var jobId = "";
	                                var idText = "";
	                                var nameText = "";
	                                for (var key in job) {
	                                        obj[key] = job[key];
	                                        if (key.toUpperCase() === "ID") {
	                                                idText = "Job Id: " + job[key];
	                                                jobId = job[key];
	                                        }
	                                        
	                                        if (key.toUpperCase() === "NAME") {
	                                                nameText = job[key];
	                                        }
	                                        
	                                }
	                                if (nameText !== "" && !jobMap[jobId]) {
	                                        jobSelect
	                                        .append("option")
	                                        .text(nameText)
	                                        .attr("value", jobId);
	                                }
	                                if (!jobMap[jobId]) {
	                                        jobMap[jobId] = obj;
	                                }
	                });
	                        if(jobObjs.length > 0) {
	                                var pxStr = jobSelect.style("left");
	                                var pxValue = parseInt(pxStr.substring(0, pxStr.indexOf("px")), 10);
	                                var pos = pxValue + 7 + jobSelect.node().clientWidth;
	                                d3.select("#stateImg")
	                                .style("display", "block")
	                                .on('mouseover', function() {
	                                        showStateTooltip(d3.event);
	                                })
	                                .on('mouseout', function() {
	                                        hideStateTooltip();
	                                });
	                                
	                                stateTooltip = d3.select("body")
	                                .append("div")
	                                .style("position", "absolute")
	                                .style("z-index", "10")
	                                .style("display", "none")
	                                .style("background-color", "white")
	                                .attr("class", "bshadow");
	                                
	                                rowsTooltip = d3.select("body")
	                                .append("div")
	                                .style("position", "absolute")
	                                .style("z-index", "10")
	                                .style("display", "none")
	                                .style("background-color", "white")
	                                .attr("class", "bshadow");
	                                
	                                // check to see if a job is already selected and it's still in the jobMap object
	                                var jobId = d3.select("#jobs").node().value;
	                                var jobObj = jobMap[jobId];
	                                // otherwise set it to the first option value
	                                if (!jobObj) {
	                                        var firstValue = d3.select("#jobs").property("options")[0].value;
	                                        d3.select("#jobs").property("value", firstValue);
	                                }
	                        } else {
	                        	// don't show the state image
                                d3.select("#stateImg")
                                .style("display", "none");
	                        }
	        }
	});
	};

fetchJobsInfo();
var first = true;

var startGraph = function(restartInterval) {
	run = setInterval(function() {
			if (!stopTimer) {
				if (first) {
					fetchJobsInfo();
					first = false;
				}
				var selectedJob = d3.select("#jobs").node().value;
				getCounterMetricsForJob(renderGraph, selectedJob, first);
				if (propWindow) {
					displayRowsTooltip(false);
				}
			}
			
			
	}, restartInterval);
	if (restartInterval < refreshInt) {
		clearInterval(run);
		startGraph(refreshInt);
	}
	
};

startGraph(1000);

