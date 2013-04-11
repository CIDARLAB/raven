//var validNavigation = false;
// 
//function endSession() {
//  // Browser or broswer tab is closed
//  // Do sth here ...
//  alert("bye");
//}
// 
//function wireUpEvents() {
//  /*
//  * For a list of events that triggers onbeforeunload on IE
//  * check http://msdn.microsoft.com/en-us/library/ms536907(VS.85).aspx
//  */
//  $(window).onbeforeunload = function() {
//          alert("session ending");
//         endSession();
//  }
// 
//  // Attach the event keypress to exclude the F5 refresh
//  $(document).bind('keypress', function(e) {
//    if (e.keyCode == 116){
//      validNavigation = true;
//    }
//  });
// 
//  // Attach the event click for all links in the page
//  $("a").bind("click", function() {
//    validNavigation = true;
//  });
// 
//  // Attach the event submit for all forms in the page
//  $("form").bind("submit", function() {
//    validNavigation = true;
//  });
// 
//  // Attach the event click for all inputs in the page
//  $("input[type=submit]").bind("click", function() {
//    validNavigation = true;
//  });
//   
//}
// 
//// Wire up the events as soon as the DOM tree is ready
//$(document).ready(function() {
//    alert("hi");
//    wireUpEvents();  
//});
$(document).ready(function() {
//    $(window).on("beforeunload", function() {
//        alert("bye");
//    });
});
