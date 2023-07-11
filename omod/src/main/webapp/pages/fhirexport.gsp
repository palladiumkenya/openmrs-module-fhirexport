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
        jq(document).on('click','#sendToServer',function () {

            jq.getJSON('${ui.actionLink("fhirexport", "exportData", "generateAndPostFhirObject")}'
            ).success(function (result) {
                var status = result.success === true ? 'FHIR object generated and exported successfully' : 'There was a problem generating FHIR objects. Please check logs for more information';
                console.log("Server result: " + status);
            });

        });
    });
</script>
<div class="fhirexport-page-content">
    <fieldset>
        <legend>Export FHIR Objects</legend>

        <button>Export to file system</button>
        <button id="sendToServer">Send to FHIR Server</button>

    </fieldset>
</div>