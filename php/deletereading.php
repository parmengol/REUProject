<?php
include_once './db_functions.php';
$db = new DB_Functions();
$x = $_POST["x"];
$y = $_POST["y"];

$res = $db->delete($x,$y);
echo "hi";
?>

