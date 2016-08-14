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
var count1=1000;
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
		
/* 		$( "#1009" ).click(function() {
			$( "#dialogBox" ).dialog( "open" );
		}); */
	});

	$( function() {
		$("#accordion").accordion({
		    collapsible: true,
		    autoHeight: false,
		    navigation: true,
		    heightStyle: "content"
		});
	  } );

  function openModalWindowForRule(currId,value)
  {
	  alert("current Count --"+currId);
	  alert("current Value --"+value);
	  alert("curr Ex"+examples[currId.substring(currId.length-1,currId.length)]);
	  alert("currCount--"+currId);
	  $('#exmplDialog').val(examples[currId.substring(currId.length-1,currId.length)]);
	  $( "#dialogBox" ).dialog( "open" );
  }
	function updateDefaultConfig()
	{
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
			//document.getElementById('loader').style.visibility = "hidden";
		
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
		$('#RuleMinerRules_response').val(" ");
		document.getElementById('loader').style.visibility = "visible";
		disableEntireForm();
		var url = '/rule_miner/webapi/RuleMiner/UpdateResult';
		//var dataString = document.getElementById('RuleMinerItem_request').value;
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
				"UpdateValidVal": ruleValidVal
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
	
	
	function ajaxPostRuleMinerItem() {
		$('#RuleMinerRules_response').val(" ");
		//$('#RuleMinerItem_response').val(" ");
		document.getElementById('loader').style.visibility = "visible";
		disableEntireForm();
		var url = '/rule_miner/webapi/RuleMiner';
		//var dataString = document.getElementById('RuleMinerItem_request').value;
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
					var resultPostJSON = opData;
					var resultPost = $.parseJSON(resultPostJSON);
					//alert("resultPost--"+resultPost);
/* 					$.each(resultPost, function (key, value) {
							alert("key --"+key+"  value--"+value);
						  //$('div#foo').append($('<div></div>').html(key + ' (' + value.length + ' results)'));
						  var list = $('<ul style="list-style-type:none"></ul>');
						  if(key == "Rules")
						  {
							  $('div#foo').append(list);
	
							  $.each(value, function (index, titleObj) {
							    list.append('<li>'
							    + '<a href= "#" onClick="return openModalWindowForRule(this.id);" id= "diag'+count1+ '" >'+ titleObj.title + '</a>'
							    + '<input type="radio" name="sex" value="yes">Valid</input>' 
							    + '<input type="radio" name="sex" value="no">Invalid</input>'
							    + '</li>');
							    
							    count1++;
	
							  });
						  }
						}); */
						
					$.each(resultPost, function (key, value) {
						//alert("key --"+key+"  value--"+value);
					  //var list = $('<ul style="list-style-type:none"></ul>');
					  if(key == "rows")
					  {
						  $.each(value, function (innerKey, innerVal) {
						  //	alert("innerKey --"+innerKey+ "  innerVal -- "+innerVal);
						  	$.each(innerVal, function (innerKey1, innerVal1) {
						  		
						  		//alert("innerKey --"+innerKey1+ "  Concat -- "+innerKey1.substring(0, 6)+ " values "+innerVal1);
						  		
								if(((innerKey1.substring(0, 11)).localeCompare("CovExamples")) == 0)
								{
									//alert("innerKey --"+innerKey1+ "  Concat -- "+innerKey1.substring(0,11)+ " values "+innerVal1);
									$('div#foo1').append('<tr><td width="100%">'+ innerVal1 + '</td></tr>');
									examples[idx]=innerVal1;
								}
								if(((innerKey1.substring(0, 6)).localeCompare("RuleID")) == 0)
								{
									rules[idx] = innerVal1;
									//alert("innerKey --"+innerKey1+ "  Concat -- "+innerKey1.substring(0, 6)+ " values "+innerVal1);
							  		$('div#foo').append('<tr><td width="90%"><a href= "#" onClick="return openModalWindowForRule(this.id,idx);" id= "diag'+idx+ '" >'+ innerVal1 + '</a></td>'
									    + '<td width="10%" ><div class="post-date"><input type="radio" id="radioVal'+idx+'" name="radioVal'+idx+'"  value="yes">Valid  </input>' 
									    + '<input type="radio" id="radioVal'+idx+'" name="radioVal'+idx+'" align="right" value="no">Invalid</input></div></td></tr>');
								}
						  	count1++;
						  	
						  	});
						  	alert(idx++);
						  });
						  
						  
						  /* $.each(value, function (index, titleObj) {
						  alert("index --"+index+ "  Title -- "+titleObj);
							  $('div#foo').append('<tr><td width="50%"><a href= "#" onClick="return openModalWindowForRule(this.id);" id= "diag'+count1+ '" >'+ titleObj.title + '</a></td>'
								    + '<td width="50%"><input type="radio" name="sex" value="yes">Valid  </input>' 
								    + '<input type="radio" name="sex" value="no">Invalid</input></td></tr>');
						  	count1++;
						  }); */
					  }
					});

				},
				complete : function(res){
						document.getElementById('loader').style.visibility = "hidden";
						enableEntireForm();
				}
			});
			//document.getElementById('loader').style.visibility = "hidden";
	}
	</script>
	
<script type="text/javascript">


</script>

</head>

<body onload="updateDefaultConfig()">
<div id="dialogBox"  title="My Dialog Title">
<table width="100%" height="100%">
<tr height="100%">
<td width="100%">
<textarea style="margin: 0px; width: 1398px;"  id="exmplDialog"></textarea>
</td> 
</tr></table>
</div>

<table width="100%" height="100%">
	<tr>
		<td colspan="6" class="headerClass"><p>
			<h1 style="color: white" align="center">RULE MINOR WEB TOOL</h1>
		</td>
	</tr>
	<tr>
		<td width="100%">
			<table width="100%" height="100%" class="tableClass">
			<tr height="20%">
				<td colspan="6" class="headerClass" >Configuration Parameters</td>
			</tr>
			<tr height="20%">
				<td>
					<label for="alphaR" class="label">&alpha;</label> 
				</td>
				<td>
						<form>
						  <input type="range" name="alphaR" id="alphaR" value="0.5" min="0" max="1" step ="0.1" oninput="alphaROp.value = alphaR.value"> 
						  <output name="alphaROp" id="alphaROp">0.5</output> 
						</form>
				</td>	
				<td>
					<label for="noOfThreads" class="label">Number of Threads</label> 
				</td>
				<td>
					<INPUT type="text" id="noOfThreads" class="textbox" name="noOfThreads" title="Number of Threads" />
				</td>	
			</tr>
			<tr height="20%">
				<td>
					<label for="betaR" class="label">&beta;</label> 
				</td>
				<td>
					<form>
						  <input type="range" name="betaR" id="betaR" value="0.5" min="0" max="1" step ="0.1" oninput="betaROp.value = betaR.value"> 
						  <output name="betaROp" id="betaROp">0.5</output> 
					</form>
				</td>
				<td>
					<label for="maxNoRule" class="label">Max. Rule Length</label> 
				</td>
				<td>
					<INPUT type="text" id="maxNoRule" class="textbox" name="maxNoRule" title="Max Rule Length" />
				</td>					
			</tr>
			<tr height="20%">
				<td>
					<label for="gammaR" class="label">&gamma;</label> 
				</td>
				<td>
					<form>
						  <input type="range" name="gammaR" id="gammaR" value="0.5" min="0" max="1" step ="0.1" oninput="gammaROp.value = gammaR.value"> 
						  <output name="gammaROp" id="gammaROp">0.5</output> 
					</form>
				</td>	
				<td>
					<label for="kBase" class="label">Knowledge Base </label> 
				</td>
				<td>
					<select name="kBase" id="kBase" width="160px" height="30px">
					  <option value="dbpedia">DBpedia</option>
					  <option value="yago">Yago</option>
					  <option value="wikidata">Wikidata</option>
					</select>
				</td>
			</tr>
			<tr height="20%">
					
				<td>
					<label for="subType" class="label">Subject Type</label> 
				</td>
				<td>
					<INPUT type="text" id="subType" class="textbox" name="subType"  title="Subject Type" />
				</td>
				<td>
					<label for="objType" class="label">Object Type</label> 
				</td>
				<td>
					<INPUT type="text" id="objType" class="textbox" name="objType" title="Object Type" />
				</td>

			</tr>
			
			<tr height="20%">
					
				<td>
					<label for="relName" class="label">Relation Name</label> 
				</td>
				<td>
					<INPUT type="text" id="relName" class="textbox" name="relName" value="" title="Relation Name" />
				</td>
				<td>
					<label for="genNegRules" class="label">Generate Negative Rules</label> 
				</td>
				<td>
					<form>
								<input type="checkbox" id="genNegRules" title="Generate Negative Rules" name="genNegRules">
							</form>
				</td>

			</tr>
			
			<tr height="20%">
					
				<td>
					<label for="edgeLimit" class="label">Edge Limit</label> 
				</td>
				<td>
					<INPUT type="text" id="edgeLimit" class="textbox" name="edgeLimit" value="" title="Edge Limit" />
				</td>
				<td>
					<label for="genLimit" class="label">Generation Limit</label> 
				</td>
				<td>
					<INPUT type="text" id="genLimit" class="textbox" name="genLimit" title="Generation Limit" />
				</td>

			</tr>
			
			<tr height="20%">
					
				<td>
					<label for="useSmartSampling" class="label">Use Smart Sampling</label> 
				</td>
				<td>
					<form>
								<input type="checkbox" id="useSmartSampling"  name="useSmartSampling" onClick="SmartFieldHandle()"/>
							</form>
				</td>

			</tr>
			<tr height="20%">
					
				<td>
					<label for="alphaSmart" id = "alphaSmartLabel" class="label">&alpha; smart Limit</label> 
				</td>
				<td>
					<INPUT type="text" id="alphaSmart" class="textbox" name="alphaSmart" value="" title="Alpha Smart" />
				</td>
				<td>
					<label for="betaSmart" id = "betaSmartLabel" class="label">&beta; smart Limit</label> 
				</td>
				<td>
					<INPUT type="text" id="betaSmart" class="textbox" name="betaSmart" title="Beta Smart Limit" />
				</td>
				
			</tr>
			<tr><td>
					<label for="gammaSmart" id ="gammaSmartLabel" class="label">&gamma; smart Limit</label> 
				</td>
				<td>
					<INPUT type="text" id="gammaSmart" class="textbox" name="gammaSmart" title="Gamma Smart Limit" />
				</td>
				
								
				<td>
					<label for="subWeight" id ="subWeightLabel" class="label">Subject Weight</label> 
				</td>
				<td>
					<INPUT type="text" id="subWeight" class="textbox" name="subWeight" value="" title="Subject Weight" />
				</td>
			</tr>
			
			<tr><td>
					<label for="objWeight" id="objWeightLabel" class="label">Object Weight</label> 
				</td>
				<td>
					<INPUT type="text" id="objWeight" class="textbox" name="objWeight" title="Object Weight" />
				</td>
				
								
				<td>
					<label for="topK" id="isTopKLabel" class="label">Top-K</label> 
				</td>
				<td>
					<form>
								<input type="checkbox" id="topK" title="Top-K" name="topK">
							</form>
				</td>
				
			</tr>
			<tr height="20%">
				<td>
					<a class="buttonClass" href="#" onClick="return ajaxPostRuleMinerItem();">Post Message</a>
				</td>	
				<td>
					<img id="loader" name ="loader" src="${images}/Preloader_3.gif" align="right">
				</td>	

				<td>
			</tr>
			</table>
		</td>
	</tr>
	<tr>
		<td width="100%">
			<table width="100%" height="100%" class="tableClass" >
			<tr>
			<td>
				<div id="accordion">
				  <div class="panelClass" >Output Rules</div>
				  <div id="foo">
				  </div>
				  <div class="panelClass" >Generation Examples</div>
				  <div id="foo1">
				  </div>
				</div>
			</td></tr>
			<tr height="70%" >
			</tr>
			<tr height="10%" >
				<td>
					<a class="buttonClass" href="#" onClick="return updateRuleStatus();">Update Result</a>
				</td>
			</tr>
			</table>
		</td>
	</tr>
</table>
<script type="text/javascript">document.getElementById("loader").style.visibility = "hidden";</script>

</body>
</html>
