<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>CAD4TB Result Uploader</title>
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script
	src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<script>
	$(document).ready(function() {
		function inputFile() {
			$(".input-file").before(
				function() {
					if ( ! $(this).prev().hasClass('input-ghost') ) {
						var element = $("<input type='file' class='input-ghost' style='visibility:hidden; height:0'>");
						element.attr("name",$(this).attr("name"));
						element.change(function(){
							element.next(element).find('input').val((element.val()).split('\\').pop());
						});
						$(this).find("button.btn-choose").click(function(){
							element.click();
						});
						$(this).find("button.btn-reset").click(function(){
							element.val(null);
							$(this).parents(".input-file").find('input').val('');
						});
						$(this).find('input').css("cursor","pointer");
						$(this).find('input').mousedown(function() {
							$(this).parents('.input-file').prev().click();
							return false;
						});
						return element;
					}
				}
			);
		}
		
		inputFile();
		
		$("#uploadBtn").on("click", function() {
			var url = "cad4tb";
			var form = $("#sampleUploadFrm")[0];
			var data = new FormData(form);
			$.ajax({
				type : "POST",
				encType : "multipart/form-data",
				url : url,
				cache : false,
				processData : false,
				contentType : false,
				data : data,
				success : function(msg) {
					document.getElementById("logTextarea").value = msg;
				},
				error : function(msg) {
					alert("Couldn't upload file");
				}
			});
		});
	});
</script>
</head>

<body>
	<div class="container">
		<div class="col-md-8 col-md-offset-2">
			<h3>Aao-TB-Mitao - CAD4TB Results Upload Service</h3>
			<form id="sampleUploadFrm" method="POST" action="#"
				enctype="multipart/form-data">
				<!-- COMPONENT START -->
				<div class="form-group">
					<div class="input-group input-file" name="file">
						<span class="input-group-btn">
							<button class="btn btn-default btn-choose" type="button">Choose</button>
						</span> <input type="text" class="form-control"
							placeholder='Choose a file...' /> <span class="input-group-btn">
							<button class="btn btn-warning btn-reset" type="button">Clear</button>
						</span>
					</div>
				</div>
				<!-- COMPONENT END -->
				<div class="form-group">
					<button type="button" class="btn btn-primary pull-right"
						id="uploadBtn">Submit</button>
					<button type="reset" class="btn btn-danger">Reset</button>
				</div>
				<div class="form-group shadow-textarea">
					<label for="logTextarea">Process log</label>
					<textarea class="form-control z-depth-1" id="logTextarea" rows="15"
						placeholder="Upload the file. Some text will eventually arrive here..."></textarea>
				</div>
			</form>
		</div>
	</div>
</body>
</html>
