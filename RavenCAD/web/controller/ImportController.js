

$(document).ready(function() { //don't run javascript until page is loaded
// FIELDS
    loaded = false;
    data = null;
// FUNCTIONS
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
        var tableBody = "<table id='dataTable' class='table table-bordered table-hover'><thead><tr><th>uuid</th><th>Name</td><th>Sequence</th><th>LO</th><th>RO</th><th>Type</th><th>Composition</th><th>Resistance</th><th>Level</th></tr></thead><tbody>";
        $.each(data, function() {
            tableBody = tableBody + "<tr>";
            $.each(this, function(key, value) {
                tableBody = tableBody + "<td>" + value + "</td>";
            });
            tableBody = tableBody + "</tr>";
        });
        tableBody = tableBody + "</tbody>";
        $("#tableArea").html(tableBody);
        $("#dataTable").dataTable({
            "sScrollY": "300px",
            "bPaginate": false,
            "bScrollCollapse": true,
        });
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


    //UPLOAD CODE 
    $(function() {
        $('#file_upload').fileUpload({
            namespace: 'file_upload_1',
            dropZone: $('#drop_zone_1')
        });
    });


    //EVENT HANDLERS
    $('#designButton').click(function() {
        if (loaded) {
            window.location = "ravencad.html";
        } else {
            alert("You probably want to upload some data first");
        }
    });

    $('#dataArea').mouseenter(function() {
        if (loaded) {
            $('#editorArea').removeClass("hidden");
        }
    });
    $('#dataArea').mouseleave(function() {
        $('#editorArea').addClass("hidden");
        $('#editorArea').html("");
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
            $.get("RavenServlet", {"command":"logout"}, function() {
                deleteCookie("authenticate");
                deleteCookie("user");
                window.location.replace("index.html");
            });
        });
    } else if (getCookie("authenticate") === "failed") {
        window.location.replace("login.html");
    }



});