

$(document).ready(function() { //don't run javascript until page is loaded
// FIELDS
    var loaded = false;
    var data = null;
    var tabResized = true;
// FUNCTIONS
    var allTable = $("#allTable").dataTable({
        "sScrollY": "300px",
        "bPaginate": false,
        "bScrollCollapse": true,
    });
    var partTable = $("#partTable").dataTable({
        "sScrollY": "300px",
        "bPaginate": false,
        "bScrollCollapse": true,
    });
    var vectorTable = $("#vectorTable").dataTable({
        "sScrollY": "300px",
        "bPaginate": false,
        "bScrollCollapse": true,
    });
    function allAddRow(rowData) {
        $('#allTable').dataTable().fnAddData([
            rowData["uuid"],
            rowData["Name"],
            rowData["Sequence"],
            rowData["LO"],
            rowData["RO"],
            rowData["Type"],
            rowData["Composition"],
            rowData["Resistance"],
            rowData["Level"]
        ]);
    }
    function partAddRow(rowData) {
        $('#partTable').dataTable().fnAddData([
            rowData["uuid"],
            rowData["Name"],
            rowData["Sequence"],
            rowData["LO"],
            rowData["RO"],
            rowData["Type"],
            rowData["Composition"],
        ]);
    }
    function vectorAddRow(rowData) {
        $('#vectorTable').dataTable().fnAddData([
            rowData["uuid"],
            rowData["Name"],
            rowData["Sequence"],
            rowData["LO"],
            rowData["RO"],
            rowData["Type"],
            rowData["Resistance"],
            rowData["Level"]
        ]);
    }
    $.fn.refreshData = function() {
        $.get("RavenServlet", {"command": "dataStatus"}, function(data) {
            if (data == "loaded") {
                loaded = true;
                $(this).getData();
                $('#uploadArea').removeClass("in");
                $('#uploadArea').addClass("out");
                //TODO add some sort of popup as a guiding hint
            } else {
                loaded = false;
                $('#uploadArea').removeClass("out");
                $('#uploadArea').addClass("in");
                //TODO add some sort of popup as a guiding hint
            }
        });
    };
//draw table
    $.fn.drawTable = function() {
        //TODO draw parts and vectors into separate tabs
        var allTableBody = "<table id='allTable' class='table table-bordered table-hover'><thead><tr><th>uuid</th><th>Name</th><th>Sequence</th><th>LO</th><th>RO</th><th>Type</th><th>Composition</th><th>Resistance</th><th>Level</th></tr></thead><tbody>";
        var partTableBody = "<table id='partTable' class='table table-bordered table-hover'><thead><tr><th>uuid</th><th>Name</th><th>Sequence</th><th>LO</th><th>RO</th><th>Type</th><th>Composition</th></tr></thead><tbody>";
        var vectorTableBody = "<table id='vectorTable' class='table table-bordered table-hover'><thead><tr><th>uuid</th><th>Name</th><th>Sequence</th><th>LO</th><th>RO</th><th>Type</th><th>Resistance</th><th>Level</th></tr></thead><tbody>";
        $.each(data, function() {
//            allTableBody = allTableBody + "<tr><td>"
//                    + this["uuid"] + "</td><td>"
//                    + this["Name"] + "</td><td>"
//                    + this["Sequence"] + "</td><td>"
//                    + this["LO"] + "</td><td>"
//                    + this["RO"] + "</td><td>"
//                    + this["Type"] + "</td><td>"
//                    + this["Composition"] + "</td><td>"
//                    + this["Resistance"] + "</td><td>"
//                    + this["Level"] + "</td></tr>";
            allAddRow({"uuid": this["uuid"],
                "Name": this["Name"],
                "Sequence": this["Sequence"],
                "LO": this["LO"],
                "RO": this["RO"],
                "Type": this["Type"],
                "Composition": this["Composition"],
                "Resistance": this["Resistance"],
                "Level": this["Level"]
            });
            if (this["Type"] === "vector") {
//                vectorTableBody = vectorTableBody + "<tr><td>"
//                        + this["uuid"] + "</td><td>"
//                        + this["Name"] + "</td><td>"
//                        + this["Sequence"] + "</td><td>"
//                        + this["LO"] + "</td><td>"
//                        + this["RO"] + "</td><td>"
//                        + this["Type"] + "</td><td>"
//                        + this["Resistance"] + "</td><td>"
//                        + this["Level"] + "</td></tr>";
                vectorAddRow({"uuid": this["uuid"],
                    "Name": this["Name"],
                    "Sequence": this["Sequence"],
                    "LO": this["LO"],
                    "RO": this["RO"],
                    "Type": this["Type"],
                    "Resistance": this["Resistance"],
                    "Level": this["Level"]
                });
            } else {
//                partTableBody = partTableBody + "<tr><td>"
//                        + this["uuid"] + "</td><td>"
//                        + this["Name"] + "</td><td>"
//                        + this["Sequence"] + "</td><td>"
//                        + this["LO"] + "</td><td>"
//                        + this["RO"] + "</td><td>"
//                        + this["Type"] + "</td><td>"
//                        + this["Composition"] + "</td></tr>"
                partAddRow({"uuid": this["uuid"],
                    "Name": this["Name"],
                    "Sequence": this["Sequence"],
                    "LO": this["LO"],
                    "RO": this["RO"],
                    "Type": this["Type"],
                    "Composition": this["Composition"]
                });
            }
        });
//        allTableBody = allTableBody + "</tbody></table>";
//        vectorTableBody = vectorTableBody + "</tbody></table>";
//        partTableBody = partTableBody + "</tbody></table>";
//        $("#allTableArea").html(allTableBody);
//        $("#partTableArea").html(partTableBody);
//        $("#vectorTableArea").html(vectorTableBody);
        $("tr").on("click", function() {
//        //TODO finish form generation
            var type = "Part";
            if ($(this).children('td:eq(1)').text() == "vector") {
                type = "Vector";
            }
            $('#editorArea').html('<form class="form-horizontal"><legend>Edit ' + type + '</legend><p>' + 'Name: ' + $(this).children('td:eq(0)').text() + '</p><p>Type: ' + $(this).children('td:eq(1)').text() + '</p></form>');
        });
    }


    $.fn.getData = function() {
        $.getJSON("RavenServlet", {"command": "fetch"}, function(json) {
            data = json;
            $(this).drawTable();
        });
    };
    $.get("RavenServlet", {"command": "load"}, function() {
        $(this).refreshData();
    });
    //EVENT HANDLERS
    $('.tablink').click(function() {
        tabResized = false;
    });
    $('#resetButton').click(function() {
        $.get("RavenServlet",{"command":"purge"},function() {
            window.location.replace("import.html");
        });
    });
    $('#designButton').click(function() {
        if (loaded) {
            window.location = "ravencad.html";
        } else {
            alert("You probably want to upload some data first");
        }
    });
    $('#allTableArea').mouseenter(function() {
        if (!tabResized) {
            allTable.fnAdjustColumnSizing();
            tabResized = true;
        }
    });
    $('#partTableArea').mouseenter(function() {
        if (!tabResized) {
            partTable.fnAdjustColumnSizing();
            tabResized = true;
        }
    });
    $('#vectorTableArea').mouseenter(function() {
        if (!tabResized) {
            vectorTable.fnAdjustColumnSizing();
            tabResized = true;
        }
    });
    $('#dataArea').mouseleave(function() {
        $('#editorArea').addClass("hidden");
        $('#editorArea').html("");
    });

    $('#dataArea').mouseenter(function() {
        $('#editorArea').removeClass("hidden");
    });
    function deleteCookie(key) {
        // Delete a cookie by setting the date of expiry to yesterday
        date = new Date();
        date.setDate(date.getDate() - 1);
        document.cookie = escape(key) + '=;expires=' + date;
    }

    if (getCookie("authenticate") !== "authenticated") {
        deleteCookie("authenticate");
        deleteCookie("user");
    }

    if (getCookie("authenticate") === "authenticated") {
        $('#loginArea').html('<p class="pull-right">You are logged in as <strong>' + getCookie("user") + '</strong> <a id="logout">Log Out</a></p>');
        $('#logout').click(function() {
            $.get("RavenServlet", {"command": "logout"}, function() {
                deleteCookie("authenticate");
                deleteCookie("user");
                window.location.replace("index.html");
            });
        });
    } else if (getCookie("authenticate") === "failed") {
        window.location.replace("login.html");
    }



});