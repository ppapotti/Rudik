<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!-- Template by quackit.com -->
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Rule Miner Console</title>
<spring:url value="/resources/csss/styleSheet.css" var="styleSheetClass" />
<spring:url value="/resources/images" var="images" />
<link href="${styleSheetClass}" rel="stylesheet" />
<script type="text/javascript"
	src="https://vkbeautify.googlecode.com/files/vkbeautify.0.99.00.beta.js"></script>
<script type="text/javascript"
	src="https://code.jquery.com/jquery-1.10.1.min.js"></script>
<link rel="stylesheet" href="//code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css">
	<script src="//code.jquery.com/ui/1.10.4/jquery-ui.js"></script>



<script type="text/javascript">
var idx=0;
var examples = [];
var rules = [];

	$(document).ready(function(){
		$("#dialogBox").dialog({
            modal: true,
            autoOpen: false,
            title: "Rule specific example ",
            show : "blind", 
            hide : "blind",
            width: "auto"
        });
	});
	
   	$(document).ready(function(){
	$("#satisfyRuleDialog").dialog({
	          modal: true,
	          autoOpen: false,
	          title: "Satisfying Sets",
	          show : "blind", 
	          hide : "blind",
	          width: "auto"
	      });
	      });
	      
   	$(document).ready(function(){
	$("#violatingRuleDialog").dialog({
	          modal: true,
	          autoOpen: false,
	          title: "Violating Sets",
	          show : "blind", 
	          hide : "blind",
	          width: "auto"
	      });
	      });

	$( function() {
		$("#accordion").accordion({
		    collapsible: true,
		    autoHeight: false,
		    navigation: true,
		    heightStyle: "content"
		});
	  } );
	  
	// Code for Drawing Tables
      var json2html = (function () {
      
          var json2html = function(json) {
              this.data = json;
              //this.rgh  = json["row group headers"];
              this.ch   = json["Column_Headers"];
              this.dft  = json["Data_For_Table"];
              //this.cus  = json["Columns under subColumns"];
              this.sc   = 1;

              var depth = 0;
              for (var i in this.ch) {
                  depth = Math.max(depth, this.ch[i].length);
              }
              this.depth = depth;
          }

          function repeat(pattern, count) {
              var result = pattern;
              while (--count > 0) {
                  result += pattern;
              }
              return result;
          }

          function join(data, td) {
              try {
                  return td + data.join('</td>' + td) + '</td>';
              } catch (e) {
                  return td + data + '</td>';
              }
          }

          function renderSubHeader(data, index, sc) {
              var html = '';
              $.each(data, function() {
                  var cs = sc;
                  for (var i = index + 1; i < this.length; i++) {
                      cs *= this[i].length;
                  }
                  var value = (typeof this[index] != 'undefined') ? this[index] : '';
                  var cell = join(value, '<td class="colHeaders"' + ((cs > 1) ? ' colspan="'+cs+'">' : '>'));
                  if (index > 1) {
                      for (var i = index - 1; i > 0; i--) {
                          html += repeat(cell, this[i].length);
                      }
                  } else {
                      html += cell;
                  }
              });
              return(html);
          }

          function renderHeader(data) {
              var html = '<tr>';
              //html += join(data.rgh, '<th rowspan="'+(data.depth + 1)+'" class="rowLabel">');
              html += renderSubHeader(data.ch, 0, data.sc);
              html += '</tr>';
              for (var index = 1; index < data.depth; index++) {
                  html += '<tr>';
                  html += renderSubHeader(data.ch, index, data.sc);
                  html += '</tr>';
              };
              return html;
          }

          /*function renderColHeader(data) {
              var html = '<tr>';
              $.each(data.dft[0].data, function(index) {
                  html += join(data.cus, '<td class="bottomLabs">');
              });
              return html+'</tr>';
          }*/

          function renderData(data) {
              var html = '';
              $.each(data.dft, function(nr) {
                  html += '<tr>';
                  //html += join(this.name, '<td class="rowHeader">');
                  $.each(this.data, function() {
	                  html += join(this, '<td class="tdData">');
                  });
                  html += '</tr>';
              });
              //alert("html -"+html);
              return html;
          }

          function mergeCells(cells, attr) {
              var rs = 1;
              var old = null;
              cells.each(function() {
                  if (old == null) {
                      old = $(this);
                      rs = 1;
                  } else {
                      if ($(this).text() == old.text()) {
                          rs++;
                          $(this).remove();
                      } else {
                          if (rs > 1) {
                              old.attr(attr, rs);
                              rs = 1;
                          }
                          old = $(this);
                      }
                  }
              });
              if (rs > 1) {
                  old.attr(attr, rs);
              }
          }

          json2html.prototype.renderTable = function(thead, tbody) {
              var startTime = new Date();
              thead.html(
                  renderHeader(this) 
                  //renderColHeader(this)
              );
              tbody.html(renderData(this));
              /* for (var i = this.rgh.length; i > 0; i--) {
                  mergeCells($('td:nth-child('+i+')', tbody), 'rowspan');
              }; */
              var endTime = new Date();
              console.log('renderTable('+this.dft.length+' rows): ' + (endTime - startTime) + 'ms');
          }

          return json2html;
      })();
	// Code for drawing tale ends here !!!!!

	function openPopUp()
	{
		 //$('#exmplDialog').val(examples[currId.substring(currId.length-1,currId.length)]);
		 
		 var data1 = {
          "Column_Headers": [
              ["Associate-Band", ["subject", "object"] ], 
              ["Associate-Artist", ["v0", "v1"] ],
              ["Associate-Band", ["subject", "v0"] ]
          ],
          "Data_For_Table": [
	          {"data": ["tests","test2","test34","test5","test3","test9"] }
	          //{"data": [[0,1],[1,2],[45,20],[0,1],[1,2],[423232323,20]] },
	          //{"data": [[0,1],[1,2],[45,20],[0,1],[1,2],[232323232323,20]] }
          ]
          };
          
		var data2 = {
		"Column_Headers": [
			["activeYearsStartDate", ["subject", "v0"]], 
			["successor", ["object", "subject"]], 
			["activeYearsEndDate", ["subject", "v0"]]
		],
			"Data_For_Table": [
				{"data": ["John_White_(Kentucky_politician)", "XMLSchema#date", "Robert_M._T._Hunter", "John_White_(Kentucky_politician)", "John_White_(Kentucky_politician)", "XMLSchema#date"]}, 
				{"data": ["John_Hickenlooper", "XMLSchema#date", "Wellington_Webb", "John_Hickenlooper", "John_Hickenlooper", "XMLSchema#date"]}, 
				{"data": ["Giuseppe_Pella", "XMLSchema#date", "Giuseppe_Saragat", "Giuseppe_Pella", "Giuseppe_Pella", "XMLSchema#date"]}
			]
		};

          
			$('#execRuleTarget thead').empty();
			$('#execRuleTarget tbody').empty();	
			var html = new json2html(data2);
			//alert(html);
			html.renderTable($('#execRuleTarget thead'), $('#execRuleTarget tbody'));
           	$('#execRuleTarget').fadeIn('slow');
            
          dialogBoxTitle = "Satisfying Sets"
          $( "#satisfyRuleDialog" ).dialog( "open" );
          
	}
	
	function openModalWindowForRule(currId,value)
	{
		 $('#exmplDialog').val(examples[currId.substring(currId.length-1,currId.length)]);
		 $( "#dialogBox" ).dialog( "open" );
	}
	
	function updateDefaultConfig()
	{
		//openPopUp();
		SmartFieldHandle();
		document.getElementById('loader').style.visibility = "visible";
		var url = '/rule_miner/webapi/RuleMiner';
		var dataString = "";
		$.ajax({
				type : 'GET',
				url : url,
				contentType : 'text/plain',
				data : dataString,
				success : function(opData) {
					var resultJSON = opData;
					var result = $.parseJSON(resultJSON);
					$.each(result, function(k, v) {
					    var key = "#"+k;
					    $(key).val(v);
					    if(key == "#alphaR")
					    	$('#alphaROp').val(v);
				    	if(key == "#betaR")
				    		$('#betaROp').val(v);
				    	if(key == "#gammaR")
				    		$('#gammaROp').val(v);
					});
					
					$('#RuleMinerRules_response').val("Default config updated !!!");
				},
				complete : function(res){
						document.getElementById('loader').style.visibility = "hidden";
						enableEntireForm();
				}
			});
		
	}

	function disableEntireForm() {
	    var inputs = document.getElementsByTagName("input");
	    for (var i = 0; i < inputs.length; i++) {
	    	inputs[i].disabled = true;
	    }
	    var selects = document.getElementsByTagName("select");
	    for (var i = 0; i < selects.length; i++) {
	    	selects[i].disabled = true;
	    }
	    var textareas = document.getElementsByTagName("textarea");
	    for (var i = 0; i < textareas.length; i++) {
	    	textareas[i].disabled = true;
	    }
	    var buttons = document.getElementsByTagName("button");
	    for (var i = 0; i < buttons.length; i++) {
	    	buttons[i].disabled = true;
	    }
	}
	
	function enableEntireForm() {
	    var inputs = document.getElementsByTagName("input");
	    for (var i = 0; i < inputs.length; i++) {
	    	inputs[i].disabled = false;
	    }
	    var selects = document.getElementsByTagName("select");
	    for (var i = 0; i < selects.length; i++) {
	    	selects[i].disabled = false;
	    }
	    var textareas = document.getElementsByTagName("textarea");
	    for (var i = 0; i < textareas.length; i++) {
	    	textareas[i].disabled = false;
	    }
	    var buttons = document.getElementsByTagName("button");
	    for (var i = 0; i < buttons.length; i++) {
	    	buttons[i].disabled = false;
	    }
	}
	
	function SmartFieldHandle()
	{
		if((document.getElementById('useSmartSampling').checked))
		{
			document.getElementById('alphaSmart').style.visibility = "visible";
			document.getElementById('alphaSmartLabel').style.visibility = "visible";
			document.getElementById('betaSmart').style.visibility = "visible";
			document.getElementById('betaSmartLabel').style.visibility = "visible";
			document.getElementById('gammaSmart').style.visibility = "visible" ;
			document.getElementById('gammaSmartLabel').style.visibility = "visible" ;
			document.getElementById('subWeight').style.visibility = "visible" ;
			document.getElementById('subWeightLabel').style.visibility = "visible" ;
			document.getElementById('objWeight').style.visibility = "visible" ;
			document.getElementById('objWeightLabel').style.visibility = "visible" ;
			document.getElementById('topK').style.visibility = "visible" ; 
			document.getElementById('isTopKLabel').style.visibility = "visible" ; 
		}
		else
		{
			document.getElementById('alphaSmart').style.visibility = "hidden";
			document.getElementById('alphaSmartLabel').style.visibility = "hidden";
			document.getElementById('betaSmart').style.visibility = "hidden";
			document.getElementById('betaSmartLabel').style.visibility = "hidden";
			document.getElementById('gammaSmart').style.visibility = "hidden" ;
			document.getElementById('gammaSmartLabel').style.visibility = "hidden" ;
			document.getElementById('subWeight').style.visibility = "hidden" ;
			document.getElementById('subWeightLabel').style.visibility = "hidden" ;
			document.getElementById('objWeight').style.visibility = "hidden" ;
			document.getElementById('objWeightLabel').style.visibility = "hidden" ;
			document.getElementById('topK').style.visibility = "hidden" ; 
			document.getElementById('isTopKLabel').style.visibility = "hidden" ; 
			
		}
		
	}
	function updateRuleStatus(){
		document.getElementById('loader').style.visibility = "visible";
		disableEntireForm();
		var url = '/rule_miner/webapi/RuleMiner/UpdateResult';
		var ct;
		var ruleValidVal = [] ;
		for(ct=0;ct<idx;ct++)
		{
			var radios = document.getElementsByName("radioVal"+ct);
			for (var i = 0, length = radios.length; i < length; i++) {
			    if (radios[i].checked) {
			        ruleValidVal[ct] = radios[i].value;
			        break;
			    }
			}
		}
		var dataString = {
				"rules": rules,
				"updateValidVal": ruleValidVal
		}
		$.ajax({
				type : 'POST',
				url : url,
				contentType : 'application/json',
				data : JSON.stringify(dataString),
				success : function(opData) {
					$('#exmplDialog').val(opData);
					$("#dialogBox").dialog("open");
				},
				complete : function(res){
						document.getElementById('loader').style.visibility = "hidden";
						enableEntireForm();
				}
			});
	}	
	function ajaxExecuteRule(currBtnId,currBtnVal,ExmplSet){
		document.getElementById('loader').style.visibility = "visible";
		disableEntireForm();
		var url = '/rule_miner/webapi/RuleMiner/ExecuteRule';
		var dataString = {
				"exmplSet": ExmplSet,
				"ruleStr": rules[currBtnId.substring(currBtnId.length-1,currBtnId.length)],
				"alpha": $("#alphaR").val(),
				"beta": $("#betaR").val(),
				"gamma": $("#gammaR").val(),
				"maxNoRule": $("#maxNoRule").val(),
				"noOfThreads": $("#noOfThreads").val(),
				"kBase": $("#kBase").val(),
				"typeOfSubject": $("#subType").val(),
				"typeOfObject": $("#objType").val(),
				"relName": $("#relName").val(),
				"edgeLimit": $("#edgeLimit").val(),
				"genLimit": $("#genLimit").val(),
				"genNegRules": document.getElementById('genNegRules').checked,
				"useSmartSampling": document.getElementById('useSmartSampling').checked,
				"alphaSmart": $("#alphaSmart").val(),
				"betaSmart": $("#betaSmart").val(),
				"gammaSmart": $("#gammaSmart").val(),
				"subWeight": $("#subWeight").val(),
				"objWeight": $("#objWeight").val(),
				"topK": document.getElementById('topK').checked
		};
		
		$.ajax({
				type : 'POST',
				url : url,
				contentType : 'application/json',
				data : JSON.stringify(dataString),
				success : function(opRuleStr) {
				
				var resultPostJSON = opRuleStr;
				var resultExecute = $.parseJSON(resultPostJSON);
						
				$('#execRuleTarget thead').empty();
				$('#execRuleTarget tbody').empty();	
				var html = new json2html(resultExecute);
				html.renderTable($('#execRuleTarget thead'), $('#execRuleTarget tbody'));
	          	$('#execRuleTarget').fadeIn('slow');
	          	
	          	if(ExmplSet == 'V')
		         	$( "#violatingRuleDialog" ).dialog( "open" );
				else
					$( "#satisfyRuleDialog" ).dialog( "open" );
									
			  	document.getElementById('loader').style.visibility = "hidden";
				enableEntireForm();
				},
				complete : function(res){
						document.getElementById('loader').style.visibility = "hidden";
						enableEntireForm();
				}
			});
	}	
	
	
	function ajaxPostRuleMinerItem() {
		document.getElementById('loader').style.visibility = "visible";
		disableEntireForm();
		var url = '/rule_miner/webapi/RuleMiner';
		var dataString = {
				"alpha": $("#alphaR").val(),
				"beta": $("#betaR").val(),
				"gamma": $("#gammaR").val(),
				"maxNoRule": $("#maxNoRule").val(),
				"noOfThreads": $("#noOfThreads").val(),
				"kBase": $("#kBase").val(),
				"typeOfSubject": $("#subType").val(),
				"typeOfObject": $("#objType").val(),
				"relName": $("#relName").val(),
				"edgeLimit": $("#edgeLimit").val(),
				"genLimit": $("#genLimit").val(),
				"genNegRules": document.getElementById('genNegRules').checked,
				"useSmartSampling": document.getElementById('useSmartSampling').checked,
				"alphaSmart": $("#alphaSmart").val(),
				"betaSmart": $("#betaSmart").val(),
				"gammaSmart": $("#gammaSmart").val(),
				"subWeight": $("#subWeight").val(),
				"objWeight": $("#objWeight").val(),
				"topK": document.getElementById('topK').checked
		};
		
		$.ajax({
				type : 'POST',
				url : url,
				contentType : 'application/json',
				data : JSON.stringify(dataString),
				success : function(opData) {
					$('#RuleMinerItem_response').html(opData);
					if(opData == "No rules identified with current configuration parameters")
					{
						$('#exmplDialog').val(opData);
					  	$( "#dialogBox" ).dialog( "open" );
					  	document.getElementById('loader').style.visibility = "hidden";
						enableEntireForm();
					}
					else
					{
						var resultPostJSON = opData;
						var resultPost = $.parseJSON(resultPostJSON);
							
						$.each(resultPost, function (key, value) {
						  if(key == "rows")
						  {
							  $.each(value, function (innerKey, innerVal) {
							  	$.each(innerVal, function (innerKey1, innerVal1) {
							  		
							  		
									if(((innerKey1.substring(0, 11)).localeCompare("CovExamples")) == 0)
									{
										examples[idx]=innerVal1;
									}
									if(((innerKey1.substring(0, 6)).localeCompare("RuleID")) == 0)
									{
										rules[idx] = innerVal1;
								  		$('div#foo').append('<tr><td width="50%"><a href= "#" onClick="return openModalWindowForRule(this.id,idx);" id= "diag'+idx+ '" >'+ innerVal1 + '</a></td>'
										    + '<td width="20%" ><div class="post-date"><input type="radio" id="radioVal'+idx+'" name="radioVal'+idx+'"  value="yes">Valid  </input>' 
										    + '<input type="radio" id="radioVal'+idx+'" name="radioVal'+idx+'" align="right" value="no">Invalid</input></div></td>'
										    + '<td width="30%" align="right"><button class="button button1" id="satfButton'+idx+ '" type="button" onclick="return ajaxExecuteRule(this.id,idx,\'S\');">Satisfying</button></td>'
										    + '<td width="30%" align="right"><button class="button button1" id="violButton'+idx+ '" type="button" onclick="return ajaxExecuteRule(this.id,idx,\'V\');">Violating</button></td></tr>');
									}
							  	
							  	});
							  	idx++
							  });
						  }
						  if(key == "Gen_Samples")
						  {
						  		$('div#foo1').append('<table width="100%"></table>');
						  		var table = $('div#foo1').children(); 
					  		 	$.each(value, function (genExKey, genExValue) {
								  	var res = genExValue.split("~~");
								  	table.append('<tr><td width="50%">'+ res[0] + '</td><td width="50%">'+ res[1] + '</td></tr>');
						  		}); 
						  }
						});
					}

				},
				complete : function(res){
						document.getElementById('loader').style.visibility = "hidden";
						enableEntireForm();
				}
			});
	}
	</script>

<script type="text/javascript">


</script>

</head>

<body onload="updateDefaultConfig()">
	<div id="dialogBox" title="My Dialog Title">
		<table width="100%" height="100%">
			<tr height="100%">
				<td width="100%"><textarea style="margin: 0px; width: 1300px;"
						id="exmplDialog"></textarea></td>
			</tr>
		</table>
	</div>

	<div id="satisfyRuleDialog" title="My Dialog Title">
		<table id="execRuleTarget" class="tableClass">
			<thead>
			</thead>
			<tbody>
			</tbody>
		</table>
	</div>
	
	<div id="violatingRuleDialog" title="My Dialog Title">
		<table id="execRuleTarget" class="tableClass">
			<thead>
			</thead>
			<tbody>
			</tbody>
		</table>
	</div>

	<table width="100%" height="100%">
		<tr>
			<td colspan="6" class="headerClass"><p>
				<h1 style="color: white" align="center">RULE MINOR WEB TOOL</h1></td>
		</tr>
		<tr>
			<td width="100%">
				<table width="100%" height="100%" class="tableClass">
					<tr height="20%">
						<td colspan="6" class="headerClass">Configuration Parameters</td>
					</tr>
					<tr height="20%">
						<td><label for="alphaR" class="label">&alpha;</label></td>
						<td>
							<form>
								<input type="range" name="alphaR" id="alphaR" value="0.5"
									min="0" max="1" step="0.1"
									oninput="alphaROp.value = alphaR.value">
								<output name="alphaROp" id="alphaROp">0.5</output>
							</form>
						</td>
						<td><label for="noOfThreads" class="label">Number of
								Threads</label></td>
						<td><INPUT type="text" id="noOfThreads" class="textbox"
							name="noOfThreads" title="Number of Threads" /></td>
					</tr>
					<tr height="20%">
						<td><label for="betaR" class="label">&beta;</label></td>
						<td>
							<form>
								<input type="range" name="betaR" id="betaR" value="0.5" min="0"
									max="1" step="0.1" oninput="betaROp.value = betaR.value">
								<output name="betaROp" id="betaROp">0.5</output>
							</form>
						</td>
						<td><label for="maxNoRule" class="label">Max. Rule
								Length</label></td>
						<td><INPUT type="text" id="maxNoRule" class="textbox"
							name="maxNoRule" title="Max Rule Length" /></td>
					</tr>
					<tr height="20%">
						<td><label for="gammaR" class="label">&gamma;</label></td>
						<td>
							<form>
								<input type="range" name="gammaR" id="gammaR" value="0.5"
									min="0" max="1" step="0.1"
									oninput="gammaROp.value = gammaR.value">
								<output name="gammaROp" id="gammaROp">0.5</output>
							</form>
						</td>
						<td><label for="kBase" class="label">Knowledge Base </label>
						</td>
						<td><select name="kBase" id="kBase" width="160px"
							height="30px">
								<option value="dbpedia">DBpedia</option>
								<option value="yago">Yago</option>
								<option value="wikidata">Wikidata</option>
						</select></td>
					</tr>
					<tr height="20%">

						<td><label for="subType" class="label">Subject Type</label></td>
						<td><INPUT type="text" id="subType" class="textbox"
							name="subType" title="Subject Type" /></td>
						<td><label for="objType" class="label">Object Type</label></td>
						<td><INPUT type="text" id="objType" class="textbox"
							name="objType" title="Object Type" /></td>

					</tr>

					<tr height="20%">

						<td><label for="relName" class="label">Relation Name</label>
						</td>
						<td><INPUT type="text" id="relName" class="textbox"
							name="relName" value="" title="Relation Name" /></td>
						<td><label for="genNegRules" class="label">Generate
								Negative Rules</label></td>
						<td>
							<form>
								<input type="checkbox" id="genNegRules"
									title="Generate Negative Rules" name="genNegRules">
							</form>
						</td>

					</tr>

					<tr height="20%">

						<td><label for="edgeLimit" class="label">Edge Limit</label></td>
						<td><INPUT type="text" id="edgeLimit" class="textbox"
							name="edgeLimit" value="" title="Edge Limit" /></td>
						<td><label for="genLimit" class="label">Generation/Sampling
								Limit</label></td>
						<td><INPUT type="text" id="genLimit" class="textbox"
							name="genLimit" title="Generation Limit" /></td>

					</tr>

					<tr height="20%">

						<td><label for="useSmartSampling" class="label">Use
								Smart Sampling</label></td>
						<td>
							<form>
								<input type="checkbox" id="useSmartSampling"
									name="useSmartSampling" onClick="SmartFieldHandle()" />
							</form>
						</td>

					</tr>
					<tr height="20%">

						<td><label for="alphaSmart" id="alphaSmartLabel"
							class="label">&alpha; smart Limit</label></td>
						<td><INPUT type="text" id="alphaSmart" class="textbox"
							name="alphaSmart" value="" title="Alpha Smart" /></td>
						<td><label for="betaSmart" id="betaSmartLabel" class="label">&beta;
								smart Limit</label></td>
						<td><INPUT type="text" id="betaSmart" class="textbox"
							name="betaSmart" title="Beta Smart Limit" /></td>

					</tr>
					<tr>
						<td><label for="gammaSmart" id="gammaSmartLabel"
							class="label">&gamma; smart Limit</label></td>
						<td><INPUT type="text" id="gammaSmart" class="textbox"
							name="gammaSmart" title="Gamma Smart Limit" /></td>


						<td><label for="subWeight" id="subWeightLabel" class="label">Subject
								Weight</label></td>
						<td><INPUT type="text" id="subWeight" class="textbox"
							name="subWeight" value="" title="Subject Weight" /></td>
					</tr>

					<tr>
						<td><label for="objWeight" id="objWeightLabel" class="label">Object
								Weight</label></td>
						<td><INPUT type="text" id="objWeight" class="textbox"
							name="objWeight" title="Object Weight" /></td>


						<td><label for="topK" id="isTopKLabel" class="label">Top-K</label>
						</td>
						<td>
							<form>
								<input type="checkbox" id="topK" title="Top-K" name="topK">
							</form>
						</td>

					</tr>
					<tr height="20%">
						<td><a class="buttonClass" href="#"
							onClick="return ajaxPostRuleMinerItem();">Post Message</a></td>
						<td><img id="loader" name="loader"
							src="${images}/Preloader_3.gif" align="right"></td>

						<td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td width="100%">
				<table width="100%" height="100%" class="tableClass">
					<tr>
						<td>
							<div id="accordion">
								<div class="panelClass">Output Rules</div>
								<div id="foo"></div>
								<div class="panelClass">Generation Examples</div>
								<div id="foo1"></div>
							</div>
						</td>
					</tr>
					<tr height="70%">
					</tr>
					<tr height="10%">
						<td><a class="buttonClass" href="#"
							onClick="return updateRuleStatus();">Update Result</a></td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
	<script type="text/javascript">document.getElementById("loader").style.visibility = "hidden";</script>

</body>
</html>
