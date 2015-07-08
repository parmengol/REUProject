<?php
include_once './db_functions.php';
$db = new DB_Functions();
$x = $_POST["x"];
$y = $_POST["y"];

$res = $db->delete($x-0.01,$x+0.01,$y-0.01,$y+0.01);
if ($res) {
    echo $x;
    echo ",";
    echo $y;
}
else {
    echo "not hi";
}
?>

