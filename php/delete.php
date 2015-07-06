<?php
include_once './db_functions.php';
//Create Object for DB_Functions class
$db = new DB_Functions(); 
//Get JSON posted by Android Application

$json = $_POST["deleteJSON"];

//Remove Slashes
if (get_magic_quotes_gpc()){
$json = stripslashes($json);
}
//Decode JSON into an Array
$data = json_decode($json);


$res = $db->deleteX($data[0]->mValues->mapx - .0001, $data[0]->mValues->mapx +  .0001, $data[0]->mValues->mapy - .0001, $data[0]->mValues->mapy + .0001);


?>
