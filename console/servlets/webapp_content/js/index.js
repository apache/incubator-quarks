/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
var layerVal = "flow";
var refreshInt = 5000;
var metricChartType = 'barChart';

var stopTimer = false;
var startGraph = null;
var run = null;
var refreshedRowValues = [];
var stateTooltip = null;
var rowsTooltip = null;

var tableMetrics = null;
var metricsTooltip = null;

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

var isRect = function(kind) {
    var k = kind.toUpperCase();
    return k.endsWith("COUNTEROP")
          || k.endsWith("STREAMSCOPE");
};

d3.select("#jobs")
.on("change", function() {
  tagsArray = [];
  streamsTags = {};

  resetAll(true);
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
		.attr("class", "start")
		.attr("title", "Resume graph")
	} else {
		stopTimer = false;
		d3.select(this).text("Pause graph");
		d3.select(this)
		.attr("class", "stop")
		.attr("title", "Pause graph");
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
	.attr("width", 340)
	.attr("transform", "translate(0," + 30 + ")")
  	.append("g")
  	.attr("width", 340)
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
	})
    .on('keydown', function() {
	if (d3.event.keyCode && d3.event.keyCode === 13) {
		displayRowsTooltip(true);
	} 
});

var showMetricsTimeout = null;

d3.select("#showMetricsTable")
	.on('mouseover', function() {
		showMetricsTooltip(d3.event);
	})
	.on('mouseout', function() {
		hideMetricsTooltip();
	})
	.on('keydown', function() {
		if (d3.event.keyCode && d3.event.keyCode === 13) {
			showMetricsTooltip(d3.event);
		} else if (d3.event.keyCode) {
			hideMetricsTooltip();
		}
	});

var showMetricsTooltip = function(event) {
	if (showMetricsTimeout) {
		clearTimeout(showMetricsTimeout);
	}
	var jobId = d3.select("#jobs").node().value;
	var content = "<div style='margin:10px; width: 300px; max-height: 300px;overflow-x: scroll;'>";
	
	var tableMetrics = new Array();
	
	var queryString = "metrics?job=" + jobId + "&getAllMetrics=true";
	var getEvent = function(){
		return event;
	}
	d3.xhr(queryString, function(error, responseData) {
		  if (error) {
			  console.log("error retrieving metrics");
		  }
		  if (responseData) {
			  if (responseData.response) {
				  var evt = getEvent();
				  tableMetrics = JSON.parse(responseData.response);
				  
					if (tableMetrics.ops.length === 0) {
						content+= "<span>There are no metrics to display.</span>";
					} else {
						var opMetrics = tableMetrics.ops;
						var countArr = new Array();
						var rateArr = new Array();
						opMetrics.forEach(function(op) {
							var metrics = op.metrics;
							var opId = op.opId;
							metrics.forEach(function(tm) {
								var rateIdx = tm["name"].toUpperCase().indexOf("RATE");
								// if it starts with Rate it is RateUnit
								if (opId) {
									tm.opId = opId;
								}
								if (rateIdx !== -1 && rateIdx !== 0) {
									rateArr.push(tm);
								} else if (rateIdx !== 0){
									countArr.push(tm);
								}
							});

						});
						
						var rowIdx = 0;
						content += "<table role='presentation'><caption>Counter oplet values</caption><tr><th tabindex=0>Operator name</th><th tabindex=0>Counter value</th></tr>";
						
						var sortFunc = function(objA, objB) {
							var a = objA.opId;
							var b = objB.opId;
							
							if (a < b) {
								return -1;
							} else if (a > b) {
								return 1;
							} else {
								return 0;
							}
						};
						
						var startTr = "<tr><td align='center' tabindex=0>";
						var startTd = "<td align='center' tabindex=0>";
						var startTdLeft = "<td align='left' tabindex=0>";
						var endTd = "</td>"
						var endTr = "</tr>";
						
						if (countArr.length > 0) {
							countArr.sort(sortFunc);

							countArr.forEach(function(counter) {
								rowIdx++;
	
								var opName = counter["opId"].substring("OP_".length, counter["opId"].length);
								content +=  startTr + opName + endTd;
	
								content += startTd + counter["value"] + endTd + endTr;	
							});
							
							content += "</table>";
						}
						
						if (rateArr.length > 0) {
							rateArr.sort(sortFunc);
							content += "<table role='presentation'><caption>Rate meter oplet values</caption><tr><th tabindex=0>Operator name</th><th tabindex=0>Rate type</th><th tabindex=0>Rate value</th></tr>";
							rateArr.forEach(function(rate) {
								rowIdx++;
								
								var opName = rate["opId"].substring("OP_".length, rate["opId"].length);
								content +=  startTr + opName + endTd;
								var rateName = rate["name"];
								if (rateName.toUpperCase().endsWith("RATE")) {
									rateName = rateName.substring(0, rateName.length - "RATE".length);
								}
								content += startTdLeft + rateName + endTd;
								var num = Number.parseFloat(rate["value"]);
								content += startTd + num.toFixed(4) + endTd + endTr;	
							});
							content += "</table>";
						}

					}
					content += "</div>";
					
					var evtX;
					var evtY;
					var target = evt.target;
					if (target) {
						if (target.x && target.y) {
							evtX = target.x;
							evtY = target.y;
						} else if (target.offsetLeft && target.offsetTop) {
							evtX = target.offsetLeft;
							evtY = target.offsetTop;
						}
					}

					metricsTooltip
					.html(content)
					.style("left", (evtX + 140) + "px")
					.style("top", evtY +"px")
					.style("padding-x", 22)
					.style("padding-y", 10)
					.style("display", "block");
					
					d3.select("#showMetricsTable").node().blur();
					metricsTooltip.node().focus();
					

					metricsTooltip
					.on("keydown", function() {
						// Escape key closes the popup
						if (d3.event.keyCode && d3.event.keyCode === 27) {
							hideMetricsTooltip();
						}
					});
					
					metricsTooltip
					.on("mouseover", function() {
						if (showMetricsTimeout) {
							clearTimeout(showMetricsTimeout);
						}
					});
					
					metricsTooltip
					.on("mouseout", function() {
						hideMetricsTooltip();
					});
				  
			  }
		  }
	});
};



var hideMetricsTooltip = function() {
	if (showMetricsTimeout) {
		clearTimeout(showMetricsTimeout);
	}
	showMetricsTimeout = setTimeout(function() {
			metricsTooltip
			.style("display", "none");
	}, 400);
};

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

var refreshTable = true;

var displayRowsTooltip = function(newRequest) {
	var rows = makeRows();
	var tableHdr = "";
	var content = "";
	var firstTime = true;
	var firstKey = true;
	var headerStr = "";
	
	for (var key in rows) {
		var row = rows[key];
		content += "<tr>";
		for (var newKey in row) {
			if (firstTime) {
				if (newKey === "Name") {
					headerStr += "<th style='width: 100px;' tabindex=0>" + newKey + "</th>";
				} else {
					headerStr += "<th style='width: 150px;' tabindex=0>" + newKey + "</th>";
				}
			}

			if (newKey === "Name" || newKey === "Tuple count" || newKey === "Oplet kind"){
				content += "<td class='center100' tabindex=0>" + row[newKey] + "</td>";
			} else {
				content += "<td class='left' tabindex=0 style='white-space:nowrap;'>" + row[newKey] + "</td>";
			}
		}
		firstTime = false;
		if (firstKey) {
			headerStr += "</tr>";
			firstKey = false;
		}
		content += "</tr>";
	}
	
	
	if (newRequest) {
		var htmlStr = "<html><head><title>Oplet properties</title><link rel='stylesheet' type='text/css' href='resources/css/main.css'></head>" + 
		"<body>";
		var buttonStr = '<button title="Pause table refresh" id="pauseTableRefresh" type="button">Pause table refresh</button>';
		var closeWinStr = '<button title="Close window" id="closeTablePropsWindow" type="button">Close window</button>';
		var tableHdr = "<table id='allPropsTable' tabindex=0 style='margin: 10px;table-layout:fixed;word-wrap: break-word;'><caption>Oplet properties</caption>";
		
		var str = htmlStr + buttonStr + closeWinStr + tableHdr + headerStr + content + "</table></body><html>";
		propWindow = window.open("", "Properties", "width=825,height=500,scrollbars=yes,dependent=yes");
		propWindow.document.body.innerHTML = "";
		propWindow.document.write(str);
		propWindow.document.body.focus();
		propWindow.onunload = function() {
			propWindow = null;
		};
		window.onunload = function() {
			if (propWindow) {
				propWindow.close();
			}
		};
		
		var btn = propWindow.document.getElementById("pauseTableRefresh");
		btn.onclick = 
			function() {
				 if (this.innerHTML === "Pause table refresh") {
					 this.innerHTML = "Resume table refresh";
					 this.title = "Resume table refresh";
					 refreshTable = false;
				 } else {
					 this.innerHTML = "Pause table refresh";
					 this.title = "Pause table refresh";
					 refreshTable = true;
				 }
		};
		
		var closeBtn = propWindow.document.getElementById("closeTablePropsWindow");
		closeBtn.onclick = 
			function() {
			if (propWindow) {
				propWindow.close();
			}
		};
	} else {
		if (refreshTable) {
			if (typeof(propWindow) === "object") {
				d3.select("#allPropsTable").innerHTML = content;
				propWindow.document.body.focus();
			}
		}
	}
};
var showStateTimeout = null;

var showStateTooltip = function(event) {
	if (showStateTimeout) {
		clearTimeout(showStateTimeout);
	}
	var jobId = d3.select("#jobs").node().value;
	var jobObj = jobMap[jobId];
	var content = "<div style='margin:10px'><table><caption>Job State</caption>";
	
	var rowPfx = "stateData";
	var startTd = "<td align='center' tabindex=0>";
	var endTd = "</td>"
	var endTr = "</tr>";
	
	var rowIdx = 0;

	for (var key in jobObj) {
		rowIdx++;
		content += "<tr>" + startTd;
		
		var idx = key.indexOf("State");
		var errIdx = key.indexOf("Error");
		
		if ( idx !== -1) {
			var name = key.substring(0, idx) + " " + key.substring(idx, key.length).toLowerCase();
			var val = jobObj[key];
			var value = val.substring(0,1) + val.substring(1,val.length).toLowerCase();
			content += name + endTd;
			content += "<td tabindex=0 id='" + rowPfx + rowIdx + "'>" + value + endTd + endTr;
		}
		
		if (errIdx !== -1) {
			var name = key.substring(0, errIdx) + " " + key.substring(errIdx, key.length).toLowerCase();
			var val = jobObj[key];
			var value = "";
			if (val) {
				value = val.substring(0,1) + val.substring(1,val.length).toLowerCase();
			}
			content += name + endTd;
			content += "<td tabindex=0 id='" + rowPfx + rowIdx + "'>" + value + endTd + endTr;
		} 
		
		if (idx === -1 && errIdx === -1) {
			content += key + endTd;
			content += "<td tabindex=0 id='" + rowPfx + rowIdx + "'>" + jobObj[key] + endTd + endTr;
		}

	}

	var evtX;
	var evtY;
	var target = d3.event.target;
	if (target) {
		if (target.x && target.y) {
			evtX = target.x;
			evtY = target.y;
		} else if (target.offsetLeft && target.offsetTop) {
			evtX = target.offsetLeft;
			evtY = target.offsetTop;
		}
	}

	content += "</div>";
	
	stateTooltip
	.html(content)
	.style("left", (evtX - 160) + "px")
	.style("top", evtY - 20 +"px")
	.style("padding-x", 22)
	.style("padding-y", 10)
	.style("display", "block");
	
	d3.select("#stateImg").node().blur();
	stateTooltip.node().focus();
	
	var lastNode = "#" + rowPfx + rowIdx;
	
	d3.select(lastNode)
	.on("keydown", function() {
		// the next tab closes the popup
		if (d3.event.keyCode && d3.event.keyCode === 9) {
			hideStateTooltip();
		}
	});


	stateTooltip
	.on("keydown", function() {
		// Escape key closes the popup
		if (d3.event.keyCode && d3.event.keyCode === 27) {
			hideStateTooltip();
		}
	});
	
	stateTooltip
	.on("mouseover", function() {
		if (showStateTimeout) {
			clearTimeout(showStateTimeout);
		}
	});
	
	stateTooltip
	.on("mouseout", function() {
		hideStateTooltip();
	});
	
};

var hideStateTooltip = function() {
	if (showStateTimeout) {
		clearTimeout(showStateTimeout);
	}
	
	stateTooltip.node().blur();
	// the focus needs to be put on the stateImg so that when the 
	// tooltip is closed, the loss of focus on that retains the tab order.
	// Now the next element to focus on is 'layers'
	d3.select("#stateImg").node().focus();
	
	showStateTimeout = setTimeout(function() {
		stateTooltip
		.style("display", "none");
	}, 400)
};

var makeRows = function() {
	var nodes = refreshedRowValues !== null ? refreshedRowValues : sankey.nodes();
	var theRows = [];
	nodes.forEach(function(n) {
		var sourceStreamTupleCountsMap = new Map();
		var sourceStreamAliasesMap = new Map();
		var sourceStreamTagsMap = new Map();
		n.targetLinks.forEach(function(trg) {
			var source = trg.sourceIdx.idx.toString();
			var sourceLinks = trg.sourceIdx.sourceLinks;
			for (var i = 0; i < sourceLinks.length; i++) {
				if (trg.sourceId == sourceLinks[i].sourceId && trg.targetId == sourceLinks[i].targetId) {
					sourceStreamTupleCountsMap.set(source, parseInt(sourceLinks[i].value));
					if (sourceLinks[i].hasOwnProperty("alias")) {
						sourceStreamAliasesMap.set(source, sourceLinks[i].alias);
					} else {
						sourceStreamAliasesMap.set(source, "");
					}
				}
			}

			if (trg.tags && trg.tags.length > 0) {
				sourceStreamTagsMap.set(source, trg.tags);
			} else {
				sourceStreamTagsMap.set(source, []);
			}
		});

		var targetStreamTupleCountsMap = new Map();
		var targetStreamAliasesMap = new Map();
		var targetStreamTagsMap = new Map();
		n.sourceLinks.forEach(function(src) {
			var target = src.targetIdx.idx.toString();
			var targetLinks = src.targetIdx.targetLinks;
			for (var i = 0; i < targetLinks.length; i++) {
				if (src.sourceId == targetLinks[i].sourceId && src.targetId == targetLinks[i].targetId) {
					targetStreamTupleCountsMap.set(target, parseInt(targetLinks[i].value));
					if (targetLinks[i].hasOwnProperty("alias")) {
						targetStreamAliasesMap.set(target, targetLinks[i].alias);
					} else {
						targetStreamAliasesMap.set(target, "");
					}
				}
			}

			if (src.tags && src.tags.length > 0) {
				targetStreamTagsMap.set(target, src.tags);
			} else {
				targetStreamTagsMap.set(target, []);
			}
		});

   	  	var kind = parseOpletKind(n.invocation.kind);
		var index = kind.indexOf("(");
		var kindName = kind.substring(0, index-1);
		var kindPkg = kind.substring(index);

   	  	var value = "";
   	  	if (n.derived === true) {
   	  		value = "Not applicable - counter not present";
   	  	} else if (n.realValue === 0 && value === 0.45) {
   	  		value = 0;
   	  	} else {
   	  		value = formatNumber(n.value);
   	  	}
   	  	
		var sStreamString = "";
		var inTupleCount = 0;
		if (sourceStreamAliasesMap.size == 0 && sourceStreamTagsMap.size == 0) {
			sStreamString = "None";
		} else {
			for (var [id, alias] of sourceStreamAliasesMap) {
				var tupleCount = sourceStreamTupleCountsMap.get(id);
				inTupleCount += parseInt(tupleCount);
				var tags = sourceStreamTagsMap.get(id);
				sStreamString += "[" + id + "] ";

				sStreamString += "<strong>tuples</strong>: " + formatNumber(tupleCount);

				if (alias != "") {
					sStreamString += ", <strong>alias</strong>: " + alias;
				}
				if (tags.length != 0) {
					sStreamString += ", <strong>tags</strong>: " + tags.join(", ");
				}

				sStreamString += "<br/>";
			}
		}

		var tStreamString = "";
		var outTupleCount = 0;
		if (targetStreamAliasesMap.size == 0 && targetStreamTagsMap.size == 0) {
			tStreamString = "None";
		} else {
			for (var [id, alias] of targetStreamAliasesMap) {
				var tupleCount = targetStreamTupleCountsMap.get(id);
				outTupleCount += parseInt(tupleCount);
				var tags = targetStreamTagsMap.get(id);
				tStreamString += "[" + id + "] ";

				tStreamString += "<strong>tuples</strong>: " + formatNumber(tupleCount);

				if (alias != "") {
					tStreamString += ", <strong>alias</strong>: " + alias;
				}
				if (tags.length != 0) {
					tStreamString += ", <strong>tags</strong>: " + tags.join(", ");
				}

				tStreamString += "<br/>";
			}
		}

   	  	var rowObj = {"Name": n.idx, "Oplet kind": kindName + "<br/>" + kindPkg,
   	  			"Tuple count": "In: " + formatNumber(inTupleCount) + "<br/>Out: " + formatNumber(outTupleCount),
   	  			"Source streams": sStreamString, "Target streams": tStreamString};
		theRows.push(rowObj);
	 });
	return theRows;
};

// Convert HSV to RGB
var hsvToRGB = function(h, s, v) {
	var r, g, b, i, f, p, q, t;
	if (arguments.length === 1) {
		s = h.s, v = h.v, h = h.h;
	}
	i = Math.floor(h * 6);
	f = h * 6 - i;
	p = v * (1 - s);
	q = v * (1 - f * s);
	t = v * (1 - (1 - f) * s);
	switch (i % 6) {
		case 0:
			r = v, g = t, b = p;
			break;
		case 1:
			r = q, g = v, b = p;
			break;
		case 2:
			r = p, g = v, b = t;
			break;
		case 3:
			r = p, g = q, b = v;
			break;
		case 4:
			r = t, g = p, b = v;
			break;
		case 5:
			r = v, g = p, b = q;
			break;
	}
	return {
		r: Math.round(r * 255),
		g: Math.round(g * 255),
		b: Math.round(b * 255)
	};
};

var componentToHex = function(c) {
	var hex = c.toString(16);
	return hex.length == 1 ? "0" + hex : hex;
};

// Convert RGB to Hex
var rgbToHex = function(r, g, b) {
	return "#" + componentToHex(r) + componentToHex(g) + componentToHex(b);
};

// Convert Hex to RGB
var hexToRGB = function(hex) {
	var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
	return result ? {
		r: parseInt(result[1], 16),
		g: parseInt(result[2], 16),
		b: parseInt(result[3], 16)
	} : null;
};

// Convert RGB to XYZ
var rgbToXYZ = function(r, g, b) {
	var _r = (r / 255);
	var _g = (g / 255);
	var _b = (b / 255);

	if (_r > 0.04045) {
		_r = Math.pow(((_r + 0.055) / 1.055), 2.4);
	} else {
		_r = _r / 12.92;
	}

	if (_g > 0.04045) {
		_g = Math.pow(((_g + 0.055) / 1.055), 2.4);
	} else {
		_g = _g / 12.92;
	}

	if (_b > 0.04045) {
		_b = Math.pow(((_b + 0.055) / 1.055), 2.4);
	} else {
		_b = _b / 12.92;
	}

	_r = _r * 100;
	_g = _g * 100;
	_b = _b * 100;

	var X = _r * 0.4124 + _g * 0.3576 + _b * 0.1805;
	var Y = _r * 0.2126 + _g * 0.7152 + _b * 0.0722;
	var Z = _r * 0.0193 + _g * 0.1192 + _b * 0.9505;

	return [X, Y, Z];
};

// Convert XYZ to LAB
var xyzToLAB = function(x, y, z) {
	var ref_X = 95.047;
	var ref_Y = 100.000;
	var ref_Z = 108.883;

	var _X = x / ref_X;
	var _Y = y / ref_Y;
	var _Z = z / ref_Z;

	if (_X > 0.008856) {
		_X = Math.pow(_X, (1 / 3));
	} else {
		_X = (7.787 * _X) + (16 / 116);
	}

	if (_Y > 0.008856) {
		_Y = Math.pow(_Y, (1 / 3));
	} else {
		_Y = (7.787 * _Y) + (16 / 116);
	}

	if (_Z > 0.008856) {
		_Z = Math.pow(_Z, (1 / 3));
	} else {
		_Z = (7.787 * _Z) + (16 / 116);
	}

	var CIE_L = (116 * _Y) - 16;
	var CIE_a = 500 * (_X - _Y);
	var CIE_b = 200 * (_Y - _Z);

	return [CIE_L, CIE_a, CIE_b];
};

// Compute Delta E using CIE94
var getDeltaE = function(x, y, isTextiles) {
	var x = {
		l: x[0],
		a: x[1],
		b: x[2]
	};
	var y = {
		l: y[0],
		a: y[1],
		b: y[2]
	};
	labx = x;
	laby = y;
	var k2;
	var k1;
	var kl;
	var kh = 1;
	var kc = 1;
	if (isTextiles) {
		k2 = 0.014;
		k1 = 0.048;
		kl = 2;
	} else {
		k2 = 0.015;
		k1 = 0.045;
		kl = 1;
	}

	var c1 = Math.sqrt(x.a * x.a + x.b * x.b);
	var c2 = Math.sqrt(y.a * y.a + y.b * y.b);

	var sh = 1 + k2 * c1;
	var sc = 1 + k1 * c1;
	var sl = 1;

	var da = x.a - y.a;
	var db = x.b - y.b;
	var dc = c1 - c2;

	var dl = x.l - y.l;
	var dh = Math.sqrt(da * da + db * db - dc * dc);

	return Math.sqrt(Math.pow((dl / (kl * sl)), 2) + Math.pow((dc / (kc * sc)), 2) + Math.pow((dh / (kh * sh)), 2));
};

// Generate a random color using the golden ratio conjugate
var genRandomColor = function(s, v) {
	var goldenRatioConjugate = 0.618033988749895;
	var h = Math.random();
	h += goldenRatioConjugate;
	h %= 1;
	var hsv = {
		h: h,
		s: s,
		v: v
	};
	var rgb = hsvToRGB(hsv.h, hsv.s, hsv.v);
	var hex = rgbToHex(rgb.r, rgb.g, rgb.b);
	return {
		hsv: hsv,
		rgb: rgb,
		hex: hex
	};
};

var checkDeltaE = function(deltaE) {
    return deltaE >= 15;
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


		graph.vertices.forEach(function(vertex){
			vertex.idx = parseInt(vertex.id.substring("OP_".length, vertex.id.length));
			if (!vertexMap[vertex.id]) {
				vertexMap[vertex.id] = vertex;
			}
		});
		
		var i = 0;
		graph.edges.forEach(function(edge) {
			// Store the real flow value so that we can access it in the static layer
			edge.flowValue = edge.value;

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

  var link = svg.append("g").selectAll(".link")
  			.data(graph.edges)
  			.enter().append("path")
  			.attr("class", "link")
  			.style("stroke", function(d){
  				var matchedTags = [];
  				
  				if (d.tags && selectedTags.length > 0) {
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
  							var color = color20(streamsTags[matchedTags[0]]);
  							return d.color =  color === "#c7c7c7" ? "#008080" : color;
  						} else {
  							// more than one tag is on this stream
  							return d.color = MULTIPLE_TAGS_COLOR;	
  						}
  						
  					} else {
  						return d.color = "#d3d3d3";
  					}
  				} else {
  					// layer is not flow, but no stream tags available
  					return d.color = "#d3d3d3";
  				}
  			})
  			.style("stroke-opacity", function(d){
  				if (d.tags && selectedTags.length > 0) {
  					// if the link has this color it is not the selected tag, make it more transparent
  					if (d.color === "#d3d3d3") {
  						return 0.6;
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
    	  if (d.alias) {
    		  retString += "\nStream alias: " + d.alias;
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
        	if (isRect(d.invocation.kind)) {
    			return document.createElementNS(d3.ns.prefix.svg, 'rect');
    		} else {
    			return document.createElementNS(d3.ns.prefix.svg, 'circle');
    		}
      });

	var assignedOpletColors = [];

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

  		// Generate a random color that is perceptually different than all assigned colors:
  		// 1. Convert the assigned oplet colors from Hex to LAB
  		// 2. Generate a random color in the RGB color space
  		// 3. Convert RGB to XYZ
  		// 4. Convert XYZ to LAB
  		// 5. For each assigned oplet color, compute Delta E between the two LAB colors (new and assigned)
  		// 6. If Delta E >= 15, consider the color to be different enough from the other colors
  		if (!opletColor[d.invocation.kind]) {
  	  		var assignedOpletColorsLAB = [];
  	  		for (var i = 0; i < assignedOpletColors.length; i++) {
  	  			var rgb = hexToRGB(assignedOpletColors[i]);
  	  			var xyz = rgbToXYZ(rgb.r, rgb.g, rgb.b);
  	  			var lab = xyzToLAB(xyz[0], xyz[1], xyz[2]);
  	  			assignedOpletColorsLAB.push(lab);
  	  		}

  	  		var deltaEs = [];
  	  		var c = null;
  	  		var uniqueColor = false;

  	  		while (!uniqueColor) {
  	  			// Use a different color scheme for non-org.apache.edgent defined oplets
  	  			if (d.invocation.kind.includes("org.apache.edgent")) {
  	  				c = genRandomColor(0.5, 0.95);
  	  			} else {
  	  				c = genRandomColor(0.99, 0.99);
  	  			}

  	  			var xyz = rgbToXYZ(c.rgb.r, c.rgb.g, c.rgb.b);
  	  			var lab = xyzToLAB(xyz[0], xyz[1], xyz[2]);

  	  			// Compare color to assigned colors and check for similarity
  	  			deltaEs = [];
  	  			for (var m = 0; m < assignedOpletColorsLAB.length; m++) {
  	  				deltaE = getDeltaE(lab, assignedOpletColorsLAB[m], false);
  	  				deltaEs.push(deltaE);
  	  			}
  	  			uniqueColor = deltaEs.every(checkDeltaE);
  	  		}

  			opletColor[d.invocation.kind] = c.hex;
  		}
  		
  		var color = getVertexFillColor(layer, d, counterMetrics);
  		assignedOpletColors.push(color);
  		return color;
  	
  	})
  	.attr("data-legend", function(d) {
  		return getLegendText(layer, d);
  	 })
  	.style("stroke", function(d) {
  		return getLegendColor(layer, d, counterMetrics);

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
  		return getVertexFillColor(layer, d, counterMetrics);  		
  	})
  	.attr("data-legend", function(d) {
  		return getLegendText(layer, d);
  	 })
  	.style("stroke", function(d) {
  		return getLegendColor(layer, d, counterMetrics);

  	});
  
  	svg.selectAll("circle")
	.on("mouseover", function(d, i) {
  	  	var kind = parseOpletKind(d.invocation.kind);
		var index = kind.indexOf("(");
		var kindName = kind.substring(0, index-1);
		var kindPkg = kind.substring(index);
		var headStr1 =  "<div style='width:100%;'><table style='width:100%;'><tr><th class='smaller'>Name</th>" +
			"<th class='smaller'>Oplet kind</th><th class='smaller'>Tuple count</th></tr>";
		var valueStr1 = "<tr><td class='smallCenter'>" + d.idx.toString() + "</td><td class='smallCenter'>" + kindName + "<br/>" + kindPkg +
			"</td><td class='smallCenter'>";

		var sourceStreamTupleCountsMap = new Map();
		var sourceStreamAliasesMap = new Map();
		var sourceStreamTagsMap = new Map();
		d.targetLinks.forEach(function(trg) {
			var source = trg.sourceIdx.idx.toString();
			var sourceLinks = trg.sourceIdx.sourceLinks;
			for (var i = 0; i < sourceLinks.length; i++) {
				if (trg.sourceId == sourceLinks[i].sourceId && trg.targetId == sourceLinks[i].targetId) {
					if (layer == "static") {
						sourceStreamTupleCountsMap.set(source, parseInt(sourceLinks[i].flowValue));
					} else {
						sourceStreamTupleCountsMap.set(source, parseInt(sourceLinks[i].value));
					}

					if (sourceLinks[i].hasOwnProperty("alias")) {
						sourceStreamAliasesMap.set(source, sourceLinks[i].alias);
					} else {
						sourceStreamAliasesMap.set(source, "");
					}
				}
			}

			if (trg.tags && trg.tags.length > 0) {
				sourceStreamTagsMap.set(source, trg.tags);
			} else {
				sourceStreamTagsMap.set(source, []);
			}
		});

		var targetStreamTupleCountsMap = new Map();
		var targetStreamAliasesMap = new Map();
		var targetStreamTagsMap = new Map();
		d.sourceLinks.forEach(function(src) {
			var target = src.targetIdx.idx.toString();
			var targetLinks = src.targetIdx.targetLinks;
			for (var i = 0; i < targetLinks.length; i++) {
				if (src.sourceId == targetLinks[i].sourceId && src.targetId == targetLinks[i].targetId) {
					if (layer == "static") {
						targetStreamTupleCountsMap.set(target, parseInt(targetLinks[i].flowValue));
					} else {
						targetStreamTupleCountsMap.set(target, parseInt(targetLinks[i].value));
					}

					if (targetLinks[i].hasOwnProperty("alias")) {
						targetStreamAliasesMap.set(target, targetLinks[i].alias);
					} else {
						targetStreamAliasesMap.set(target, "");
					}
				}
			}

			if (src.tags && src.tags.length > 0) {
				targetStreamTagsMap.set(target, src.tags);
			} else {
				targetStreamTagsMap.set(target, []);
			}
		});
		
		var sStreamString = "";
		var inTupleCount = 0;
		if (sourceStreamAliasesMap.size == 0 && sourceStreamTagsMap.size == 0) {
			sStreamString = "None";
		} else {
			for (var [id, alias] of sourceStreamAliasesMap) {
				var tupleCount = sourceStreamTupleCountsMap.get(id);
				inTupleCount += parseInt(tupleCount);
				var tags = sourceStreamTagsMap.get(id);
				sStreamString += "[" + id + "] ";

				sStreamString += "<strong>tuples</strong>: " + formatNumber(tupleCount);

				if (alias != "") {
					sStreamString += ", <strong>alias</strong>: " + alias;
				}
				if (tags.length != 0) {
					sStreamString += ", <strong>tags</strong>: " + tags.join(", ");
				}

				sStreamString += "<br/>";
			}
		}

		var tStreamString = "";
		var outTupleCount = 0;
		if (targetStreamAliasesMap.size == 0 && targetStreamTagsMap.size == 0) {
			tStreamString = "None";
		} else {
			for (var [id, alias] of targetStreamAliasesMap) {
				var tupleCount = targetStreamTupleCountsMap.get(id);
				outTupleCount += parseInt(tupleCount);
				var tags = targetStreamTagsMap.get(id);
				tStreamString += "[" + id + "] ";

				tStreamString += "<strong>tuples</strong>: " + formatNumber(tupleCount);

				if (alias != "") {
					tStreamString += ", <strong>alias</strong>: " + alias;
				}
				if (tags.length != 0) {
					tStreamString += ", <strong>tags</strong>: " + tags.join(", ");
				}

				tStreamString += "<br/>";
			}
		}

		valueStr1 += "In: " + formatNumber(inTupleCount) + "<br/>Out: " + formatNumber(outTupleCount) + "</td></tr></table>";

		var headStr2 =  "<table style='width:100%;'><tr><th class='smaller'>Source streams</th>" + "<th class='smaller'>Target streams</th></tr>";
		var valueStr2 = "<tr><td class='smallLeft'>" + sStreamString + "</td>";
		valueStr2 += "<td class='smallLeft'>" + tStreamString + "</td></tr></table></div>";

		var str = headStr1 + valueStr1 + headStr2 + valueStr2;
		showTooltip(str, d, i, d3.event);
	})
	.on("mouseout", function(d, i){
		hideTooltip(d, i);
	});
  	
  	svg.selectAll("rect")
  	.on("mouseover", function(d, i){
  		var kind = parseOpletKind(d.invocation.kind);
		var index = kind.indexOf("(");
		var kindName = kind.substring(0, index-1);
		var kindPkg = kind.substring(index);
  		var headStr = "<div><table style='table-layout:fixed;word-wrap: break-word;'><tr><th class='smaller'>Name</th>" +
		"<th class='smaller'>Oplet kind</th></tr>";
  		var valueStr = "<tr><td class='smallCenter'>" + d.idx.toString() + "</td><td class='smallCenter'>" + kindName + "<br/>" + kindPkg + "</td></tr><table></div>";
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
  
  if (layer === "flow" && counterMetrics.length > 0) {
	  var maxBucketIdx = getTupleMaxBucketIdx();
	  var bucketScale = d3.scale.linear().domain([0,maxBucketIdx.buckets.length - 1]).range(tupleColorRange);
	  var flowItems = getFormattedTupleLegend(maxBucketIdx, bucketScale);
	  legend = svgLegend
	  .append("g")
	  .attr("class","legend")
	  .attr("transform","translate(10,10)")
	  .style("font-size","11px")
	  .call(d3.legend, svg, flowItems, "Tuple count");
  } 
  
  var showTagsChecked = $("#showTags").prop("checked");
  // add a second legend for tags, even if opletColor has been chosen
  if (tagsArray.length > 0  && showTagsChecked) {
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
    	metricFunction(selectedJobId, metricSelected, true);
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
	                                })
	                                .on('keydown', function() {
	                                	if (d3.event.keyCode && d3.event.keyCode === 13) {
	                                		showStateTooltip(d3.event);
	                                	} else if (d3.event.keyCode) {
	                                		hideStateTooltip();
	                                	}
	                                });
	                                
	                                stateTooltip = d3.select("body")
	                                .append("div")
	                                .style("position", "absolute")
	                                .style("z-index", "10")
	                                .style("display", "none")
	                                .style("background-color", "white")
	                                .attr("class", "bshadow")
	                                .attr("tabindex", "0");
	                                
	                                rowsTooltip = d3.select("body")
	                                .append("div")
	                                .style("position", "absolute")
	                                .style("z-index", "10")
	                                .style("display", "none")
	                                .style("background-color", "white")
	                                .attr("class", "bshadow");
	                                
	                                metricsTooltip = d3.select("body")
	                                .append("div")
	                                .style("position", "absolute")
	                                .style("z-index", "10")
	                                .style("display", "none")
	                                .style("background-color", "white")
	                                .attr("class", "bshadow")
	                                .attr("tabindex", "0");
	                                
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

