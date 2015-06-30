<html>
<head><title>View Users</title>
<style>
body {
  font: normal medium/1.4 sans-serif;
}
table {
  border-collapse: collapse;
  width: 20%;
   margin-left: auto;
    margin-right: auto;
}
tr > td {
  padding: 0.25rem;
  text-align: center;
  border: 1px solid #ccc;
}
tr:nth-child(even) {
  background: #FAE1EE;
}
tr:nth-child(odd) {
  background: #edd3ff;
}
tr#header{
background: #c1e2ff;
}
div.header{
padding: 10px;
background: #e0ffc1;
width:30%;
color: #008000;
margin:5px;
}
div.refresh{
margin-top:10px;
width: 5%;
margin-left: auto;
margin-right: auto;
}
div#norecord{
margin-top:10px;
width: 15%;
margin-left: auto;
margin-right: auto;
}
</style>
<script>
function refreshPage(){
location.reload();
}
</script>
</head>
<body>
<center>
<div class="header">
Android SQLite and MySQL Sync Results
</div>
</center>
<?php
    include_once 'db_functions.php';
    $db = new DB_Functions();
    $readings = $db->getAllReadings();
    if ($readings != false)
        $no_of_readings = mysql_num_rows($readings);
    else
        $no_of_readings = 0;
?>
<?php
    if ($no_of_readings > 0) {
?>
<table>
<tr id="header"><td>_id</td><td>datetime</td><td>mapx</td><td>mapy</td><td>rss</td><td>ap_name</td><td>mac</td><td>map</td></tr>
<?php
    while ($row = mysql_fetch_array($readings)) {
?> 
<tr>
<td><span><?php echo $row["_id"] ?></span></td>
<td><span><?php echo $row["datetime"] ?></span></td>
<td><span><?php echo $row["mapx"] ?></span></td>
<td><span><?php echo $row["mapy"] ?></span></td>
<td><span><?php echo $row["rss"] ?></span></td>
<td><span><?php echo $row["ap_name"] ?></span></td>
<td><span><?php echo $row["mac"] ?></span></td>
<td><span><?php echo $row["map"] ?></span></td>
</tr>
<?php } ?>
</table>
<?php }else{ ?>
<div id="norecord">
No records in MySQL DB
</div>
<?php } ?>
<div class="refresh">
<button onclick="refreshPage()">Refresh</button>
</div>
</body>
</html>
