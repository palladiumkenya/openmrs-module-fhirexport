<% ui.decorateWith("appui", "standardEmrPage") %>
<style>
.fhirexport-page-content {
    padding: 5px;
    background-color: white;
}

</style>
<script type="text/javascript">
    jq = jQuery;
    jq(function () {
        jq('#showLoader').hide();
        jq(document).on('click','#export',function () {
            let patientIds = jq("#patientIds").val();
            let exportToFhirServer = jq("#exportToFhirServer").is(':checked');
            let fhirServerUrl = jq("#fhirServerUrl").val();
            let exportToFileSystem = jq("#exportToFileSystem").is(':checked');
            let fileSystemPath = jq("#fileSystemPath").val();

            jq('#showLoader').show();
            jq('#export').prop('disabled', true);
            jq('.status').html('Please wait...');
            let params = {
                patientIds: patientIds,
                exportToFhirServer: exportToFhirServer,
                fhirServerUrl: fhirServerUrl,
                exportToFileSystem: exportToFileSystem,
                fileSystemPath: fileSystemPath
            };
            jq.ajax({
                type: "POST",
                url: '${ui.actionLink("fhirexport", "exportData", "generateAndPostFhirObject")}',
                data: params,
                dataType: "json",
                success: function(result) {
                    jq('#showLoader').hide();
                    jq('#export').prop('disabled', false);

                    let status = result.success === 'true' ? 'FHIR object generated and exported successfully' : 'There was a problem generating FHIR objects. Please check logs for more information';

                    jq('.status').html(status);
                    console.log("Server result: " + result.payload);
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    jq('#showLoader').hide();
                    jq('#export').prop('disabled', false);

                    jq('.status').html('An error occurred: ' + errorThrown);
                }
            });


        });
    });
</script>
<div class="fhirexport-page-content">
    <fieldset>
        <legend>Export FHIR Objects</legend>

        <div>
            <label for="exportToFhirServer">Export to FHIR Server: </label> <input type="checkbox" id="exportToFhirServer" checked />
        </div>
        <div>
            <label for="fhirServerUrl">FHIR Server Url: </label><input type="text" id="fhirServerUrl" value="http://localhost:8081/fhir" style="width: 300px" />
        </div>
        <div>
            <label for="exportToFileSystem">Export to File System: </label> <input type="checkbox" id="exportToFileSystem"/>
        </div>
        <div>
            <label for="fileSystemPath">File System path: </label> <input type="text" id="fileSystemPath" style="width: 300px" value="/tmp/" />
        </div>
        <div>
            <label for="patientIds">Comma Separated Patient Ids to export (Optional)</label><br/>
            <textarea name="patientId" id="patientIds" cols="50" rows="5"></textarea>
        </div>
        <div class="status">

        </div>
        <div>
            <button id="export">Export</button>
        </div>
        <div id="showLoader" style="text-align: center;">
            <img src="${ui.resourceLink("kenyaui", "images/loader_small.gif")}"/>
        </div>
    </fieldset>
</div>
