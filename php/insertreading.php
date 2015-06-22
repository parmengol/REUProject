<?php
include_once './db_functions.php';
//Create Object for DB_Functions clas
$db = new DB_Functions(); 
//Get JSON posted by Android Application
$json = $_POST["readingsJSON"];
//Remove Slashes
if (get_magic_quotes_gpc()){
$json = stripslashes($json);
}
//Decode JSON into an Array
$data = json_decode($json);
//Util arrays to create response JSON
$a=array();
$b=array();
//Loop through an Array and insert data read from JSON into MySQL DB
for($i=0; $i<count($data) ; $i++)
{
//Store Reading into MySQL DB
$res = $db->storeReading($data[$i]->mValues->datetime,$data[$i]->mValues->mapx,$data[$i]->mValues->mapy,$data[$i]->mValues->rss,$data[$i]->mValues->ap_name,$data[$i]->mValues->mac,$data[$i]->mValues->map);
//$res = $db->storeReading(0, 0.1, 0.1, 0, 'a', 0, 0);
    //Based on inserttion, create JSON response
    if($res){
        $b["id"] = $data[$i]->mValues->id;

        $b["datetime"] = $data[$i]->mValues->datetime;
        $b["mapx"] = $data[$i]->mValues->mapx;
        $b["mapy"] = $data[$i]->mValues->mapy;
        $b["rss"] = $data[$i]->mValues->rss;
        $b["apname"] = $data[$i]->mValues->ap_name;
        $b["mac"] = $data[$i]->mValues->mac;
        $b["map"] = $data[$i]->mValues->map;
        $b["status"] = 1;
        array_push($a,$b);
    }else{
        $b["id"] = $data[$i]->mValues->id;
        $b["datetime"] = $data[$i]->mValues->datetime;
        $b["mapx"] = $data[$i]->mValues->mapx;
        $b["mapy"] = $data[$i]->mValues->mapy;
        $b["rss"] = $data[$i]->mValues->rss;
        $b["apname"] = $data[$i]->mValues->ap_name;
        $b["mac"] = $data[$i]->mValues->mac;
        $b["map"] = $data[$i]->mValues->map;
        
        $b["status"] = 0;
        array_push($a,$b);
    }
  
}
//Post JSON response back to Android Application
echo json_encode($a);
?>
