$(document).ready(function() { //don't run javascript until page is loaded
    var designCount = 0;
    var loaded = false;
    var data = null;
    var method = "BioBrick";
    var uuidCompositionHash = {}; //really just a json object...key: uuid, value: string composition
//EVENT HANDLERS
    $('#sidebar').click(function() {
        $('#designTabHeader a:first').tab('show');
    });
    $('#designTabHeader a:first').click(function(){
        refreshData();
    });
    
    $('#methodSelection').change(function() {
        method = $("#methodSelection :selected").text();
        updateSummary();
    });
    //target part button event handlers
    $('#targetSelectAllButton').click(function() {
        $("#availableTargetPartList option").each(function() {
            $("#availableLibraryPartList #" + $(this).attr("id")).remove();
            $("#libraryPartList #" + $(this).attr("id")).remove();

        });
        $('#targetPartList').append($('#availableTargetPartList option'));
        drawIntermediates();
    });
    $('#targetDeselectAllButton').click(function() {
        $("#targetPartList option").each(function() {
            $("#availableLibraryPartList").append('<option id="' + $(this).attr("id") + '">' + $(this).text() + '</option>');
            $("#availableLibraryPartList #" + $(this).attr("id")).addClass("composite");
        });
        $('#availableTargetPartList').append($('#targetPartList option'));
        drawIntermediates();
    });
    $('#targetSelectButton').click(function() {
        $('#availableTargetPartList :selected').each(function() {
            $('#availableLibraryPartList #' + $(this).attr("id")).remove();
            $('#libraryPartList #' + $(this).attr("id")).remove();
        });
        $('#targetPartList').append($('#availableTargetPartList :selected'));
        drawIntermediates();
    });
    $('#targetDeselectButton').click(function() {
        $('#targetPartList :selected').each(function() {
            $('#availableLibraryPartList').append('<option id="' + $(this).attr("id") + '">' + $(this).text() + '</option>');
            $("#availableLibraryPartList #" + $(this).attr("id")).addClass("composite");
        });
        $('#availableTargetPartList').append($('#targetPartList :selected'));
        drawIntermediates();
    });
    //library part button event handlers
    $('#libraryPartSelectAllButton').click(function() {
        $('#availableLibraryPartList option').each(function() {
            $('#availableTargetPartList #' + $(this).attr("id")).remove();
            $('#targetPartList #' + $(this).attr("id")).remove();
        });
        $('#libraryPartList').append($('#availableLibraryPartList option'));
        drawIntermediates();
    });
    $('#libraryPartDeselectAllButton').click(function() {
        $('#libraryPartList option.composite').each(function() {
            $('#availableTargetPartList').append('<option id="' + $(this).attr("id") + '">' + $(this).text() + '</option>');
        });
        $('#availableLibraryPartList').append($('#libraryPartList option'));
        drawIntermediates();
    });
    $('#libraryPartSelectButton').click(function() {
        $('#availableLibraryPartList :selected').each(function() {
            $('#availableTargetPartList #' + $(this).attr("id")).remove();
            $('#targetPartList #' + $(this).attr("id")).remove();
        });
        $('#libraryPartList').append($('#availableLibraryPartList :selected'));
        drawIntermediates();
    });
    $('#libraryPartDeselectButton').click(function() {
        $('#libraryPartList :selected.composite').each(function() {
            $('#availableTargetPartList').append('<option id="' + $(this).attr("id") + '">' + $(this).text() + '</option>');
        });
        $('#availableLibraryPartList').append($('#libraryPartList :selected'));
        drawIntermediates();
    });

    $('#libraryVectorSelectAllButton').click(function() {
        $('#libraryVectorList').append($('#availableLibraryVectorList option'));
    });
    $('#libraryVectorDeselectAllButton').click(function() {
        $('#availableLibraryVectorList').append($('#libraryVectorList option'));
    });
    $('#libraryVectorSelectButton').click(function() {
        $('#libraryVectorList').append($('#availableLibraryVectorList :selected'));
    });
    $('#libraryVectorDeselectButton').click(function() {
        $('#availableLibraryVectorList').append($('#libraryVectorList :selected'));
    });
    $('#resetIntermediatesButton').click(function() {
        $(':checked').attr("checked", false);
    });


    $('.btn').click(function() {
        updateSummary();
    });
    $('#runButton').click(function() {
        var targets = ""; //goal parts
        $('#targetPartList option').each(function() {
            targets = targets + $(this).attr("id") + ",";
        });
        if (targets.length > 1) {
            designCount = designCount + 1;
            $('#designTabHeader').append('<li><a href="#designTab' + designCount + '" data-toggle="tab">Design ' + designCount +
                    '</a></li>');
            $('#designTabContent').append('<div class="tab-pane" id="designTab' + designCount + '">');
            $('#designTabHeader a:last').tab('show'); //TODO figure out how to show new tab

            //generate main skeleton
            $('#designTab' + designCount).append('<div class="row-fluid"><div class="span10"><div class="tabbable" id="resultTabs' + designCount +
                    '"></div></div></div>' +
                    '<div class="row-fluid"><div class="span7"><div class="well" id="stat' + designCount +
                    '"><h4>Assembly Statistics</h4></div></div><div class="span3"><div class="well" id="download' + designCount + '"></div></div></div>');
            //add menu
            $('#resultTabs' + designCount).append('<ul id="resultTabsHeader' + designCount + '" class="nav nav-tabs">' +
                    '<li class="active"><a href="#imageTab' + designCount + '" data-toggle="tab" >Image</a></li>' +
                    '<li><a href="#instructionTab' + designCount + '" data-toggle="tab">Instructions</a></li></ul>');
            //add tab content
            $('#resultTabs' + designCount).append('<div class="tab-content" id="resultTabsContent' + designCount + '"><div class="tab-pane active" id="imageTab'
                    + designCount + '"><div class="well" id="resultImage' + designCount +
                    '">Please wait while RavenCAD generates your image<div class="progress progress-striped active"><div class="bar" style="width:100%"></div></div></div></div><div class="tab-pane" id="instructionTab' + designCount +
                    '"><div class="well" id="instructionArea' + designCount +
                    '">Please wait while RavenCAD generates instructions for your assembly<div class="progress progress-striped active"><div class="bar" style="width:100%"></div></div></div></div></div>');
            //add download buttons and bind events to them
            $('#download' + designCount).append('<h4>Download Options</h4><div>' +
                    '<p><a id="downloadImage' + designCount + '">Download Graph Image</a></p>' +
                    '<p><a id="downloadInstructions' + designCount + '">Download Instructions</a></p>' +
                    '<p><a id="downloadParts' + designCount + '">Download Parts/Vectors List</a></p>' +
                    '<p><small>Please use right-click, then save as to download the files</small></div>');
            var partLibrary = ""; //parts to use in library
            var vectorLibrary = ""; //vectors to use in library
            var rec = ""; //recommended intermediates
            var req = ""; //required intermediates
            var forbid = ""; //forbidden intermediates
            var eug = ""; //eugene file for rec/required forbidden
            var config = ""; //csv configuration file?
            $('#libraryPartList option').each(function() {
                partLibrary = partLibrary + $(this).attr("id") + ",";
            });
            $('#libraryVectorList option').each(function() {
                vectorLibrary = vectorLibrary + $(this).attr("id") + ",";
            });
            $('.required:checked').each(function() {
                req = req + $(this).val() + ";";
            });
            $('.recommended:checked').each(function() {
                rec = rec + $(this).val() + ";";
            });
            $('.forbidden:checked').each(function() {
                forbid = forbid + $(this).val() + ";";
            });
            forbid = forbid.substring(0, forbid.length - 1);
            req = req.substring(0, req.length - 1);
            rec = rec.substring(0, rec.length - 1);
            targets = targets.substring(0, targets.length - 1);
            partLibrary = partLibrary.substring(0, partLibrary.length - 1);

            var requestInput = {"command": "run", "designCount": "" + designCount, "targets": "" + targets, "method": "" + method, "partLibrary": "" + partLibrary, "vectorLibrary": "" + vectorLibrary, "recommended": "" + rec, "required": "" + req, "forbidden": "" + forbid};
            $.get("RavenServlet", requestInput, function(data) {
                if (data["status"] === "good") {
                    $("#resultImage" + designCount).html("<img src='" + data["result"] + "'/>");
                    $('#resultImage' + designCount + ' img').wrap('<span style="width:640;height:360px;display:inline-block"></span>').css('display', 'block').parent().zoom();
                    $('#instructionArea' + designCount).html('<div class="alert alert-danger">' + data["instructions"] + '</div>');
                    $('#stat' + designCount).html('<h4>Assembly Statistics</h4><table class="table">' +
                            '<tr><td><strong>Number of Goal Parts</strong></td><td>' + data["statistics"]["goalParts"] + '</td></tr>' +
                            '<tr><td><strong>Number of Assembly Steps</strong></td><td>' + data["statistics"]["steps"] + '</td></tr>' +
                            '<tr><td><strong>Number of Assembly Stages</strong></td><td>' + data["statistics"]["stages"] + '</td></tr>' +
                            '<tr><td><strong>Number of Reactions</strong></td><td>' + data["statistics"]["reactions"] + '</td></tr>' +
                            '<tr><td><strong>Number of Recommended Parts</strong></td><td>' + data["statistics"]["recommended"] + '</td></tr>' +
                            '<tr><td><strong>Assembly Efficiency</strong></td><td>' + data["statistics"]["efficiency"] + '</td></tr>' +
                            '<tr><td><strong>Modularity of Assembled Parts</strong></td><td>' + data["statistics"]["modularity"] + '</td></tr>' +
                            '<tr><td><strong>Algorithm Runtime</strong></td><td>' + data["statistics"]["time"] + '</td></tr></table>');
                    $('#downloadImage' + designCount).attr("href", data["result"]);
                    $('#downloadInstructions' + designCount).attr("href","data/instructions" + designCount + ".txt");
                    $('#downloadParts' + designCount).attr("href","data/partsList" + designCount + ".csv");
                } else {
                    $("#designTab" + designCount).html('<div class="alert alert-danger">' +
                            '<strong>Oops, an error occured while generating your assembly plan</strong>' +
                            '<p>Please send the following to <a href="mailto:jenhantao@gmail.com">jenhantao@gmail.com</a></p>' +
                            '<ul><li>The error stacktrace shown below</li><li>Your input file. <small>Feel free to remove all of the sequences</small></li>' +
                            '<li>A brief summary of what you were trying to do</li></ul>' +
                            '<p>We appreciate your feedback. We\'re working to make your experience better</p><hr/>'
                            + data["result"] + '</div>');
                }
            });
        } else {
            alert("Please select some target parts");
        }
    });
    //FUNCTIONS
    var refreshData = function() {
        $.get("RavenServlet", {"command": "dataStatus"}, function(data) {
            if (data == "loaded") {
                loaded = true;
                getData();
            } else {
                loaded = false;
                //TODO add some sort of popup as a guiding hint
            }
        });
    };
//draw target part options list
    var drawPartVectorLists = function() {
        var targetListBody = "<select id=\"availableTargetPartList\" multiple=\"multiple\" class=\"fixedHeight\">";
        var libraryPartListBody = "<select id=\"libraryPartList\" multiple=\"multiple\" class=\"fixedHeight\">";
        var libraryVectorListBody = "<select id=\"libraryVectorList\" multiple=\"multiple\" class=\"fixedHeight\">";
        $.each(data, function() {
            if (this["Type"] == "composite") {
                targetListBody = targetListBody + "<option class=\"composite ui-state-default\" id=\"" + this["uuid"] + "\">" + this["Name"] + "</option>";
            }
            if (this["Type"] == "vector") {
                libraryVectorListBody = libraryVectorListBody + "<option class=\"vector ui-state-default\" id=\"" + this["uuid"] + "\">" + this["Name"] + "</option>";
            } else {
                libraryPartListBody = libraryPartListBody + "<option class=\"basic ui-state-default\" id=\"" + this["uuid"] + "\">" + this["Name"] + "</option>";
            }

        });
        targetListBody = targetListBody + "</select>";
        libraryVectorListBody = libraryVectorListBody + "</select>";
        libraryPartListBody = libraryPartListBody + "</select>";
        $("#availableTargetPartListArea").html(targetListBody);
        $("#libraryPartListArea").html(libraryPartListBody);
        $("#libraryVectorListArea").html(libraryVectorListBody);
        
        //clear lists
        $('#targetPartList').html("");
        $('#availableLibraryPartList').html("");
        $('#availableLibraryVectorList').html("");
    };
    var getData = function() {
        $.getJSON("RavenServlet", {"command": "fetch"}, function(json) {
            data = json;
            drawPartVectorLists();
            //generate uuidCompositionHash
            $.each(data, function() {
                if (this["Type"].toLowerCase() != "vector") {
                    uuidCompositionHash[this["uuid"]] = this["Composition"];
                }
            });
        });
    };
    $.get("RavenServlet", {"command": "load"}, function() {
        refreshData();
    });
    var generateIntermediates = function(composition) {
        toSplit = composition.substring(1, composition.length - 1);
        var toReturn = [];
        var compositionArray = toSplit.split(",");
        var seenIntermediates = {};
        for (var start = 0; start < compositionArray.length; start++) {
            for (var end = start + 1; end < compositionArray.length + 1; end++) {
                var intermediate = compositionArray.slice(start, end);
                var name = "";
                if (intermediate.length > 1) {
                    for (var i = 0; i < intermediate.length; i++) {
                        name = name + intermediate[i] + ",";
                    }
                    name = "[" + name.substring(0, name.length - 1).trim() + "]";
                    if (name !== composition) {
                        if (seenIntermediates[name] !== "seen") {
                            seenIntermediates[name] = "seen";
                            toReturn.push(name);
                        }
                    }
                }
            }
        }
        return toReturn;
    };
    var drawIntermediates = function() {
        var targets = "";
        var tableBody = "<table id='intermediateTable' class='table table-bordered table-hover'><thead><tr><th>Composition</th><th>Recommended</th><th>Required</th><th>Forbidden</th></tr></thead><tbody>";
        var seen = {};
        $("#targetPartList option").each(function() {
            targets = targets + "\n" + uuidCompositionHash[$(this).attr("id")];
            var intermediates = generateIntermediates(uuidCompositionHash[$(this).attr("id")]);
            $.each(intermediates, function() {
                if (seen[this] != "seen") {
                    tableBody = tableBody + '<tr><td>' + this + '<td><input class="recommended" type="checkbox" value="' + this + '"></td><td><input class="required" type="checkbox" value="' + this + '"></td><td><input class="forbidden" type="checkbox" value="' + this + '"></td></tr>';
                    seen[this] = "seen";
                }
            });
        });
        seen = null;
        tableBody = tableBody + '</tbody>';
        $('#intermediateTableArea').html(tableBody);
        $("#intermediateTable").dataTable({
            "sScrollY": "300px",
            "bPaginate": false,
            "bScrollCollapse": true
        });
        $(':checkbox').change(function() {
            updateSummary();
        });

    };

    var updateSummary = function() {
        var summary = "<p>You're trying to assemble</p>";
        if ($('#targetPartList option').length > 0) {
            summary = summary + "<ul>";
            $('#targetPartList option').each(function() {
                summary = summary + '<li>' + $(this).text() + '</li>';
            });
            summary = summary + "</ul>";
        } else {
            summary = summary + '<div class="alert alert-danger"><strong>Nothing</strong>. Try selecting some target parts</div>';
        }
        summary = summary + '<p>You will be using the <strong>' + method + '</strong> assembly method</p>';
        if ($('.recommended:checked').length > 0) {
            summary = summary + '<p>The following intermediates are recommended:</p>';
            summary = summary + '<ul>';
            $('.recommended:checked').each(function() {
                summary = summary + '<li>' + $(this).val() + '</li>';
            });
            summary = summary + '</ul>';
        } else {
            summary = summary + '<p>No intermediates are recommended</p>';
        }
        if ($('.required:checked').length > 0) {
            summary = summary + '<p>The following intermediates are required:</p>';
            summary = summary + '<ul>';
            $('.required:checked').each(function() {
                summary = summary + '<li>' + $(this).val() + '</li>'
            });
            summary = summary + '</ul>';
        } else {
            summary = summary + '<p>No intermediates are required</p>';
        }
        if ($('.forbidden:checked').length > 0) {
            summary = summary + '<p>The following intermediates are forbidden:</p>';
            summary = summary + '<ul>';
            $('.forbidden:checked').each(function() {
                summary = summary + '<li>' + $(this).val() + '</li>'
            });
            summary = summary + '</ul>';
        } else {
            summary = summary + '<p>No intermediates are forbidden</p>';
        }
        $('#designSummaryArea').html(summary);
    };


    $("#intermediateTable").dataTable({
        "sScrollY": "300px",
        "bPaginate": false,
        "bScrollCollapse": true
    });


});



