// Copyright 2019-2020 The MathWorks, Inc.
// This script file is used to disable all web elements of depricated MATLAB build step.


//Disable all previous UI elements.
var testMode = getElementsByXpath("//td[contains(text(),'Test mode')]/../td[3]/select");
disableAllElements(testMode);

var taPDFReportChkBx = getElementsByXpath("//input[@name='taPDFReportChkBx']");
disableAllElements(taPDFReportChkBx);

var tatapChkBx = getElementsByXpath("//input[@name='tatapChkBx']");
disableAllElements(tatapChkBx);

var taJunitChkBx = getElementsByXpath("//input[@name='taJunitChkBx']");
disableAllElements(taJunitChkBx);

var taSTMResultsChkBx = getElementsByXpath("//input[@name='taSTMResultsChkBx']");
disableAllElements(taSTMResultsChkBx);

var taCoberturaChkBx = getElementsByXpath("//input[@name='taCoberturaChkBx']");
disableAllElements(taCoberturaChkBx);

var taModelCoverageChkBx = getElementsByXpath("//input[@name='taModelCoverageChkBx']");
disableAllElements(taModelCoverageChkBx);


//Function to disable all occurrences of given web element.
function disableAllElements(nodeName){
  var a =[];
  a.push(nodeName.iterateNext());
  while(a[a.length-1] !== null){
      a.push(nodeName.iterateNext());
  }

  var i=0;
  while(a[i] !== null){
      a[i].disable();
      i=i+1;
  }
}

// Function to get list of elements by Xpath

function getElementsByXpath(xPath){
  var elm = document.evaluate(xPath, document, null, XPathResult.UNORDERED_NODE_ITERATOR_TYPE, null);
  return elm;
}
