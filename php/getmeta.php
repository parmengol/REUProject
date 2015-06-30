<?php
/**
 * Creates Unsynced rows data as JSON
 */
    include_once 'db_functions.php';
    $db = new DB_Functions();
    $meta = $db->getMetaData();
    $a = array();
    $b = array();
    if ($meta != false){
        while ($row = mysql_fetch_array($meta)) { 
            $b["mapx"] = $row["mapx"];
            $b["mapy"] = $row["mapy"];
            array_push($a,$b);
        }
        echo json_encode($a);
    }
?>
