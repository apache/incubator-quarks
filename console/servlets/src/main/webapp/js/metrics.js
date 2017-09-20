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
var margin = {top: 15, right: 20, bottom: 30, left: 40};
var metricCWidth = 860 - margin.left - margin.right;
var metricCWideWidth = 1160 - margin.left - margin.right;
var svgCounterPadding = 40;
var metricCHeight = 380 - margin.top - margin.bottom - svgCounterPadding;
var runLineChart = null;
var max_bucket_idx = null;

stopLineChart = function() {
    if (runLineChart) {
    	clearInterval(runLineChart);
    }
};

getCounterMetricsForJob = function(callback, jobId, bIsNewJob) {

	var queryString = "metrics?job=" + jobId + "&metric=name:Count,type:counter";
	var metricData = [];
		
		d3.xhr(queryString, function(error, responseData) {
		  if (error) {
			  console.log("error retrieving metrics");
		  }
		  if (responseData) {
			   var metData = JSON.parse(responseData.response);
			   
			   if (metData.ops && metData.ops.length >= 1) {
				   				   
				   var data = [];
			
				   var ops = metData.ops;
				   ops.forEach(function(op) {
					  var obj = {};
					  obj.opId = op.opId;
					  // this presumes each metric array is of length one .. is that always true?
					  var metrics = op.metrics;
					  metrics.forEach(function(met) {
						 obj.type = met.type;
						 obj.opId = obj.opId;
						 obj.name = met.name;
						 obj.value = met.value;
					  });
					  data.push(obj);
				   });
				   metricData = data;
				   if (bIsNewJob) {
					   clearTupleMaxBucketIdx();
				   }
			   }
			   callback(jobId, metricData, bIsNewJob);
		  }
		});
};

getTupleCountMaxAndMin = function(counterMetrics) {
	if (counterMetrics.length > 0) {
		
		var getValue = function(metric) {
			return parseInt(metric.value, 10);
		};
		var minMax = d3.extent(counterMetrics, getValue);
		minMax.min = minMax[0]; //minMax[0] === minMax[1] ? 1 : minMax[0];
		minMax.max = minMax[1];
		return minMax;
	}
	return null;
};

formatNumber = function(number) {
	return d3.format(",")(number);
};

getTupleCountBucketsIndex = function(counterMetrics, aValue, bIsDerivedValue, isZero) {

	var maxMin = getTupleCountMaxAndMin(counterMetrics);
	var min = maxMin.min;
	var max = maxMin.max;
	
	var fMin = formatNumber(maxMin.min);
	var fMax = formatNumber(maxMin.max);
	var returnObj = {};
	
	var buckets = [];
	var whichBucket = null;
	
	var diff = max - min;
	var mid = parseInt((max - min)/2 + min, 10);
	var minMid = parseInt(mid - min/2, 10);
	
	var fMinMid = formatNumber(minMid);
	var fMid = formatNumber(mid);
	var x = 0;
	
	if (diff <= 100) {
		if (bIsDerivedValue) {
			buckets[x] = {id: x, name: "Not applicable - counter not present"};
			x++;
		}
		buckets[x] = {id: x, name: "0"};
		x++;
		buckets[x] = {id: x, name: "1 - " + fMid};
		x++;
		if (max > mid) {
			buckets[x] = {id: x, name: formatNumber(mid + 1) + " - " + fMax};
		}
		if (bIsDerivedValue) {
			whichBucket = 0;
		} else if (isZero || aValue === 0) {
			whichBucket = 0;
		} else if (aValue >=1 && aValue <= (mid + 1)) {
			whichBucket = 1;
		} else {
			whichBucket = 2;
		}
	} else if (diff > 100 && diff <= 1000) {
		if (bIsDerivedValue) {
			buckets[x] = {id: x, name: "Not applicable - counter not present"};
			x++;
		}
		buckets[x] = {id: x, name: "0"};
		x++;
		buckets[x] = {id: x, name: "1 - " + fMinMid};
		x++;
		if (min === 0) {
			buckets[x] = {id: x, name: formatNumber(mid + 1) + " - " + fMax};
			x++;
		} else {
			buckets[x] = {id: x, name: formatNumber(minMid + 1) + " - " + fMid};
			x++;
			buckets[x] = {id: x, name: formatNumber(mid + 1) + " - " + fMax};
		}
		
		
		if (bIsDerivedValue) {
			whichBucket = 0;
		} else if (isZero || aValue === 0) {
			whichBucket = 0;
		} else if (aValue >=1 && aValue <= minMid) {
			whichBucket = 1;
		} else if (min === 0 && aValue >= mid + 1) {
			whichBucket = 2;
		} else if (aValue >= minMid + 1 && aValue <= mid) {
			whichBucket = 2;
		} else {
			whichBucket = 3;
		}
	} else if (diff > 1000) {
		if (bIsDerivedValue) {
			buckets[x] = {id: x, name: "Not applicable - counter not present"};
			x++;
		}
		buckets[x] = {id: x, name: "0"};
		x++;
		
		var quarter = parseInt(min + ((max - min)/4), 10);
		var fQuarter = formatNumber(parseInt(min + (max - min)/4, 10));

		var quarter3 = parseInt(min + ((max - min) * 0.75), 10);
		var fQuarter3 = formatNumber(parseInt(min + ((max - min) * 0.75), 10));

		buckets[x] = {id: x, name: formatNumber(min + 1) + " - " + fQuarter};
		x++;
		buckets[x] = {id: x, name: formatNumber(quarter + 1) + " - " + fMid};
		x++;
		buckets[x] = {id: x, name: formatNumber(mid + 1) + " - " + fQuarter3};
		x++;
		buckets[x] = {id: x, name: formatNumber(quarter3 + 1) + " - " + fMax};
		if (bIsDerivedValue) {
			whichBucket = 0;
		} else if (isZero === 0 || aValue === 0) {
			whichBucket = 0;
		} else if (aValue >= 1 && aValue <= quarter) {
			whichBucket = 1;
		} else if (aValue >= quarter + 1 && aValue <= mid) {
			whichBucket = 2;
		} else if (aValue >= mid + 1 && aValue <= quarter3) {
			whichBucket = 3;
		} else {
			whichBucket = 4;
		}
	}
	returnObj.bucketIdx = whichBucket;
	returnObj.buckets = buckets;
	
	if (max_bucket_idx === null) {
		setTupleMaxBucketIdx(returnObj);
	} else if (returnObj.buckets.length >= max_bucket_idx.buckets.length) {
		setTupleMaxBucketIdx(returnObj);
	}

	return returnObj;	
};

setTupleMaxBucketIdx = function(nVal) {
	max_bucket_idx = nVal;
}

getTupleMaxBucketIdx = function() {
	return max_bucket_idx;
};

clearTupleMaxBucketIdx = function() {
	max_bucket_idx = null;
};

metricFunction = function(selectedJobId, metricSelected, bUseWideWidth) {
	stopLineChart();
	// metricSelected, i.e: name:Count,type:counter
	var queryString = "metrics?job=" + selectedJobId + "&metric=" + metricSelected;
	
	var metricName = metricSelected.split(",")[0].split(":")[1];
	var metricType = metricSelected.split(",")[1].split(":")[1];
	
	var chartWidth = metricCWideWidth;
	
	// rangeRoundBands specifies the width of each bar 
	// and the distance between each bar (second arg) and
	// the distance at the ends, third argument
	var x = d3.scale.ordinal()
	    .rangeRoundBands([0, chartWidth - margin.left - 200], 0.1, 0.5);
	
	var y = d3.scale.linear()
	    .range([metricCHeight, 0]);
	
	var xAxis = d3.svg.axis()
	    .scale(x)
	    .orient("bottom");
	
	if (bUseWideWidth) {
		 xAxis.tickFormat("");
	  }
	
	var yAxis = d3.svg.axis()
	    .scale(y)
	    .orient("left")
	    .ticks(10, "");
	
	var svgCounter = d3.select("#metricsChart").append("svg")
	    .attr("width", chartWidth + margin.left + margin.right)
	    .attr("height", metricCHeight + margin.top + margin.bottom + svgCounterPadding)
	    .append("g")
	    .attr("transform", "translate(" + (margin.left + 80) +"," + margin.top + ")");
	
	d3.xhr(queryString, function(error, responseData) {
	  if (error) {
		  console.log("error retrieving metrics");
	  }
	  // remove the old metric chart
	  d3.select("#metricsChart").selectAll("*").remove();
	  
	  svgCounter = d3.select("#metricsChart").append("svg")
	   .attr("width", chartWidth + margin.left + margin.right)
	  .attr("height", metricCHeight + margin.top + margin.bottom + svgCounterPadding)
	  .append("g")
	  .attr("transform", "translate(" + (margin.left + 80) + "," + margin.top + ")");
	 
	   if (responseData) {
	   var metData = JSON.parse(responseData.response);
	   
	   if (metData.ops && metData.ops.length >= 1) {
		   var data = [];
	
		   var ops = metData.ops;
		   ops.forEach(function(op) {
			  var obj = {};
			  obj.opId = op.opId;
			  // this presumes each metric array is of length one .. is that always true?
			  var metrics = op.metrics;
			  metrics.forEach(function(met) {
				 obj.type = met.type;
				 obj.name = obj.opId;
				 obj.shortName = obj.opId.substring("OP_".length, obj.opId.length);
				 obj.value = met.value;
				 obj.metricName = met.name;
				 obj.opKind = vertexMap[obj.opId].invocation.kind;
			  });
			  data.push(obj);
		   });
		   
		   var sortData = function(objA, objB) {
        		 if (parseInt(objA.shortName) < parseInt(objB.shortName))  {
	         	    	return -1;
	         	     } else if (parseInt(objA.shortName) > parseInt(objB.shortName)){
	         	    	return 1;
	         	     } else {
	         	    	return 0;
	         	    }
		   };
		   
		   data.sort(sortData);
		   
	   x.domain(
			   data.map(
					   function(d) { 
						   // this is the value shown on the x axis tick marks
						   return  d.shortName; }));
	   
	   
	   var max = d3.max(data, function(d) {
		   if (d.type === "double") {
			   return parseFloat(d.value);
		   } else if (d.type === "long") {
			   return parseInt(d.value, 10);
		   } else {
			   return d.value;
		   }
		});
	   y.domain([0, max]);
	
	  svgCounter.append("g")
	      .attr("class", "x axis")
	      .attr("transform", "translate(0," + metricCHeight + ")")
	      .call(xAxis)
	      .append("text")
	      .attr("transform", "translate(" + (chartWidth - margin.left - 200)/2 + "," + svgCounterPadding + ")")
	      //.attr("dy", "1.2em") // this moves the text down relative to the axis
	      .style("text-anchor", "middle")
	      .style("font-size", "1.3em")
	      .text("Oplets");
	  
	  svgCounter.append("g")
	      .attr("class", "axis")
	      .call(yAxis)
	      .append("text")
	      .attr("transform", "rotate(-90)")
	      .attr("y", 6)
	      .attr("dy", ".9em")
	      .style("text-anchor", "end")
	      .style("font-size", "1.2em")
	      .data(data)
	      .text(function(d) {
	    	  if (d.metricName) {
	    		  return d.metricName;
	    	  }
	      });
	
	  var rect = svgCounter.selectAll(".foo")
	      .data(data)
	      .enter().append("rect")
	      .style("fill", function(d) {
	    		return opletColor[d.opKind];
	      })
	      .attr("x", function(d) { return x(d.shortName); })
	      .attr("width", x.rangeBand())
	      .attr("y", function(d) { 
	   	   if (d.type === "double") {
			   return y(parseFloat(d.value));
		   } else if (d.type === "long") {
			   return y(parseInt(d.value, 10));
		   } else {
			   return d.value;
		   }
	      })
	      .attr("height", function(d) { 
	   	   if (d.type === "double") {
			   return metricCHeight - y(parseFloat(d.value));
		   } else if (d.type === "long") {
			   return metricCHeight - y(parseInt(d.value, 10));
		   } else {
			   return d.value;
		   }
	      });
	  
	  rect.append("title")
      .text(function(d) {
    	  return "Name: " + d.shortName +
    	  "\nValue: " + d.value;
    	  });
	  
	   }
	   } 

	});

	function type(d) {
	  d.Count = +d.Count;
	  return d;
	}
	
};

plotMetricChartType = function(jobId, metricSelected) {
	var refreshT = 2500;
	var numPoints = 20;
	var n = 1;
	var data = [];
	var firstT = true;
    var queryString = "metrics?job=" + jobId + "&metric=" + metricSelected;
    var svgLineChart;
    var path;
    var line;
    var x;
    var yAxisScale;
    var yAxis;
    var clipPath;
        
    if (runLineChart) {
    	clearInterval(runLineChart);
    }
	
	if (firstT) {
		d3.select("#metricsChart").selectAll("*").remove();
	
		svgLineChart = d3.select("#metricsChart").append("svg")
		.attr("width", metricCWidth + margin.left + margin.right)
		.attr("height", metricCHeight + margin.top + margin.bottom + svgCounterPadding)
		.append("g")
		.attr("id", "lineChartG")
		.attr("transform", "translate(" + (margin.left + 80) + "," + margin.top + ")");
		
		
		svgLineChart.append("defs").append("clipPath")
	    .attr("id", "clip")
	    .append("rect")
	    .attr("width", metricCWidth)
	    .attr("height", metricCHeight);
		
		   x = d3.scale.linear()
		   .domain([0, numPoints - 1]) 
		    .range([0, metricCWidth - margin.left - 200]);

		   yAxisScale = d3.scale.linear()
		    .range([metricCHeight, 0]);
		   
		   d3.select("[class='x axis']").remove();

		   svgLineChart.append("g")
		    .attr("class", "x axis")
		    .attr("transform", "translate(0," + yAxisScale(0) + ")")
		    .call(d3.svg.axis().scale(x).orient("bottom"))
		    .append("text")
		    .attr("transform", "translate(" + (metricCWidth - margin.left -200)/2 + "," + svgCounterPadding + ")")
		      //.attr("dy", "1.2em") // this moves the text down relative to the axis
		    .style("text-anchor", "middle")
		    .style("font-size", "1.3em")
		    .text("Last 20 measures");

		
	} else {
		svgLineChart = d3.select("#lineChartG");
	}
   
	var getMetric = function(timeInt) {
		runLineChart = setInterval(function() {
			d3.xhr(queryString, function(error, responseData) {
		
			  if (error) {
				  console.log("error retrieving metrics");
			  }
			  if (responseData) {
				   var metData = JSON.parse(responseData.response);
		
				   if (metData !== "") {
			 
					   var ops = metData.ops;
					   ops.forEach(function(op) {
						  var obj = {};
						  obj.opId = op.opId;
						  // this presumes each metric array is of length one .. is that always true?
						  var metrics = op.metrics;
						  metrics.forEach(function(met) {
							 obj.type = met.type;
							 obj.name = obj.opId;
							 obj.value = met.value;
							 obj.metricName = met.name;
						  });

						  data.push(obj);
						  if (n >= numPoints) {
							 data.shift();
						  }
						  n++;
					   });
				   }
				   
				   var type = "long";
				   
				   var max = d3.max(data, function(d) {
					   if (d.type === "double") {
						   type = "double";
						 return parseFloat(d.value);
					   } else if (d.type === "long") {
						   type = "long";
						   return parseInt(d.value, 10);
					   } else {
						   return d.value;
					   }
					});
				   
				   var min = d3.min(data, function(d) {
					   if (d.type === "double") {
						 return parseFloat(d.value);
					   } else if (d.type === "long") {
						   return parseInt(d.value, 10);
					   } else {
						   return d.value;
					   }
					});
				   
				   if (min > 0) {
					   min -= 0.01 * min;
				   } 

				   if (type === "long") {
					   yAxisScale.domain([parseInt(min, 10), parseInt((max + max * 0.01), 10)]);
				   } else if (type === "double") {
					   yAxisScale.domain([parseFloat(min), parseFloat(max + max * 0.01)]);
				   }
				   
				   yAxis = d3.svg.axis()
				   .scale(yAxisScale)
				   .orient("left");
				   
				   // remove the old one, then add this one
				   d3.select("[class='y axis']").remove();
				   
				   svgLineChart.append("g")
				    .attr("class", "y axis")
				    .call(yAxis)
				    .append("text")
				    .attr("transform", "rotate(-90)")
				    .attr("y", 6)
				    .attr("dy", ".9em")
				    .style("text-anchor", "end")
				    .style("font-size", "1.2em")
				    .data(data)
				    .text(function(d) {
				    	if (d.metricName) {
				    		return d.metricName;
				    	}
				    });
				   
				   
				   line = d3.svg.line()
				    .x(function(d, i) {return x(i); })
				    .y(
				    		function(d, i) {
				    			
				    			if (d.type === "double") {
				    				return yAxisScale(parseFloat(d.value));
				    			} else if (d.type === "long") {
				    				return yAxisScale(parseInt(d.value, 10));
				    			} else {
				    				return yAxisScale(d.value);
				    			}
				    		});
			   if (!firstT) {
				   // first remove the old path, then add the new one
				   d3.select("#linePath").remove();

					   path = clipPath
				   		.append("path")
				   		.datum(data)
				   		.attr("id", "linePath")
				   		.attr("class", "line")
				   		.attr("d", line);
				 
				 
					   path
					   .attr("d", line)
					   .attr("transform", null)
					   .transition()
					   .duration(2500)
					   .ease("linear");
				   
			   }
			   if (timeInt < refreshT) {
				   clearInterval(runLineChart);
				   getMetric(refreshT);
			   }
			 }
		});
		}, refreshT);
	};
	
	var d = new Date();
	getMetric(100);
	clipPath = svgLineChart
		.append("g")
		.attr("clip-path", "url(#clip)");
	
	path = clipPath
		.append("path")
		.datum(data)
		.attr("id", "linePath")
		.attr("class", "line")
		.attr("d", line);
			
	firstT = false;
				     
};

/*
 * This returns only the name and type of the metric
 * i.e, name: Count, type: "counter" or type "meter"
 */
metricsAvailable = function(queryString, jobId, bIsNewJob) {
	d3.xhr(queryString, function(error, responseData) {
		  if (error) {
			  console.log("error retrieving available metrics");
		  }
		  if (!responseData) {
			  return;
		  }
		  if (responseData === "") {
			  return;
		  }
		   var metData = JSON.parse(responseData.response);

		   var ops = metData.ops;
		   var objectArray = [];
		   
		   // iterate over the metrics, getting the name to populate the drop down
           if (ops.length > 0) {
        	   // don't repeat the metric type, just show the metric once, and which operator has it
        	   var metricMap = {};
        	   
        	   ops.forEach(function(op) {
        		   var metrics = op.metrics;
        		   metrics.forEach(function(metric) {
        			   if (!metricMap[metric.name]) {
        				   metricMap[metric.name] = {"name": metric.name, "type": metric.type, "value": metric.value ? metric.value : null, "ops": []};
        				   metricMap[metric.name].ops.push(op.opId);
        				   objectArray.push(metricMap[metric.name]);
        			   } else {
        				   metricMap[metric.name].ops.push(op.opId);
        			   }
        		   });
        	   });
        	   
        	   var sortF = function(objA, objB) {
        		 if (objA.name < objB.name)  {
        	    	return -1;
        	     } else if (objA.name > objB.name) {
        	    	return 1;
        	     } else {
        	    	return 0;
        	    }
        	   };
        		
        		// there are metrics available
        	    if (objectArray && objectArray.length > 0) {
        	    	// make sure the flow option is available
             	   var flowOption = d3.select("#layers")
            	   .selectAll("option")
            	   	.filter(function (d, i){
            	   		return this.value === "flow";
            	   	});
             	   
             	   if (flowOption.property("disabled") === true) {
             		  flowOption.property("disabled", false);
             	   }

            	   
        	    	objectArray.sort(sortF);

        	       var metricsDiv = d3.select("#metricsDiv");
        	       if (metricsDiv.style("display") === "none") {
        	    	   metricsDiv.style("display", "block");
        	    	   // reset the chart type to bar
               	      d3.select("#mChartType").node().value = "barChart";
        	       }
        	    	// don't deselect the previously selected metric, but reset to bar charts
        	    	
        	    	var metricsSelect = d3.select("#metrics");
        	    	var previouslySelected = metricsSelect.node().value;
        	    	metricsSelect.selectAll("*").remove();
        	    	var selectWidth = 0;
        	    	var rateUnitVal = "";
        	    	var useWideWidth = false;
        	    	objectArray.forEach(function(obj) {
        	    		var tempWidth;
        	    		var value = "name:" + obj.name +",type:" + obj.type;
        	    		if (obj.name === "RateUnit" && obj.value) {
        	    			// get the value for the Rate Unit, append it for all "meter" types
        	    			rateUnitVal = obj.value;
        	    		} else {
        	    		   var opsText = "";
        	    		   var sortOps = function(opA, opB) {
        	          		 if (parseInt(opA.substring("OP_".length, opA.length)) < parseInt(opB.substring("OP_".length, opB.length)))  {
        	         	    	return -1;
        	         	     } else if (parseInt(opA.substring("OP_".length, opA.length)) > parseInt(opB.substring("OP_".length, opB.length))){
        	         	    	return 1;
        	         	     } else {
        	         	    	return 0;
        	         	    }
        	    		   };
        	    		   obj.ops.sort(sortOps);
        	    		   obj.ops.forEach(function (op){
        	    			   opsText += op.substring("OP_".length, op.length) + ",";
        	    		   });
        	    		   if (opsText.length > 100) {
        	    			   var splitOps = opsText.split(",");
        	    			   var scale = 100/opsText.length;
        	    			   var num = parseInt(scale * splitOps.length, 10);
        	    			   opsText = splitOps.splice(0,num).toString() + " ...";
        	    			   useWideWidth = true;
        	    		   } 

	        	           var opt = metricsSelect
	        	           .append("option")
	        	           .text(obj.name + ", oplets: " + opsText)
	        	           .attr("multipleops", obj.ops.length > 1 ? true : false)
	        	           .attr("value", "name:" + obj.name +",type:" + obj.type);
	        	           if (value === previouslySelected) {
	        	    			opt.attr("selected", true);
	        	    		}
	        	           // append to the span with the id of rateUnit the value
	        	           tempWidth = metricsSelect.node().clientWidth;
	        	           if (tempWidth > selectWidth) {
	        	        	   selectWidth = tempWidth;
	        	           }
        	    		}
        		   });
        	    	
        	    	var rateUnitSpan = d3.select("#rateUnit");
        	    	var selectedMetric = metricsSelect.node().value;
        	    	var isCounter = selectedMetric.split(":")[1].toUpperCase().indexOf("COUNT") !== -1;
        	    	if (rateUnitVal !== "" && !isCounter) {
        	    		rateUnitSpan.style("visibility", "visible");
        	    		var metricLeft = metricsSelect.style("left");
        	    		var left = parseInt(metricLeft, 10);
        	    		rateUnitSpan.style("margin-left", left + selectWidth + 10 + "px");
        	    		rateUnitSpan.text(rateUnitVal);
        	    		useWideWidth = false;
        	    	} else {
        	    		rateUnitSpan.style("visibility", "hidden");
        	    	}
        	    	
        	    	var isMultipleOpsSelected = metricsSelect
        	        .selectAll("option")
        	        .filter(function (d, i) { 
        	            return this.selected; 
        	        });
        	    	
        	    	var multiple = isMultipleOpsSelected.attr("multipleops");
        	    	var chartType = d3.select("#mChartType");
        	    	var selectedChart = chartType.node().value;
        	    	
        	    	var lineChartOption = chartType.selectAll("option")
	    			.filter(function (d, i){
	    				return this.value === "lineChart";
	    			});
        	    	var jobId = d3.select("#jobs").node().value;
        	    	if ( selectedChart === "barChart") {
        	    		metricFunction(jobId, metricsSelect.node().value, useWideWidth);
        	    		// if multiple is true, disable the linechart option
        	    		if (multiple === "true") {
        	    			lineChartOption.property("disabled", true);
        	    		} else {
        	    			lineChartOption.property("disabled", false);
        	    		}
        	    	} else {
        	    		// it's a line chart - if the metric selected has multiple oplets, deselect linechart and select bar chart
        	    		if (multiple === "true") {
        	    			stopLineChart();
        	    			lineChartOption.property("disabled", true);
        	    			d3.select("#mChartType").node().value = "barChart";
            	    		metricFunction(jobId, metricsSelect.node().value, useWideWidth);	
        	    		} else if (multiple === "false" && bIsNewJob && bIsNewJob === true) {
        	    			stopLineChart();
        	    			// restart it
        	    			d3.select("#mChartType").node().value = "lineChart";
        	    			var jobId = d3.select("#jobs").node().value;
        	    			var metricSelected = d3.select("#metrics").node().value;
        	    			plotMetricChartType(jobId, metricSelected);
        	    		} 
         	    	}
        	    	
        	    }
           } else {
        	   d3.select("#metricsDiv")
        	   .style("display", "none");
        	  // disable the "flow" option
        	   var layerSelect = d3.select("#layers");
        	   var selected = layerSelect.node().value;
        	   // if there are no metrics there are no tuple counts
        	   var flowOption = layerSelect
        	   .selectAll("option")
        	   	.filter(function (d, i){
        	   		return this.value === "flow";
        	   	});

        	   flowOption.property("disabled", true);
        	   if (selected === "flow") {
        		   // select opletType
        		   layerSelect.node().value = "opletColor";
        		   resetAll();  
        	   }
           }
           
	});
};
